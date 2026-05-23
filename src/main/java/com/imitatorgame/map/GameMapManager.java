package com.imitatorgame.map;

import org.bukkit.*;
import org.bukkit.block.Block;
import java.util.*;

public class GameMapManager {

    private final World world;
    private final Location center;
    private final int halfSize = 15;
    private final Material floorMaterial = Material.STONE_BRICKS;
    private final Map<Location, Material> coalBlocks = new LinkedHashMap<>();
    private final List<Location> taskLodestones = new ArrayList<>();
    private Location furnaceLoc;
    private Location craftingTableLoc;
    private final Random random = new Random();

    public GameMapManager(World world, Location center) {
        this.world = world;
        this.center = center.clone();
        this.center.setY(64);
    }

    public void buildPlatform() {
        int y = 64, cx = center.getBlockX(), cz = center.getBlockZ(), hs = halfSize;
        for (int x = -hs; x <= hs; x++) {
            for (int z = -hs; z <= hs; z++) {
                Block floor = world.getBlockAt(cx + x, y, cz + z);
                floor.setType(floorMaterial);
                if (Math.abs(x) == hs || Math.abs(z) == hs) {
                    for (int dy = 1; dy <= 3; dy++)
                        world.getBlockAt(cx + x, y + dy, cz + z).setType(Material.BARRIER);
                }
                for (int dy = 1; dy < 10; dy++) {
                    Block above = world.getBlockAt(cx + x, y + dy, cz + z);
                    if (abs(x) != hs && abs(z) != hs && above.getType() != Material.AIR)
                        above.setType(Material.AIR);
                }
            }
        }
        world.getBlockAt(cx, y, cz).setType(Material.LODESTONE);

        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.FALL_DAMAGE, false);
        world.setTime(6000);
        world.setClearWeatherDuration(Integer.MAX_VALUE);

        // Place glowstone grid every 5 blocks
        placeGlowstoneGrid();
        // Place furnace + crafting table in 10x10 area near center
        placeUtilities();
    }

    private void placeGlowstoneGrid() {
        int y = 65, cx = center.getBlockX(), cz = center.getBlockZ(), hs = halfSize;
        for (int x = -hs; x <= hs; x += 5) {
            for (int z = -hs; z <= hs; z += 5) {
                world.getBlockAt(cx + x, y, cz + z).setType(Material.GLOWSTONE);
            }
        }
    }

    private void placeUtilities() {
        int cx = center.getBlockX(), cz = center.getBlockZ(), y = 65;
        // Furnace
        for (int attempt = 0; attempt < 50; attempt++) {
            int x = cx + random.nextInt(21) - 10;
            int z = cz + random.nextInt(21) - 10;
            if (Math.abs(x) > halfSize - 1 || Math.abs(z) > halfSize - 1) continue;
            Block b = world.getBlockAt(x, y, z);
            if (b.getType() == Material.AIR && world.getBlockAt(x, y - 1, z).getType() == floorMaterial) {
                b.setType(Material.FURNACE);
                furnaceLoc = new Location(world, x + 0.5, y, z + 0.5);
                break;
            }
        }
        // Crafting table (don't overlap furnace)
        for (int attempt = 0; attempt < 50; attempt++) {
            int x = cx + random.nextInt(21) - 10;
            int z = cz + random.nextInt(21) - 10;
            if (Math.abs(x) > halfSize - 1 || Math.abs(z) > halfSize - 1) continue;
            Block b = world.getBlockAt(x, y, z);
            Block furnaceBlock = furnaceLoc != null ? furnaceLoc.getBlock() : null;
            if (b.getType() == Material.AIR && world.getBlockAt(x, y - 1, z).getType() == floorMaterial
                    && (furnaceBlock == null || !b.equals(furnaceBlock))) {
                b.setType(Material.CRAFTING_TABLE);
                craftingTableLoc = new Location(world, x + 0.5, y, z + 0.5);
                break;
            }
        }
    }

    public void placeCoalBlock(Location loc) {
        Location key = loc.getBlock().getLocation().clone();
        if (!coalBlocks.containsKey(key)) {
            coalBlocks.put(key, loc.getBlock().getType());
        }
        loc.getBlock().setType(Material.COAL_BLOCK);
    }

    public void revertCoalBlocks() {
        for (var entry : new LinkedHashMap<>(coalBlocks).entrySet()) {
            entry.getKey().getBlock().setType(entry.getValue());
        }
        coalBlocks.clear();
    }

    public void removeCorpsesAndCoal() {
        // Remove all bone blocks
        for (int x = -halfSize; x <= halfSize; x++) {
            for (int z = -halfSize; z <= halfSize; z++) {
                Block b = world.getBlockAt(center.getBlockX() + x, 65, center.getBlockZ() + z);
                if (b.getType() == Material.BONE_BLOCK) b.setType(Material.AIR);
            }
        }
        revertCoalBlocks();
    }

    public Location getCenterSpawn() { return new Location(world, center.getBlockX() + 0.5, 65, center.getBlockZ() + 0.5); }

    public List<Location> getRandomSpawns(int count) {
        List<Location> spawns = new ArrayList<>();
        int hs = halfSize - 1, cx = center.getBlockX(), cz = center.getBlockZ();
        for (int i = 0; i < count; i++) {
            for (int attempt = 0; attempt < 30; attempt++) {
                int x = cx + random.nextInt(2 * hs) - hs;
                int z = cz + random.nextInt(2 * hs) - hs;
                Location loc = new Location(world, x + 0.5, 65, z + 0.5);
                if (world.getBlockAt(x, 64, z).getType() == floorMaterial) {
                    spawns.add(loc);
                    break;
                }
            }
        }
        return spawns.isEmpty() ? List.of(getCenterSpawn()) : spawns;
    }

    public List<Location> getTaskLodestones() { return taskLodestones; }
    public Location getFurnaceLoc() { return furnaceLoc; }
    public Location getCraftingTableLoc() { return craftingTableLoc; }
    private static int abs(int v) { return v < 0 ? -v : v; }
}
