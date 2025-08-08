package fr.neocle.flexgateway.data.collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Iterator;

public class PlayerAchievementCollector {
    private final JavaPlugin plugin;

    public PlayerAchievementCollector(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    JsonObject getPlayerAchievements(Player player) {
        JsonObject achievements = new JsonObject();
        JsonArray completed = new JsonArray();
        JsonArray inProgress = new JsonArray();

        Iterator<Advancement> advancementIterator = plugin.getServer().advancementIterator();
        int completedCount = 0;
        int totalCount = 0;

        while (advancementIterator.hasNext()) {
            Advancement advancement = advancementIterator.next();
            if (advancement.getKey().getNamespace().equals("minecraft") && isRealAchievement(advancement)) {
                totalCount++;
                AdvancementProgress progress = player.getAdvancementProgress(advancement);

                if (progress.isDone()) {
                    JsonObject advObj = new JsonObject();
                    advObj.addProperty("key", advancement.getKey().getKey());
                    advObj.addProperty("title", getAdvancementTitle(advancement));
                    advObj.addProperty("description", getAdvancementDescription(advancement));
                    advObj.addProperty("hidden", advancement.getDisplay().isHidden());

                    completed.add(advObj);
                    completedCount++;
                } else if (progress.getAwardedCriteria().size() > 0) {
                    JsonObject advObj = new JsonObject();
                    advObj.addProperty("key", advancement.getKey().getKey());
                    advObj.addProperty("title", getAdvancementTitle(advancement));
                    advObj.addProperty("progress", progress.getAwardedCriteria().size() + "/" + (progress.getAwardedCriteria().size() + progress.getRemainingCriteria().size()));

                    inProgress.add(advObj);
                }
            }
        }

        achievements.addProperty("completedCount", completedCount);
        achievements.addProperty("totalCount", totalCount);
        achievements.addProperty("completionPercentage", totalCount > 0 ? Math.round(completedCount * 100.0 / totalCount * 10.0) / 10.0 : 0);
        achievements.add("completed", completed);
        achievements.add("inProgress", inProgress);

        return achievements;
    }

    private String getAdvancementTitle(Advancement advancement) {
        try {
            if (advancement.getDisplay() != null) {
                Component titleComponent = advancement.getDisplay().title();
                return PlainTextComponentSerializer.plainText().serialize(titleComponent);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting advancement title: " + e.getMessage());
        }

        return getReadableTitle(advancement.getKey().getKey());
    }

    private String getAdvancementDescription(Advancement advancement) {
        try {
            if (advancement.getDisplay() != null) {
                Component descriptionComponent = advancement.getDisplay().description();
                return PlainTextComponentSerializer.plainText().serialize(descriptionComponent);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting advancement description: " + e.getMessage());
        }

        return "";
    }

    private boolean isRealAchievement(Advancement advancement) {
        String key = advancement.getKey().getKey();

        if (key.startsWith("recipes/")) {
            return false;
        }

        /*if (advancement.getDisplay() == null) {
            return false;
        }

        if (advancement.getDisplay().isHidden() && !advancement.getKey().getKey().contains("root")) {
            return false;
        }*/

        return true;
    }

    private String getReadableTitle(String key) {
        String[] parts = key.split("/");
        String lastPart = parts[parts.length - 1];

        return Arrays.stream(lastPart.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .reduce((a, b) -> a + " " + b)
                .orElse(lastPart);
    }
}
