package fr.neocle.flexgateway.bukkit.communication;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.neocle.flexgateway.FlexGatewayBukkit;
import fr.neocle.flexgateway.api.loader.ExtensionLoader;
import fr.neocle.flexgateway.bukkit.data.DataManager;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;

public class TcpCommunicationServer {
    private final FlexGatewayBukkit plugin;
    private final DataManager dataManager;
    private final ExtensionLoader extensionLoader;
    private final JsonParser jsonParser = new JsonParser();

    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private volatile boolean running = false;
    private final int port;

    public TcpCommunicationServer(FlexGatewayBukkit plugin, DataManager dataManager, ExtensionLoader extensionLoader, int port) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.extensionLoader = extensionLoader;
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(10);
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;

        plugin.getLogger().info("TCP Communication Server started on port: " + port);

        // Accept connections in a separate thread
        threadPool.submit(() -> {
            while (running && !serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    plugin.getLogger().info("TCP connection accepted from: " + clientSocket.getRemoteSocketAddress());

                    // Handle each connection in a separate thread
                    threadPool.submit(() -> handleConnection(clientSocket));

                } catch (IOException e) {
                    if (running) {
                        plugin.getLogger().warning("Error accepting TCP connection: " + e.getMessage());
                    }
                }
            }
        });
    }

    public void stop() {
        running = false;

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Error closing TCP server socket: " + e.getMessage());
        }

        if (threadPool != null) {
            threadPool.shutdown();
        }

        plugin.getLogger().info("TCP Communication Server stopped");
    }

    private void handleConnection(Socket clientSocket) {
        plugin.getLogger().info("=== TCP CONNECTION HANDLER START ===");
        plugin.getLogger().info("Client: " + clientSocket.getRemoteSocketAddress());

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            plugin.getLogger().info("✅ TCP streams created successfully");

            String requestLine = reader.readLine();
            if (requestLine == null) {
                plugin.getLogger().warning("❌ Received null request from TCP client");
                return;
            }

            plugin.getLogger().info("✅ TCP Request received: " + requestLine);

            try {
                JsonObject request = jsonParser.parse(requestLine).getAsJsonObject();
                String action = request.get("action").getAsString();
                String requestId = request.get("requestId").getAsString();
                JsonObject requestData = request.get("data").getAsJsonObject();

                plugin.getLogger().info("✅ Request parsed - Action: " + action + ", ID: " + requestId);

                // Use CountDownLatch to wait for main thread processing
                final CountDownLatch latch = new CountDownLatch(1);
                final JsonObject[] responseHolder = new JsonObject[1];
                final Exception[] exceptionHolder = new Exception[1];

                // Process the request on the main thread
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    try {
                        plugin.getLogger().info("📋 Processing request on main thread...");
                        JsonObject response = handleRequest(action, requestData);
                        response.addProperty("requestId", requestId);
                        response.addProperty("success", true);
                        response.addProperty("timestamp", System.currentTimeMillis());

                        responseHolder[0] = response;
                        plugin.getLogger().info("✅ Request processed successfully on main thread");

                    } catch (Exception e) {
                        plugin.getLogger().severe("❌ Error processing TCP request: " + e.getMessage());
                        e.printStackTrace();
                        exceptionHolder[0] = e;
                    } finally {
                        latch.countDown();
                    }
                });

                // Wait for main thread processing (with timeout)
                plugin.getLogger().info("⏳ Waiting for main thread processing...");
                if (latch.await(30, java.util.concurrent.TimeUnit.SECONDS)) {
                    plugin.getLogger().info("✅ Main thread processing completed");

                    if (exceptionHolder[0] != null) {
                        // Handle exception
                        JsonObject errorResponse = new JsonObject();
                        errorResponse.addProperty("requestId", requestId);
                        errorResponse.addProperty("success", false);
                        errorResponse.addProperty("error", "Internal server error: " + exceptionHolder[0].getMessage());
                        errorResponse.addProperty("status", 500);
                        errorResponse.addProperty("timestamp", System.currentTimeMillis());

                        String errorJson = errorResponse.toString();
                        plugin.getLogger().info("📤 Sending error response: " + errorJson.length() + " characters");
                        writer.println(errorJson);
                        writer.flush(); // Ensure it's sent
                        plugin.getLogger().info("❌ Error response sent and flushed");

                    } else if (responseHolder[0] != null) {
                        // Send successful response
                        String responseJson = responseHolder[0].toString();
                        plugin.getLogger().info("📤 Sending response: " + responseJson.length() + " characters");
                        plugin.getLogger().info("Response preview: " + (responseJson.length() > 100 ? responseJson.substring(0, 100) + "..." : responseJson));

                        writer.println(responseJson);
                        writer.flush(); // Ensure it's sent
                        plugin.getLogger().info("✅ Response sent and flushed");

                        // Small delay to ensure data is transmitted
                        Thread.sleep(100);
                        plugin.getLogger().info("✅ Response transmission delay completed");
                    }
                } else {
                    plugin.getLogger().severe("⏰ Main thread processing timed out!");
                    JsonObject timeoutResponse = new JsonObject();
                    timeoutResponse.addProperty("requestId", requestId);
                    timeoutResponse.addProperty("success", false);
                    timeoutResponse.addProperty("error", "Request processing timed out");
                    timeoutResponse.addProperty("status", 408);
                    timeoutResponse.addProperty("timestamp", System.currentTimeMillis());

                    writer.println(timeoutResponse.toString());
                    writer.flush();
                    plugin.getLogger().info("⏰ Timeout response sent");
                }

            } catch (Exception e) {
                plugin.getLogger().severe("❌ Error parsing TCP request: " + e.getMessage());
                e.printStackTrace();

                JsonObject errorResponse = new JsonObject();
                errorResponse.addProperty("success", false);
                errorResponse.addProperty("error", "Invalid request format: " + e.getMessage());
                errorResponse.addProperty("status", 400);
                errorResponse.addProperty("timestamp", System.currentTimeMillis());

                writer.println(errorResponse.toString());
                writer.flush();
                plugin.getLogger().info("❌ Parse error response sent");
            }

        } catch (IOException e) {
            plugin.getLogger().warning("❌ Error handling TCP connection: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (!clientSocket.isClosed()) {
                    clientSocket.close();
                    plugin.getLogger().info("🔌 TCP client socket closed");
                }
            } catch (IOException e) {
                plugin.getLogger().warning("❌ Error closing TCP client socket: " + e.getMessage());
            }
            plugin.getLogger().info("=== TCP CONNECTION HANDLER END ===");
        }
    }

    private JsonObject handleRequest(String action, JsonObject requestData) {
        plugin.getLogger().info("=== HANDLING TCP REQUEST ===");
        plugin.getLogger().info("Action: " + action);

        java.util.Map<String, String> params = extractParams(requestData);
        plugin.getLogger().info("Extracted " + params.size() + " parameters");

        switch (action) {
            case "server":
                plugin.getLogger().info("Handling server info request");
                return handleServerRequest(params);
            case "players":
                plugin.getLogger().info("Handling players list request");
                return handlePlayersRequest(params);
            case "player":
                return handlePlayerRequest(requestData.get("identifier").getAsString(), params);
            case "worlds":
                return handleWorldsRequest(params);
            case "world":
                return handleWorldRequest(requestData.get("worldName").getAsString(), params);
            case "status":
                return handleStatusRequest(params);
            case "extensions":
                return handleExtensionsRequest();
            default:
                plugin.getLogger().warning("Unknown action received: " + action);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Unknown action: " + action);
                error.addProperty("status", 400);
                return error;
        }
    }

    private JsonObject handleServerRequest(java.util.Map<String, String> params) {
        plugin.getLogger().info("Getting server info...");
        try {
            JsonObject serverInfo = dataManager.getServerInfo();
            plugin.getLogger().info("Server info retrieved successfully");
            return extensionLoader.enhanceBaseEndpoint("server", serverInfo, null, params);
        } catch (Exception e) {
            plugin.getLogger().severe("Error getting server info: " + e.getMessage());
            e.printStackTrace();
            JsonObject error = new JsonObject();
            error.addProperty("error", "Failed to get server info: " + e.getMessage());
            error.addProperty("status", 500);
            return error;
        }
    }

    private JsonObject handlePlayersRequest(java.util.Map<String, String> params) {
        plugin.getLogger().info("Getting players list...");
        try {
            JsonObject response = new JsonObject();
            response.add("players", dataManager.getOnlinePlayers());
            response.addProperty("count", dataManager.getOnlinePlayers().size());
            plugin.getLogger().info("Players list retrieved successfully - count: " + dataManager.getOnlinePlayers().size());
            return extensionLoader.enhanceBaseEndpoint("players", response, null, params);
        } catch (Exception e) {
            plugin.getLogger().severe("Error getting players list: " + e.getMessage());
            e.printStackTrace();
            JsonObject error = new JsonObject();
            error.addProperty("error", "Failed to get players list: " + e.getMessage());
            error.addProperty("status", 500);
            return error;
        }
    }

    private JsonObject handlePlayerRequest(String identifier, java.util.Map<String, String> params) {
        plugin.getLogger().info("Getting player info for: " + identifier);
        JsonObject playerInfo = dataManager.getPlayerInfo(identifier);
        if (playerInfo == null) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "Player not found");
            error.addProperty("status", 404);
            return error;
        }
        return extensionLoader.enhanceBaseEndpoint("player", playerInfo, identifier, params);
    }

    private JsonObject handleWorldsRequest(java.util.Map<String, String> params) {
        plugin.getLogger().info("Getting worlds list...");
        JsonObject response = new JsonObject();
        response.add("worlds", dataManager.getWorlds());
        response.addProperty("count", dataManager.getWorlds().size());
        return extensionLoader.enhanceBaseEndpoint("worlds", response, null, params);
    }

    private JsonObject handleWorldRequest(String worldName, java.util.Map<String, String> params) {
        plugin.getLogger().info("Getting world info for: " + worldName);
        JsonObject worldInfo = dataManager.getWorldInfo(worldName);
        if (worldInfo == null) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "World not found");
            error.addProperty("status", 404);
            return error;
        }
        return extensionLoader.enhanceBaseEndpoint("world", worldInfo, worldName, params);
    }

    private JsonObject handleStatusRequest(java.util.Map<String, String> params) {
        plugin.getLogger().info("Getting status...");
        JsonObject status = new JsonObject();
        status.addProperty("status", "online");
        status.addProperty("timestamp", System.currentTimeMillis());
        status.addProperty("api_version", "1.0.0");
        status.addProperty("server_name", plugin.getServer().getName());
        status.addProperty("online_players", plugin.getServer().getOnlinePlayers().size());
        status.addProperty("max_players", plugin.getServer().getMaxPlayers());
        status.addProperty("extensions_loaded", extensionLoader.getExtensions().size());
        status.addProperty("mode", "proxy");
        status.addProperty("communication", "tcp");

        return extensionLoader.enhanceBaseEndpoint("status", status, null, params);
    }

    private JsonObject handleExtensionsRequest() {
        plugin.getLogger().info("Getting extensions info...");
        return extensionLoader.getExtensionsInfo();
    }

    private java.util.Map<String, String> extractParams(JsonObject request) {
        java.util.Map<String, String> params = new java.util.HashMap<>();
        if (request.has("params")) {
            JsonObject paramsObj = request.get("params").getAsJsonObject();
            for (String key : paramsObj.keySet()) {
                params.put(key, paramsObj.get(key).getAsString());
            }
        }
        return params;
    }

    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return port;
    }
}