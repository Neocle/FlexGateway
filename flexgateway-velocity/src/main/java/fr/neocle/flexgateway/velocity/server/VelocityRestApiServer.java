package fr.neocle.flexgateway.velocity.server;

import com.sun.net.httpserver.HttpServer;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexgateway.velocity.data.ServerDataManager;
import fr.neocle.flexgateway.velocity.communication.TcpMessageManager;
import fr.neocle.flexgateway.velocity.server.handlers.ServerApiHandler;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class VelocityRestApiServer {
    private final ProxyServer server;
    private final Logger logger;
    private final ServerDataManager dataManager;
    private final TcpMessageManager messageManager;
    private final String host;
    private final int port;
    private HttpServer httpServer;

    public VelocityRestApiServer(ProxyServer server, Logger logger, ServerDataManager dataManager,
                                 TcpMessageManager messageManager, String host, int port) {
        this.server = server;
        this.logger = logger;
        this.dataManager = dataManager;
        this.messageManager = messageManager;
        this.host = host;
        this.port = port;
    }

    public void start() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(host, port), 0);

        // Register handlers
        httpServer.createContext("/api/", new ServerApiHandler(server, logger, dataManager, messageManager));
        httpServer.createContext("/api/servers", new fr.neocle.flexgateway.velocity.server.handlers.ServersListHandler(server, logger));
        httpServer.createContext("/health", new fr.neocle.flexgateway.velocity.server.handlers.HealthHandler(server, logger));

        httpServer.setExecutor(Executors.newFixedThreadPool(10));
        httpServer.start();

        logger.info("FlexGateway Velocity REST API server started on {}:{}", host, port);
        logAvailableEndpoints();
    }

    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
            logger.info("HTTP server stopped");
        }
    }

    private void logAvailableEndpoints() {
        logger.info("=== AVAILABLE ENDPOINTS ===");
        logger.info("  - GET  /api/{server-name}/{endpoint} (proxy server data via TCP)");
        logger.info("  - GET  /api/servers (list of connected servers)");
        logger.info("  - GET  /health (proxy health check)");
        logger.info("  - GET  /debug/tcp (TCP communication debug info)");
        logger.info("Example: GET /api/lobby/players");
        logger.info("         GET /api/lobby/server");
    }
}