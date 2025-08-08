package fr.neocle.flexgateway.data.collectors;

import com.google.gson.JsonObject;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerStatisticsCollector {
    private final JavaPlugin plugin;

    public PlayerStatisticsCollector(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public JsonObject getPlayerStatistics(Player player) {
        JsonObject statistics = new JsonObject();

        try {
            statistics.addProperty("playTime", player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20 / 3600.0);
            statistics.addProperty("walkDistance", player.getStatistic(Statistic.WALK_ONE_CM) / 100.0);
            statistics.addProperty("jumpCount", player.getStatistic(Statistic.JUMP));
            statistics.addProperty("damageDealt", player.getStatistic(Statistic.DAMAGE_DEALT) / 10.0);
            statistics.addProperty("damageTaken", player.getStatistic(Statistic.DAMAGE_TAKEN) / 10.0);
            statistics.addProperty("mobKills", player.getStatistic(Statistic.MOB_KILLS));
            statistics.addProperty("playerKills", player.getStatistic(Statistic.PLAYER_KILLS));
            statistics.addProperty("deaths", player.getStatistic(Statistic.DEATHS));
            statistics.addProperty("timeSinceRest", player.getStatistic(Statistic.TIME_SINCE_REST) / 20 / 3600.0);

            JsonObject blockStats = new JsonObject();
            blockStats.addProperty("blocksBroken", player.getStatistic(Statistic.MINE_BLOCK, Material.STONE));
            blockStats.addProperty("blocksPlaced", player.getStatistic(Statistic.USE_ITEM, Material.STONE));
            statistics.add("blocks", blockStats);

        } catch (Exception e) {
            plugin.getLogger().warning("Error retrieving player statistics: " + e.getMessage());
        }

        return statistics;
    }
}
