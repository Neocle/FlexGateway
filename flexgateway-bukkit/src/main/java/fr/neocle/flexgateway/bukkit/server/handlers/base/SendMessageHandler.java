package fr.neocle.flexgateway.bukkit.server.handlers.base;

import fr.neocle.flexgateway.api.loader.ExtensionLoader;
import fr.neocle.flexgateway.bukkit.data.DataManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public class SendMessageHandler extends EnhancedBaseHandler {
    public SendMessageHandler(JavaPlugin plugin, DataManager dataManager, ExtensionLoader extensionLoader) {
        super(plugin, dataManager, extensionLoader);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            exchange.sendResponseHeaders(200, -1);
            return;
        }
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }
        try {
            String requestBody = readRequestBody(exchange);
            JsonParser parser = new JsonParser();
            JsonObject jsonRequest = parser.parse(requestBody).getAsJsonObject();
            String senderUuid = jsonRequest.has("senderUuid") ? jsonRequest.get("senderUuid").getAsString() : null;
            String recipientUuid = jsonRequest.has("recipientUuid") ? jsonRequest.get("recipientUuid").getAsString() : null;
            String message = jsonRequest.has("message") ? jsonRequest.get("message").getAsString() : null;
            String messageType = jsonRequest.has("messageType") ? jsonRequest.get("messageType").getAsString() : "API";
            if (message == null || message.trim().isEmpty()) {
                sendError(exchange, 400, "Message is required");
                return;
            }
            if (message.length() > 1000) {
                sendError(exchange, 400, "Message too long (max 1000 characters)");
                return;
            }
            executeOnMainThread(exchange, () -> {
                boolean success = dataManager.sendMessage(senderUuid, recipientUuid, message.trim(), messageType);
                JsonObject response = new JsonObject();
                response.addProperty("success", success);
                response.addProperty("message", success ? "Message sent successfully" : "Failed to send message");
                if (success) {
                    response.addProperty("timestamp", System.currentTimeMillis());
                    response.addProperty("messageLength", message.trim().length());
                }
                return response;
            });
        } catch (Exception e) {
            plugin.getLogger().warning("Error parsing send message request: " + e.getMessage());
            sendError(exchange, 400, "Invalid JSON request");
        }
    }
}