package fr.neocle.flexgateway.api;

import fr.neocle.flexgateway.api.handler.ActionHandler;
import fr.neocle.flexgateway.api.enhancer.BaseEndpointEnhancer;
import fr.neocle.flexgateway.api.provider.DataProvider;
import fr.neocle.flexgateway.api.config.ExtensionConfig;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Logger;

/**
 * Context provided to extensions for accessing plugin services
 */
public interface ExtensionContext {

    /**
     * Get the main FlexGateway plugin instance
     */
    JavaPlugin getPlugin();

    /**
     * Get the Bukkit server instance
     */
    Server getServer();

    /**
     * Get a logger for this extension
     */
    Logger getLogger();

    /**
     * Get the extension's data folder
     */
    File getDataFolder();

    /**
     * Register a data provider for this extension (/api/{extension}/{endpoint})
     */
    boolean registerDataProvider(DataProvider provider);

    /**
     * Register an action handler for this extension (/api/{extension}/{endpoint})
     */
    boolean registerActionHandler(ActionHandler handler);

    /**
     * Register an enhancer for base plugin endpoints
     * This allows adding data to /api/server, /api/players, etc.
     */
    boolean registerBaseEndpointEnhancer(BaseEndpointEnhancer enhancer);

    /**
     * Unregister a data provider
     */
    boolean unregisterDataProvider(String endpoint);

    /**
     * Unregister an action handler
     */
    boolean unregisterActionHandler(String endpoint);

    /**
     * Unregister a base endpoint enhancer
     */
    boolean unregisterBaseEndpointEnhancer(String baseEndpoint, String enhancementKey);

    /**
     * Get extension configuration
     */
    ExtensionConfig getConfig();

    /**
     * Save a resource from the extension jar to the data folder
     */
    void saveResource(String resourcePath, boolean replace);
}