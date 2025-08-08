package fr.neocle.flexgateway.bukkit.data.collectors;

import com.google.gson.JsonObject;

public class SystemInfoCollector {

    public JsonObject getMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        JsonObject memoryInfo = new JsonObject();
        memoryInfo.addProperty("maxMemoryMB", maxMemory / (1024 * 1024));
        memoryInfo.addProperty("totalMemoryMB", totalMemory / (1024 * 1024));
        memoryInfo.addProperty("usedMemoryMB", usedMemory / (1024 * 1024));
        memoryInfo.addProperty("freeMemoryMB", freeMemory / (1024 * 1024));

        return memoryInfo;
    }

    public String getServerUptime() {
        long uptimeMs = System.currentTimeMillis() - System.getProperty("java.class.path").hashCode();
        long seconds = uptimeMs / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    public double getCurrentTPS() {
        try {
            Class<?> serverClass = Class.forName("net.minecraft.server.MinecraftServer");
            Object server = serverClass.getMethod("getServer").invoke(null);
            double[] tps = (double[]) server.getClass().getField("recentTps").get(server);
            return Math.min(Math.round(tps[0] * 100.0) / 100.0, 20.0);
        } catch (Exception e) {
            return -1.0;
        }
    }
}