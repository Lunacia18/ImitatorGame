package com.imitatorgame.map;

import com.imitatorgame.ImitatorGamePlugin;
import com.imitatorgame.util.Constants;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.util.Random;

public class LobbyManager {

    private final ImitatorGamePlugin plugin;
    private World lobbyWorld;
    private Location spawnPoint;
    private boolean initialized;

    private static final int PLATFORM_Y = 64;
    private static final int PLATFORM_RADIUS = 20;
    private static final Material PLATFORM_MATERIAL = Material.QUARTZ_BLOCK;
    private static final Material CENTER_MATERIAL = Material.GOLD_BLOCK;

    public LobbyManager(ImitatorGamePlugin plugin) {
        this.plugin = plugin;
    }

    public void initLobby() {
        String worldName = plugin.getConfigManager().getConfig()
                .getString("map.lobby-world", "imitator_lobby");

        World existing = Bukkit.getWorld(worldName);
        if (existing != null) {
            lobbyWorld = existing;
        } else {
            WorldCreator creator = new WorldCreator(worldName);
            creator.environment(World.Environment.NORMAL);
            creator.type(WorldType.FLAT);
            creator.generator(new VoidGenerator());
            creator.generateStructures(false);
            lobbyWorld = creator.createWorld();
        }

        if (lobbyWorld == null) {
            plugin.getLogger().warning("Failed to create lobby world!");
            return;
        }

        lobbyWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        lobbyWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        lobbyWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        lobbyWorld.setGameRule(GameRule.DO_FIRE_TICK, false);
        lobbyWorld.setGameRule(GameRule.FALL_DAMAGE, false);
        lobbyWorld.setGameRule(GameRule.KEEP_INVENTORY, true);
        lobbyWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        lobbyWorld.setGameRule(GameRule.DO_INSOMNIA, false);
        lobbyWorld.setGameRule(GameRule.DO_PATROL_SPAWNING, false);
        lobbyWorld.setGameRule(GameRule.DO_TRADER_SPAWNING, false);
        lobbyWorld.setGameRule(GameRule.DO_WARDEN_SPAWNING, false);
        lobbyWorld.setTime(6000);
        lobbyWorld.setClearWeatherDuration(Integer.MAX_VALUE);
        lobbyWorld.setDifficulty(Difficulty.PEACEFUL);

        buildPlatform();
        initialized = true;
        plugin.getLogger().info("Lobby world '" + worldName + "' initialized");
    }

    private void buildPlatform() {
        int y = PLATFORM_Y;
        int r = PLATFORM_RADIUS;

        // Main platform floor
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                double dist = Math.sqrt(x * x + z * z);
                if (dist <= r) {
                    Block block = lobbyWorld.getBlockAt(x, y, z);
                    if (dist <= 1.5) {
                        block.setType(Material.GOLD_BLOCK);  // center mark
                    } else if (dist > r - 1) {
                        block.setType(Material.BARRIER);  // invisible edge barrier in floor
                    } else {
                        block.setType(PLATFORM_MATERIAL);
                    }
                }
            }
        }

        // Barrier walls (3 blocks high around the edge)
        for (int x = -r - 1; x <= r + 1; x++) {
            for (int z = -r - 1; z <= r + 1; z++) {
                double dist = Math.sqrt(x * x + z * z);
                if (dist > r && dist <= r + 1.5) {
                    for (int dy = 0; dy < 3; dy++) {
                        lobbyWorld.getBlockAt(x, y + 1 + dy, z).setType(Material.BARRIER);
                    }
                }
            }
        }

        // Roof barrier (prevents ender pearl glitching out)
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                double dist = Math.sqrt(x * x + z * z);
                if (dist <= r) {
                    lobbyWorld.getBlockAt(x, y + 8, z).setType(Material.BARRIER);
                }
            }
        }

        spawnPoint = new Location(lobbyWorld, 0.5, PLATFORM_Y + 1, 0.5);
    }

    public Location getSpawnPoint() {
        if (spawnPoint == null) {
            return new Location(lobbyWorld, 0.5, PLATFORM_Y + 1, 0.5);
        }
        return spawnPoint.clone();
    }

    public void teleportToLobby(Player player) {
        if (!initialized || lobbyWorld == null) {
            player.sendMessage(Constants.PREFIX + "§c大厅尚未初始化，请联系管理员");
            return;
        }
        player.teleport(getSpawnPoint());
        player.setGameMode(GameMode.ADVENTURE);
        player.getInventory().clear();
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setSaturation(10);
        player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
    }

    public World getLobbyWorld() {
        return lobbyWorld;
    }

    public boolean isInLobbyWorld(Player player) {
        return lobbyWorld != null && player.getWorld().equals(lobbyWorld);
    }

    public void cleanup() {
        initialized = false;
        // Don't unload the world — it may be needed again
    }

    /**
     * Generates an empty void — no blocks at all.
     */
    public static class VoidGenerator extends ChunkGenerator {
        @Override
        public void generateNoise(WorldInfo worldInfo, Random random, int chunkX, int chunkZ,
                                   ChunkData chunkData) {
            // Generate nothing — pure void
        }
    }
}
