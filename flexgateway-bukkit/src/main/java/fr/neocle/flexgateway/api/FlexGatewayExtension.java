package fr.neocle.flexgateway.api;

import com.google.gson.JsonObject;

/**
 * Main interface for FlexGateway extensions
 * Extensions are loaded from JAR files in the extensions folder
 */
public interface FlexGatewayExtension {

    /**
     * Get the extension name (used in API paths)
     * Should be lowercase and URL-safe (no spaces, special chars)
     */
    String getName();

    /**
     * Get the extension version
     */
    String getVersion();

    /**
     * Get extension description
     */
    String getDescription();

    /**
     * Get extension author
     */
    String getAuthor();

    /**
     * Called when the extension is loaded
     * ExtensionContext is automatically available via getContext()
     */
    void onLoad();

    /**
     * Called when the extension is enabled
     */
    void onEnable();

    /**
     * Called when the extension is disabled
     */
    void onDisable();

    /**
     * Get the extension context (managed by the plugin)
     * This is automatically set before onLoad() is called
     */
    ExtensionContext getContext();

    /**
     * Set the extension context (called by the plugin - don't override this)
     */
    void setContext(ExtensionContext context);

    /**
     * Get extension metadata/info
     */
    default JsonObject getInfo() {
        JsonObject info = new JsonObject();
        info.addProperty("name", getName());
        info.addProperty("version", getVersion());
        info.addProperty("description", getDescription());
        info.addProperty("author", getAuthor());
        return info;
    }
}