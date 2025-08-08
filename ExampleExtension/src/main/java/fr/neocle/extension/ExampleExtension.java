package fr.neocle.extension;

import fr.neocle.flexgateway.api.extension.BaseExtension;
import fr.neocle.flexgateway.api.extension.enhancer.BaseEndpointEnhancer;
import fr.neocle.flexgateway.api.extension.handler.ActionHandler;
import fr.neocle.flexgateway.api.extension.provider.DataProvider;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;

public class ExampleExtension extends BaseExtension {

    @Override
    public String getName() {
        return "example";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Example extension for FlexGateway";
    }

    @Override
    public String getAuthor() {
        return "Neocle";
    }

    @Override
    public void onLoad() {
        getLogger().info("Example extension loaded!");
        saveDefaultConfig();
    }

    @Override
    public void onEnable() {
        getLogger().info("Example extension enabled!");

        // Register extension endpoints
        registerDataProvider(new PlayersProvider());
        registerDataProvider(new StatsProvider());
        registerActionHandler(new BroadcastAction());

        // Register base endpoint enhancers
        registerBaseEndpointEnhancer(new ServerPerformanceEnhancer());
        registerBaseEndpointEnhancer(new ServerCustomDataEnhancer());
        registerBaseEndpointEnhancer(new PlayerDetailedEnhancer());
        registerBaseEndpointEnhancer(new PlayersStatsEnhancer());

        getLogger().info("Registered extension endpoints and base enhancers");
    }

    @Override
    public void onDisable() {
        getLogger().info("Example extension disabled!");
    }

    // Extension endpoints remain the same...
    private class PlayersProvider implements DataProvider {
        @Override
        public String getEndpoint() {
            return "players";
        }

        @Override
        public JsonElement getData(Map<String, String> parameters) {
            JsonArray players = new JsonArray();
            boolean includeHealth = "true".equals(parameters.get("health"));

            for (Player player : Bukkit.getOnlinePlayers()) {
                JsonObject playerObj = new JsonObject();
                playerObj.addProperty("name", player.getName());
                playerObj.addProperty("level", player.getLevel());

                if (includeHealth) {
                    playerObj.addProperty("health", player.getHealth());
                    playerObj.addProperty("maxHealth", player.getMaxHealth());
                }

                players.add(playerObj);
            }

            return players;
        }

        @Override
        public String getDescription() {
            return "Get list of online players with optional health info";
        }

        @Override
        public String[] getSupportedParameters() {
            return new String[]{"health"};
        }
    }

    private class StatsProvider implements DataProvider {
        @Override
        public String getEndpoint() {
            return "stats";
        }

        @Override
        public JsonElement getData(Map<String, String> parameters) {
            JsonObject stats = new JsonObject();
            stats.addProperty("onlinePlayers", Bukkit.getOnlinePlayers().size());
            stats.addProperty("maxPlayers", Bukkit.getMaxPlayers());
            stats.addProperty("serverVersion", Bukkit.getVersion());
            stats.addProperty("extensionVersion", getVersion());
            return stats;
        }

        @Override
        public String getDescription() {
            return "Get server statistics";
        }
    }

    private class BroadcastAction implements ActionHandler {
        @Override
        public String getEndpoint() {
            return "broadcast";
        }

        @Override
        public JsonObject handleAction(String method, Map<String, String> parameters, JsonObject body) {
            JsonObject response = new JsonObject();

            if (body == null || !body.has("message")) {
                response.addProperty("error", "Message required in request body");
                return response;
            }

            String message = body.get("message").getAsString();
            String prefix = getConfig().get("broadcast-prefix", "[Extension]");

            Bukkit.broadcastMessage(prefix + " " + message);

            response.addProperty("success", true);
            response.addProperty("message", "Broadcast sent successfully");
            response.addProperty("recipients", Bukkit.getOnlinePlayers().size());

            return response;
        }

        @Override
        public String[] getSupportedMethods() {
            return new String[]{"POST"};
        }

        @Override
        public String getDescription() {
            return "Broadcast a message to all online players";
        }
    }

    // Multiple enhancers for the same endpoint - each adds different keys
    private class ServerPerformanceEnhancer implements BaseEndpointEnhancer {
        @Override
        public String getBaseEndpoint() {
            return "server";
        }

        @Override
        public void enhanceResponse(JsonObject response, JsonObject originalData, String identifier, Map<String, String> parameters) {
            // Add performance metrics
            Runtime runtime = Runtime.getRuntime();
            JsonObject performance = new JsonObject();
            performance.addProperty("freeMemoryMB", runtime.freeMemory() / 1024 / 1024);
            performance.addProperty("totalMemoryMB", runtime.totalMemory() / 1024 / 1024);
            performance.addProperty("maxMemoryMB", runtime.maxMemory() / 1024 / 1024);
            performance.addProperty("usedMemoryMB", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024);

            double memoryUsagePercent = ((double)(runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory()) * 100;
            performance.addProperty("memoryUsagePercent", Math.round(memoryUsagePercent * 100.0) / 100.0);

            response.add("performance", performance);

            // Add system info
            JsonObject system = new JsonObject();
            system.addProperty("javaVersion", System.getProperty("java.version"));
            system.addProperty("osName", System.getProperty("os.name"));
            system.addProperty("osVersion", System.getProperty("os.version"));
            system.addProperty("processors", Runtime.getRuntime().availableProcessors());

            response.add("systemInfo", system);
        }

        @Override
        public String getDescription() {
            return "Adds performance and system information to server endpoint";
        }

        @Override
        public int getPriority() {
            return 50; // Lower priority, runs first
        }
    }

    private class ServerCustomDataEnhancer implements BaseEndpointEnhancer {
        @Override
        public String getBaseEndpoint() {
            return "server";
        }

        @Override
        public void enhanceResponse(JsonObject response, JsonObject originalData, String identifier, Map<String, String> parameters) {
            // Add extension metadata
            JsonObject extensionData = new JsonObject();
            extensionData.addProperty("name", getName());
            extensionData.addProperty("version", getVersion());
            extensionData.addProperty("author", getAuthor());
            extensionData.addProperty("active", true);
            extensionData.addProperty("enhancementTime", System.currentTimeMillis());

            response.add("exampleExtension", extensionData);

            // Add some custom server features
            JsonObject features = new JsonObject();
            features.addProperty("broadcastingEnabled", true);
            features.addProperty("playerTrackingEnabled", true);
            features.addProperty("performanceMonitoring", true);

            response.add("extensionFeatures", features);

            // Add a simple message
            response.addProperty("welcomeMessage", getConfig().get("welcome-message", "Hello from extension!"));
        }

        @Override
        public String getDescription() {
            return "Adds extension metadata and custom features to server endpoint";
        }

        @Override
        public int getPriority() {
            return 100; // Normal priority
        }
    }

    private class PlayerDetailedEnhancer implements BaseEndpointEnhancer {
        @Override
        public String getBaseEndpoint() {
            return "player";
        }

        @Override
        public void enhanceResponse(JsonObject response, JsonObject originalData, String identifier, Map<String, String> parameters) {
            Player player = Bukkit.getPlayer(identifier);
            if (player == null) {
                return; // Player not online, no enhancement
            }

            // Add detailed stats
            JsonObject stats = new JsonObject();
            stats.addProperty("health", player.getHealth());
            stats.addProperty("maxHealth", player.getMaxHealth());
            stats.addProperty("healthPercent", (player.getHealth() / player.getMaxHealth()) * 100);
            stats.addProperty("foodLevel", player.getFoodLevel());
            stats.addProperty("totalExperience", player.getTotalExperience());
            stats.addProperty("level", player.getLevel());

            response.add("detailedStats", stats);

            // Add game state
            JsonObject gameState = new JsonObject();
            gameState.addProperty("gameMode", player.getGameMode().toString());
            gameState.addProperty("flying", player.isFlying());
            gameState.addProperty("allowFlight", player.getAllowFlight());
            gameState.addProperty("sneaking", player.isSneaking());
            gameState.addProperty("sprinting", player.isSprinting());
            gameState.addProperty("swimming", player.isSwimming());

            response.add("gameState", gameState);

            // Add precise location
            JsonObject preciseLocation = new JsonObject();
            preciseLocation.addProperty("world", player.getWorld().getName());
            preciseLocation.addProperty("x", Math.round(player.getLocation().getX() * 1000.0) / 1000.0);
            preciseLocation.addProperty("y", Math.round(player.getLocation().getY() * 1000.0) / 1000.0);
            preciseLocation.addProperty("z", Math.round(player.getLocation().getZ() * 1000.0) / 1000.0);
            preciseLocation.addProperty("yaw", Math.round(player.getLocation().getYaw() * 100.0) / 100.0);
            preciseLocation.addProperty("pitch", Math.round(player.getLocation().getPitch() * 100.0) / 100.0);

            response.add("preciseLocation", preciseLocation);

            // Add session info
            JsonObject session = new JsonObject();
            session.addProperty("joinTime", player.getFirstPlayed());
            session.addProperty("playTime", player.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE));
            session.addProperty("enhancedBy", getName());

            response.add("sessionInfo", session);
        }

        @Override
        public String getDescription() {
            return "Adds detailed stats, game state, location and session info to player endpoint";
        }
    }

    private class PlayersStatsEnhancer implements BaseEndpointEnhancer {
        @Override
        public String getBaseEndpoint() {
            return "players";
        }

        @Override
        public void enhanceResponse(JsonObject response, JsonObject originalData, String identifier, Map<String, String> parameters) {
            // Add aggregate statistics
            double totalHealth = 0;
            int totalLevel = 0;
            int flyingPlayers = 0;
            int creativePlayers = 0;
            int survivalPlayers = 0;

            for (Player player : Bukkit.getOnlinePlayers()) {
                totalHealth += player.getHealth();
                totalLevel += player.getLevel();
                if (player.isFlying()) {
                    flyingPlayers++;
                }
                switch (player.getGameMode()) {
                    case CREATIVE -> creativePlayers++;
                    case SURVIVAL -> survivalPlayers++;
                }
            }

            int playerCount = Bukkit.getOnlinePlayers().size();

            JsonObject aggregateStats = new JsonObject();
            if (playerCount > 0) {
                aggregateStats.addProperty("averageHealth", Math.round(totalHealth / playerCount * 100.0) / 100.0);
                aggregateStats.addProperty("averageLevel", Math.round((double) totalLevel / playerCount * 100.0) / 100.0);
                aggregateStats.addProperty("totalLevels", totalLevel);
            } else {
                aggregateStats.addProperty("averageHealth", 0);
                aggregateStats.addProperty("averageLevel", 0);
                aggregateStats.addProperty("totalLevels", 0);
            }

            response.add("aggregateStats", aggregateStats);

            // Add player distribution
            JsonObject distribution = new JsonObject();
            distribution.addProperty("flyingPlayers", flyingPlayers);
            distribution.addProperty("creativePlayers", creativePlayers);
            distribution.addProperty("survivalPlayers", survivalPlayers);
            distribution.addProperty("otherGameModePlayers", playerCount - creativePlayers - survivalPlayers);

            response.add("playerDistribution", distribution);

            // Add enhancement metadata
            JsonObject metadata = new JsonObject();
            metadata.addProperty("enhancedBy", getName());
            metadata.addProperty("enhancementTime", System.currentTimeMillis());
            metadata.addProperty("dataFreshness", "real-time");

            response.add("enhancementMetadata", metadata);
        }

        @Override
        public String getDescription() {
            return "Adds aggregate statistics and player distribution to players endpoint";
        }
    }
}