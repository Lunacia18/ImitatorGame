package com.imitatorgame.map;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.*;

public class GameMapManager {

    private final World world;
    private final Location center;
    private final int halfSize = 15; // 30x30
    private final Material floorMaterial = Material.STONE_BRICKS;
    private final Map<Location, Material> coalBlocks = new LinkedHashMap<>(); // coal → original

    public GameMapManager(World world, Location center) {
        this.world = world;
        this.center = center.clone();
        this.center.setY(64);
    }

    public void buildPlatform() {
        int y = 64;
        int cx = center.getBlockX();
        int cz = center.getBlockZ();
        int hs = halfSize;

        // Clear area and build platform
        for (int x = -hs; x <= hs; x++) {
            for (int z = -hs; z <= hs; z++) {
                // Floor
                Block floor = world.getBlockAt(cx + x, y, cz + z);
                floor.setType(floorMaterial);

                // Walls (3 high barrier around edge)
                if (Math.abs(x) == hs || Math.abs(z) == hs) {
                    for (int dy = 1; dy <= 3; dy++) {
                        world.getBlockAt(cx + x, y + dy, cz + z).setType(Material.BARRIER);
                    }
                }

                // Clear above
                for (int dy = 1; dy < 10; dy++) {
                    Block above = world.getBlockAt(cx + x, y + dy, cz + z);
                    if (abs(x) != hs && abs(z) != hs && above.getType() != Material.AIR) {
                        above.setType(Material.AIR);
                    }
                }
            }
        }

        // Center marker
        world.getBlockAt(cx, y, cz).setType(Material.LODESTONE);

        // World settings
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.FALL_DAMAGE, false);
        world.setTime(6000);
        world.setClearWeatherDuration(Integer.MAX_VALUE);
    }

    public void placeCoalBlock(Location loc) {
        Location key = loc.getBlock().getLocation().clone();
        Material original = loc.getBlock().getType();
        coalBlocks.put(key, original);
        loc.getBlock().setType(Material.COAL_BLOCK);
    }

    public void revertCoalBlocks() {
        for (var entry : new LinkedHashMap<>(coalBlocks).entrySet()) {
            entry.getKey().getBlock().setType(entry.getValue());
        }
        coalBlocks.clear();
    }

    public Location getCenterSpawn() {
        return new Location(world, center.getBlockX() + 0.5, 65, center.getBlockZ() + 0.5);
    }

    public List<Location> getDistributedSpawns(int count) {
        List<Location> spawns = new ArrayList<>();
        double angleStep = 2 * Math.PI / count;
        double radius = halfSize * 0.6;
        int cx = center.getBlockX();
        int cz = center.getBlockZ();
        for (int i = 0; i < count; i++) {
            double angle = i * angleStep;
            double x = cx + 0.5 + radius * Math.cos(angle);
            double z = cz + 0.5 + radius * Math.sin(angle);
            spawns.add(new Location(world, x, 65, z));
        }
        return spawns;
    }

    private static int abs(int v) { return v < 0 ? -v : v; }
}
