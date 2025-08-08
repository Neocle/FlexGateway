package fr.neocle.flexgateway.server.handlers.base;

import fr.neocle.flexgateway.api.loader.ExtensionLoader;
import fr.neocle.flexgateway.server.handlers.HttpRequestHandler;
import fr.neocle.flexgateway.data.DataManager;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class EnhancedBaseHandler extends HttpRequestHandler {
    protected final DataManager dataManager;
    protected final ExtensionLoader extensionLoader;

    public EnhancedBaseHandler(JavaPlugin plugin, DataManager dataManager, ExtensionLoader extensionLoader) {
        super(plugin);
        this.dataManager = dataManager;
        this.extensionLoader = extensionLoader;
    }
}