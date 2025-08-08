package fr.neocle.flexgateway.velocity.server.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.slf4j.Logger;

import java.io.IOException;

public class DebugHandler extends BaseHandler {

    public DebugHandler(ProxyServer server, Logger logger) {
        super(server, logger);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        JsonObject debug = new JsonObject();
        debug.addProperty("proxy_name", server.getVersion().getName());
        debug.addProperty("proxy_version", server.getVersion().getVersion());
        debug.addProperty("total_players", server.getPlayerCount());

        JsonArray serversArray = new JsonArray();
        for (RegisteredServer registeredServer : server.getAllServers()) {
            JsonObject serverObj = new JsonObject();
            serverObj.addProperty("name", registeredServer.getServerInfo().getName());
            serverObj.addProperty("address", registeredServer.getServerInfo().getAddress().toString());
            serverObj.addProperty("players_connected", registeredServer.getPlayersConnected().size());

            JsonArray playersArray = new JsonArray();
            registeredServer.getPlayersConnected().forEach(player ->
                    playersArray.add(player.getUsername()));
            serverObj.add("players", playersArray);

            serversArray.add(serverObj);
        }

        debug.add("servers", serversArray);
        debug.addProperty("timestamp", System.currentTimeMillis());

        sendJsonResponse(exchange, 200, debug);
    }
}