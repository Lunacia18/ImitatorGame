package com.imitatorgame.config;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MapConfig {

    private String worldName = "world";
    private Location lobbySpawn;
    private List<Location> gameSpawns = new ArrayList<>();
    private Location meetingRoom;
    private Location radioLocation;
    private Map<String, RoomCfg> rooms = new LinkedHashMap<>();
    private List<Location> ironDoors = new ArrayList<>();
    private List<TaskPosition> tasks = new ArrayList<>();
    private Location powerOutageFix;
    private Location floodingFix1;
    private Location floodingFix2;

    public record RoomCfg(String name, String type, Location center, double radius) {}

    public record TaskPosition(Location location, String type, String targetBlock, String room) {}

    public String getWorldName() { return worldName; }
    public void setWorldName(String v) { worldName = v; }

    public Location getLobbySpawn() { return lobbySpawn; }
    public void setLobbySpawn(Location v) { lobbySpawn = v; }

    public List<Location> getGameSpawns() { return gameSpawns; }
    public void setGameSpawns(List<Location> v) { gameSpawns = v; }

    public Location getMeetingRoom() { return meetingRoom; }
    public void setMeetingRoom(Location v) { meetingRoom = v; }

    public Location getRadioLocation() { return radioLocation; }
    public void setRadioLocation(Location v) { radioLocation = v; }

    public Map<String, RoomCfg> getRooms() { return rooms; }
    public void setRooms(Map<String, RoomCfg> v) { rooms = v; }

    public List<Location> getIronDoors() { return ironDoors; }
    public void setIronDoors(List<Location> v) { ironDoors = v; }

    public List<TaskPosition> getTasks() { return tasks; }
    public void setTasks(List<TaskPosition> v) { tasks = v; }

    public Location getPowerOutageFix() { return powerOutageFix; }
    public void setPowerOutageFix(Location v) { powerOutageFix = v; }

    public Location getFloodingFix1() { return floodingFix1; }
    public void setFloodingFix1(Location v) { floodingFix1 = v; }

    public Location getFloodingFix2() { return floodingFix2; }
    public void setFloodingFix2(Location v) { floodingFix2 = v; }
}
