package fr.neocle.flexgateway.velocity.config;

import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private final Path dataDirectory;
    private final Logger logger;
    private Map<String, Object> config;

    public ConfigManager(Path dataDirectory, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
    }

    public void loadConfig() {
        try {
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }

            Path configFile = dataDirectory.resolve("config.yml");

            if (!Files.exists(configFile)) {
                createDefaultConfig(configFile);
            }

            Yaml yaml = new Yaml();
            try (FileInputStream fis = new FileInputStream(configFile.toFile())) {
                config = yaml.load(fis);
                if (config == null) {
                    config = new HashMap<>();
                }
            }

            logger.info("Configuration loaded from: {}", configFile);
            logServerConfiguration();

        } catch (IOException e) {
            logger.error("Failed to load configuration", e);
            config = new HashMap<>();
        }
    }

    private void createDefaultConfig(Path configFile) throws IOException {
        Map<String, Object> defaultConfig = new HashMap<>();

        // API settings
        Map<String, Object> apiConfig = new HashMap<>();
        apiConfig.put("host", "localhost");
        apiConfig.put("port", 8081);
        defaultConfig.put("api", apiConfig);

        // Server mappings for TCP communication
        Map<String, Object> serversConfig = new HashMap<>();

        Map<String, Object> lobbyConfig = new HashMap<>();
        lobbyConfig.put("host", "127.0.0.1");
        lobbyConfig.put("tcp_port", 25599);
        serversConfig.put("lobby", lobbyConfig);

        Map<String, Object> minigamesConfig = new HashMap<>();
        minigamesConfig.put("host", "127.0.0.1");
        minigamesConfig.put("tcp_port", 25600);
        serversConfig.put("minigames", minigamesConfig);

        Map<String, Object> factionsConfig = new HashMap<>();
        factionsConfig.put("host", "127.0.0.1");
        factionsConfig.put("tcp_port", 25601);
        serversConfig.put("factions", factionsConfig);

        defaultConfig.put("servers", serversConfig);

        Yaml yaml = new Yaml();
        try (FileWriter writer = new FileWriter(configFile.toFile())) {
            writer.write("# FlexGateway Velocity Configuration\n");
            writer.write("# TCP Communication Settings\n\n");
            yaml.dump(defaultConfig, writer);
        }

        logger.info("Default configuration created: {}", configFile);
    }

    private void logServerConfiguration() {
        logger.info("=== SERVER TCP CONFIGURATION ===");
        Map<String, Object> servers = getServersConfig();
        if (servers != null) {
            for (String serverName : servers.keySet()) {
                String host = getServerHost(serverName);
                int port = getServerTcpPort(serverName);
                logger.info("  {} -> {}:{}", serverName, host, port);
            }
        } else {
            logger.warn("No server configuration found, using defaults");
        }
    }

    public String getApiHost() {
        Map<String, Object> apiConfig = (Map<String, Object>) config.get("api");
        if (apiConfig != null) {
            return (String) apiConfig.getOrDefault("host", "localhost");
        }
        return "localhost";
    }

    public int getApiPort() {
        Map<String, Object> apiConfig = (Map<String, Object>) config.get("api");
        if (apiConfig != null) {
            Object portObj = apiConfig.get("port");
            if (portObj instanceof Integer) {
                return (Integer) portObj;
            }
        }
        return 8081;
    }

    public String getServerHost(String serverName) {
        Map<String, Object> servers = getServersConfig();
        if (servers != null && servers.containsKey(serverName)) {
            Map<String, Object> serverConfig = (Map<String, Object>) servers.get(serverName);
            return (String) serverConfig.getOrDefault("host", "127.0.0.1");
        }
        return "127.0.0.1"; // Default
    }

    public int getServerTcpPort(String serverName) {
        Map<String, Object> servers = getServersConfig();
        if (servers != null && servers.containsKey(serverName)) {
            Map<String, Object> serverConfig = (Map<String, Object>) servers.get(serverName);
            Object portObj = serverConfig.get("tcp_port");
            if (portObj instanceof Integer) {
                return (Integer) portObj;
            }
        }
        return -1; // Not configured
    }

    private Map<String, Object> getServersConfig() {
        return (Map<String, Object>) config.get("servers");
    }
}