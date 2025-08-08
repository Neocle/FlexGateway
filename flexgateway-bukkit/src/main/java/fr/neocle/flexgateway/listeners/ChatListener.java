package fr.neocle.flexgateway.listeners;

import fr.neocle.flexgateway.database.DatabaseManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final DatabaseManager databaseManager;

    public ChatListener(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        Location location = player.getLocation();

        databaseManager.saveMessage(
                player.getUniqueId().toString(),
                player.getName(),
                null, // No specific recipient for public chat
                null,
                message,
                "CHAT",
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ()
        );
    }
}