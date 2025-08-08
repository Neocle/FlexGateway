package fr.neocle.flexgateway.server.handlers.base;

import fr.neocle.flexgateway.api.loader.ExtensionLoader;
import fr.neocle.flexgateway.data.DataManager;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.Map;

public class StatusHandler extends EnhancedBaseHandler {
    public StatusHandler(JavaPlugin plugin, DataManager dataManager, ExtensionLoader extensionLoader) {
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
            JsonObject status = new JsonObject();
            status.addProperty("status", "online");
            status.addProperty("timestamp", System.currentTimeMillis());
            status.addProperty("api_version", "1.0.0");
            status.addProperty("server_name", plugin.getServer().getName());
            status.addProperty("online_players", plugin.getServer().getOnlinePlayers().size());
            status.addProperty("max_players", plugin.getServer().getMaxPlayers());
            status.addProperty("extensions_loaded", extensionLoader.getExtensions().size());
            // Enhance with extensions
            return extensionLoader.enhanceBaseEndpoint("status", status, null, params);
        });
    }
}