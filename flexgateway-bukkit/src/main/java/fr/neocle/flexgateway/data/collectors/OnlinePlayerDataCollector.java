package fr.neocle.flexgateway.data.collectors;

import fr.neocle.flexgateway.utils.TimeUtils;
import com.google.gson.JsonObject;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import fr.neocle.flexgateway.database.DatabaseManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.SimpleDateFormat;
import java.util.Date;

public class OnlinePlayerDataCollector {

    private final DatabaseManager databaseManager;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final PlayerAchievementCollector achievementCollector;
    private final PlayerStatisticsCollector statisticsCollector;

    public OnlinePlayerDataCollector(DatabaseManager databaseManager, JavaPlugin plugin) {
        this.databaseManager = databaseManager;
        this.achievementCollector = new PlayerAchievementCollector(plugin);
        this.statisticsCollector = new PlayerStatisticsCollector(plugin);
    }

    public JsonObject collectPlayerData(Player player) {
        JsonObject playerData = new JsonObject();

        addBasicPlayerInfo(playerData, player);
        addPlayerLocation(playerData, player);
        addPlayerTimes(playerData, player);
        addMessageCounts(playerData, player);

        return playerData;
    }

    public JsonObject collectDetailedPlayerData(Player player) {
        JsonObject playerData = collectPlayerData(player);

        playerData.addProperty("online", true);
        playerData.add("achievements", achievementCollector.getPlayerAchievements(player));
        playerData.add("statistics", statisticsCollector.getPlayerStatistics(player));

        return playerData;
    }

    private void addBasicPlayerInfo(JsonObject playerData, Player player) {
        playerData.addProperty("uuid", player.getUniqueId().toString());
        playerData.addProperty("name", player.getName());
        playerData.addProperty("displayName", player.getDisplayName());
        playerData.addProperty("level", player.getLevel());
        playerData.addProperty("exp", player.getExp());
        playerData.addProperty("totalExperience", player.getTotalExperience());
        playerData.addProperty("health", player.getHealth());
        playerData.addProperty("maxHealth", player.getMaxHealth());
        playerData.addProperty("foodLevel", player.getFoodLevel());
        playerData.addProperty("gameMode", player.getGameMode().name());
        playerData.addProperty("flying", player.isFlying());
        playerData.addProperty("sneaking", player.isSneaking());
        playerData.addProperty("sprinting", player.isSprinting());
        playerData.addProperty("op", player.isOp());
    }

    private void addPlayerLocation(JsonObject playerData, Player player) {
        Location loc = player.getLocation();
        JsonObject location = new JsonObject();
        location.addProperty("world", loc.getWorld().getName());
        location.addProperty("x", loc.getX());
        location.addProperty("y", loc.getY());
        location.addProperty("z", loc.getZ());
        location.addProperty("pitch", loc.getPitch());
        location.addProperty("yaw", loc.getYaw());
        playerData.add("location", location);
    }

    private void addPlayerTimes(JsonObject playerData, Player player) {
        playerData.addProperty("firstPlayed", dateFormat.format(new Date(player.getFirstPlayed())));
        playerData.addProperty("lastPlayed", dateFormat.format(new Date(player.getLastPlayed())));
        playerData.addProperty("playTime", TimeUtils.formatPlayTime(player));
    }

    private void addMessageCounts(JsonObject playerData, Player player) {
        String playerUuid = player.getUniqueId().toString();
        playerData.addProperty("totalMessages", databaseManager.getPlayerMessageCount(playerUuid));
        playerData.addProperty("sentMessages", databaseManager.getPlayerSentMessageCount(playerUuid));
    }
}