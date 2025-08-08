package fr.neocle.flexgateway.bukkit.server.handlers.base;

import fr.neocle.flexgateway.api.loader.ExtensionLoader;
import fr.neocle.flexgateway.bukkit.data.DataManager;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.Map;

public class ServerHandler extends EnhancedBaseHandler {

    public ServerHandler(JavaPlugin plugin, DataManager dataManager, ExtensionLoader extensionLoader) {
        super(plugin, dataManager, extensionLoader);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        Map<String, String> params = parseQueryParameters(exchange.getRequestURI().getQuery());

        executeOnMainThread(exchange, () -> {
            JsonObject serverInfo = dataManager.getServerInfo();
            return extensionLoader.enhanceBaseEndpoint("server", serverInfo, null, params);
        });
    }
}