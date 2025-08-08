package fr.neocle.flexgateway.bukkit.server.handlers;

import fr.neocle.flexgateway.api.loader.ExtensionLoader;
import fr.neocle.flexgateway.bukkit.server.handlers.base.*;
import fr.neocle.flexgateway.bukkit.server.handlers.extension.DynamicExtensionHandler;
import fr.neocle.flexgateway.bukkit.server.handlers.extension.ExtensionsHandler;
import fr.neocle.flexgateway.bukkit.data.DataManager;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.plugin.java.JavaPlugin;

public class HandlerRegistry {
    private final JavaPlugin plugin;
    private final DataManager dataManager;
    private final ExtensionLoader extensionLoader;

    public HandlerRegistry(JavaPlugin plugin, DataManager dataManager, ExtensionLoader extensionLoader) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.extensionLoader = extensionLoader;
    }

    public void registerHandlers(HttpServer server) {
        // Base plugin endpoints
        server.createContext("/api/server", new ServerHandler(plugin, dataManager, extensionLoader));
        server.createContext("/api/players", new PlayersHandler(plugin, dataManager, extensionLoader));
        server.createContext("/api/player/", new PlayerHandler(plugin, dataManager, extensionLoader));
        server.createContext("/api/worlds", new WorldsHandler(plugin, dataManager, extensionLoader));
        server.createContext("/api/world/", new WorldHandler(plugin, dataManager, extensionLoader));
        server.createContext("/api/messages", new MessagesHandler(plugin, dataManager, extensionLoader));
        server.createContext("/api/send-message", new SendMessageHandler(plugin, dataManager, extensionLoader));
        server.createContext("/api/status", new StatusHandler(plugin, dataManager, extensionLoader));

        // Extension management
        server.createContext("/api/extensions", new ExtensionsHandler(plugin, dataManager, extensionLoader));

        // Dynamic extension endpoints
        server.createContext("/api/", new DynamicExtensionHandler(plugin, extensionLoader));
    }
}