package fr.neocle.flexgateway.data.services;

import com.google.gson.JsonArray;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import fr.neocle.flexgateway.database.DatabaseManager;
import java.util.UUID;

public class MessageService {

    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;

    public MessageService(JavaPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    public JsonArray getMessages(int limit, int offset, String playerUuid, String messageType) {
        return databaseManager.getMessages(limit, offset, playerUuid, messageType);
    }

    public boolean sendMessage(String senderUuid, String recipientUuid, String message, String messageType) {
        try {
            MessageContext context = buildMessageContext(senderUuid, recipientUuid, message, messageType);

            saveMessageToDatabase(context);
            deliverInGameMessage(context);

            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send message: " + e.getMessage());
            return false;
        }
    }

    private MessageContext buildMessageContext(String senderUuid, String recipientUuid, String message, String messageType) {
        Player sender = senderUuid != null ? plugin.getServer().getPlayer(UUID.fromString(senderUuid)) : null;
        Player recipient = recipientUuid != null ? plugin.getServer().getPlayer(UUID.fromString(recipientUuid)) : null;

        return new MessageContext(sender, recipient, senderUuid, recipientUuid, message, messageType);
    }

    private void saveMessageToDatabase(MessageContext context) {
        Location senderLocation = context.getSender() != null ? context.getSender().getLocation() : null;

        databaseManager.saveMessage(
                context.getSenderUuid(),
                context.getSenderName(),
                context.getRecipientUuid(),
                context.getRecipientName(),
                context.getMessage(),
                context.getMessageType(),
                senderLocation != null ? senderLocation.getWorld().getName() : null,
                senderLocation != null ? senderLocation.getX() : null,
                senderLocation != null ? senderLocation.getY() : null,
                senderLocation != null ? senderLocation.getZ() : null
        );
    }

    private void deliverInGameMessage(MessageContext context) {
        if (context.getRecipient() != null && context.getRecipient().isOnline()) {
            String formattedMessage = String.format("%s → %s: %s",
                    context.getSenderName(), context.getRecipientName(), context.getMessage());
            context.getRecipient().sendMessage(formattedMessage);

            if (context.getSender() != null && context.getSender().isOnline()) {
                context.getSender().sendMessage("Message sent to " + context.getRecipientName() + ": " + context.getMessage());
            }
        }
    }

    private static class MessageContext {
        private final Player sender;
        private final Player recipient;
        private final String senderUuid;
        private final String recipientUuid;
        private final String message;
        private final String messageType;

        public MessageContext(Player sender, Player recipient, String senderUuid, String recipientUuid, String message, String messageType) {
            this.sender = sender;
            this.recipient = recipient;
            this.senderUuid = senderUuid;
            this.recipientUuid = recipientUuid;
            this.message = message;
            this.messageType = messageType != null ? messageType : "API";
        }

        public Player getSender() { return sender; }
        public Player getRecipient() { return recipient; }
        public String getSenderUuid() { return senderUuid; }
        public String getRecipientUuid() { return recipientUuid; }
        public String getMessage() { return message; }
        public String getMessageType() { return messageType; }

        public String getSenderName() {
            return sender != null ? sender.getName() : "API";
        }

        public String getRecipientName() {
            return recipient != null ? recipient.getName() : null;
        }
    }
}