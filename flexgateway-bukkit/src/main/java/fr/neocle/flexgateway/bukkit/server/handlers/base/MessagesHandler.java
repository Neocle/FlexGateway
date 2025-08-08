package fr.neocle.flexgateway.bukkit.server.handlers.base;

import fr.neocle.flexgateway.api.loader.ExtensionLoader;
import fr.neocle.flexgateway.bukkit.data.DataManager;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.Map;

public class MessagesHandler extends EnhancedBaseHandler {
    public MessagesHandler(JavaPlugin plugin, DataManager dataManager, ExtensionLoader extensionLoader) {
        super(plugin, dataManager, extensionLoader);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        Map<String, String> params = parseQueryParameters(exchange.getRequestURI().getQuery());

        int limit = 50;
        int offset = 0;
        String playerUuid = null;
        String messageType = null;

        try {
            if (params.containsKey("limit")) {
                limit = Math.min(Math.max(Integer.parseInt(params.get("limit")), 1), 500);
            }
            if (params.containsKey("offset")) {
                offset = Math.max(Integer.parseInt(params.get("offset")), 0);
            }
            if (params.containsKey("player")) {
                playerUuid = params.get("player");
            }
            if (params.containsKey("type")) {
                messageType = params.get("type");
            }
        } catch (NumberFormatException e) {
            sendError(exchange, 400, "Invalid numeric parameter");
            return;
        }

        final int finalLimit = limit;
        final int finalOffset = offset;
        final String finalPlayerUuid = playerUuid;
        final String finalMessageType = messageType;

        executeOnMainThread(exchange, () -> {
            JsonObject response = new JsonObject();
            response.add("messages", dataManager.getMessages(finalLimit, finalOffset, finalPlayerUuid, finalMessageType));
            response.addProperty("limit", finalLimit);
            response.addProperty("offset", finalOffset);
            if (finalPlayerUuid != null) {
                response.addProperty("player", finalPlayerUuid);
            }
            if (finalMessageType != null) {
                response.addProperty("type", finalMessageType);
            }
            // Enhance with extensions
            return extensionLoader.enhanceBaseEndpoint("messages", response, null, params);
        });
    }
}
