package fr.neocle.flexgateway.velocity.server.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.slf4j.Logger;

import java.io.IOException;

public class ServersListHandler extends BaseHandler {

    public ServersListHandler(ProxyServer server, Logger logger) {
        super(server, logger);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        JsonObject response = new JsonObject();
        JsonArray serversArray = new JsonArray();

        for (RegisteredServer registeredServer : server.getAllServers()) {
            JsonObject serverObj = new JsonObject();
            serverObj.addProperty("name", registeredServer.getServerInfo().getName());
            serverObj.addProperty("address", registeredServer.getServerInfo().getAddress().toString());
            serverObj.addProperty("online_players", registeredServer.getPlayersConnected().size());

            try {
                registeredServer.ping().thenAccept(ping -> {
                    // This is async, so we can't include ping info in the immediate response
                });
            } catch (Exception ignored) {
                // Ping failed, server might be offline
            }

            serversArray.add(serverObj);
        }

        response.add("servers", serversArray);
        response.addProperty("count", serversArray.size());
        response.addProperty("proxy_name", server.getVersion().getName());
        response.addProperty("proxy_version", server.getVersion().getVersion());

        sendJsonResponse(exchange, 200, response);
    }
}