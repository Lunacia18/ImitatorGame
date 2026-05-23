package com.imitatorgame.config;

import com.imitatorgame.util.LocationSerializer;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private GameConfig gameConfig;
    private MapConfig mapConfig;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        this.gameConfig = parseGameConfig();
        this.mapConfig = parseMapConfig();
    }

    public GameConfig getGameConfig() { return gameConfig; }
    public MapConfig getMapConfig() { return mapConfig; }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        this.gameConfig = parseGameConfig();
        this.mapConfig = parseMapConfig();
    }

    public void saveConfig() {
        plugin.saveConfig();
    }

    public FileConfiguration getConfig() {
        return config;
    }

    private GameConfig parseGameConfig() {
        ConfigurationSection g = config.getConfigurationSection("game");
        if (g == null) return GameConfig.defaults();

        ConfigurationSection r = g.getConfigurationSection("role-distribution");
        ConfigurationSection t = g.getConfigurationSection("timers");
        ConfigurationSection a = g.getConfigurationSection("abilities");

        return new GameConfig(
                g.getInt("min-players", 10),
                g.getInt("max-players", 12),
                r != null ? r.getInt("detective-count", 6) : 6,
                r != null ? r.getInt("imitator-count", 2) : 2,
                r != null ? r.getInt("mystery-guest-count", 2) : 2,
                t != null ? t.getInt("lobby-wait-seconds", 60) : 60,
                t != null ? t.getInt("role-reveal-seconds", 5) : 5,
                t != null ? t.getInt("discussion-seconds", 30) : 30,
                t != null ? t.getInt("voting-seconds", 20) : 20,
                t != null ? t.getInt("result-seconds", 10) : 10,
                t != null ? t.getInt("power-outage-seconds", 60) : 60,
                t != null ? t.getInt("flooding-seconds", 75) : 75,
                a != null ? a.getInt("hunter-max-uses", 1) : 1,
                a != null ? a.getInt("conspirator-max-guesses", 2) : 2,
                a != null ? a.getInt("master-thief-invisibility-seconds", 10) : 10,
                a != null ? a.getInt("master-thief-cooldown-seconds", 30) : 30,
                a != null ? a.getInt("disguise-duration-seconds", 30) : 30,
                a != null ? a.getInt("disguise-cooldown-seconds", 45) : 45,
                a != null ? a.getInt("spy-tracker-duration-seconds", 60) : 60,
                a != null ? a.getInt("spy-tracker-cooldown-seconds", 20) : 20,
                a != null ? a.getInt("vagabond-min-interactions", 5) : 5,
                a != null ? a.getInt("deliveryman-targets", 10) : 10,
                a != null ? a.getInt("bomb-fuse-seconds", 5) : 5,
                a != null ? a.getInt("bomb-radius", 3) : 3,
                a != null ? a.getInt("imitator-event-cooldown-seconds", 90) : 90,
                g.getInt("tasks-per-player", 5)
        );
    }

    @SuppressWarnings("unchecked")
    private MapConfig parseMapConfig() {
        ConfigurationSection m = config.getConfigurationSection("map");
        MapConfig mc = new MapConfig();
        if (m == null) return mc;

        mc.setWorldName(m.getString("world", "world"));
        mc.setLobbySpawn(LocationSerializer.fromConfig(m.get("lobby-spawn")));
        mc.setMeetingRoom(LocationSerializer.fromConfig(m.get("meeting-room")));
        mc.setRadioLocation(LocationSerializer.fromConfig(m.get("radio-location")));
        mc.setPowerOutageFix(LocationSerializer.fromConfig(m.get("power-outage-fix")));
        mc.setFloodingFix1(LocationSerializer.fromConfig(m.get("flooding-fix-1")));
        mc.setFloodingFix2(LocationSerializer.fromConfig(m.get("flooding-fix-2")));

        List<Location> spawns = new ArrayList<>();
        List<Map<?, ?>> spawnList = m.getMapList("game-spawns");
        for (Map<?, ?> sm : spawnList) {
            Location loc = LocationSerializer.fromMap((Map<String, Object>) sm);
            if (loc != null) spawns.add(loc);
        }
        mc.setGameSpawns(spawns);

        Map<String, MapConfig.RoomCfg> rooms = new LinkedHashMap<>();
        ConfigurationSection roomsSec = m.getConfigurationSection("rooms");
        if (roomsSec != null) {
            for (String key : roomsSec.getKeys(false)) {
                ConfigurationSection rs = roomsSec.getConfigurationSection(key);
                if (rs == null) continue;
                Location center = LocationSerializer.fromConfig(rs.get("center"));
                MapConfig.RoomCfg room = new MapConfig.RoomCfg(
                        key,
                        rs.getString("type", "HALLWAY"),
                        center,
                        rs.getDouble("radius", 5)
                );
                rooms.put(key, room);
            }
        }
        mc.setRooms(rooms);

        List<Location> doors = new ArrayList<>();
        List<Map<?, ?>> doorList = m.getMapList("iron-doors");
        for (Map<?, ?> dm : doorList) {
            Location loc = LocationSerializer.fromMap((Map<String, Object>) dm);
            if (loc != null) doors.add(loc);
        }
        mc.setIronDoors(doors);

        List<MapConfig.TaskPosition> tasks = new ArrayList<>();
        List<Map<?, ?>> taskList = m.getMapList("tasks");
        for (Map<?, ?> tm : taskList) {
            Location loc = LocationSerializer.fromMap((Map<String, Object>) tm.get("location"));
            if (loc != null) {
                Object typeObj = tm.get("type");
                Object blockObj = tm.get("target-block");
                Object roomObj = tm.get("room");
                tasks.add(new MapConfig.TaskPosition(
                        loc,
                        typeObj instanceof String s ? s : "PRESS_BUTTON",
                        blockObj instanceof String s ? s : "STONE_BUTTON",
                        roomObj instanceof String s ? s : ""
                ));
            }
        }
        mc.setTasks(tasks);

        return mc;
    }

    public void setConfigValue(String path, Object value) {
        config.set(path, value);
    }
}
