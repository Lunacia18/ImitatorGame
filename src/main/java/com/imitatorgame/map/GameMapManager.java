package com.imitatorgame.map;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.*;
import java.util.*;

/**
 * Manages the game map: builds a 50×50 themed platform with rooms,
 * meeting room, task points, iron doors, and event fix locations.
 *
 * Map layout inspired by Identity V's Imitator Game (模仿者游戏).
 */
public class GameMapManager {

    private final World world;
    private final Location center;
    private final int halfSize = 25; // 50×50 platform
    private static final int FLOOR_Y = 64;
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
        this.center.setY(FLOOR_Y);
    }

    // ─── Main build entry point ───────────────────────────────────────────

    public void buildPlatform() {
        int cx = center.getBlockX(), cz = center.getBlockZ();

        // 0. ═══ DESTROY old map completely before building new one ═══
        clearEntireGameArea(cx, cz);

        // 1. Clear and lay the main floor
        buildMainFloor(cx, cz);

        // 2. Build themed rooms
        buildDiningRoom(cx + 30, cz + 10);       // 餐厅
        buildSurveillanceRoom(cx - 20, cz + 30);  // 监控室
        buildDormitory(cx + 40, cz - 15);         // 宿舍
        buildPowerRoom(cx - 30, cz - 20);         // 电力室
        buildBathroom(cx + 15, cz - 30);          // 浴室1
        buildBathroom(cx + 25, cz + 35);          // 浴室2

        // 3. Build hallways connecting rooms to center
        buildHallways(cx, cz);

        // 4. Build outer walls along platform edges
        buildOuterWalls(cx, cz);

        // 5. Place iron doors at opposite corners
        door1Loc = placeIronDoor(cx - halfSize + 2, FLOOR_Y + 1, cz - halfSize + 2, BlockFace.NORTH);
        door2Loc = placeIronDoor(cx + halfSize - 2, FLOOR_Y + 1, cz + halfSize - 2, BlockFace.SOUTH);

        // 6. Cartography table (emergency meeting trigger) near center
        world.getBlockAt(cx + 3, FLOOR_Y + 1, cz + 3).setType(Material.CARTOGRAPHY_TABLE);
        cartographyTableLoc = new Location(world, cx + 3.5, FLOOR_Y + 1, cz + 3.5);

        // 7. Build meeting room south of the platform
        buildMeetingRoom(cx, FLOOR_Y, cz + halfSize + 10);

        // 8. Place task lodestones inside rooms
        placeTaskLodestonesInRooms();

        // 9. Place furnace in power room, crafting table in dormitory
        furnaceLoc = new Location(world, cx - 29.5, FLOOR_Y + 1, cz - 19.5);
        world.getBlockAt(cx - 30, FLOOR_Y + 1, cz - 20).setType(Material.FURNACE);
        craftingTableLoc = new Location(world, cx + 41.5, FLOOR_Y + 1, cz - 16.5);
        world.getBlockAt(cx + 41, FLOOR_Y + 1, cz - 17).setType(Material.CRAFTING_TABLE);

        // 10. Make the power room furnace and bathroom furnaces usable for event fixes
        world.getBlockAt(cx - 30, FLOOR_Y + 1, cz - 20).setType(Material.FURNACE); // power fix
        world.getBlockAt(cx + 15, FLOOR_Y + 1, cz - 31).setType(Material.FURNACE); // flood fix 1
        world.getBlockAt(cx + 25, FLOOR_Y + 1, cz + 36).setType(Material.FURNACE); // flood fix 2

        // World settings
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.FALL_DAMAGE, false);
        world.setTime(6000);
        world.setClearWeatherDuration(Integer.MAX_VALUE);
    }

    // ─── Old Map Destruction ────────────────────────────────────────────────

    /**
     * Completely wipes the entire game area — platform, rooms, meeting room,
     * hallways, walls, everything. Runs BEFORE any new building.
     * Clear zone: ~75×75 blocks horizontally, y=64→78 vertically.
     */
    private void clearEntireGameArea(int cx, int cz) {
        int clearRadius = halfSize + 25; // covers all rooms + meeting room
        for (int x = -clearRadius; x <= clearRadius; x++) {
            for (int z = -clearRadius; z <= clearRadius; z++) {
                // Clear from floor level up to above the meeting room roof
                for (int y = FLOOR_Y; y <= FLOOR_Y + 8; y++) {
                    Block b = world.getBlockAt(cx + x, y, cz + z);
                    if (b.getType() != Material.AIR && b.getType() != Material.BEDROCK) {
                        b.setType(Material.AIR, false);
                    }
                }
                // Also clear elevated structures higher up (meeting room glass ceiling)
                for (int y = FLOOR_Y + 9; y <= FLOOR_Y + 14; y++) {
                    Block b = world.getBlockAt(cx + x, y, cz + z);
                    if (b.getType() != Material.AIR && b.getType() != Material.BEDROCK) {
                        b.setType(Material.AIR, false);
                    }
                }
            }
        }
    }

    // ─── Main Floor ────────────────────────────────────────────────────────

    private void buildMainFloor(int cx, int cz) {
        int hs = halfSize;
        for (int x = -hs; x <= hs; x++) {
            for (int z = -hs; z <= hs; z++) {
                Block floor = world.getBlockAt(cx + x, FLOOR_Y, cz + z);
                floor.setType(floorMaterial);
                // Glowstone embedded every 5 blocks
                if (x % 5 == 0 && z % 5 == 0) floor.setType(Material.GLOWSTONE);
                // Clear above
                for (int dy = 1; dy < 12; dy++) {
                    world.getBlockAt(cx + x, FLOOR_Y + dy, cz + z).setType(Material.AIR);
                }
            }
        }
        // Center lodestone marker
        world.getBlockAt(cx, FLOOR_Y, cz).setType(Material.LODESTONE);
        // 3×3 gold highlight at center
        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++)
                world.getBlockAt(cx + x, FLOOR_Y, cz + z).setType(Material.SMOOTH_STONE);
        world.getBlockAt(cx, FLOOR_Y, cz).setType(Material.LODESTONE);
    }

    // ─── Room Builders ─────────────────────────────────────────────────────

    /** 餐厅 - Dining Room: warm oak interior, tables, chairs */
    private void buildDiningRoom(int rx, int rz) {
        int r = 8;
        buildRoomShell(rx, rz, r, Material.OAK_PLANKS, Material.OAK_LOG, Material.OAK_PLANKS);
        // Long dining table in center
        for (int tx = -3; tx <= 3; tx++)
            for (int tz = -1; tz <= 1; tz++)
                world.getBlockAt(rx + tx, FLOOR_Y + 1, rz + tz).setType(Material.OAK_PLANKS);
        // Glowstone chandelier above
        world.getBlockAt(rx, FLOOR_Y + 4, rz).setType(Material.GLOWSTONE);
        // Chairs (oak stairs) around table
        placeStairs(rx - 3, FLOOR_Y + 1, rz, BlockFace.EAST, Material.OAK_STAIRS);
        placeStairs(rx + 3, FLOOR_Y + 1, rz, BlockFace.WEST, Material.OAK_STAIRS);
        // Paintings on walls
        world.getBlockAt(rx, FLOOR_Y + 2, rz + r - 1).setType(Material.PAINTING);
        world.getBlockAt(rx, FLOOR_Y + 2, rz - r + 1).setType(Material.PAINTING);
        // Doorway facing center
        makeDoorway(rx, FLOOR_Y + 1, rz - r, BlockFace.NORTH);
    }

    /** 监控室 - Surveillance Room: dark tech theme */
    private void buildSurveillanceRoom(int rx, int rz) {
        int r = 6;
        buildRoomShell(rx, rz, r, Material.POLISHED_DEEPSLATE, Material.DEEPSLATE_TILES, Material.OBSIDIAN);
        // "Monitor screens" — glowstone panels on wall
        for (int i = -2; i <= 2; i++) {
            world.getBlockAt(rx + i, FLOOR_Y + 2, rz + r - 1).setType(Material.GLOWSTONE);
            world.getBlockAt(rx + i, FLOOR_Y + 3, rz + r - 1).setType(Material.GLOWSTONE);
        }
        // Redstone torches for "surveillance active" feel
        world.getBlockAt(rx - 3, FLOOR_Y + 2, rz).setType(Material.REDSTONE_TORCH);
        world.getBlockAt(rx + 3, FLOOR_Y + 2, rz).setType(Material.REDSTONE_TORCH);
        // Observer blocks as "cameras"
        world.getBlockAt(rx, FLOOR_Y + 2, rz + 2).setType(Material.OBSERVER);
        world.getBlockAt(rx, FLOOR_Y + 2, rz - 2).setType(Material.OBSERVER);
        makeDoorway(rx, FLOOR_Y + 1, rz - r, BlockFace.NORTH);
    }

    /** 宿舍 - Dormitory: cozy bedroom */
    private void buildDormitory(int rx, int rz) {
        int r = 7;
        buildRoomShell(rx, rz, r, Material.OAK_PLANKS, Material.STRIPPED_OAK_LOG, Material.OAK_PLANKS);
        // Beds along walls
        placeBed(rx - 5, FLOOR_Y + 1, rz, BlockFace.EAST, Material.RED_BED);
        placeBed(rx + 5, FLOOR_Y + 1, rz, BlockFace.WEST, Material.BLUE_BED);
        placeBed(rx, FLOOR_Y + 1, rz - 5, BlockFace.SOUTH, Material.YELLOW_BED);
        // Chests
        world.getBlockAt(rx - 4, FLOOR_Y + 1, rz + 4).setType(Material.CHEST);
        world.getBlockAt(rx + 4, FLOOR_Y + 1, rz - 4).setType(Material.CHEST);
        // Bookshelf
        world.getBlockAt(rx + 3, FLOOR_Y + 1, rz + 3).setType(Material.BOOKSHELF);
        // Lantern hanging
        world.getBlockAt(rx, FLOOR_Y + 5, rz).setType(Material.LANTERN);
        makeDoorway(rx, FLOOR_Y + 1, rz - r, BlockFace.NORTH);
    }

    /** 电力室 - Power Room: industrial, redstone theme */
    private void buildPowerRoom(int rx, int rz) {
        int r = 5;
        buildRoomShell(rx, rz, r, Material.STONE_BRICKS, Material.IRON_BLOCK, Material.STONE_BRICKS);
        // Redstone lamps (flickering feel)
        world.getBlockAt(rx, FLOOR_Y + 2, rz + 2).setType(Material.REDSTONE_LAMP);
        world.getBlockAt(rx, FLOOR_Y + 2, rz - 2).setType(Material.REDSTONE_LAMP);
        // Levers on walls
        world.getBlockAt(rx - 2, FLOOR_Y + 1, rz + r - 1).setType(Material.LEVER);
        world.getBlockAt(rx + 2, FLOOR_Y + 1, rz + r - 1).setType(Material.LEVER);
        // "Generator" — iron blocks + redstone block
        world.getBlockAt(rx, FLOOR_Y + 1, rz).setType(Material.IRON_BLOCK);
        world.getBlockAt(rx, FLOOR_Y + 1, rz + 1).setType(Material.REDSTONE_BLOCK);
        // Furnace for fixing power outage
        world.getBlockAt(rx - 1, FLOOR_Y + 1, rz - 1).setType(Material.FURNACE);
        makeDoorway(rx, FLOOR_Y + 1, rz - r, BlockFace.NORTH);
    }

    /** 浴室 - Bathroom: white/quartz, water features */
    private void buildBathroom(int rx, int rz) {
        int r = 4;
        buildRoomShell(rx, rz, r, Material.QUARTZ_BLOCK, Material.SMOOTH_QUARTZ, Material.QUARTZ_BLOCK);
        // Cauldron with water
        world.getBlockAt(rx, FLOOR_Y + 1, rz + 1).setType(Material.WATER_CAULDRON);
        // Waterlogged feel — blue glass
        world.getBlockAt(rx + 1, FLOOR_Y + 1, rz - 1).setType(Material.LIGHT_BLUE_STAINED_GLASS);
        world.getBlockAt(rx - 1, FLOOR_Y + 1, rz - 1).setType(Material.LIGHT_BLUE_STAINED_GLASS);
        // Sea lantern for lighting
        world.getBlockAt(rx, FLOOR_Y + 3, rz).setType(Material.SEA_LANTERN);
        // Furnace for fixing flooding
        world.getBlockAt(rx, FLOOR_Y + 1, rz + 2).setType(Material.FURNACE);
        makeDoorway(rx, FLOOR_Y + 1, rz - r, BlockFace.NORTH);
    }

    // ─── Room Shell Helper ─────────────────────────────────────────────────

    /** Builds an enclosed room: floor, walls (3 high), ceiling, doorway */
    private void buildRoomShell(int rx, int rz, int r, Material floorMat, Material wallMat, Material ceilingMat) {
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                // Floor
                Block floor = world.getBlockAt(rx + x, FLOOR_Y, rz + z);
                floor.setType(floorMat);
                // Ceiling
                world.getBlockAt(rx + x, FLOOR_Y + 4, rz + z).setType(ceilingMat);
                // Walls (3 blocks high)
                if (Math.abs(x) == r || Math.abs(z) == r) {
                    for (int dy = 1; dy <= 3; dy++)
                        world.getBlockAt(rx + x, FLOOR_Y + dy, rz + z).setType(wallMat);
                }
            }
        }
        // Glowstone corners for lighting
        world.getBlockAt(rx - r + 1, FLOOR_Y + 3, rz - r + 1).setType(Material.GLOWSTONE);
        world.getBlockAt(rx + r - 1, FLOOR_Y + 3, rz - r + 1).setType(Material.GLOWSTONE);
        world.getBlockAt(rx - r + 1, FLOOR_Y + 3, rz + r - 1).setType(Material.GLOWSTONE);
        world.getBlockAt(rx + r - 1, FLOOR_Y + 3, rz + r - 1).setType(Material.GLOWSTONE);
    }

    /** Creates a 2-wide×2-high doorway in a wall */
    private void makeDoorway(int x, int y, int z, BlockFace facing) {
        world.getBlockAt(x, y, z).setType(Material.AIR);
        world.getBlockAt(x, y + 1, z).setType(Material.AIR);
        world.getBlockAt(x + 1, y, z).setType(Material.AIR);
        world.getBlockAt(x + 1, y + 1, z).setType(Material.AIR);
    }

    /** Place a stair block with correct facing */
    private void placeStairs(int x, int y, int z, BlockFace facing, Material mat) {
        Block b = world.getBlockAt(x, y, z);
        b.setType(mat);
        if (b.getBlockData() instanceof Stairs s) {
            s.setFacing(facing);
            b.setBlockData(s);
        }
    }

    /** Place a bed (head + foot) */
    private void placeBed(int x, int y, int z, BlockFace facing, Material bedMat) {
        Block head = world.getBlockAt(x, y, z);
        Block foot = world.getBlockAt(x + facing.getOppositeFace().getModX(), y,
                z + facing.getOppositeFace().getModZ());
        head.setType(bedMat);
        if (head.getBlockData() instanceof Bed h) {
            h.setPart(Bed.Part.HEAD);
            h.setFacing(facing);
            head.setBlockData(h);
        }
        foot.setType(bedMat);
        if (foot.getBlockData() instanceof Bed f) {
            f.setPart(Bed.Part.FOOT);
            f.setFacing(facing);
            foot.setBlockData(f);
        }
    }

    // ─── Hallways ──────────────────────────────────────────────────────────

    /** Build lit hallways connecting rooms to the central plaza */
    private void buildHallways(int cx, int cz) {
        // Hallway: center → dining room (northeast)
        buildHallwaySegment(cx, cz, cx + 30, cz + 10);
        // Hallway: center → surveillance room (northwest)
        buildHallwaySegment(cx, cz, cx - 20, cz + 30);
        // Hallway: center → dormitory (southeast)
        buildHallwaySegment(cx, cz, cx + 40, cz - 15);
        // Hallway: center → power room (southwest)
        buildHallwaySegment(cx, cz, cx - 30, cz - 20);
        // Hallway: center → bathroom 1 (south)
        buildHallwaySegment(cx, cz, cx + 15, cz - 30);
        // Hallway: center → bathroom 2 (north)
        buildHallwaySegment(cx, cz, cx + 25, cz + 35);
    }

    private void buildHallwaySegment(int fromX, int fromZ, int toX, int toZ) {
        // Draw a 3-wide path from origin to destination
        int dx = Integer.signum(toX - fromX);
        int dz = Integer.signum(toZ - fromZ);
        int x = fromX, z = fromZ;
        int maxSteps = Math.max(Math.abs(toX - fromX), Math.abs(toZ - fromZ)) + 1;

        for (int i = 0; i < maxSteps; i++) {
            for (int wx = -1; wx <= 1; wx++) {
                for (int wz = -1; wz <= 1; wz++) {
                    int bx = x + wx, bz = z + wz;
                    Block floor = world.getBlockAt(bx, FLOOR_Y, bz);
                    Block above = world.getBlockAt(bx, FLOOR_Y + 1, bz);
                    // Only change if it's stone bricks (platform) or air
                    if (floor.getType() == floorMaterial || floor.getType() == Material.AIR)
                        floor.setType(Material.POLISHED_ANDESITE);
                    if (above.getType() == Material.AIR || above.getType() == floorMaterial)
                        above.setType(Material.AIR);
                }
            }
            // Place lanterns every 5 blocks
            if (i % 5 == 2) {
                world.getBlockAt(x, FLOOR_Y + 3, z).setType(Material.LANTERN);
                world.getBlockAt(x, FLOOR_Y, z).setType(Material.GLOWSTONE);
            }
            if (x != toX) x += dx;
            if (z != toZ) z += dz;
        }
    }

    // ─── Outer Walls ───────────────────────────────────────────────────────

    private void buildOuterWalls(int cx, int cz) {
        int hs = halfSize;
        Material wallMat = Material.DEEPSLATE_TILES;
        for (int i = -hs; i <= hs; i++) {
            int absI = Math.abs(i);
            if (absI > hs - 1) continue;
            // North wall (cz + hs)
            if (world.getBlockAt(cx + i, FLOOR_Y, cz + hs).getType() == floorMaterial
                    || world.getBlockAt(cx + i, FLOOR_Y, cz + hs).getType() == Material.GLOWSTONE) {
                for (int dy = 1; dy <= 3; dy++)
                    world.getBlockAt(cx + i, FLOOR_Y + dy, cz + hs).setType(wallMat);
            }
            // South wall
            if (world.getBlockAt(cx + i, FLOOR_Y, cz - hs).getType() == floorMaterial
                    || world.getBlockAt(cx + i, FLOOR_Y, cz - hs).getType() == Material.GLOWSTONE) {
                for (int dy = 1; dy <= 3; dy++)
                    world.getBlockAt(cx + i, FLOOR_Y + dy, cz - hs).setType(wallMat);
            }
            // East wall
            if (world.getBlockAt(cx + hs, FLOOR_Y, cz + i).getType() == floorMaterial
                    || world.getBlockAt(cx + hs, FLOOR_Y, cz + i).getType() == Material.GLOWSTONE) {
                for (int dy = 1; dy <= 3; dy++)
                    world.getBlockAt(cx + hs, FLOOR_Y + dy, cz + i).setType(wallMat);
            }
            // West wall
            if (world.getBlockAt(cx - hs, FLOOR_Y, cz + i).getType() == floorMaterial
                    || world.getBlockAt(cx - hs, FLOOR_Y, cz + i).getType() == Material.GLOWSTONE) {
                for (int dy = 1; dy <= 3; dy++)
                    world.getBlockAt(cx - hs, FLOOR_Y + dy, cz + i).setType(wallMat);
            }
        }
    }

    // ─── Meeting Room ──────────────────────────────────────────────────────

    private void buildMeetingRoom(int cx, int y, int cz) {
        int mx = cx, mz = cz, mr = 5;
        // Blackstone floor + quartz walls
        for (int x = -mr; x <= mr; x++) {
            for (int z = -mr; z <= mr; z++) {
                world.getBlockAt(mx + x, y, mz + z).setType(Material.POLISHED_BLACKSTONE);
                if (Math.abs(x) == mr || Math.abs(z) == mr) {
                    for (int dy = 1; dy <= 5; dy++)
                        world.getBlockAt(mx + x, y + dy, mz + z).setType(Material.QUARTZ_BLOCK);
                }
                // Glass ceiling
                world.getBlockAt(mx + x, y + 5, mz + z).setType(Material.TINTED_GLASS);
                // Clear interior
                for (int dy = 1; dy < 5; dy++) {
                    Block b = world.getBlockAt(mx + x, y + dy, mz + z);
                    if (Math.abs(x) != mr && Math.abs(z) != mr) b.setType(Material.AIR);
                }
            }
        }
        // Entrance (south side, 2-wide)
        for (int dy = 1; dy <= 2; dy++) {
            world.getBlockAt(mx, y + dy, mz + mr).setType(Material.AIR);
            world.getBlockAt(mx + 1, y + dy, mz + mr).setType(Material.AIR);
        }
        // Red carpet path leading to entrance
        for (int z = 1; z <= 5; z++) {
            world.getBlockAt(mx, y, mz + mr + z).setType(Material.RED_CARPET);
            world.getBlockAt(mx + 1, y, mz + mr + z).setType(Material.RED_CARPET);
        }
        // Central table: 3×3 oak + glowstone center
        for (int tx = -1; tx <= 1; tx++)
            for (int tz = -1; tz <= 1; tz++)
                world.getBlockAt(mx + tx, y + 1, mz + tz).setType(Material.OAK_PLANKS);
        world.getBlockAt(mx, y + 1, mz).setType(Material.GLOWSTONE);
        // Meeting room lanterns in corners
        world.getBlockAt(mx - mr + 1, y + 4, mz - mr + 1).setType(Material.LANTERN);
        world.getBlockAt(mx + mr - 1, y + 4, mz - mr + 1).setType(Material.LANTERN);
        world.getBlockAt(mx - mr + 1, y + 4, mz + mr - 1).setType(Material.LANTERN);
        world.getBlockAt(mx + mr - 1, y + 4, mz + mr - 1).setType(Material.LANTERN);
        meetingRoomCenter = new Location(world, mx + 0.5, y + 1, mz + 0.5);
    }

    // ─── Utility placement ─────────────────────────────────────────────────

    private Location placeIronDoor(int x, int y, int z, BlockFace facing) {
        Block lower = world.getBlockAt(x, y, z);
        Block upper = world.getBlockAt(x, y + 1, z);
        lower.setType(Material.IRON_DOOR);
        upper.setType(Material.IRON_DOOR);
        if (lower.getBlockData() instanceof Door d) {
            d.setFacing(facing);
            d.setHalf(Bisected.Half.BOTTOM);
            lower.setBlockData(d);
        }
        if (upper.getBlockData() instanceof Door d) {
            d.setFacing(facing);
            d.setHalf(Bisected.Half.TOP);
            upper.setBlockData(d);
        }
        return new Location(world, x + 0.5, y, z + 0.5);
    }

    /** Place 6 task lodestones at strategic locations inside rooms */
    private void placeTaskLodestonesInRooms() {
        int cx = center.getBlockX(), cz = center.getBlockZ();
        taskLodestones.clear();
        int[][] placements = {
                {cx + 30, cz + 8},    // dining room
                {cx - 20, cz + 28},   // surveillance room
                {cx + 38, cz - 15},   // dormitory
                {cx - 28, cz - 20},   // power room
                {cx + 15, cz - 28},   // bathroom 1
                {cx + 25, cz + 33},   // bathroom 2
        };
        for (int[] p : placements) {
            world.getBlockAt(p[0], FLOOR_Y + 1, p[1]).setType(Material.LODESTONE);
            taskLodestones.add(new Location(world, p[0] + 0.5, FLOOR_Y + 1, p[1] + 0.5));
        }
    }

    // ─── Glow toggle for event fixes ───────────────────────────────────────

    public void setBlockGlowing(Location loc, boolean glowing) {
        if (loc == null) return;
        Block b = loc.getBlock();
        if (glowing) {
            if (b.getType() == Material.FURNACE || b.getType() == Material.CRAFTING_TABLE) {
                b.setType(Material.REDSTONE_LAMP);
            }
        } else {
            if (b.getType() == Material.REDSTONE_LAMP) {
                // Restore original type based on location
                Material restore = b.getLocation().equals(furnaceLoc) ? Material.FURNACE : Material.CRAFTING_TABLE;
                b.setType(restore);
            }
        }
    }

    // ─── Bomb / Coal management ────────────────────────────────────────────

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
        int cx = center.getBlockX(), cz = center.getBlockZ();
        for (int x = -halfSize; x <= halfSize; x++)
            for (int z = -halfSize; z <= halfSize; z++)
                if (world.getBlockAt(cx + x, FLOOR_Y + 1, cz + z).getType() == Material.BONE_BLOCK)
                    world.getBlockAt(cx + x, FLOOR_Y + 1, cz + z).setType(Material.AIR);
        revertCoalBlocks();
    }

    // ─── Meeting ring ──────────────────────────────────────────────────────

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

    // ─── Getters ───────────────────────────────────────────────────────────

    public Location getCenterSpawn() {
        return new Location(world, center.getBlockX() + 0.5, FLOOR_Y + 1, center.getBlockZ() + 0.5);
    }

    public List<Location> getRandomSpawns(int count) {
        List<Location> spawns = new ArrayList<>();
        int hs = halfSize - 2, cx = center.getBlockX(), cz = center.getBlockZ();
        for (int i = 0; i < count; i++) {
            for (int a = 0; a < 50; a++) {
                int x = cx + random.nextInt(2 * hs) - hs;
                int z = cz + random.nextInt(2 * hs) - hs;
                Material mat = world.getBlockAt(x, FLOOR_Y, z).getType();
                if (mat != Material.AIR && mat != Material.GLOWSTONE) {
                    spawns.add(new Location(world, x + 0.5, FLOOR_Y + 1, z + 0.5));
                    break;
                }
            }
        }
        if (spawns.isEmpty()) spawns.add(getCenterSpawn());
        return spawns;
    }

    public List<Location> getTaskLodestones() { return taskLodestones; }
    public Location getFurnaceLoc() { return furnaceLoc; }
    public Location getCraftingTableLoc() { return craftingTableLoc; }
    public Location getCartographyTableLoc() { return cartographyTableLoc; }
    public Location getMeetingRoomCenter() { return meetingRoomCenter; }
    public Location getDoor1Loc() { return door1Loc; }
    public Location getDoor2Loc() { return door2Loc; }
}
