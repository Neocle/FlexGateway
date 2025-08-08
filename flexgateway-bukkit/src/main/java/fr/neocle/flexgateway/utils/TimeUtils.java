package fr.neocle.flexgateway.utils;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class TimeUtils {

    public static String formatPlayTime(Player player) {
        long playTimeMs = (System.currentTimeMillis() - player.getFirstPlayed()) -
                (System.currentTimeMillis() - player.getLastPlayed());
        long minutes = playTimeMs / (1000 * 60);
        long hours = minutes / 60;
        return String.format("%d hours, %d minutes", hours, minutes % 60);
    }

    public static String getLastSeenTime(OfflinePlayer player) {
        long lastSeen = System.currentTimeMillis() - player.getLastPlayed();
        long days = lastSeen / (1000 * 60 * 60 * 24);
        long hours = (lastSeen % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);

        if (days > 0) {
            return days + " days ago";
        } else if (hours > 0) {
            return hours + " hours ago";
        } else {
            return "Less than an hour ago";
        }
    }
}