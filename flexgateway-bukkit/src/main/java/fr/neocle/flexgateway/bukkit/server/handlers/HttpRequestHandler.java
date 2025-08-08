package fr.neocle.flexgateway.bukkit.server.handlers;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public abstract class HttpRequestHandler implements HttpHandler {
    protected final JavaPlugin plugin;

    public HttpRequestHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    protected void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    protected void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        JsonObject error = new JsonObject();
        error.addProperty("error", message);
        error.addProperty("status", statusCode);
        sendResponse(exchange, statusCode, error.toString());
    }

    protected void executeOnMainThread(HttpExchange exchange, MainThreadTask task) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                JsonObject result = task.execute();
                future.complete(result);
            } catch (Exception e) {
                plugin.getLogger().warning("Error executing main thread task: " + e.getMessage());
                future.completeExceptionally(e);
            }
        });

        try {
            JsonObject result = future.get(30, TimeUnit.SECONDS);
            sendResponse(exchange, 200, result.toString());
        } catch (Exception e) {
            try {
                plugin.getLogger().warning("Main thread task failed: " + e.getMessage());
                sendError(exchange, 500, "Internal server error");
            } catch (IOException ioException) {
                plugin.getLogger().severe("Failed to send error response: " + ioException.getMessage());
            }
        }
    }

    protected Map<String, String> parseQueryParameters(String query) {
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
                        plugin.getLogger().warning("Error parsing query parameter: " + e.getMessage());
                    }
                }
            }
        }
        return params;
    }

    protected String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    protected void handleCorsPreflightRequest(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(200, -1);
    }

    @FunctionalInterface
    protected interface MainThreadTask {
        JsonObject execute() throws Exception;
    }
}