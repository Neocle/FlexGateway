package fr.neocle.flexgateway.data.providers;

import fr.neocle.flexgateway.data.collectors.OfflinePlayerDataCollector;
import fr.neocle.flexgateway.data.collectors.OnlinePlayerDataCollector;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import fr.neocle.flexgateway.database.DatabaseManager;
import java.util.UUID;

public class PlayerDataProvider {

    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;
    private final OnlinePlayerDataCollector onlinePlayerCollector;
    private final OfflinePlayerDataCollector offlinePlayerCollector;

    public PlayerDataProvider(JavaPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.onlinePlayerCollector = new OnlinePlayerDataCollector(databaseManager, plugin);
        this.offlinePlayerCollector = new OfflinePlayerDataCollector(plugin, databaseManager);
    }

    public JsonArray getOnlinePlayers() {
        JsonArray players = new JsonArray();

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            players.add(onlinePlayerCollector.collectPlayerData(player));
        }

        return players;
    }

    public JsonObject getPlayerInfo(String identifier) {
        Player onlinePlayer = findOnlinePlayer(identifier);

        if (onlinePlayer != null) {
            return onlinePlayerCollector.collectDetailedPlayerData(onlinePlayer);
        } else {
            return offlinePlayerCollector.collectOfflinePlayerData(identifier);
        }
    }

    private Player findOnlinePlayer(String identifier) {
        try {
            UUID uuid = UUID.fromString(identifier);
            return plugin.getServer().getPlayer(uuid);
        } catch (IllegalArgumentException e) {
            return plugin.getServer().getPlayer(identifier);
        }
    }
}