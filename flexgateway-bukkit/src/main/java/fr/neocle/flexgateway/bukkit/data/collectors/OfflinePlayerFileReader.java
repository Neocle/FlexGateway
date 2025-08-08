package fr.neocle.flexgateway.bukkit.data.collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.OfflinePlayer;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.plugin.java.JavaPlugin;

public class OfflinePlayerFileReader {
    private final JavaPlugin plugin;

    public OfflinePlayerFileReader(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public JsonObject getOfflinePlayerAchievements(OfflinePlayer offlinePlayer) {
        JsonObject achievements = new JsonObject();

        try {
            File worldContainer = plugin.getServer().getWorldContainer();
            File advancementsFolder = new File(worldContainer, "world/advancements");
            File advancementsFile = new File(advancementsFolder, offlinePlayer.getUniqueId().toString() + ".json");

            if (advancementsFile.exists()) {
                String content = new String(Files.readAllBytes(advancementsFile.toPath()));
                achievements = parseAdvancementsFromJSON(content);
            } else {
                achievements.addProperty("completedCount", 0);
                achievements.addProperty("totalCount", 0);
                achievements.addProperty("completionPercentage", 0);
                achievements.add("completed", new JsonArray());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error reading offline player achievements: " + e.getMessage());
            achievements.addProperty("error", "Could not load achievements");
        }

        return achievements;
    }

    public JsonObject getOfflinePlayerStatistics(OfflinePlayer offlinePlayer) {
        JsonObject statistics = new JsonObject();

        try {
            File worldContainer = plugin.getServer().getWorldContainer();
            File statsFolder = new File(worldContainer, "world/stats");
            File statsFile = new File(statsFolder, offlinePlayer.getUniqueId().toString() + ".json");

            if (statsFile.exists()) {
                String content = new String(Files.readAllBytes(statsFile.toPath()));
                statistics = parseStatisticsFromJSON(content);
            } else {
                statistics.addProperty("playTime", 0);
                statistics.addProperty("error", "No statistics found");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error reading offline player statistics: " + e.getMessage());
            statistics.addProperty("error", "Could not load statistics");
        }

        return statistics;
    }

    private JsonObject parseStatisticsFromJSON(String jsonContent) {
        JsonObject statistics = new JsonObject();

        try {
            com.google.gson.JsonParser parser = new com.google.gson.JsonParser();
            JsonObject root = parser.parse(jsonContent).getAsJsonObject();

            if (root.has("stats")) {
                JsonObject stats = root.get("stats").getAsJsonObject();

                if (stats.has("minecraft:custom")) {
                    JsonObject custom = stats.get("minecraft:custom").getAsJsonObject();

                    if (custom.has("minecraft:play_time")) {
                        int playTimeTicks = custom.get("minecraft:play_time").getAsInt();
                        statistics.addProperty("playTime", playTimeTicks / 20.0 / 3600.0);
                    }

                    if (custom.has("minecraft:walk_one_cm")) {
                        statistics.addProperty("walkDistance", custom.get("minecraft:walk_one_cm").getAsInt() / 100.0);
                    }

                    if (custom.has("minecraft:jump")) {
                        statistics.addProperty("jumpCount", custom.get("minecraft:jump").getAsInt());
                    }

                    if (custom.has("minecraft:damage_dealt")) {
                        statistics.addProperty("damageDealt", custom.get("minecraft:damage_dealt").getAsInt() / 10.0);
                    }

                    if (custom.has("minecraft:damage_taken")) {
                        statistics.addProperty("damageTaken", custom.get("minecraft:damage_taken").getAsInt() / 10.0);
                    }

                    if (custom.has("minecraft:mob_kills")) {
                        statistics.addProperty("mobKills", custom.get("minecraft:mob_kills").getAsInt());
                    }

                    if (custom.has("minecraft:player_kills")) {
                        statistics.addProperty("playerKills", custom.get("minecraft:player_kills").getAsInt());
                    }

                    if (custom.has("minecraft:deaths")) {
                        statistics.addProperty("deaths", custom.get("minecraft:deaths").getAsInt());
                    }
                }
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Error parsing statistics JSON: " + e.getMessage());
        }

        return statistics;
    }

    private JsonObject parseAdvancementsFromJSON(String jsonContent) {
        JsonObject achievements = new JsonObject();
        JsonArray completed = new JsonArray();

        try {
            com.google.gson.JsonParser parser = new com.google.gson.JsonParser();
            JsonObject root = parser.parse(jsonContent).getAsJsonObject();

            int completedCount = 0;

            for (Map.Entry<String, com.google.gson.JsonElement> entry : root.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("minecraft:") && !key.contains("recipes/") && entry.getValue().isJsonObject()) {
                    JsonObject advancement = entry.getValue().getAsJsonObject();
                    if (advancement.has("done") && advancement.get("done").getAsBoolean()) {
                        JsonObject advObj = new JsonObject();
                        String cleanKey = key.replace("minecraft:", "");
                        advObj.addProperty("key", cleanKey);
                        advObj.addProperty("title", getReadableTitle(cleanKey));

                        if (advancement.has("criteria")) {
                            JsonObject criteria = advancement.get("criteria").getAsJsonObject();
                            for (Map.Entry<String, com.google.gson.JsonElement> criterion : criteria.entrySet()) {
                                if (criterion.getValue().isJsonPrimitive()) {
                                    advObj.addProperty("completedDate", criterion.getValue().getAsString());
                                    break;
                                }
                            }
                        }

                        completed.add(advObj);
                        completedCount++;
                    }
                }
            }

            achievements.addProperty("completedCount", completedCount);
            achievements.add("completed", completed);

        } catch (Exception e) {
            plugin.getLogger().warning("Error parsing advancements JSON: " + e.getMessage());
        }

        return achievements;
    }

    public JsonObject getOfflinePlayerLocation(OfflinePlayer offlinePlayer) {
        try {
            File worldContainer = plugin.getServer().getWorldContainer();
            File playerDataFolder = new File(worldContainer, "world/playerdata");
            File playerDataFile = new File(playerDataFolder, offlinePlayer.getUniqueId().toString() + ".dat");

            if (!playerDataFile.exists()) {
                return null;
            }

            String playerDataContent = readNBTFile(playerDataFile);
            if (playerDataContent != null) {
                return parseLocationFromNBT(playerDataContent);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error reading offline player location: " + e.getMessage());
        }

        return null;
    }

    private String getReadableTitle(String key) {
        String[] parts = key.split("/");
        String lastPart = parts[parts.length - 1];

        return Arrays.stream(lastPart.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .reduce((a, b) -> a + " " + b)
                .orElse(lastPart);
    }

    private String readNBTFile(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[(int) file.length()];
            fis.read(buffer);
            return new String(buffer);
        } catch (Exception e) {
            plugin.getLogger().warning("Error reading NBT file: " + e.getMessage());
            return null;
        }
    }

    private JsonObject parseLocationFromNBT(String nbtContent) {
        JsonObject location = new JsonObject();

        try {
            Pattern posPattern = Pattern.compile("Pos:\\[([0-9.-]+)d,([0-9.-]+)d,([0-9.-]+)d\\]");
            Pattern rotPattern = Pattern.compile("Rotation:\\[([0-9.-]+)f,([0-9.-]+)f\\]");
            Pattern dimPattern = Pattern.compile("Dimension:\"([^\"]+)\"");

            Matcher posMatcher = posPattern.matcher(nbtContent);
            Matcher rotMatcher = rotPattern.matcher(nbtContent);
            Matcher dimMatcher = dimPattern.matcher(nbtContent);

            if (posMatcher.find()) {
                location.addProperty("x", Double.parseDouble(posMatcher.group(1)));
                location.addProperty("y", Double.parseDouble(posMatcher.group(2)));
                location.addProperty("z", Double.parseDouble(posMatcher.group(3)));
            }

            if (rotMatcher.find()) {
                location.addProperty("yaw", Float.parseFloat(rotMatcher.group(1)));
                location.addProperty("pitch", Float.parseFloat(rotMatcher.group(2)));
            }

            if (dimMatcher.find()) {
                String dimension = dimMatcher.group(1);
                String worldName = dimension.replace("minecraft:", "");
                if (worldName.equals("overworld")) worldName = "world";
                else if (worldName.equals("the_nether")) worldName = "world_nether";
                else if (worldName.equals("the_end")) worldName = "world_the_end";
                location.addProperty("world", worldName);
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Error parsing location from NBT: " + e.getMessage());
        }

        return location;
    }
}
