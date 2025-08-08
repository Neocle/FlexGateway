package fr.neocle.flexgateway.velocity.server.handlers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexgateway.velocity.communication.TcpMessageManager;
import fr.neocle.flexgateway.velocity.data.ServerDataManager;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ServerApiHandler extends BaseHandler {
    private final TcpMessageManager messageManager;
    private final ServerDataManager dataManager;

    public ServerApiHandler(ProxyServer server, Logger logger, ServerDataManager dataManager, TcpMessageManager messageManager) {
        super(server, logger);
        this.dataManager = dataManager;
        this.messageManager = messageManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        logger.info("Received request: {} {}", method, path);

        if ("OPTIONS".equals(method)) {
            handleCorsPreflightRequest(exchange);
            return;
        }

        // Parse path: /api/{server-name}/{endpoint}
        String[] pathParts = path.split("/");
        if (pathParts.length < 4) {
            logger.warn("Invalid path format: {}", path);
            sendError(exchange, 400, "Invalid path format. Expected: /api/{server-name}/{endpoint}");
            return;
        }

        String serverName = pathParts[2];
        String endpoint = String.join("/", java.util.Arrays.copyOfRange(pathParts, 3, pathParts.length));

        logger.info("Routing request to server: {}, endpoint: {}", serverName, endpoint);

        // Check if server exists
        if (!server.getServer(serverName).isPresent()) {
            logger.warn("Server not found: {}", serverName);
            sendError(exchange, 404, "Server not found: " + serverName);
            return;
        }

        Map<String, String> queryParams = parseQueryParameters(exchange.getRequestURI().getQuery());
        String cacheKey = generateCacheKey(method, endpoint, queryParams);

        // Check cache first for GET requests
        if ("GET".equals(method) && dataManager.hasCachedResponse(serverName, cacheKey)) {
            JsonObject cachedResponse = dataManager.getCachedResponse(serverName, cacheKey);
            if (cachedResponse != null) {
                logger.info("Returning cached response for {}/{}", serverName, endpoint);
                sendJsonResponse(exchange, 200, cachedResponse);
                return;
            }
        }

        // Prepare request data
        JsonObject requestData = new JsonObject();
        requestData.addProperty("endpoint", endpoint);
        requestData.addProperty("method", method);

        // Add query parameters
        if (!queryParams.isEmpty()) {
            JsonObject paramsObj = new JsonObject();
            queryParams.forEach(paramsObj::addProperty);
            requestData.add("params", paramsObj);
        }

        // Add request body for write operations
        if ("POST".equals(method) || "PUT".equals(method)) {
            try {
                String requestBody = readRequestBody(exchange);
                if (!requestBody.isEmpty()) {
                    JsonParser parser = new JsonParser();
                    JsonObject body = parser.parse(requestBody).getAsJsonObject();
                    requestData.add("body", body);
                }
            } catch (Exception e) {
                logger.warn("Failed to parse request body", e);
            }
        }

        // Handle specific endpoints
        String action = mapEndpointToAction(endpoint);

        // Add endpoint-specific data
        if (endpoint.startsWith("player/")) {
            String playerIdentifier = endpoint.substring("player/".length());
            requestData.addProperty("identifier", playerIdentifier);
        } else if (endpoint.startsWith("world/")) {
            String worldName = endpoint.substring("world/".length());
            requestData.addProperty("worldName", worldName);
        } else if (endpoint.startsWith("extension/")) {
            // Handle extension endpoints
            String[] extensionParts = endpoint.substring("extension/".length()).split("/", 2);
            if (extensionParts.length >= 2) {
                requestData.addProperty("extensionName", extensionParts[0]);
                requestData.addProperty("endpointName", extensionParts[1]);
                action = "extension";
            }
        } else if (endpoint.equals("messages")) {
            // Add message-specific parameters
            if (queryParams.containsKey("limit")) {
                try {
                    requestData.addProperty("limit", Integer.parseInt(queryParams.get("limit")));
                } catch (NumberFormatException ignored) {}
            }
            if (queryParams.containsKey("offset")) {
                try {
                    requestData.addProperty("offset", Integer.parseInt(queryParams.get("offset")));
                } catch (NumberFormatException ignored) {}
            }
            if (queryParams.containsKey("player")) {
                requestData.addProperty("player", queryParams.get("player"));
            }
            if (queryParams.containsKey("type")) {
                requestData.addProperty("type", queryParams.get("type"));
            }
        }

        logger.info("Sending request to backend server {} with action: {}", serverName, action);

        // Send request to backend server
        CompletableFuture<JsonObject> future = messageManager.sendRequest(serverName, action, requestData);

        future.thenAccept(response -> {
            try {
                logger.info("Received response from server {}: {}", serverName, response.has("error") ? "error" : "success");

                // Cache successful GET responses
                if ("GET".equals(method) && !response.has("error")) {
                    dataManager.cacheResponse(serverName, cacheKey, response);
                }

                // Determine status code
                int statusCode = 200;
                if (response.has("status")) {
                    statusCode = response.get("status").getAsInt();
                }

                // Add proxy metadata
                response.addProperty("proxy_server", server.getVersion().getName());
                response.addProperty("backend_server", serverName);
                response.addProperty("request_timestamp", System.currentTimeMillis());

                sendJsonResponse(exchange, statusCode, response);

            } catch (IOException e) {
                logger.error("Failed to send response for server {} endpoint {}", serverName, endpoint, e);
            }
        }).exceptionally(throwable -> {
            try {
                logger.error("Request failed for server {} endpoint {}", serverName, endpoint, throwable);
                sendError(exchange, 500, "Internal server error: " + throwable.getMessage());
            } catch (IOException e) {
                logger.error("Failed to send error response", e);
            }
            return null;
        });
    }

    private String mapEndpointToAction(String endpoint) {
        if (endpoint.equals("server")) return "server";
        if (endpoint.equals("players")) return "players";
        if (endpoint.startsWith("player/")) return "player";
        if (endpoint.equals("worlds")) return "worlds";
        if (endpoint.startsWith("world/")) return "world";
        if (endpoint.equals("messages")) return "messages";
        if (endpoint.equals("send-message")) return "send-message";
        if (endpoint.equals("status")) return "status";
        if (endpoint.equals("extensions")) return "extensions";
        if (endpoint.startsWith("extension/")) return "extension";

        // For other endpoints, use the first part as action
        return endpoint.split("/")[0];
    }

    private String generateCacheKey(String method, String endpoint, Map<String, String> queryParams) {
        StringBuilder key = new StringBuilder();
        key.append(method).append(":").append(endpoint);

        if (!queryParams.isEmpty()) {
            key.append("?");
            queryParams.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> key.append(entry.getKey()).append("=").append(entry.getValue()).append("&"));

            // Remove trailing &
            if (key.charAt(key.length() - 1) == '&') {
                key.setLength(key.length() - 1);
            }
        }

        return key.toString();
    }

    private Map<String, String> parseQueryParameters(String query) {
        Map<String, String> params = new HashMap<>();
        if (query != null && !query.isEmpty()) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    try {
                        String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                        String value = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                        params.put(key, value);
                    } catch (Exception e) {
                        logger.warn("Error parsing query parameter: {}", e.getMessage());
                    }
                }
            }
        }
        return params;
    }
}