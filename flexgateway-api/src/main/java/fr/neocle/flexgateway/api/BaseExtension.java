package fr.neocle.flexgateway.api;

import fr.neocle.flexgateway.api.handler.ActionHandler;
import fr.neocle.flexgateway.api.enhancer.BaseEndpointEnhancer;
import fr.neocle.flexgateway.api.provider.DataProvider;
import fr.neocle.flexgateway.api.config.ExtensionConfig;

/**
 * Base class for extensions that handles context management
 */
public abstract class BaseExtension implements FlexGatewayExtension {

    private ExtensionContext context;

    @Override
    public final ExtensionContext getContext() {
        return context;
    }

    @Override
    public final void setContext(ExtensionContext context) {
        this.context = context;
    }

    /**
     * Convenience method to get the logger
     */
    protected final java.util.logging.Logger getLogger() {
        return context.getLogger();
    }

    /**
     * Convenience method to get the data folder
     */
    protected final java.io.File getDataFolder() {
        return context.getDataFolder();
    }

    /**
     * Convenience method to get the config
     */
    protected final ExtensionConfig getConfig() {
        return context.getConfig();
    }

    /**
     * Convenience method to register a data provider
     */
    protected final boolean registerDataProvider(DataProvider provider) {
        return context.registerDataProvider(provider);
    }

    /**
     * Convenience method to register an action handler
     */
    protected final boolean registerActionHandler(ActionHandler handler) {
        return context.registerActionHandler(handler);
    }

    /**
     * Convenience method to register a base endpoint enhancer
     */
    protected final boolean registerBaseEndpointEnhancer(BaseEndpointEnhancer enhancer) {
        return context.registerBaseEndpointEnhancer(enhancer);
    }

    /**
     * Convenience method to save a resource
     */
    protected final void saveResource(String resourcePath, boolean replace) {
        context.saveResource(resourcePath, replace);
    }

    /**
     * Convenience method to save default config
     */
    protected final void saveDefaultConfig() {
        saveResource("config.yml", false);
    }
}