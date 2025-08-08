package fr.neocle.flexgateway.server.handlers.base;

import fr.neocle.flexgateway.api.loader.ExtensionLoader;
import fr.neocle.flexgateway.data.DataManager;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.Map;

public class PlayerHandler extends EnhancedBaseHandler {
    public PlayerHandler(JavaPlugin plugin, DataManager dataManager, ExtensionLoader extensionLoader) {
        super(plugin, dataManager, extensionLoader);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String identifier = path.substring("/api/player/".length());

        if (identifier.isEmpty()) {
            sendError(exchange, 400, "Player identifier required");
            return;
        }

        Map<String, String> params = parseQueryParameters(exchange.getRequestURI().getQuery());

        executeOnMainThread(exchange, () -> {
            JsonObject playerInfo = dataManager.getPlayerInfo(identifier);
            if (playerInfo == null) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Player not found");
                error.addProperty("status", 404);
                return error;
            }
            // Enhance with extensions
            return extensionLoader.enhanceBaseEndpoint("player", playerInfo, identifier, params);
        });
    }
}