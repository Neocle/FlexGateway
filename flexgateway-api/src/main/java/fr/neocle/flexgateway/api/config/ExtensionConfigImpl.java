package fr.neocle.flexgateway.api.config;

import fr.neocle.flexgateway.api.loader.ExtensionLoader;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;

public class ExtensionConfigImpl implements ExtensionConfig {

    private final ExtensionLoader.ExtensionContextImpl context;
    private final File configFile;
    private FileConfiguration config;

    public ExtensionConfigImpl(ExtensionLoader.ExtensionContextImpl context) {
        this.context = context;
        this.configFile = new File(context.getDataFolder(), "config.yml");
        reload();
    }

    @Override
    public FileConfiguration getConfig() {
        return config;
    }

    @Override
    public void save() {
        try {
            config.save(configFile);
        } catch (Exception e) {
            context.getLogger().severe("Failed to save config: " + e.getMessage());
        }
    }

    @Override
    public void reload() {
        config = YamlConfiguration.loadConfiguration(configFile);

        // Load defaults from jar if available
        try {
            InputStream defaultConfig = context.classLoader.getResourceAsStream("config.yml");
            if (defaultConfig != null) {
                YamlConfiguration defaultConfiguration = YamlConfiguration.loadConfiguration(
                        new java.io.InputStreamReader(defaultConfig)
                );
                config.setDefaults(defaultConfiguration);
            }
        } catch (Exception e) {
            context.getLogger().warning("Could not load default config: " + e.getMessage());
        }
    }

    @Override
    public <T> T get(String path, T defaultValue) {
        Object value = config.get(path, defaultValue);
        try {
            return (T) value;
        } catch (ClassCastException e) {
            context.getLogger().warning("Config value at '" + path + "' is not the expected type, using default");
            return defaultValue;
        }
    }

    @Override
    public void set(String path, Object value) {
        config.set(path, value);
    }
}