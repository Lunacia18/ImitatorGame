package com.imitatorgame.task;

import org.bukkit.Location;

import java.util.UUID;

public class TaskInstance {

    private final Location location;
    private final TaskType type;
    private final UUID assignedPlayer;
    private boolean completed = false;

    public TaskInstance(Location location, TaskType type, UUID assignedPlayer) {
        this.location = location;
        this.type = type;
        this.assignedPlayer = assignedPlayer;
    }

    public Location getLocation() { return location; }
    public TaskType getType() { return type; }
    public UUID getAssignedPlayer() { return assignedPlayer; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean v) { completed = v; }
}
