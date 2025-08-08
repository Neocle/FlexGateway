package fr.neocle.flexgateway.bukkit.data;

import fr.neocle.flexgateway.bukkit.data.providers.PlayerDataProvider;
import fr.neocle.flexgateway.bukkit.data.providers.ServerInfoProvider;
import fr.neocle.flexgateway.bukkit.data.providers.WorldDataProvider;
import fr.neocle.flexgateway.bukkit.data.services.MessageService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.plugin.java.JavaPlugin;
import fr.neocle.flexgateway.bukkit.database.DatabaseManager;

public class DataManager {

    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;
    private final ServerInfoProvider serverInfoProvider;
    private final PlayerDataProvider playerDataProvider;
    private final WorldDataProvider worldDataProvider;
    private final MessageService messageService;

    public DataManager(JavaPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.serverInfoProvider = new ServerInfoProvider(plugin, databaseManager);
        this.playerDataProvider = new PlayerDataProvider(plugin, databaseManager);
        this.worldDataProvider = new WorldDataProvider(plugin);
        this.messageService = new MessageService(plugin, databaseManager);
    }

    public JsonObject getServerInfo() {
        return serverInfoProvider.getServerInfo();
    }

    public JsonArray getOnlinePlayers() {
        return playerDataProvider.getOnlinePlayers();
    }

    public JsonObject getPlayerInfo(String identifier) {
        return playerDataProvider.getPlayerInfo(identifier);
    }

    public JsonArray getWorlds() {
        return worldDataProvider.getWorlds();
    }

    public JsonObject getWorldInfo(String worldName) {
        return worldDataProvider.getWorldInfo(worldName);
    }

    public JsonArray getMessages(int limit, int offset, String playerUuid, String messageType) {
        return messageService.getMessages(limit, offset, playerUuid, messageType);
    }

    public boolean sendMessage(String senderUuid, String recipientUuid, String message, String messageType) {
        return messageService.sendMessage(senderUuid, recipientUuid, message, messageType);
    }
}