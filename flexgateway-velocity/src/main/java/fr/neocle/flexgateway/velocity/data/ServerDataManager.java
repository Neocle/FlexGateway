package fr.neocle.flexgateway.velocity.data;

import com.google.gson.JsonObject;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerDataManager {
    private final Logger logger;
    private final ConcurrentHashMap<String, ServerData> serverDataMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public ServerDataManager(Logger logger) {
        this.logger = logger;

        // Start cleanup task for expired cache entries
        scheduler.scheduleAtFixedRate(this::cleanupExpiredData, 5, 5, TimeUnit.MINUTES);
    }

    public void handleServerUpdate(String serverName, String dataType, JsonObject data) {
        ServerData serverData = serverDataMap.computeIfAbsent(serverName, k -> new ServerData(serverName));
        serverData.updateData(dataType, data);

        logger.debug("Updated {} data for server {}", dataType, serverName);
    }

    public JsonObject getCachedData(String serverName, String dataType) {
        ServerData serverData = serverDataMap.get(serverName);
        if (serverData != null) {
            return serverData.getCachedData(dataType);
        }
        return null;
    }

    public boolean hasCachedData(String serverName, String dataType) {
        ServerData serverData = serverDataMap.get(serverName);
        return serverData != null && serverData.hasCachedData(dataType);
    }

    public void cacheResponse(String serverName, String cacheKey, JsonObject response) {
        ServerData serverData = serverDataMap.computeIfAbsent(serverName, k -> new ServerData(serverName));
        serverData.cacheResponse(cacheKey, response);
    }

    public JsonObject getCachedResponse(String serverName, String cacheKey) {
        ServerData serverData = serverDataMap.get(serverName);
        if (serverData != null) {
            return serverData.getCachedResponse(cacheKey);
        }
        return null;
    }

    public boolean hasCachedResponse(String serverName, String cacheKey) {
        ServerData serverData = serverDataMap.get(serverName);
        return serverData != null && serverData.hasCachedResponse(cacheKey);
    }

    private void cleanupExpiredData() {
        long currentTime = System.currentTimeMillis();
        serverDataMap.values().forEach(serverData -> serverData.cleanupExpiredData(currentTime));

        // Remove servers with no data
        serverDataMap.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        logger.debug("Cleaned up expired cache data");
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static class ServerData {
        private final String serverName;
        private final ConcurrentHashMap<String, CachedData> dataCache = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, CachedData> responseCache = new ConcurrentHashMap<>();
        private static final long CACHE_TTL = TimeUnit.MINUTES.toMillis(5);

        public ServerData(String serverName) {
            this.serverName = serverName;
        }

        public void updateData(String dataType, JsonObject data) {
            dataCache.put(dataType, new CachedData(data, System.currentTimeMillis() + CACHE_TTL));
        }

        public JsonObject getCachedData(String dataType) {
            CachedData cached = dataCache.get(dataType);
            if (cached != null && !cached.isExpired()) {
                return cached.data;
            }
            return null;
        }

        public boolean hasCachedData(String dataType) {
            CachedData cached = dataCache.get(dataType);
            return cached != null && !cached.isExpired();
        }

        public void cacheResponse(String cacheKey, JsonObject response) {
            responseCache.put(cacheKey, new CachedData(response, System.currentTimeMillis() + CACHE_TTL));
        }

        public JsonObject getCachedResponse(String cacheKey) {
            CachedData cached = responseCache.get(cacheKey);
            if (cached != null && !cached.isExpired()) {
                return cached.data;
            }
            return null;
        }

        public boolean hasCachedResponse(String cacheKey) {
            CachedData cached = responseCache.get(cacheKey);
            return cached != null && !cached.isExpired();
        }

        public void cleanupExpiredData(long currentTime) {
            dataCache.entrySet().removeIf(entry -> entry.getValue().isExpired(currentTime));
            responseCache.entrySet().removeIf(entry -> entry.getValue().isExpired(currentTime));
        }

        public boolean isEmpty() {
            return dataCache.isEmpty() && responseCache.isEmpty();
        }

        private static class CachedData {
            final JsonObject data;
            final long expiryTime;

            CachedData(JsonObject data, long expiryTime) {
                this.data = data;
                this.expiryTime = expiryTime;
            }

            boolean isExpired() {
                return isExpired(System.currentTimeMillis());
            }

            boolean isExpired(long currentTime) {
                return currentTime > expiryTime;
            }
        }
    }
}