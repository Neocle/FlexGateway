package fr.neocle.flexgateway.velocity.communication;

import com.google.gson.JsonObject;
import fr.neocle.flexgateway.velocity.communication.TcpCommunicationClient;
import fr.neocle.flexgateway.velocity.config.ConfigManager;
import fr.neocle.flexgateway.velocity.data.ServerDataManager;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

public class TcpMessageManager {
    private final Logger logger;
    private final ServerDataManager dataManager;
    private final ConfigManager configManager;
    private final TcpCommunicationClient tcpClient;

    public TcpMessageManager(Logger logger, ServerDataManager dataManager, ConfigManager configManager) {
        this.logger = logger;
        this.dataManager = dataManager;
        this.configManager = configManager;
        this.tcpClient = new TcpCommunicationClient(logger);

        logger.info("TcpMessageManager initialized - using direct TCP communication");
        logger.info("No plugin messaging channels required");
        logger.info("Communication works regardless of online players");
    }

    public CompletableFuture<JsonObject> sendRequest(String serverName, String action, JsonObject requestData) {
        logger.info("=== TCP REQUEST TO SERVER ===");
        logger.info("Server: {}", serverName);
        logger.info("Action: {}", action);
        logger.info("Communication method: Direct TCP connection");

        // Get server connection info from config
        String serverHost = configManager.getServerHost(serverName);
        int serverPort = getServerTcpPort(serverName);

        logger.info("Target: {}:{}", serverHost, serverPort);

        return tcpClient.sendRequest(serverHost, serverPort, action, requestData)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        logger.error("TCP request failed for server {}: {}", serverName, throwable.getMessage());
                    } else {
                        logger.info("TCP request completed successfully for server {}", serverName);
                    }
                });
    }

    public int getServerTcpPort(String serverName) {
        // Get from config, with fallback to default mapping
        int configPort = configManager.getServerTcpPort(serverName);
        if (configPort > 0) {
            return configPort;
        }

        // Fallback mapping based on server name
        switch (serverName.toLowerCase()) {
            case "lobby":
                return 25599;
            case "minigames":
                return 25600;
            case "factions":
                return 25601;
            case "creative":
                return 25602;
            case "survival":
                return 25603;
            default:
                logger.warn("No TCP port configured for server '{}', using default 25599", serverName);
                return 25599;
        }
    }

    public void shutdown() {
        logger.info("TcpMessageManager shutdown complete");
        // TCP client doesn't need explicit cleanup as it uses per-request connections
    }
}