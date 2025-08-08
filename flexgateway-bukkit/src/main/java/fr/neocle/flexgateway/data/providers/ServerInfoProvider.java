package fr.neocle.flexgateway.data.providers;

import fr.neocle.flexgateway.data.collectors.SystemInfoCollector;
import com.google.gson.JsonObject;
import org.bukkit.plugin.java.JavaPlugin;
import fr.neocle.flexgateway.database.DatabaseManager;

public class ServerInfoProvider {

    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;
    private final SystemInfoCollector systemInfoCollector;

    public ServerInfoProvider(JavaPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.systemInfoCollector = new SystemInfoCollector();
    }

    public JsonObject getServerInfo() {
        JsonObject serverInfo = new JsonObject();

        addBasicServerInfo(serverInfo);
        addSystemInfo(serverInfo);
        addPerformanceInfo(serverInfo);
        addMessageStats(serverInfo);

        return serverInfo;
    }

    private void addBasicServerInfo(JsonObject serverInfo) {
        var server = plugin.getServer();
        serverInfo.addProperty("name", server.getName());
        serverInfo.addProperty("version", server.getVersion());
        serverInfo.addProperty("bukkitVersion", server.getBukkitVersion());
        serverInfo.addProperty("motd", server.getMotd());
        serverInfo.addProperty("maxPlayers", server.getMaxPlayers());
        serverInfo.addProperty("onlinePlayers", server.getOnlinePlayers().size());
        serverInfo.addProperty("viewDistance", server.getViewDistance());
        serverInfo.addProperty("allowFlight", server.getAllowFlight());
        serverInfo.addProperty("allowNether", server.getAllowNether());
        serverInfo.addProperty("allowEnd", server.getAllowEnd());
        serverInfo.addProperty("hardcoreMode", server.isHardcore());
        serverInfo.addProperty("onlineMode", server.getOnlineMode());
        serverInfo.addProperty("serverPort", server.getPort());
        serverInfo.addProperty("serverIp", server.getIp());
        serverInfo.addProperty("defaultGameMode", server.getDefaultGameMode().name());
    }

    private void addSystemInfo(JsonObject serverInfo) {
        serverInfo.add("memory", systemInfoCollector.getMemoryInfo());
    }

    private void addPerformanceInfo(JsonObject serverInfo) {
        serverInfo.addProperty("uptime", systemInfoCollector.getServerUptime());
        serverInfo.addProperty("tps", systemInfoCollector.getCurrentTPS());
    }

    private void addMessageStats(JsonObject serverInfo) {
        serverInfo.addProperty("totalMessages", databaseManager.getTotalMessageCount());
    }
}