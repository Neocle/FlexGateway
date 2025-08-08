package fr.neocle.flexgateway.database;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {

    private final JavaPlugin plugin;
    private final String databasePath;
    private Connection connection;

    public DatabaseConnection(JavaPlugin plugin) {
        this.plugin = plugin;
        this.databasePath = plugin.getDataFolder().getAbsolutePath() + File.separator + "messages";
        initialize();
    }

    private void initialize() {
        try {
            ensureDataFolderExists();
            loadSQLiteDriver();
            establishConnection();
            createTables();
            plugin.getLogger().info("Database initialized successfully");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void ensureDataFolderExists() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            boolean created = dataFolder.mkdirs();
            if (!created) {
                throw new RuntimeException("Failed to create plugin data folder: " + dataFolder.getAbsolutePath());
            }
        }
    }

    private void loadSQLiteDriver() {
        try {
            Class.forName("org.sqlite.JDBC");
            plugin.getLogger().info("SQLite driver loaded successfully");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite driver not found! Make sure SQLite is included in the plugin JAR.", e);
        }
    }

    private void establishConnection() throws SQLException {
        String url = "jdbc:sqlite:" + databasePath + ".db";
        plugin.getLogger().info("Connecting to database at: " + url);

        connection = DriverManager.getConnection(url, "sa", "");

        if (connection == null) {
            throw new SQLException("Failed to create database connection - connection is null");
        }

        if (connection.isClosed()) {
            throw new SQLException("Database connection is closed immediately after creation");
        }

        plugin.getLogger().info("Database connection established successfully");
    }

    private void createTables() throws SQLException {
        if (connection == null || connection.isClosed()) {
            throw new SQLException("Cannot create tables: database connection is null or closed");
        }

        DatabaseSchema schema = new DatabaseSchema();

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(schema.getCreateMessagesTableSQL());
            plugin.getLogger().info("Messages table created/verified successfully");

            schema.createIndexes(stmt);
            plugin.getLogger().info("Database indexes created/verified successfully");
        }
    }

    public Connection getConnection() throws SQLException {
        if (!ensureConnection()) {
            throw new SQLException("Database connection unavailable");
        }
        return connection;
    }

    public boolean ensureConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                plugin.getLogger().warning("Database connection lost, attempting to reconnect...");
                establishConnection();
                return connection != null && !connection.isClosed();
            }
            return true;
        } catch (SQLException e) {
            plugin.getLogger().warning("Error checking/restoring database connection: " + e.getMessage());
            return false;
        }
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.createStatement().execute("VACUUM");
                connection.close();
                plugin.getLogger().info("Database connection closed successfully");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error closing database connection: " + e.getMessage());
        }
    }
}