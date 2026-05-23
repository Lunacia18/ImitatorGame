package com.imitatorgame.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Map;

public final class LocationSerializer {

    private LocationSerializer() {
    }

    public static Location fromMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return null;
        World world = Bukkit.getWorld((String) map.getOrDefault("world", "world"));
        if (world == null) return null;
        double x = ((Number) map.getOrDefault("x", 0)).doubleValue();
        double y = ((Number) map.getOrDefault("y", 64)).doubleValue();
        double z = ((Number) map.getOrDefault("z", 0)).doubleValue();
        float yaw = ((Number) map.getOrDefault("yaw", 0)).floatValue();
        float pitch = ((Number) map.getOrDefault("pitch", 0)).floatValue();
        return new Location(world, x, y, z, yaw, pitch);
    }

    @SuppressWarnings("unchecked")
    public static Location fromConfig(Object section) {
        if (section instanceof Map) {
            return fromMap((Map<String, Object>) section);
        }
        return null;
    }

    public static Map<String, Object> toMap(Location loc) {
        return Map.of(
                "world", loc.getWorld() != null ? loc.getWorld().getName() : "world",
                "x", loc.getX(),
                "y", loc.getY(),
                "z", loc.getZ(),
                "yaw", (double) loc.getYaw(),
                "pitch", (double) loc.getPitch()
        );
    }
}
