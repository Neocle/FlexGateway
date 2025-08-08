package fr.neocle.flexgateway.velocity.communication;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

public class TcpCommunicationClient {
    private final Logger logger;
    private final JsonParser jsonParser = new JsonParser();

    public TcpCommunicationClient(Logger logger) {
        this.logger = logger;
    }

    public CompletableFuture<JsonObject> sendRequest(String serverHost, int serverPort, String action, JsonObject requestData) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        // Set timeout
        future.orTimeout(30, TimeUnit.SECONDS);

        // Process in separate thread to avoid blocking
        CompletableFuture.runAsync(() -> {
            try (Socket socket = new Socket(serverHost, serverPort);
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

                String requestId = UUID.randomUUID().toString();

                // Create request
                JsonObject request = new JsonObject();
                request.addProperty("action", action);
                request.addProperty("requestId", requestId);
                request.add("data", requestData);

                logger.info("=== SENDING TCP REQUEST ===");
                logger.info("Server: {}:{}", serverHost, serverPort);
                logger.info("Action: {}", action);
                logger.info("Request ID: {}", requestId);

                // Send request
                writer.println(request.toString());
                logger.info("✅ TCP request sent");

                // Read response
                String responseLine = reader.readLine();
                if (responseLine == null) {
                    throw new IOException("No response received from server");
                }

                logger.info("✅ TCP response received: {} characters", responseLine.length());

                JsonObject response = jsonParser.parse(responseLine).getAsJsonObject();

                // Verify request ID matches
                if (response.has("requestId") && !requestId.equals(response.get("requestId").getAsString())) {
                    throw new IOException("Response request ID mismatch");
                }

                future.complete(response);
                logger.info("✅ TCP request completed successfully");

            } catch (Exception e) {
                logger.error("❌ TCP request failed: {}", e.getMessage());
                JsonObject error = new JsonObject();
                error.addProperty("error", "TCP communication failed: " + e.getMessage());
                error.addProperty("status", 500);
                future.complete(error);
            }
        });

        return future;
    }
}