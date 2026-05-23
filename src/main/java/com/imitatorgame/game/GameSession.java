package com.imitatorgame.game;

import com.imitatorgame.ImitatorGamePlugin;
import com.imitatorgame.config.MapConfig;
import com.imitatorgame.event.FloodingEvent;
import com.imitatorgame.event.PowerOutageEvent;
import com.imitatorgame.event.TimedEventManager;
import com.imitatorgame.meeting.MeetingManager;
import com.imitatorgame.meeting.VotingManager;
import com.imitatorgame.role.Role;
import com.imitatorgame.role.RoleAssignment;
import com.imitatorgame.task.TaskManager;
import com.imitatorgame.ui.ScoreboardManager;
import com.imitatorgame.util.Constants;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameSession {

    private final ImitatorGamePlugin plugin;
    private final GameStateMachine stateMachine;
    private final Map<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();
    private final Set<UUID> lobbyPlayers = new HashSet<>();
    private final Set<UUID> alivePlayers = new HashSet<>();
    private final Set<UUID> deadPlayers = new HashSet<>();
    private final List<UUID> imitatorPlayers = new ArrayList<>();

    private MeetingManager meetingManager;
    private VotingManager votingManager;
    private TaskManager taskManager;
    private ScoreboardManager scoreboardManager;
    private TimedEventManager eventManager;
    private BukkitTask currentTimer;
    private BukkitTask scoreboardTask;
    private int countdownSeconds;

    private int totalTasksCompleted = 0;
    private int targetTotalTasks = 0;

    public GameSession(ImitatorGamePlugin plugin) {
        this.plugin = plugin;
        this.stateMachine = new GameStateMachine();
    }

    public boolean isActive() {
        return stateMachine.isInGame();
    }

    public boolean isInMeeting() {
        return stateMachine.isMeeting();
    }

    public GamePhase getPhase() {
        return stateMachine.getCurrentPhase();
    }

    public boolean start() {
        // Auto-add all online players in the lobby world who haven't joined yet
        var lobbyWorld = plugin.getLobbyManager().getLobbyWorld();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (lobbyWorld != null && p.getWorld().equals(lobbyWorld)
                    && !lobbyPlayers.contains(p.getUniqueId())) {
                lobbyPlayers.add(p.getUniqueId());
            }
        }

        if (lobbyPlayers.size() < plugin.getConfigManager().getGameConfig().minPlayers()) {
            broadcastToAll(Constants.PREFIX + "§c玩家不足！当前 "
                    + lobbyPlayers.size() + " 人，需要 "
                    + plugin.getConfigManager().getGameConfig().minPlayers() + " 人");
            return false;
        }
        stateMachine.transitionTo(GamePhase.STARTING);
        countdownSeconds = plugin.getConfigManager().getGameConfig().lobbyWaitSeconds();

        Bukkit.getOnlinePlayers().stream()
                .filter(p -> lobbyPlayers.contains(p.getUniqueId()))
                .forEach(p -> playerDataMap.put(p.getUniqueId(), new PlayerData(p.getUniqueId())));

        currentTimer = new BukkitRunnable() {
            @Override
            public void run() {
                if (countdownSeconds <= 0) {
                    this.cancel();
                    revealRoles();
                    return;
                }
                if (countdownSeconds % 10 == 0 || countdownSeconds <= 5) {
                    broadcastMessage(Constants.PREFIX + "§e游戏将在 §6" + countdownSeconds + " §e秒后开始！");
                }
                countdownSeconds--;
            }
        }.runTaskTimer(plugin, 0, 20);
        return true;
    }

    private void revealRoles() {
        stateMachine.transitionTo(GamePhase.ROLE_REVEAL);

        List<UUID> playerList = new ArrayList<>(lobbyPlayers);
        RoleAssignment ra = new RoleAssignment(plugin.getConfigManager().getGameConfig());
        Map<UUID, Role> assignments = ra.assign(playerList);

        for (Map.Entry<UUID, Role> entry : assignments.entrySet()) {
            PlayerData pd = playerDataMap.get(entry.getKey());
            if (pd == null) continue;
            pd.setRole(entry.getValue());
            alivePlayers.add(entry.getKey());

            if (pd.getFaction() == com.imitatorgame.role.Faction.IMITATOR) {
                imitatorPlayers.add(entry.getKey());
            }

            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                pd.setPlayer(player);
                player.sendMessage(Constants.PREFIX + "§a你的身份是: " + pd.getRole().getDisplayName());
                player.sendMessage(Constants.PREFIX + "§7阵营: " + pd.getFaction().getDisplayName());
                player.sendMessage(Constants.PREFIX + "§7能力: " + pd.getRole().getDescription());
                player.showTitle(Title.title(
                        Component.text(pd.getRole().getDisplayName()),
                        Component.text(pd.getFaction().getDisplayName()),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(500))
                ));
                pd.getRole().giveAbilityItems(player);
            }
        }

        taskManager = new TaskManager(this);
        taskManager.assignTasks();

        meetingManager = new MeetingManager(this);
        votingManager = new VotingManager(this);

        scoreboardManager = new ScoreboardManager(this);
        scoreboardManager.createScoreboard();

        eventManager = new TimedEventManager(this);
        eventManager.startTicking();

        targetTotalTasks = taskManager.getTotalAssignedTasks();

        countdownSeconds = plugin.getConfigManager().getGameConfig().roleRevealSeconds();
        currentTimer = new BukkitRunnable() {
            @Override
            public void run() {
                if (countdownSeconds <= 0) {
                    this.cancel();
                    startFreeAction();
                    return;
                }
                countdownSeconds--;
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    private void startFreeAction() {
        stateMachine.transitionTo(GamePhase.FREE_ACTION);
        MapConfig mapConfig = plugin.getConfigManager().getMapConfig();
        List<Location> spawns = mapConfig.getGameSpawns();

        int idx = 0;
        for (UUID uuid : alivePlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;
            player.setGameMode(GameMode.ADVENTURE);
            if (idx < spawns.size()) {
                player.teleport(spawns.get(idx));
            }
            player.getInventory().clear();
            PlayerData pd = playerDataMap.get(uuid);
            if (pd != null) {
                pd.getRole().giveAbilityItems(player);
            }
            idx++;
        }

        scoreboardTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (scoreboardManager != null) scoreboardManager.update();
            }
        }.runTaskTimer(plugin, 0, 40);

        broadcastMessage(Constants.PREFIX + "§a自由行动阶段开始！");
    }

    public void callMeeting(Player reporter, UUID victimUuid) {
        if (!stateMachine.transitionTo(GamePhase.MEETING_DISCUSSION)) return;
        cancelTimer();

        for (UUID uuid : alivePlayers) {
            PlayerData pd = playerDataMap.get(uuid);
            if (pd == null) continue;
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                pd.setPreMeetingPosition(p.getLocation());
            }
        }

        if (meetingManager != null) {
            meetingManager.startMeeting(reporter, victimUuid);
        }
    }

    public void startVoting() {
        stateMachine.transitionTo(GamePhase.MEETING_VOTING);
        if (votingManager != null) {
            votingManager.startVoting();
        }
    }

    public void endMeeting() {
        if (votingManager != null) {
            votingManager.processResults();
        }
        if (!stateMachine.transitionTo(GamePhase.MEETING_RESULT)) return;

        countdownSeconds = plugin.getConfigManager().getGameConfig().resultSeconds();
        currentTimer = new BukkitRunnable() {
            @Override
            public void run() {
                if (countdownSeconds <= 0) {
                    this.cancel();
                    checkWinAndResume();
                    return;
                }
                countdownSeconds--;
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    private void checkWinAndResume() {
        WinConditionChecker checker = new WinConditionChecker(this);
        if (checker.checkAllConditions()) {
            return;
        }
        returnToFreeAction();
    }

    public void returnToFreeAction() {
        stateMachine.transitionTo(GamePhase.FREE_ACTION);
        for (UUID uuid : alivePlayers) {
            PlayerData pd = playerDataMap.get(uuid);
            if (pd == null) continue;
            pd.resetForNewRound();
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && pd.getPreMeetingPosition() != null) {
                p.teleport(pd.getPreMeetingPosition());
                p.setGameMode(GameMode.ADVENTURE);
                pd.getRole().giveAbilityItems(p);
            }
        }
        broadcastMessage(Constants.PREFIX + "§a会议结束，自由行动阶段继续！");
    }

    public void endGame() {
        cancelTimer();
        if (scoreboardTask != null) { scoreboardTask.cancel(); scoreboardTask = null; }
        if (eventManager != null) { eventManager.stopAll(); eventManager = null; }
        if (scoreboardManager != null) { scoreboardManager.remove(); scoreboardManager = null; }

        stateMachine.forcePhase(GamePhase.GAME_OVER);
        playerDataMap.clear();
        lobbyPlayers.clear();
        alivePlayers.clear();
        deadPlayers.clear();
        imitatorPlayers.clear();
        totalTasksCompleted = 0;
        targetTotalTasks = 0;
        taskManager = null;
        meetingManager = null;
        votingManager = null;

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setGameMode(GameMode.ADVENTURE);
            p.getInventory().clear();
            var lobby = plugin.getLobbyManager();
            if (lobby != null) {
                lobby.teleportToLobby(p);
            }
        }
        stateMachine.forcePhase(GamePhase.LOBBY);
    }

    public void triggerPowerOutage() {
        if (eventManager == null) return;
        if (eventManager.hasActiveEvent(PowerOutageEvent.class)) {
            broadcastMessage(Constants.PREFIX + "§c停电事件已在进行中");
            return;
        }
        if (stateMachine.getCurrentPhase() != GamePhase.FREE_ACTION) return;

        int duration = plugin.getConfigManager().getGameConfig().powerOutageSeconds();
        eventManager.addEvent(new PowerOutageEvent(duration));
    }

    public void triggerFlooding() {
        if (eventManager == null) return;
        if (eventManager.hasActiveEvent(FloodingEvent.class)) {
            broadcastMessage(Constants.PREFIX + "§c水淹事件已在进行中");
            return;
        }
        if (stateMachine.getCurrentPhase() != GamePhase.FREE_ACTION) return;

        int duration = plugin.getConfigManager().getGameConfig().floodingSeconds();
        eventManager.addEvent(new FloodingEvent(duration));
    }

    public TimedEventManager getEventManager() { return eventManager; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }

    public List<com.imitatorgame.event.TimedGameEvent> getActiveEvents() {
        if (eventManager == null) return List.of();
        return eventManager.getActiveEvents();
    }

    public void handleDeath(UUID victimUuid) {
        alivePlayers.remove(victimUuid);
        deadPlayers.add(victimUuid);
        PlayerData pd = playerDataMap.get(victimUuid);
        if (pd != null) {
            pd.setAlive(false);
        }
        Player victim = Bukkit.getPlayer(victimUuid);
        if (victim != null) {
            victim.setGameMode(GameMode.SPECTATOR);
        }
    }

    public void eliminateByVote(UUID targetUuid) {
        handleDeath(targetUuid);
        PlayerData pd = playerDataMap.get(targetUuid);
        broadcastMessage(Constants.PREFIX + "§c" +
                (pd != null ? Bukkit.getPlayer(targetUuid).getName() : "玩家") + " 被投票淘汰！");
    }

    public void cancelTimer() {
        if (currentTimer != null && !currentTimer.isCancelled()) {
            currentTimer.cancel();
            currentTimer = null;
        }
    }

    public void addLobbyPlayer(UUID uuid) {
        lobbyPlayers.add(uuid);
        if (!playerDataMap.containsKey(uuid)) {
            playerDataMap.put(uuid, new PlayerData(uuid));
        }
    }

    public void removeLobbyPlayer(UUID uuid) {
        lobbyPlayers.remove(uuid);
        if (!isActive()) {
            playerDataMap.remove(uuid);
        }
    }

    public boolean isInLobby(UUID uuid) { return lobbyPlayers.contains(uuid); }
    public boolean isAlive(UUID uuid) { return alivePlayers.contains(uuid); }
    public boolean isDead(UUID uuid) { return deadPlayers.contains(uuid); }
    public boolean isImitator(UUID uuid) { return imitatorPlayers.contains(uuid); }
    public Set<UUID> getLobbyPlayers() { return Collections.unmodifiableSet(lobbyPlayers); }
    public Set<UUID> getAlivePlayers() { return Collections.unmodifiableSet(alivePlayers); }
    public Set<UUID> getDeadPlayers() { return Collections.unmodifiableSet(deadPlayers); }
    public List<UUID> getImitatorPlayers() { return Collections.unmodifiableList(imitatorPlayers); }
    public int getLobbySize() { return lobbyPlayers.size(); }
    public int getAliveCount() { return alivePlayers.size(); }

    public PlayerData getPlayerData(UUID uuid) { return playerDataMap.get(uuid); }
    public Map<UUID, PlayerData> getAllPlayerData() { return Collections.unmodifiableMap(playerDataMap); }

    public ImitatorGamePlugin getPlugin() { return plugin; }
    public MeetingManager getMeetingManager() { return meetingManager; }
    public VotingManager getVotingManager() { return votingManager; }
    public TaskManager getTaskManager() { return taskManager; }

    public int getTotalTasksCompleted() { return totalTasksCompleted; }
    public void incrementTotalTasksCompleted() { totalTasksCompleted++; }
    public int getTargetTotalTasks() { return targetTotalTasks; }
    public void setTargetTotalTasks(int v) { targetTotalTasks = v; }

    public void broadcastMessage(String msg) {
        for (UUID uuid : alivePlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(msg);
        }
        for (UUID uuid : deadPlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage("§7[旁观] " + msg);
        }
    }

    public void broadcastToAll(String msg) {
        Bukkit.broadcast(Component.text(msg));
    }
}
