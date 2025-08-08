package fr.neocle.flexgateway.database.messages;

import fr.neocle.flexgateway.database.DatabaseConnection;
import com.google.gson.JsonArray;

import java.sql.*;
import java.text.SimpleDateFormat;

public class MessageRepository {

    private final DatabaseConnection connectionManager;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public MessageRepository(DatabaseConnection connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void saveMessage(String senderUuid, String senderName, String recipientUuid,
                            String recipientName, String message, String messageType,
                            String worldName, Double x, Double y, Double z) {

        MessageInsertCommand command = new MessageInsertCommand(
                senderUuid, senderName, recipientUuid, recipientName,
                message, messageType, worldName, x, y, z
        );

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(command.getSQL())) {

            command.setParameters(stmt);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                connectionManager.getPlugin().getLogger().fine("Message saved successfully from " + senderName);
            }

        } catch (SQLException e) {
            connectionManager.getPlugin().getLogger().warning("Failed to save message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public JsonArray getMessages(int limit, int offset, String playerUuid, String messageType) {
        MessageQueryBuilder queryBuilder = new MessageQueryBuilder()
                .withPlayerFilter(playerUuid)
                .withMessageTypeFilter(messageType)
                .withPagination(limit, offset);

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(queryBuilder.build())) {

            queryBuilder.setParameters(stmt);

            try (ResultSet rs = stmt.executeQuery()) {
                return convertResultSetToJsonArray(rs);
            }

        } catch (SQLException e) {
            connectionManager.getPlugin().getLogger().warning("Failed to retrieve messages: " + e.getMessage());
            e.printStackTrace();
            return new JsonArray();
        }
    }

    public int getPlayerMessageCount(String playerUuid) {
        String sql = "SELECT COUNT(*) FROM messages WHERE sender_uuid = ? OR recipient_uuid = ?";
        return executeCountQuery(sql, playerUuid, playerUuid);
    }

    public int getPlayerSentMessageCount(String playerUuid) {
        String sql = "SELECT COUNT(*) FROM messages WHERE sender_uuid = ?";
        return executeCountQuery(sql, playerUuid);
    }

    public int getTotalMessageCount() {
        String sql = "SELECT COUNT(*) FROM messages";
        return executeCountQuery(sql);
    }

    private int executeCountQuery(String sql, String... parameters) {
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < parameters.length; i++) {
                stmt.setString(i + 1, parameters[i]);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            connectionManager.getPlugin().getLogger().warning("Failed to execute count query: " + e.getMessage());
        }

        return 0;
    }

    private JsonArray convertResultSetToJsonArray(ResultSet rs) throws SQLException {
        JsonArray messages = new JsonArray();
        MessageJsonConverter converter = new MessageJsonConverter(dateFormat);

        while (rs.next()) {
            messages.add(converter.convertToJson(rs));
        }

        return messages;
    }
}