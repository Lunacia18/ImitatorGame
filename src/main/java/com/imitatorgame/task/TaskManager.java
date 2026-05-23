package com.imitatorgame.task;

import com.imitatorgame.game.GameSession;
import com.imitatorgame.game.PlayerData;
import com.imitatorgame.role.Faction;
import com.imitatorgame.config.MapConfig;

import java.util.*;

public class TaskManager {

    private final GameSession session;
    private final Map<UUID, List<TaskInstance>> playerTasks = new LinkedHashMap<>();
    private final List<TaskInstance> allTasks = new ArrayList<>();

    public TaskManager(GameSession session) {
        this.session = session;
    }

    public void assignTasks() {
        playerTasks.clear();
        allTasks.clear();

        int tasksPerPlayer = session.getPlugin().getConfigManager().getGameConfig().tasksPerPlayer();
        List<MapConfig.TaskPosition> taskPositions = session.getPlugin().getConfigManager().getMapConfig().getTasks();

        if (taskPositions.isEmpty()) return;

        int idx = 0;
        for (UUID uuid : session.getAlivePlayers()) {
            PlayerData pd = session.getPlayerData(uuid);
            if (pd == null || pd.getFaction() != Faction.DETECTIVE) continue;

            List<TaskInstance> tasks = new ArrayList<>();
            for (int i = 0; i < tasksPerPlayer; i++) {
                MapConfig.TaskPosition tp = taskPositions.get(idx % taskPositions.size());
                TaskInstance ti = new TaskInstance(tp.location(), TaskType.valueOf(tp.type()), uuid);
                tasks.add(ti);
                allTasks.add(ti);
                idx++;
            }
            playerTasks.put(uuid, tasks);
            pd.setTotalTasks(tasksPerPlayer);
        }

        session.setTargetTotalTasks(allTasks.size());
    }

    public TaskInstance getTaskAt(org.bukkit.Location location) {
        for (TaskInstance ti : allTasks) {
            if (!ti.isCompleted() && ti.getLocation().getBlock().equals(location.getBlock())) {
                return ti;
            }
        }
        return null;
    }

    public boolean completeTask(UUID playerUuid, TaskInstance task) {
        if (task.isCompleted()) return false;
        if (!task.getAssignedPlayer().equals(playerUuid)) return false;

        task.setCompleted(true);
        session.incrementTotalTasksCompleted();

        PlayerData pd = session.getPlayerData(playerUuid);
        if (pd != null) {
            pd.incrementTaskProgress();
        }
        return true;
    }

    public int getTotalAssignedTasks() {
        return allTasks.size();
    }

    public int getCompletedTasks() {
        return session.getTotalTasksCompleted();
    }

    public Map<UUID, List<TaskInstance>> getPlayerTasks() { return playerTasks; }
    public List<TaskInstance> getAllTasks() { return allTasks; }
}
