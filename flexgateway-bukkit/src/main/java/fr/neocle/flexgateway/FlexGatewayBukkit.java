package fr.neocle.flexgateway;

import fr.neocle.flexgateway.communication.TcpCommunicationServer;
import fr.neocle.flexgateway.server.RestApiServer;
import fr.neocle.flexgateway.api.loader.ExtensionLoader;
import fr.neocle.flexgateway.data.DataManager;
import fr.neocle.flexgateway.database.DatabaseManager;
import fr.neocle.flexgateway.listeners.ChatListener;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Optional;

public class FlexGatewayBukkit extends JavaPlugin {

    private RestApiServer apiServer;
    private DataManager dataManager;
    private DatabaseManager databaseManager;
    private ExtensionLoader extensionLoader;
    private TcpCommunicationServer tcpServer;
    private boolean proxyMode = false;
    private boolean standaloneMode = true;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Detect proxy mode first
        detectProxyMode();

        // Initialize extension loader
        extensionLoader = new ExtensionLoader(this);

        // Initialize database
        getLogger().info("Initializing database...");
        databaseManager = new DatabaseManager(this);

        if (!databaseManager.isConnected()) {
            getLogger().severe("Failed to initialize database! Message features will be disabled.");
        } else {
            getLogger().info("Database initialized successfully");
        }

        dataManager = new DataManager(this, databaseManager);

        // Register chat listener
        if (databaseManager.isConnected()) {
            getServer().getPluginManager().registerEvents(new ChatListener(databaseManager), this);
            getLogger().info("Chat listener registered");
        }

        // Load extensions
        extensionLoader.loadExtensions();
        extensionLoader.enableExtensions();

        if (proxyMode) {
            setupProxyMode();
        } else {
            setupStandaloneMode();
        }
    }

    private void setupProxyMode() {
        getLogger().info("=== SETTING UP PROXY MODE ===");
        getLogger().info("Starting TCP communication server instead of plugin messaging");

        // Get TCP port from config (default to 25599)
        int tcpPort = getConfig().getInt("tcp.port", 25599);

        try {
            // Start TCP server
            tcpServer = new TcpCommunicationServer(this, dataManager, extensionLoader, tcpPort);
            tcpServer.start();

            getLogger().info("=== PROXY MODE SETUP COMPLETE ===");
            getLogger().info("TCP Communication server started on port: " + tcpPort);
            getLogger().info("Ready to communicate with Velocity proxy via TCP");

        } catch (Exception e) {
            getLogger().severe("Failed to start TCP communication server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupStandaloneMode() {
        getLogger().info("=== SETTING UP STANDALONE MODE ===");
        getLogger().info("Starting web server for direct API access");

        int port = getConfig().getInt("api.port", 8080);
        String host = getConfig().getString("api.host", "localhost");

        apiServer = new RestApiServer(this, dataManager, extensionLoader, host, port);

        try {
            apiServer.start();
            getLogger().info("REST API server started on " + host + ":" + port);
            getLogger().info("Extensions loaded: " + extensionLoader.getExtensions().size());

        } catch (Exception e) {
            getLogger().severe("Failed to start REST API server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void detectProxyMode() {
        getLogger().info("=== DETECTING PROXY MODE ===");
        boolean bungeeCordEnabled = false;
        boolean velocityEnabled = false;
        File serverRoot = Bukkit.getServer().getWorldContainer();

        try {
            File spigotYml = new File(serverRoot, "spigot.yml");
            if (spigotYml.exists()) {
                YamlConfiguration spigotConfig = YamlConfiguration.loadConfiguration(spigotYml);
                bungeeCordEnabled = spigotConfig.getBoolean("settings.bungeecord", false);
                getLogger().info("Spigot config found - BungeeCord enabled: " + bungeeCordEnabled);
            } else {
                getLogger().info("No spigot.yml found");
            }

            File paperConfigFile = Optional.of(new File(serverRoot, "config/paper-global.yml"))
                    .filter(File::exists)
                    .orElseGet(() -> {
                        File legacy = new File(serverRoot, "paper-global.yml");
                        return legacy.exists() ? legacy : null;
                    });

            if (paperConfigFile != null) {
                YamlConfiguration paperConfig = YamlConfiguration.loadConfiguration(paperConfigFile);
                velocityEnabled = paperConfig.getBoolean("proxies.velocity.enabled", false)
                        || paperConfig.getBoolean("settings.velocity-support.enabled", false);
                getLogger().info("Paper config found at " + paperConfigFile.getName() + " - Velocity enabled: " + velocityEnabled);
            } else {
                getLogger().info("No Paper config found");
            }

        } catch (Exception e) {
            getLogger().warning("Failed to detect proxy configuration: " + e.getMessage());
            e.printStackTrace();
        }

        this.proxyMode = bungeeCordEnabled || velocityEnabled;
        this.standaloneMode = !this.proxyMode;

        getLogger().info("=== PROXY DETECTION RESULTS ===");
        getLogger().info("BungeeCord: " + bungeeCordEnabled);
        getLogger().info("Velocity: " + velocityEnabled);
        getLogger().info("Proxy Mode: " + proxyMode);
        getLogger().info("Standalone Mode: " + standaloneMode);
    }

    @Override
    public void onDisable() {
        if (extensionLoader != null) {
            extensionLoader.disableExtensions();
        }

        if (apiServer != null) {
            try {
                apiServer.stop();
                getLogger().info("REST API server stopped");
            } catch (Exception e) {
                getLogger().severe("Error stopping REST API server: " + e.getMessage());
            }
        }

        if (tcpServer != null) {
            tcpServer.stop();
        }

        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ExtensionLoader getExtensionLoader() {
        return extensionLoader;
    }

    public boolean isProxyMode() {
        return proxyMode;
    }

    public boolean isStandaloneMode() {
        return standaloneMode;
    }
}