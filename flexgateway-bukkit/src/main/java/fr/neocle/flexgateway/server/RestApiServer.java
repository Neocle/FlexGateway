package fr.neocle.flexgateway.server;

import fr.neocle.flexgateway.api.loader.ExtensionLoader;
import fr.neocle.flexgateway.server.handlers.HandlerRegistry;
import fr.neocle.flexgateway.data.DataManager;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class RestApiServer {
    private final JavaPlugin plugin;
    private final String host;
    private final int port;
    private final HandlerRegistry handlerRegistry;
    private HttpServer server;

    public RestApiServer(JavaPlugin plugin, DataManager dataManager, ExtensionLoader extensionLoader, String host, int port) {
        this.plugin = plugin;
        this.host = host;
        this.port = port;
        this.handlerRegistry = new HandlerRegistry(plugin, dataManager, extensionLoader);
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(host, port), 0);

        // Register all handlers
        handlerRegistry.registerHandlers(server);

        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        plugin.getLogger().info("RestApiServer started with enhanced extension support");
        logAvailableEndpoints();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void logAvailableEndpoints() {
        plugin.getLogger().info("Available endpoints:");
        plugin.getLogger().info("  - /api/{extension}/{endpoint} (extension endpoints)");
        plugin.getLogger().info("  - Base endpoints enhanced by extensions");
    }
}