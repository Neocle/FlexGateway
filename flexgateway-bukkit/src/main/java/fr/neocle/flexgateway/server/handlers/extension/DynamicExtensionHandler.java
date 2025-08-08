package fr.neocle.flexgateway.server.handlers.extension;

import fr.neocle.flexgateway.api.handler.ActionHandler;
import fr.neocle.flexgateway.api.loader.ExtensionLoader;
import fr.neocle.flexgateway.api.provider.DataProvider;
import fr.neocle.flexgateway.server.handlers.HttpRequestHandler;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.Map;

public class DynamicExtensionHandler extends HttpRequestHandler {
    private final ExtensionLoader extensionLoader;

    public DynamicExtensionHandler(JavaPlugin plugin, ExtensionLoader extensionLoader) {
        super(plugin);
        this.extensionLoader = extensionLoader;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        // Skip if it's a base endpoint
        if (isBaseEndpoint(path)) {
            sendError(exchange, 404, "Endpoint not found");
            return;
        }

        ExtensionEndpoint endpoint = parseExtensionEndpoint(path);
        if (endpoint == null) {
            sendError(exchange, 400, "Extension and endpoint required. Format: /api/{extension}/{endpoint}");
            return;
        }

        String method = exchange.getRequestMethod();
        Map<String, String> params = parseQueryParameters(exchange.getRequestURI().getQuery());

        if ("OPTIONS".equals(method)) {
            handleCorsPreflightRequest(exchange);
            return;
        }

        if ("GET".equals(method)) {
            handleDataProvider(exchange, endpoint, params);
        } else if (isWriteMethod(method)) {
            handleActionHandler(exchange, endpoint, method, params);
        } else {
            sendError(exchange, 405, "Method not allowed");
        }
    }

    private boolean isBaseEndpoint(String path) {
        return path.matches("/api/(server|players|player|worlds|world|messages|send-message|status|extensions).*");
    }

    private boolean isWriteMethod(String method) {
        return "POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method);
    }

    private ExtensionEndpoint parseExtensionEndpoint(String path) {
        String pathAfterApi = path.substring("/api/".length());
        String[] pathParts = pathAfterApi.split("/", 2);

        if (pathParts.length < 2) {
            return null;
        }

        return new ExtensionEndpoint(pathParts[0], pathParts[1]);
    }

    private void handleDataProvider(HttpExchange exchange, ExtensionEndpoint endpoint, Map<String, String> params) throws IOException {
        DataProvider provider = extensionLoader.getDataProvider(endpoint.extensionName, endpoint.endpointName);

        if (provider == null) {
            sendError(exchange, 404, "Extension endpoint not found: " + endpoint.extensionName + "/" + endpoint.endpointName);
            return;
        }

        executeOnMainThread(exchange, () -> {
            try {
                JsonElement data = provider.getData(params);
                JsonObject response = new JsonObject();
                response.add("data", data);
                response.addProperty("extension", endpoint.extensionName);
                response.addProperty("endpoint", endpoint.endpointName);
                response.addProperty("timestamp", System.currentTimeMillis());
                return response;
            } catch (Exception e) {
                plugin.getLogger().warning("Error in data provider " + endpoint.extensionName + "/" + endpoint.endpointName + ": " + e.getMessage());
                JsonObject error = new JsonObject();
                error.addProperty("error", "Extension error: " + e.getMessage());
                error.addProperty("status", 500);
                return error;
            }
        });
    }

    private void handleActionHandler(HttpExchange exchange, ExtensionEndpoint endpoint, String method, Map<String, String> params) throws IOException {
        ActionHandler handler = extensionLoader.getActionHandler(endpoint.extensionName, endpoint.endpointName);

        if (handler == null) {
            sendError(exchange, 404, "Extension action endpoint not found: " + endpoint.extensionName + "/" + endpoint.endpointName);
            return;
        }

        // Check if the method is supported by this handler using the interface method
        if (!isMethodSupported(handler, method)) {
            sendError(exchange, 405, "Method " + method + " not supported for this endpoint");
            return;
        }

        executeOnMainThread(exchange, () -> {
            try {
                JsonObject body = null;
                if ("POST".equals(method) || "PUT".equals(method)) {
                    String requestBody = readRequestBody(exchange);
                    if (!requestBody.isEmpty()) {
                        JsonParser parser = new JsonParser();
                        body = parser.parse(requestBody).getAsJsonObject();
                    }
                }

                JsonObject result = handler.handleAction(method, params, body);
                if (result == null) {
                    result = new JsonObject();
                    result.addProperty("success", true);
                }

                result.addProperty("extension", endpoint.extensionName);
                result.addProperty("endpoint", endpoint.endpointName);
                result.addProperty("method", method);

                return result;
            } catch (Exception e) {
                plugin.getLogger().warning("Error in action handler " + endpoint.extensionName + "/" + endpoint.endpointName + ": " + e.getMessage());
                JsonObject error = new JsonObject();
                error.addProperty("error", "Extension error: " + e.getMessage());
                error.addProperty("status", 500);
                return error;
            }
        });
    }

    /**
     * Check if the given method is supported by the ActionHandler
     */
    private boolean isMethodSupported(ActionHandler handler, String method) {
        String[] supportedMethods = handler.getSupportedMethods();
        for (String supportedMethod : supportedMethods) {
            if (supportedMethod.equalsIgnoreCase(method)) {
                return true;
            }
        }
        return false;
    }

    private record ExtensionEndpoint(String extensionName, String endpointName) {}
}