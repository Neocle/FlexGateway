package fr.neocle.flexgateway.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexgateway.velocity.config.ConfigManager;
import fr.neocle.flexgateway.velocity.communication.TcpMessageManager;
import fr.neocle.flexgateway.velocity.server.VelocityRestApiServer;
import fr.neocle.flexgateway.velocity.data.ServerDataManager;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;

@Plugin(
        id = "flexgateway-velocity",
        name = "FlexGateway Velocity",
        version = "1.0.0",
        description = "FlexGateway proxy integration for Velocity",
        authors = {"Neocle"}
)
public class FlexGatewayVelocity {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private ConfigManager configManager;
    private TcpMessageManager messageManager;
    private ServerDataManager dataManager;
    private VelocityRestApiServer apiServer;

    @Inject
    public FlexGatewayVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = Paths.get("plugins", "FlexGateway");
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("Starting FlexGateway Velocity plugin...");

        try {
            // Initialize configuration
            configManager = new ConfigManager(dataDirectory, logger);
            configManager.loadConfig();

            // Initialize data manager
            dataManager = new ServerDataManager(logger);

            // Initialize TCP message manager (no plugin messaging needed)
            messageManager = new TcpMessageManager(logger, dataManager, configManager);

            // Start REST API server
            String host = configManager.getApiHost();
            int port = configManager.getApiPort();

            apiServer = new VelocityRestApiServer(server, logger, dataManager, messageManager, host, port);

            apiServer.start();
            logger.info("FlexGateway Velocity REST API server started on {}:{}", host, port);

            // Log server communication info
            logServerCommunicationInfo();

            logger.info("FlexGateway Velocity plugin initialized successfully");

        } catch (Exception e) {
            logger.error("Failed to initialize FlexGateway Velocity plugin", e);
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("Shutting down FlexGateway Velocity plugin...");

        if (apiServer != null) {
            try {
                apiServer.stop();
                logger.info("REST API server stopped");
            } catch (Exception e) {
                logger.error("Error stopping REST API server", e);
            }
        }

        if (messageManager != null) {
            messageManager.shutdown();
        }

        if (dataManager != null) {
            dataManager.shutdown();
        }

        logger.info("FlexGateway Velocity plugin shutdown complete");
    }

    private void logServerCommunicationInfo() {
        logger.info("=== SERVER COMMUNICATION INFO ===");
        logger.info("Communication method: TCP (direct connection)");
        logger.info("Registered servers and their TCP ports:");

        for (var registeredServer : server.getAllServers()) {
            String serverName = registeredServer.getServerInfo().getName();
            int tcpPort = messageManager.getServerTcpPort(serverName);
            logger.info("  - {} -> TCP port {}", serverName, tcpPort);
        }

        logger.info("Benefits: Works without players online, more reliable");
    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ServerDataManager getDataManager() {
        return dataManager;
    }

    public TcpMessageManager getMessageManager() {
        return messageManager;
    }
}