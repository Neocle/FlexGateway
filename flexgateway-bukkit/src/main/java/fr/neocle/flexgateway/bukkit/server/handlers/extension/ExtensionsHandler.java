package fr.neocle.flexgateway.bukkit.server.handlers.extension;

import fr.neocle.flexgateway.api.loader.ExtensionLoader;
import fr.neocle.flexgateway.bukkit.server.handlers.base.EnhancedBaseHandler;
import fr.neocle.flexgateway.bukkit.data.DataManager;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public class ExtensionsHandler extends EnhancedBaseHandler {
    public ExtensionsHandler(JavaPlugin plugin, DataManager dataManager, ExtensionLoader extensionLoader) {
        super(plugin, dataManager, extensionLoader);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        try {
            JsonObject extensionsInfo = extensionLoader.getExtensionsInfo();
            sendResponse(exchange, 200, extensionsInfo.toString());
        } catch (Exception e) {
            plugin.getLogger().severe("Error handling extensions request: " + e.getMessage());
            sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
}