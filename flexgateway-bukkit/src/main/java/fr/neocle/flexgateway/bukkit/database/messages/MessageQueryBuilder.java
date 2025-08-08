package fr.neocle.flexgateway.bukkit.database.messages;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MessageQueryBuilder {

    private String playerUuid;
    private String messageType;
    private int limit;
    private int offset;
    private final List<String> parameters = new ArrayList<>();

    public MessageQueryBuilder withPlayerFilter(String playerUuid) {
        this.playerUuid = playerUuid;
        return this;
    }

    public MessageQueryBuilder withMessageTypeFilter(String messageType) {
        this.messageType = messageType;
        return this;
    }

    public MessageQueryBuilder withPagination(int limit, int offset) {
        this.limit = limit;
        this.offset = offset;
        return this;
    }

    public String build() {
        StringBuilder sql = new StringBuilder("SELECT * FROM messages WHERE 1=1");
        parameters.clear();

        if (playerUuid != null && !playerUuid.isEmpty()) {
            sql.append(" AND (sender_uuid = ? OR recipient_uuid = ?)");
            parameters.add(playerUuid);
            parameters.add(playerUuid);
        }

        if (messageType != null && !messageType.isEmpty()) {
            sql.append(" AND message_type = ?");
            parameters.add(messageType);
        }

        sql.append(" ORDER BY timestamp DESC LIMIT ? OFFSET ?");
        parameters.add(String.valueOf(limit));
        parameters.add(String.valueOf(offset));

        return sql.toString();
    }

    public void setParameters(PreparedStatement stmt) throws SQLException {
        for (int i = 0; i < parameters.size() - 2; i++) {
            stmt.setString(i + 1, parameters.get(i));
        }

        stmt.setInt(parameters.size() - 1, limit);
        stmt.setInt(parameters.size(), offset);
    }
}