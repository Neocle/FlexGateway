package fr.neocle.flexgateway.bukkit.database;

import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseSchema {

    public String getCreateMessagesTableSQL() {
        return """
            CREATE TABLE IF NOT EXISTS messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sender_uuid TEXT NOT NULL,
                sender_name TEXT NOT NULL,
                recipient_uuid TEXT,
                recipient_name TEXT,
                message TEXT NOT NULL,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                message_type TEXT DEFAULT 'CHAT',
                world_name TEXT,
                x REAL,
                y REAL,
                z REAL
            )
        """;
    }

    public void createIndexes(Statement stmt) throws SQLException {
        String[] indexes = {
                "CREATE INDEX IF NOT EXISTS idx_sender ON messages(sender_uuid)",
                "CREATE INDEX IF NOT EXISTS idx_recipient ON messages(recipient_uuid)",
                "CREATE INDEX IF NOT EXISTS idx_timestamp ON messages(timestamp)",
                "CREATE INDEX IF NOT EXISTS idx_type ON messages(message_type)"
        };

        for (String index : indexes) {
            stmt.execute(index);
        }
    }
}