package fr.neocle.flexgateway.bukkit.data.providers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class WorldDataProvider {

    private final JavaPlugin plugin;

    public WorldDataProvider(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public JsonArray getWorlds() {
        JsonArray worlds = new JsonArray();

        for (World world : plugin.getServer().getWorlds()) {
            worlds.add(createWorldSummary(world));
        }

        return worlds;
    }

    public JsonObject getWorldInfo(String worldName) {
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            return null;
        }

        return createDetailedWorldInfo(world);
    }

    private JsonObject createWorldSummary(World world) {
        JsonObject worldData = new JsonObject();

        addBasicWorldInfo(worldData, world);
        addWorldEnvironment(worldData, world);
        addWorldStats(worldData, world);
        addSimplePlayerList(worldData, world);

        return worldData;
    }

    private JsonObject createDetailedWorldInfo(World world) {
        JsonObject worldData = createWorldSummary(world);
        addDetailedPlayerList(worldData, world);
        return worldData;
    }

    private void addBasicWorldInfo(JsonObject worldData, World world) {
        worldData.addProperty("name", world.getName());
        worldData.addProperty("environment", world.getEnvironment().name());
        worldData.addProperty("worldType", world.getWorldType().name());
        worldData.addProperty("seed", world.getSeed());

        Location spawn = world.getSpawnLocation();
        worldData.addProperty("spawnX", spawn.getX());
        worldData.addProperty("spawnY", spawn.getY());
        worldData.addProperty("spawnZ", spawn.getZ());
    }

    private void addWorldEnvironment(JsonObject worldData, World world) {
        worldData.addProperty("difficulty", world.getDifficulty().name());
        worldData.addProperty("hardcore", world.isHardcore());
        worldData.addProperty("pvp", world.getPVP());
        worldData.addProperty("time", world.getTime());
        worldData.addProperty("fullTime", world.getFullTime());
        worldData.addProperty("hasStorm", world.hasStorm());
        worldData.addProperty("thundering", world.isThundering());
        worldData.addProperty("weatherDuration", world.getWeatherDuration());
        worldData.addProperty("thunderDuration", world.getThunderDuration());
        worldData.addProperty("keepSpawnInMemory", world.getKeepSpawnInMemory());
        worldData.addProperty("autoSave", world.isAutoSave());
    }

    private void addWorldStats(JsonObject worldData, World world) {
        worldData.addProperty("playersCount", world.getPlayers().size());
        worldData.addProperty("loadedChunks", world.getLoadedChunks().length);
        worldData.addProperty("entityCount", world.getEntities().size());
    }

    private void addSimplePlayerList(JsonObject worldData, World world) {
        JsonArray players = new JsonArray();
        for (Player player : world.getPlayers()) {
            players.add(player.getName());
        }
        worldData.add("players", players);
    }

    private void addDetailedPlayerList(JsonObject worldData, World world) {
        JsonArray players = new JsonArray();
        for (Player player : world.getPlayers()) {
            JsonObject playerInfo = new JsonObject();
            playerInfo.addProperty("name", player.getName());
            playerInfo.addProperty("uuid", player.getUniqueId().toString());

            Location loc = player.getLocation();
            playerInfo.addProperty("x", loc.getX());
            playerInfo.addProperty("y", loc.getY());
            playerInfo.addProperty("z", loc.getZ());

            players.add(playerInfo);
        }
        worldData.add("players", players);
    }
}