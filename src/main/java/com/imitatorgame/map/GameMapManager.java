package com.imitatorgame.map;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Door;
import java.util.*;

public class GameMapManager {

    private final World world;
    private final Location center;
    private final int halfSize = 25; // 50x50
    private final Material floorMaterial = Material.STONE_BRICKS;
    private final Map<Location, Material> coalBlocks = new LinkedHashMap<>();
    private final List<Location> taskLodestones = new ArrayList<>();
    private Location furnaceLoc, craftingTableLoc, cartographyTableLoc;
    private Location meetingRoomCenter;
    private Location door1Loc, door2Loc;
    private final Random random = new Random();

    public GameMapManager(World world, Location center) {
        this.world = world;
        this.center = center.clone();
        this.center.setY(64);
    }

    public void buildPlatform() {
        int y = 64, cx = center.getBlockX(), cz = center.getBlockZ(), hs = halfSize;
        // Floor + walls + clear
        for (int x = -hs; x <= hs; x++) {
            for (int z = -hs; z <= hs; z++) {
                Block floor = world.getBlockAt(cx + x, y, cz + z);
                floor.setType(floorMaterial);
                // Glowstone embedded every 5 blocks at floor level
                if (x % 5 == 0 && z % 5 == 0) floor.setType(Material.GLOWSTONE);
                // Edge: no barrier, just clear area (fall protection via block-break prevention)
                // Clear above
                for (int dy = 1; dy < 12; dy++) {
                    Block above = world.getBlockAt(cx + x, y + dy, cz + z);
                    if (abs(x) != hs && abs(z) != hs && above.getType() != Material.AIR && above.getType() != floorMaterial)
                        above.setType(Material.AIR);
                }
            }
        }

        // Center lodestone
        world.getBlockAt(cx, y, cz).setType(Material.LODESTONE);

        // Two iron doors in corners (bottom part + top part)
        door1Loc = placeIronDoor(cx - hs + 2, y + 1, cz - hs + 2, "north");
        door2Loc = placeIronDoor(cx + hs - 2, y + 1, cz + hs - 2, "south");

        // Cartography table near center
        world.getBlockAt(cx + 3, y + 1, cz + 3).setType(Material.CARTOGRAPHY_TABLE);
        cartographyTableLoc = new Location(world, cx + 3.5, y + 1, cz + 3.5);

        // Meeting room: build at offset from main platform
        buildMeetingRoom(cx, y, cz + hs + 10);

        // Place 6 random lodestones for tasks
        placeRandomLodestones();
        // Place furnace + crafting table
        placeUtilities();

        // World settings
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.FALL_DAMAGE, false);
        world.setTime(6000);
        world.setClearWeatherDuration(Integer.MAX_VALUE);
    }

    private Location placeIronDoor(int x, int y, int z, String facing) {
        world.getBlockAt(x, y, z).setType(Material.IRON_DOOR);
        world.getBlockAt(x, y + 1, z).setType(Material.IRON_DOOR);
        Block b = world.getBlockAt(x, y, z);
        if (b.getBlockData() instanceof Door d) {
            d.setFacing(org.bukkit.block.BlockFace.valueOf(facing.toUpperCase()));
            b.setBlockData(d);
        }
        return new Location(world, x + 0.5, y, z + 0.5);
    }

    private void buildMeetingRoom(int cx, int y, int cz) {
        // 10x10x5 meeting room
        int mx = cx, mz = cz, mr = 5;
        for (int x = -mr; x <= mr; x++) {
            for (int z = -mr; z <= mr; z++) {
                // Floor
                world.getBlockAt(mx + x, y, mz + z).setType(Material.POLISHED_BLACKSTONE);
                // Walls (5 high)
                if (Math.abs(x) == mr || Math.abs(z) == mr) {
                    for (int dy = 1; dy <= 5; dy++)
                        world.getBlockAt(mx + x, y + dy, mz + z).setType(Material.QUARTZ_BLOCK);
                }
                // Ceiling
                world.getBlockAt(mx + x, y + 5, mz + z).setType(Material.GLASS);
                // Clear interior
                for (int dy = 1; dy < 5; dy++) {
                    Block b = world.getBlockAt(mx + x, y + dy, mz + z);
                    if (abs(x) != mr && abs(z) != mr) b.setType(Material.AIR);
                }
            }
        }
        // Entrance (gap in wall on south side)
        for (int dy = 1; dy <= 2; dy++) {
            world.getBlockAt(mx, y + dy, mz + mr).setType(Material.AIR);
            world.getBlockAt(mx + 1, y + dy, mz + mr).setType(Material.AIR);
        }
        meetingRoomCenter = new Location(world, mx + 0.5, y + 1, mz + 0.5);
    }

    public List<Location> getMeetingRingPositions(int count) {
        List<Location> positions = new ArrayList<>();
        if (meetingRoomCenter == null) return positions;
        double angleStep = 2 * Math.PI / count;
        double radius = 3.5;
        for (int i = 0; i < count; i++) {
            double angle = i * angleStep;
            double x = meetingRoomCenter.getX() + radius * Math.cos(angle);
            double z = meetingRoomCenter.getZ() + radius * Math.sin(angle);
            positions.add(new Location(world, x, meetingRoomCenter.getY(), z));
        }
        return positions;
    }

    private void placeRandomLodestones() {
        int y = 65, cx = center.getBlockX(), cz = center.getBlockZ(), hs = halfSize - 1;
        taskLodestones.clear();
        for (int i = 0; i < 6; i++) {
            for (int a = 0; a < 50; a++) {
                int x = cx + random.nextInt(2 * hs) - hs;
                int z = cz + random.nextInt(2 * hs) - hs;
                Block b = world.getBlockAt(x, y, z);
                if (b.getType() == Material.AIR || b.getType() == Material.GLOWSTONE) {
                    b.setType(Material.LODESTONE);
                    taskLodestones.add(new Location(world, x + 0.5, y, z + 0.5));
                    break;
                }
            }
        }
    }

    private void placeUtilities() {
        int cx = center.getBlockX(), cz = center.getBlockZ(), y = 65;
        for (int a = 0; a < 50; a++) {
            int x = cx + random.nextInt(21) - 10, z = cz + random.nextInt(21) - 10;
            if (Math.abs(x) > halfSize - 1 || Math.abs(z) > halfSize - 1) continue;
            Block b = world.getBlockAt(x, y, z);
            if ((b.getType() == Material.AIR || b.getType() == Material.GLOWSTONE) && world.getBlockAt(x, y - 1, z).getType() == floorMaterial) {
                b.setType(Material.FURNACE); furnaceLoc = new Location(world, x + 0.5, y, z + 0.5); break;
            }
        }
        for (int a = 0; a < 50; a++) {
            int x = cx + random.nextInt(21) - 10, z = cz + random.nextInt(21) - 10;
            if (Math.abs(x) > halfSize - 1 || Math.abs(z) > halfSize - 1) continue;
            Block b = world.getBlockAt(x, y, z);
            if ((b.getType() == Material.AIR || b.getType() == Material.GLOWSTONE) && world.getBlockAt(x, y - 1, z).getType() == floorMaterial
                    && !(furnaceLoc != null && b.getLocation().equals(furnaceLoc.getBlock().getLocation()))) {
                b.setType(Material.CRAFTING_TABLE); craftingTableLoc = new Location(world, x + 0.5, y, z + 0.5); break;
            }
        }
    }

    public void setBlockGlowing(Location loc, boolean glowing) {
        if (loc == null) return;
        Block b = loc.getBlock();
        if (glowing) b.setType(Material.REDSTONE_LAMP);
        else if (b.getType() == Material.REDSTONE_LAMP) b.setType(b.getLocation().equals(furnaceLoc.getBlock().getLocation()) ? Material.FURNACE : Material.CRAFTING_TABLE);
    }

    public void revertCoalBlocks() {
        for (var e : new LinkedHashMap<>(coalBlocks).entrySet()) {
            e.getKey().getBlock().setType(e.getValue());
        }
        coalBlocks.clear();
    }

    public void placeCoalBlock(Location loc) {
        Location key = loc.getBlock().getLocation().clone();
        if (!coalBlocks.containsKey(key)) coalBlocks.put(key, loc.getBlock().getType());
        loc.getBlock().setType(Material.COAL_BLOCK);
    }

    public void removeCorpsesAndCoal() {
        for (int x = -halfSize; x <= halfSize; x++)
            for (int z = -halfSize; z <= halfSize; z++)
                if (world.getBlockAt(center.getBlockX() + x, 65, center.getBlockZ() + z).getType() == Material.BONE_BLOCK)
                    world.getBlockAt(center.getBlockX() + x, 65, center.getBlockZ() + z).setType(Material.AIR);
        revertCoalBlocks();
    }

    // Getters
    public Location getCenterSpawn() { return new Location(world, center.getBlockX() + 0.5, 65, center.getBlockZ() + 0.5); }
    public List<Location> getRandomSpawns(int count) {
        List<Location> spawns = new ArrayList<>();
        int hs = halfSize - 1, cx = center.getBlockX(), cz = center.getBlockZ();
        for (int i = 0; i < count; i++) {
            for (int a = 0; a < 30; a++) {
                int x = cx + random.nextInt(2 * hs) - hs, z = cz + random.nextInt(2 * hs) - hs;
                if (world.getBlockAt(x, 64, z).getType() == floorMaterial || world.getBlockAt(x, 64, z).getType() == Material.GLOWSTONE) {
                    spawns.add(new Location(world, x + 0.5, 65, z + 0.5)); break;
                }
            }
        }
        return spawns.isEmpty() ? List.of(getCenterSpawn()) : spawns;
    }
    public List<Location> getTaskLodestones() { return taskLodestones; }
    public Location getFurnaceLoc() { return furnaceLoc; }
    public Location getCraftingTableLoc() { return craftingTableLoc; }
    public Location getCartographyTableLoc() { return cartographyTableLoc; }
    public Location getMeetingRoomCenter() { return meetingRoomCenter; }
    public Location getDoor1Loc() { return door1Loc; }
    public Location getDoor2Loc() { return door2Loc; }
    private static int abs(int v) { return v < 0 ? -v : v; }
}
