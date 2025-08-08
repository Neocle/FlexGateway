package fr.neocle.flexgateway.database;

import fr.neocle.flexgateway.database.messages.MessageRepository;
import com.google.gson.JsonArray;
import org.bukkit.plugin.java.JavaPlugin;

public class DatabaseManager {

    private final DatabaseConnection connectionManager;
    private final MessageRepository messageRepository;

    public DatabaseManager(JavaPlugin plugin) {
        this.connectionManager = new DatabaseConnection(plugin);
        this.messageRepository = new MessageRepository(connectionManager);
    }

    public void saveMessage(String senderUuid, String senderName, String recipientUuid,
                            String recipientName, String message, String messageType,
                            String worldName, Double x, Double y, Double z) {
        messageRepository.saveMessage(senderUuid, senderName, recipientUuid, recipientName,
                message, messageType, worldName, x, y, z);
    }

    public JsonArray getMessages(int limit, int offset, String playerUuid, String messageType) {
        return messageRepository.getMessages(limit, offset, playerUuid, messageType);
    }

    public int getPlayerMessageCount(String playerUuid) {
        return messageRepository.getPlayerMessageCount(playerUuid);
    }

    public int getPlayerSentMessageCount(String playerUuid) {
        return messageRepository.getPlayerSentMessageCount(playerUuid);
    }

    public int getTotalMessageCount() {
        return messageRepository.getTotalMessageCount();
    }

    public boolean isConnected() {
        return connectionManager.isConnected();
    }

    public void close() {
        connectionManager.close();
    }
}