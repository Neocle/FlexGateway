package fr.neocle.flexgateway.server.handlers.base;

import fr.neocle.flexgateway.api.loader.ExtensionLoader;
import fr.neocle.flexgateway.data.DataManager;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.Map;

public class WorldHandler extends EnhancedBaseHandler {
    public WorldHandler(JavaPlugin plugin, DataManager dataManager, ExtensionLoader extensionLoader) {
        super(plugin, dataManager, extensionLoader);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String worldName = path.substring("/api/world/".length());

        if (worldName.isEmpty()) {
            sendError(exchange, 400, "World name required");
            return;
        }

        Map<String, String> params = parseQueryParameters(exchange.getRequestURI().getQuery());

        executeOnMainThread(exchange, () -> {
            JsonObject worldInfo = dataManager.getWorldInfo(worldName);
            if (worldInfo == null) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "World not found");
                error.addProperty("status", 404);
                return error;
            }
            // Enhance with extensions
            return extensionLoader.enhanceBaseEndpoint("world", worldInfo, worldName, params);
        });
    }
}
