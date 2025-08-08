package fr.neocle.flexgateway.data.collectors;

import fr.neocle.flexgateway.utils.TimeUtils;
import com.google.gson.JsonObject;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import fr.neocle.flexgateway.database.DatabaseManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class OfflinePlayerDataCollector {

    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final OfflinePlayerFileReader fileReader;

    public OfflinePlayerDataCollector(JavaPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.fileReader = new OfflinePlayerFileReader(plugin);
    }

    public JsonObject collectOfflinePlayerData(String identifier) {
        OfflinePlayer offlinePlayer = findOfflinePlayer(identifier);

        if (offlinePlayer == null || !offlinePlayer.hasPlayedBefore()) {
            return null;
        }

        JsonObject playerData = new JsonObject();

        addBasicOfflineInfo(playerData, offlinePlayer);
        addOfflineLocation(playerData, offlinePlayer);
        addMessageCounts(playerData, offlinePlayer);
        addOfflineGameData(playerData, offlinePlayer);

        return playerData;
    }

    private OfflinePlayer findOfflinePlayer(String identifier) {
        try {
            UUID uuid = UUID.fromString(identifier);
            return plugin.getServer().getOfflinePlayer(uuid);
        } catch (IllegalArgumentException e) {
            return plugin.getServer().getOfflinePlayer(identifier);
        }
    }

    private void addBasicOfflineInfo(JsonObject playerData, OfflinePlayer offlinePlayer) {
        playerData.addProperty("online", false);
        playerData.addProperty("uuid", offlinePlayer.getUniqueId().toString());
        playerData.addProperty("name", offlinePlayer.getName());
        playerData.addProperty("op", offlinePlayer.isOp());
        playerData.addProperty("banned", offlinePlayer.isBanned());
        playerData.addProperty("whitelisted", offlinePlayer.isWhitelisted());
        playerData.addProperty("firstPlayed", dateFormat.format(new Date(offlinePlayer.getFirstPlayed())));
        playerData.addProperty("lastPlayed", dateFormat.format(new Date(offlinePlayer.getLastPlayed())));
        playerData.addProperty("lastSeen", TimeUtils.getLastSeenTime(offlinePlayer));
    }

    private void addOfflineLocation(JsonObject playerData, OfflinePlayer offlinePlayer) {
        JsonObject location = fileReader.getOfflinePlayerLocation(offlinePlayer);
        if (location != null) {
            playerData.add("lastLocation", location);
        }
    }

    private void addMessageCounts(JsonObject playerData, OfflinePlayer offlinePlayer) {
        String playerUuid = offlinePlayer.getUniqueId().toString();
        playerData.addProperty("totalMessages", databaseManager.getPlayerMessageCount(playerUuid));
        playerData.addProperty("sentMessages", databaseManager.getPlayerSentMessageCount(playerUuid));
    }

    private void addOfflineGameData(JsonObject playerData, OfflinePlayer offlinePlayer) {
        playerData.add("achievements", fileReader.getOfflinePlayerAchievements(offlinePlayer));
        playerData.add("statistics", fileReader.getOfflinePlayerStatistics(offlinePlayer));
    }
}
