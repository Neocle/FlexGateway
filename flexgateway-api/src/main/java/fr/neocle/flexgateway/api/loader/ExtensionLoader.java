package fr.neocle.flexgateway.api.loader;

import fr.neocle.flexgateway.api.config.ExtensionConfig;
import fr.neocle.flexgateway.api.config.ExtensionConfigImpl;
import fr.neocle.flexgateway.api.ExtensionContext;
import fr.neocle.flexgateway.api.FlexGatewayExtension;
import fr.neocle.flexgateway.api.enhancer.BaseEndpointEnhancer;
import fr.neocle.flexgateway.api.handler.ActionHandler;
import fr.neocle.flexgateway.api.provider.DataProvider;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

public class ExtensionLoader {

    private final JavaPlugin plugin;
    private final File extensionsFolder;
    private final Map<String, LoadedExtension> loadedExtensions = new ConcurrentHashMap<>();
    private final Map<String, Map<String, DataProvider>> dataProviders = new ConcurrentHashMap<>();
    private final Map<String, Map<String, ActionHandler>> actionHandlers = new ConcurrentHashMap<>();
    private final Map<String, List<BaseEndpointEnhancer>> baseEndpointEnhancers = new ConcurrentHashMap<>();

    public ExtensionLoader(JavaPlugin plugin) {
        this.plugin = plugin;
        this.extensionsFolder = new File(plugin.getDataFolder(), "extensions");

        if (!extensionsFolder.exists()) {
            extensionsFolder.mkdirs();
            plugin.getLogger().info("Created extensions folder: " + extensionsFolder.getAbsolutePath());
        }
    }

    /**
     * Load all extensions from the extensions folder
     */
    public void loadExtensions() {
        File[] extensionFiles = extensionsFolder.listFiles((dir, name) -> name.endsWith(".jar"));

        if (extensionFiles == null || extensionFiles.length == 0) {
            plugin.getLogger().info("No extensions found in " + extensionsFolder.getAbsolutePath());
            return;
        }

        plugin.getLogger().info("Loading " + extensionFiles.length + " extension(s)...");

        for (File extensionFile : extensionFiles) {
            try {
                loadExtension(extensionFile);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load extension: " + extensionFile.getName());
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("Successfully loaded " + loadedExtensions.size() + " extension(s)");
    }

    /**
     * Load a single extension from a JAR file
     */
    private void loadExtension(File jarFile) throws Exception {
        plugin.getLogger().info("Loading extension: " + jarFile.getName());

        ExtensionManifest manifest = readManifest(jarFile);
        if (manifest == null) {
            plugin.getLogger().warning("No valid manifest found in " + jarFile.getName() + " - skipping");
            return;
        }

        if (loadedExtensions.containsKey(manifest.name)) {
            plugin.getLogger().warning("Extension '" + manifest.name + "' already loaded - skipping " + jarFile.getName());
            return;
        }

        URL[] urls = {jarFile.toURI().toURL()};
        URLClassLoader classLoader = new URLClassLoader(urls, this.getClass().getClassLoader());

        Class<?> mainClass = classLoader.loadClass(manifest.mainClass);
        if (!FlexGatewayExtension.class.isAssignableFrom(mainClass)) {
            throw new IllegalArgumentException("Main class does not implement FlexGatewayExtension");
        }

        FlexGatewayExtension extension = (FlexGatewayExtension) mainClass.getDeclaredConstructor().newInstance();
        ExtensionContextImpl context = new ExtensionContextImpl(extension, jarFile, classLoader);
        extension.setContext(context);

        LoadedExtension loadedExtension = new LoadedExtension(extension, context, classLoader, jarFile);

        // Initialize maps
        dataProviders.put(extension.getName(), new ConcurrentHashMap<>());
        actionHandlers.put(extension.getName(), new ConcurrentHashMap<>());

        extension.onLoad();
        loadedExtensions.put(extension.getName(), loadedExtension);

        plugin.getLogger().info("Loaded extension: " + extension.getName() + " v" + extension.getVersion() + " by " + extension.getAuthor());
    }

    /**
     * Enable all loaded extensions
     */
    public void enableExtensions() {
        for (LoadedExtension loadedExtension : loadedExtensions.values()) {
            try {
                loadedExtension.extension.onEnable();
                loadedExtension.enabled = true;
                plugin.getLogger().info("Enabled extension: " + loadedExtension.extension.getName());
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to enable extension: " + loadedExtension.extension.getName());
                e.printStackTrace();
            }
        }
    }

    /**
     * Disable all extensions
     */
    public void disableExtensions() {
        for (LoadedExtension loadedExtension : loadedExtensions.values()) {
            if (loadedExtension.enabled) {
                try {
                    loadedExtension.extension.onDisable();
                    loadedExtension.enabled = false;
                    plugin.getLogger().info("Disabled extension: " + loadedExtension.extension.getName());
                } catch (Exception e) {
                    plugin.getLogger().severe("Error disabling extension: " + loadedExtension.extension.getName());
                    e.printStackTrace();
                }
            }
        }

        for (LoadedExtension loadedExtension : loadedExtensions.values()) {
            try {
                loadedExtension.classLoader.close();
            } catch (Exception e) {
                plugin.getLogger().warning("Error closing class loader for: " + loadedExtension.extension.getName());
            }
        }

        loadedExtensions.clear();
        dataProviders.clear();
        actionHandlers.clear();
        baseEndpointEnhancers.clear();
    }

    // Enhanced data methods for base endpoints
    public JsonObject enhanceBaseEndpoint(String baseEndpoint, JsonObject originalData, String identifier, Map<String, String> parameters) {
        List<BaseEndpointEnhancer> enhancers = baseEndpointEnhancers.get(baseEndpoint);
        if (enhancers == null || enhancers.isEmpty()) {
            return originalData;
        }

        // Sort by priority (higher numbers processed later)
        enhancers.sort(Comparator.comparingInt(BaseEndpointEnhancer::getPriority));

        // Create a copy to work with
        JsonObject enhanced = originalData.deepCopy();

        for (BaseEndpointEnhancer enhancer : enhancers) {
            try {
                // Pass the response object to be modified directly
                enhancer.enhanceResponse(enhanced, originalData, identifier, parameters);
                plugin.getLogger().fine("Enhanced " + baseEndpoint + " with enhancer from priority " + enhancer.getPriority());
            } catch (Exception e) {
                plugin.getLogger().warning("Error in base endpoint enhancer: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return enhanced;
    }

    /**
     * Read extension manifest from JAR
     */
    private ExtensionManifest readManifest(File jarFile) {
        try (JarFile jar = new JarFile(jarFile)) {
            JarEntry manifestEntry = jar.getJarEntry("extension.yml");
            if (manifestEntry == null) {
                manifestEntry = jar.getJarEntry("plugin.yml");
                if (manifestEntry == null) {
                    return null;
                }
            }

            try (InputStream is = jar.getInputStream(manifestEntry)) {
                String content = new String(is.readAllBytes());
                ExtensionManifest manifest = new ExtensionManifest();

                for (String line : content.split("\n")) {
                    line = line.trim();
                    if (line.startsWith("name:")) {
                        manifest.name = line.substring(5).trim().replace("\"", "").replace("'", "");
                    } else if (line.startsWith("main:")) {
                        manifest.mainClass = line.substring(5).trim().replace("\"", "").replace("'", "");
                    } else if (line.startsWith("version:")) {
                        manifest.version = line.substring(8).trim().replace("\"", "").replace("'", "");
                    } else if (line.startsWith("author:")) {
                        manifest.author = line.substring(7).trim().replace("\"", "").replace("'", "");
                    } else if (line.startsWith("description:")) {
                        manifest.description = line.substring(12).trim().replace("\"", "").replace("'", "");
                    }
                }

                if (manifest.name != null && manifest.mainClass != null) {
                    return manifest;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error reading manifest from " + jarFile.getName() + ": " + e.getMessage());
        }

        return null;
    }

    // Getters for RestApiServer
    public DataProvider getDataProvider(String extensionName, String endpoint) {
        Map<String, DataProvider> providers = dataProviders.get(extensionName.toLowerCase());
        return providers != null ? providers.get(endpoint.toLowerCase()) : null;
    }

    public ActionHandler getActionHandler(String extensionName, String endpoint) {
        Map<String, ActionHandler> handlers = actionHandlers.get(extensionName.toLowerCase());
        return handlers != null ? handlers.get(endpoint.toLowerCase()) : null;
    }

    public Collection<FlexGatewayExtension> getExtensions() {
        return loadedExtensions.values().stream()
                .map(le -> le.extension)
                .toList();
    }

    public JsonObject getExtensionsInfo() {
        JsonObject info = new JsonObject();
        JsonArray extensionsList = new JsonArray();

        for (LoadedExtension loadedExtension : loadedExtensions.values()) {
            FlexGatewayExtension extension = loadedExtension.extension;
            JsonObject extInfo = extension.getInfo();
            extInfo.addProperty("enabled", loadedExtension.enabled);
            extInfo.addProperty("jarFile", loadedExtension.jarFile.getName());

            // Add endpoint information
            JsonArray dataEndpoints = new JsonArray();
            Map<String, DataProvider> providers = dataProviders.get(extension.getName().toLowerCase());
            if (providers != null) {
                for (DataProvider provider : providers.values()) {
                    JsonObject endpoint = new JsonObject();
                    endpoint.addProperty("endpoint", provider.getEndpoint());
                    endpoint.addProperty("description", provider.getDescription());
                    endpoint.addProperty("method", "GET");
                    endpoint.addProperty("path", "/api/" + extension.getName() + "/" + provider.getEndpoint());

                    JsonArray params = new JsonArray();
                    for (String param : provider.getSupportedParameters()) {
                        params.add(param);
                    }
                    endpoint.add("parameters", params);

                    dataEndpoints.add(endpoint);
                }
            }
            extInfo.add("dataEndpoints", dataEndpoints);

            JsonArray actionEndpoints = new JsonArray();
            Map<String, ActionHandler> handlers = actionHandlers.get(extension.getName().toLowerCase());
            if (handlers != null) {
                for (ActionHandler handler : handlers.values()) {
                    JsonObject endpoint = new JsonObject();
                    endpoint.addProperty("endpoint", handler.getEndpoint());
                    endpoint.addProperty("description", handler.getDescription());
                    endpoint.addProperty("path", "/api/" + extension.getName() + "/" + handler.getEndpoint());

                    JsonArray methods = new JsonArray();
                    for (String method : handler.getSupportedMethods()) {
                        methods.add(method);
                    }
                    endpoint.add("methods", methods);

                    actionEndpoints.add(endpoint);
                }
            }
            extInfo.add("actionEndpoints", actionEndpoints);

            JsonArray baseEnhancements = new JsonArray();
            for (Map.Entry<String, List<BaseEndpointEnhancer>> entry : baseEndpointEnhancers.entrySet()) {
                for (BaseEndpointEnhancer enhancer : entry.getValue()) {
                    // Check if this enhancer belongs to this extension (simple check by class loader)
                    if (enhancer.getClass().getClassLoader() == loadedExtension.classLoader) {
                        JsonObject enhancement = new JsonObject();
                        enhancement.addProperty("baseEndpoint", enhancer.getBaseEndpoint());
                        enhancement.addProperty("description", enhancer.getDescription());
                        enhancement.addProperty("priority", enhancer.getPriority());
                        baseEnhancements.add(enhancement);
                    }
                }
            }
            extInfo.add("baseEnhancements", baseEnhancements);

            extensionsList.add(extInfo);
        }

        info.add("extensions", extensionsList);
        info.addProperty("count", loadedExtensions.size());

        return info;
    }

    // Internal classes remain the same...
    private static class ExtensionManifest {
        String name;
        String mainClass;
        String version = "1.0.0";
        String author = "Unknown";
        String description = "";
    }

    private static class LoadedExtension {
        final FlexGatewayExtension extension;
        final ExtensionContextImpl context;
        final URLClassLoader classLoader;
        final File jarFile;
        boolean enabled = false;

        LoadedExtension(FlexGatewayExtension extension, ExtensionContextImpl context, URLClassLoader classLoader, File jarFile) {
            this.extension = extension;
            this.context = context;
            this.classLoader = classLoader;
            this.jarFile = jarFile;
        }
    }

    // Extension context implementation
    public class ExtensionContextImpl implements ExtensionContext {
        private final FlexGatewayExtension extension;
        private final File dataFolder;
        public final URLClassLoader classLoader;
        private final Logger logger;
        private ExtensionConfigImpl config;

        public ExtensionContextImpl(FlexGatewayExtension extension, File jarFile, URLClassLoader classLoader) {
            this.extension = extension;
            this.classLoader = classLoader;
            this.dataFolder = new File(extensionsFolder, extension.getName());
            this.logger = Logger.getLogger("FlexGateway-" + extension.getName());

            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
        }

        @Override
        public JavaPlugin getPlugin() {
            return plugin;
        }

        @Override
        public org.bukkit.Server getServer() {
            return plugin.getServer();
        }

        @Override
        public Logger getLogger() {
            return logger;
        }

        @Override
        public File getDataFolder() {
            return dataFolder;
        }

        @Override
        public boolean registerDataProvider(DataProvider provider) {
            Map<String, DataProvider> providers = dataProviders.get(extension.getName().toLowerCase());
            if (providers == null) return false;

            String endpoint = provider.getEndpoint().toLowerCase();
            if (providers.containsKey(endpoint)) {
                logger.warning("Data provider endpoint '" + endpoint + "' already exists");
                return false;
            }

            providers.put(endpoint, provider);
            logger.info("Registered data provider: " + endpoint);
            return true;
        }

        @Override
        public boolean registerActionHandler(ActionHandler handler) {
            Map<String, ActionHandler> handlers = actionHandlers.get(extension.getName().toLowerCase());
            if (handlers == null) return false;

            String endpoint = handler.getEndpoint().toLowerCase();
            if (handlers.containsKey(endpoint)) {
                logger.warning("Action handler endpoint '" + endpoint + "' already exists");
                return false;
            }

            handlers.put(endpoint, handler);
            logger.info("Registered action handler: " + endpoint);
            return true;
        }

        @Override
        public boolean registerBaseEndpointEnhancer(BaseEndpointEnhancer enhancer) {
            String baseEndpoint = enhancer.getBaseEndpoint().toLowerCase();
            List<BaseEndpointEnhancer> enhancers = baseEndpointEnhancers.computeIfAbsent(baseEndpoint, k -> new ArrayList<>());

            enhancers.add(enhancer);
            logger.info("Registered base endpoint enhancer for: " + baseEndpoint + " (priority: " + enhancer.getPriority() + ")");
            return true;
        }

        @Override
        public boolean unregisterBaseEndpointEnhancer(String baseEndpoint, String enhancementKey) {
            // This method signature is no longer relevant, but keep for compatibility
            List<BaseEndpointEnhancer> enhancers = baseEndpointEnhancers.get(baseEndpoint.toLowerCase());
            if (enhancers == null) return false;

            // Remove all enhancers from this extension's class loader
            boolean removed = enhancers.removeIf(e -> e.getClass().getClassLoader() == this.classLoader);
            return removed;
        }

        @Override
        public boolean unregisterDataProvider(String endpoint) {
            Map<String, DataProvider> providers = dataProviders.get(extension.getName().toLowerCase());
            return providers != null && providers.remove(endpoint.toLowerCase()) != null;
        }

        @Override
        public boolean unregisterActionHandler(String endpoint) {
            Map<String, ActionHandler> handlers = actionHandlers.get(extension.getName().toLowerCase());
            return handlers != null && handlers.remove(endpoint.toLowerCase()) != null;
        }

        @Override
        public ExtensionConfig getConfig() {
            if (config == null) {
                config = new ExtensionConfigImpl(this);
            }
            return config;
        }

        @Override
        public void saveResource(String resourcePath, boolean replace) {
            try {
                InputStream resource = classLoader.getResourceAsStream(resourcePath);
                if (resource == null) {
                    logger.warning("Resource not found: " + resourcePath);
                    return;
                }

                File targetFile = new File(dataFolder, resourcePath);
                if (targetFile.exists() && !replace) {
                    return;
                }

                targetFile.getParentFile().mkdirs();
                try (FileOutputStream out = new FileOutputStream(targetFile)) {
                    resource.transferTo(out);
                }

            } catch (Exception e) {
                logger.severe("Failed to save resource " + resourcePath + ": " + e.getMessage());
            }
        }
    }
}