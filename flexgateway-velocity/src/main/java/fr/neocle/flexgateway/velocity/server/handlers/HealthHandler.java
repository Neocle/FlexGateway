package fr.neocle.flexgateway.velocity.server.handlers;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.io.IOException;

public class HealthHandler extends BaseHandler {

    public HealthHandler(ProxyServer server, Logger logger) {
        super(server, logger);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        JsonObject health = new JsonObject();
        health.addProperty("status", "healthy");
        health.addProperty("timestamp", System.currentTimeMillis());
        health.addProperty("proxy_name", server.getVersion().getName());
        health.addProperty("proxy_version", server.getVersion().getVersion());
        health.addProperty("connected_players", server.getPlayerCount());
        health.addProperty("registered_servers", server.getAllServers().size());

        sendJsonResponse(exchange, 200, health);
    }
}