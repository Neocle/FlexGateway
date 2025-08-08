package fr.neocle.flexgateway.bukkit.database.messages;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class MessageInsertCommand {

    private final String senderUuid;
    private final String senderName;
    private final String recipientUuid;
    private final String recipientName;
    private final String message;
    private final String messageType;
    private final String worldName;
    private final Double x;
    private final Double y;
    private final Double z;

    public MessageInsertCommand(String senderUuid, String senderName, String recipientUuid,
                                String recipientName, String message, String messageType,
                                String worldName, Double x, Double y, Double z) {
        this.senderUuid = senderUuid;
        this.senderName = senderName;
        this.recipientUuid = recipientUuid;
        this.recipientName = recipientName;
        this.message = message;
        this.messageType = messageType;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public String getSQL() {
        return """
            INSERT INTO messages
            (sender_uuid, sender_name, recipient_uuid, recipient_name, message, message_type, world_name, x, y, z)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
    }

    public void setParameters(PreparedStatement stmt) throws SQLException {
        stmt.setString(1, senderUuid);
        stmt.setString(2, senderName);
        stmt.setString(3, recipientUuid);
        stmt.setString(4, recipientName);
        stmt.setString(5, message);
        stmt.setString(6, messageType);
        stmt.setString(7, worldName);

        setDoubleOrNull(stmt, 8, x);
        setDoubleOrNull(stmt, 9, y);
        setDoubleOrNull(stmt, 10, z);
    }

    private void setDoubleOrNull(PreparedStatement stmt, int parameterIndex, Double value) throws SQLException {
        if (value != null) {
            stmt.setDouble(parameterIndex, value);
        } else {
            stmt.setNull(parameterIndex, Types.DOUBLE);
        }
    }
}