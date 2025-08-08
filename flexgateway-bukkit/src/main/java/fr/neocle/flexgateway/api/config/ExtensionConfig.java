package fr.neocle.flexgateway.api.config;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Configuration wrapper for extensions
 */
public interface ExtensionConfig {

    /**
     * Get the underlying configuration
     */
    FileConfiguration getConfig();

    /**
     * Save the configuration
     */
    void save();

    /**
     * Reload the configuration
     */
    void reload();

    /**
     * Get a value from config with default
     */
    <T> T get(String path, T defaultValue);

    /**
     * Set a value in config
     */
    void set(String path, Object value);
}