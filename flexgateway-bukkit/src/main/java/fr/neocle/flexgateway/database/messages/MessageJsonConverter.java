package fr.neocle.flexgateway.database.messages;

import com.google.gson.JsonObject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

public class MessageJsonConverter {

    private final SimpleDateFormat dateFormat;

    public MessageJsonConverter(SimpleDateFormat dateFormat) {
        this.dateFormat = dateFormat;
    }

    public JsonObject convertToJson(ResultSet rs) throws SQLException {
        JsonObject message = new JsonObject();

        addBasicMessageInfo(message, rs);
        addRecipientInfo(message, rs);
        addLocationInfo(message, rs);

        return message;
    }

    private void addBasicMessageInfo(JsonObject message, ResultSet rs) throws SQLException {
        message.addProperty("id", rs.getLong("id"));
        message.addProperty("senderUuid", rs.getString("sender_uuid"));
        message.addProperty("senderName", rs.getString("sender_name"));
        message.addProperty("message", rs.getString("message"));
        message.addProperty("timestamp", dateFormat.format(rs.getTimestamp("timestamp")));
        message.addProperty("messageType", rs.getString("message_type"));
    }

    private void addRecipientInfo(JsonObject message, ResultSet rs) throws SQLException {
        String recipientUuid = rs.getString("recipient_uuid");
        String recipientName = rs.getString("recipient_name");

        if (recipientUuid != null) {
            message.addProperty("recipientUuid", recipientUuid);
        }
        if (recipientName != null) {
            message.addProperty("recipientName", recipientName);
        }
    }

    private void addLocationInfo(JsonObject message, ResultSet rs) throws SQLException {
        String worldName = rs.getString("world_name");
        if (worldName != null) {
            message.addProperty("worldName", worldName);
        }

        Double x = rs.getObject("x", Double.class);
        Double y = rs.getObject("y", Double.class);
        Double z = rs.getObject("z", Double.class);

        if (x != null && y != null && z != null) {
            JsonObject location = new JsonObject();
            location.addProperty("x", x);
            location.addProperty("y", y);
            location.addProperty("z", z);
            message.add("location", location);
        }
    }
}