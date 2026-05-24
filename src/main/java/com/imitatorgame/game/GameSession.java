package com.imitatorgame.game;

import com.imitatorgame.ImitatorGamePlugin;
import com.imitatorgame.event.*;
import com.imitatorgame.map.GameMapManager;
import com.imitatorgame.meeting.MeetingManager;
import com.imitatorgame.meeting.VotingManager;
import com.imitatorgame.role.Role;
import com.imitatorgame.role.RoleAssignment;
import com.imitatorgame.ui.ScoreboardManager;
import com.imitatorgame.util.Constants;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
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
    private final Set<UUID> readyPlayers = new HashSet<>();
    private final Set<UUID> alivePlayers = new HashSet<>();
    private final Set<UUID> deadPlayers = new HashSet<>();
    private final List<UUID> imitatorPlayers = new ArrayList<>();

    private MeetingManager meetingManager;
    private VotingManager votingManager;
    private ScoreboardManager scoreboardManager;
    private TimedEventManager eventManager;
    private DeathManager deathManager;
    private GameMapManager gameMapManager;
    private BukkitTask currentTimer;
    private BukkitTask scoreboardTask;
    private BukkitTask invisibleHandsTask;
    private BukkitTask bombTickTask;
    private BukkitTask rushHourTask;
    private int countdownSeconds;
    private int rushHourSeconds;

    private int totalTasksCompleted;
    private final int TARGET_TASKS = 55;
    private boolean hasDeliveryman;
    private final Map<UUID, Long> taskCooldowns = new HashMap<>();
    private static final long TASK_CD = 5_000;

    // Door hideout tracking
    private final Set<UUID> door1Hiders = new HashSet<>();
    private final Set<UUID> door2Hiders = new HashSet<>();
    private long door1LockedUntil;
    private long door2LockedUntil;

    public GameSession(ImitatorGamePlugin plugin) {
        this.plugin = plugin;
        this.stateMachine = new GameStateMachine();
    }

    public boolean isActive() { return stateMachine.isInGame(); }
    public GameStateMachine getStateMachine() { return stateMachine; }
    public boolean isInMeeting() { return stateMachine.isMeeting(); }
    public GamePhase getPhase() { return stateMachine.getCurrentPhase(); }

    public boolean start() {
        var lobbyWorld = plugin.getLobbyManager().getLobbyWorld();
        for (Player p : Bukkit.getOnlinePlayers())
            if (lobbyWorld != null && p.getWorld().equals(lobbyWorld) && !lobbyPlayers.contains(p.getUniqueId()))
                lobbyPlayers.add(p.getUniqueId());
        if (lobbyPlayers.size() < plugin.getConfigManager().getGameConfig().minPlayers()) {
            broadcastToAll(Constants.PREFIX + "§c玩家不足！当前 " + lobbyPlayers.size() + " 人"); return false;
        }
        stateMachine.transitionTo(GamePhase.STARTING);
        revealRoles();
        return true;
    }

    private void revealRoles() {
        List<UUID> playerList = new ArrayList<>(lobbyPlayers);
        RoleAssignment ra = new RoleAssignment(plugin.getConfigManager().getGameConfig());
        Map<UUID, Role> assignments = ra.assign(playerList);
        hasDeliveryman = false;
        for (Map.Entry<UUID, Role> entry : assignments.entrySet()) {
            PlayerData pd = playerDataMap.get(entry.getKey());
            if (pd == null) continue;
            pd.setRole(entry.getValue()); alivePlayers.add(entry.getKey());
            if (pd.getFaction() == com.imitatorgame.role.Faction.IMITATOR) imitatorPlayers.add(entry.getKey());
            if (pd.getRole() == Role.DELIVERYMAN) hasDeliveryman = true;
            pd.setAbilityCooldownMillis(pd.getRole().getCooldownMillis());
            pd.setGlobalUsesRemaining(pd.getRole().getGlobalUses());
            pd.setHasKnife(pd.getRole().hasKnife());
            pd.setTotalTasks(TARGET_TASKS);
            if (pd.getRole().getCooldownMillis() > 0) pd.setLastAbilityUseTime(System.currentTimeMillis());
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                pd.setPlayer(player);
                player.sendMessage(Constants.PREFIX + "§a你的身份: " + pd.getRole().getDisplayName());
                player.sendMessage(Constants.PREFIX + "§7阵营: " + pd.getFaction().getDisplayName());
                player.showTitle(Title.title(Component.text(pd.getRole().getDisplayName()),
                        Component.text(pd.getFaction().getDisplayName()),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(500))));
            }
        }
        scoreboardManager = new ScoreboardManager(this); scoreboardManager.createScoreboard();
        eventManager = new TimedEventManager(this); eventManager.startTicking();
        deathManager = new DeathManager(this);
        World gameWorld = Bukkit.getWorld(plugin.getConfigManager().getMapConfig().getWorldName());
        Location center = gameWorld != null ? gameWorld.getSpawnLocation() : new Location(gameWorld, 0, 64, 0);
        if (center == null) center = new Location(gameWorld, 0, 64, 0);
        gameMapManager = new GameMapManager(gameWorld, center);
        gameMapManager.buildPlatform();

        // 5-second countdown with titles
        countdownSeconds = 5;
        currentTimer = new BukkitRunnable() {
            public void run() {
                if (countdownSeconds <= 0) {
                    this.cancel();
                    startFreeAction();
                    return;
                }
                String msg = countdownSeconds == 1 ? "§c" + countdownSeconds : "§e" + countdownSeconds;
                Title title = Title.title(Component.text(msg),
                        Component.text("§7游戏即将开始..."),
                        Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ZERO));
                for (UUID u : lobbyPlayers) {
                    Player p = Bukkit.getPlayer(u);
                    if (p != null) p.showTitle(title);
                }
                countdownSeconds--;
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    private void startFreeAction() {
        stateMachine.transitionTo(GamePhase.FREE_ACTION);
        List<Location> spawns = gameMapManager.getRandomSpawns(alivePlayers.size());
        int idx = 0;
        for (UUID uuid : alivePlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;
            player.setGameMode(GameMode.ADVENTURE);
            player.teleport(idx < spawns.size() ? spawns.get(idx) : gameMapManager.getCenterSpawn());
            player.getInventory().clear();
            PlayerData pd = playerDataMap.get(uuid);
            if (pd != null) pd.getRole().giveAbilityItems(player);
            idx++;
        }
        scoreboardTask = new BukkitRunnable() { public void run() { if (scoreboardManager != null) scoreboardManager.update(); } }.runTaskTimer(plugin, 0, 40);
        invisibleHandsTask = new BukkitRunnable() {
            public void run() {
                ItemStack air = new ItemStack(Material.AIR);
                for (UUID u : alivePlayers) { Player p = Bukkit.getPlayer(u); if (p == null) continue;
                    for (UUID o : alivePlayers) { if (o.equals(u)) continue; Player other = Bukkit.getPlayer(o); if (other != null) other.sendEquipmentChange(p, EquipmentSlot.HAND, air); } }
            }
        }.runTaskTimer(plugin, 0, 10);
        bombTickTask = new BukkitRunnable() {
            public void run() {
                for (UUID u : alivePlayers) {
                    PlayerData pd = playerDataMap.get(u); if (pd == null || !pd.hasBomb()) continue;
                    long rem = pd.getBombExpireTime() - System.currentTimeMillis();
                    if (rem <= 0) { Player p = Bukkit.getPlayer(u); if (p != null) { gameMapManager.placeCoalBlock(p.getLocation()); handleDeath(u); p.sendMessage(Constants.PREFIX + "§4§l炸弹爆炸！"); } pd.setHasBomb(false); pd.setBombExpireTime(0); pd.setBombVisibleToAll(false); }
                    else if (!pd.isBombVisibleToAll() && rem < 11_000) { pd.setBombVisibleToAll(true); Player p = Bukkit.getPlayer(u); if (p != null) broadcastMessage(Constants.PREFIX + "§c" + p.getName() + " 身上冒着烟花..."); }
                }
            }
        }.runTaskTimer(plugin, 0, 5);
        broadcastMessage(Constants.PREFIX + "§a自由行动阶段开始！");
    }

    public void callMeeting(Player reporter, UUID victimUuid) {
        if (stateMachine.getCurrentPhase() == GamePhase.RUSH_HOUR) { if (reporter != null) reporter.sendMessage(Constants.PREFIX + "§c急速时刻无法开会！"); return; }
        if (!stateMachine.transitionTo(GamePhase.MEETING_DISCUSSION)) return;
        cancelTimer();
        // Pop swallowed
        for (UUID uuid : new ArrayList<>(alivePlayers)) {
            PlayerData pd = playerDataMap.get(uuid);
            if (pd != null && pd.getSwallowedTarget() != null) {
                UUID sw = pd.getSwallowedTarget(); Player sp = Bukkit.getPlayer(sw);
                if (sp != null && isAlive(sw)) { sp.setInvisible(false); sp.setSpectatorTarget(null); broadcastMessage(Constants.PREFIX + "§e" + sp.getName() + " 脱出！"); }
                pd.setSwallowedTarget(null);
            }
        }
        // Exit door hiders
        for (UUID u : new HashSet<>(door1Hiders)) exitDoorHideout(u);
        for (UUID u : new HashSet<>(door2Hiders)) exitDoorHideout(u);
        if (gameMapManager != null) gameMapManager.removeCorpsesAndCoal();
        if (deathManager != null) deathManager.removeAllCorpses();
        for (UUID uuid : alivePlayers) { PlayerData pd = playerDataMap.get(uuid); if (pd == null) continue; Player p = Bukkit.getPlayer(uuid); if (p != null) pd.setPreMeetingPosition(p.getLocation()); }
        if (meetingManager == null) meetingManager = new MeetingManager(this);
        meetingManager.startMeeting(reporter, victimUuid);
    }

    public void startVoting() { stateMachine.transitionTo(GamePhase.MEETING_VOTING); if (votingManager == null) votingManager = new VotingManager(this); votingManager.startVoting(); }
    public void endMeeting() { if (votingManager != null) votingManager.processResults(); if (!stateMachine.transitionTo(GamePhase.MEETING_RESULT)) return;
        countdownSeconds = plugin.getConfigManager().getGameConfig().resultSeconds();
        currentTimer = new BukkitRunnable() { public void run() { if (countdownSeconds <= 0) { this.cancel(); checkWinAndResume(); return; } countdownSeconds--; } }.runTaskTimer(plugin, 0, 20);
    }

    public void checkWinAndResume() { WinConditionChecker c = new WinConditionChecker(this); if (c.checkAllConditions()) return; returnToFreeAction(); }

    public void returnToFreeAction() {
        stateMachine.transitionTo(GamePhase.FREE_ACTION);
        for (UUID uuid : alivePlayers) { PlayerData pd = playerDataMap.get(uuid); if (pd == null) continue; pd.resetForNewRound(); Player p = Bukkit.getPlayer(uuid); if (p != null && pd.getPreMeetingPosition() != null) { p.teleport(pd.getPreMeetingPosition()); p.setGameMode(GameMode.ADVENTURE); pd.getRole().giveAbilityItems(p); } }
        broadcastMessage(Constants.PREFIX + "§a会议结束，自由行动继续！");
    }

    // --- Door hideouts ---
    public boolean enterDoorHideout(Player player, int doorNum) {
        PlayerData pd = playerDataMap.get(player.getUniqueId());
        if (pd == null) return false;
        if (pd.getFaction() != com.imitatorgame.role.Faction.IMITATOR && pd.getRole() != Role.LOCKSMITH) { player.sendMessage(Constants.PREFIX + "§c只有模仿者和锁匠能进入"); return false; }
        if (doorNum == 1 && (System.currentTimeMillis() < door1LockedUntil)) { player.sendMessage(Constants.PREFIX + "§c此门已锁定"); return false; }
        if (doorNum == 2 && (System.currentTimeMillis() < door2LockedUntil)) { player.sendMessage(Constants.PREFIX + "§c此门已锁定"); return false; }
        (doorNum == 1 ? door1Hiders : door2Hiders).add(player.getUniqueId());
        player.setInvisible(true); player.setCollidable(false); player.setWalkSpeed(0);
        Location cam = doorNum == 1 ? gameMapManager.getDoor1Loc() : gameMapManager.getDoor2Loc();
        if (cam != null) player.teleport(cam.clone().add(0, 1, 0));
        player.sendMessage(Constants.PREFIX + "§a进入隐蔽点 #" + doorNum + "，按Shift退出");
        return true;
    }

    public void exitDoorHideout(UUID uuid) {
        door1Hiders.remove(uuid); door2Hiders.remove(uuid);
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) { p.setInvisible(false); p.setCollidable(true); p.setWalkSpeed(0.2f); p.sendMessage(Constants.PREFIX + "§a退出隐蔽点"); }
    }

    public void lockDoor(int doorNum, Player player) {
        if (doorNum == 1) { door1LockedUntil = System.currentTimeMillis() + 15_000; player.sendMessage(Constants.PREFIX + "§a门1已锁定15秒"); }
        else { door2LockedUntil = System.currentTimeMillis() + 15_000; player.sendMessage(Constants.PREFIX + "§a门2已锁定15秒"); }
    }

    public boolean isDoorLocked(int doorNum) { return doorNum == 1 ? System.currentTimeMillis() < door1LockedUntil : System.currentTimeMillis() < door2LockedUntil; }

    // --- Task ---
    public boolean completeDetectiveTask(UUID uuid, Location lodestone) {
        PlayerData pd = playerDataMap.get(uuid);
        if (pd == null || pd.getFaction() != com.imitatorgame.role.Faction.DETECTIVE) return false;
        Long last = taskCooldowns.get(uuid);
        if (last != null && System.currentTimeMillis() - last < TASK_CD) return false;
        taskCooldowns.put(uuid, System.currentTimeMillis());
        pd.incrementTaskProgress();
        totalTasksCompleted++;
        if (totalTasksCompleted >= TARGET_TASKS) { broadcastMessage(Constants.PREFIX + "§b全部55个任务完成！侦探团获胜！"); new WinConditionChecker(this).checkAllConditions(); return true; }
        return true;
    }

    // --- rest same as before ---
    public void startRushHour() {
        stateMachine.transitionTo(GamePhase.RUSH_HOUR); rushHourSeconds = 65;
        broadcastMessage(Constants.PREFIX + "§4§l急速时刻！65秒倒计时！");
        broadcastMessage(Constants.PREFIX + "§c淘汰送货员否则它获胜！");
        rushHourTask = new BukkitRunnable() { public void run() {
            if (rushHourSeconds <= 0) { this.cancel(); for (UUID u : alivePlayers) { PlayerData pd = playerDataMap.get(u); if (pd != null && pd.getRole() == Role.DELIVERYMAN) { broadcastToAll(Constants.PREFIX + "§e送货员急速时刻存活！胜利！"); endGame(); return; } } return; }
            if (rushHourSeconds % 10 == 0 || rushHourSeconds <= 5) broadcastMessage(Constants.PREFIX + "§4急速剩余 §6" + rushHourSeconds + " §4秒");
            for (UUID u : alivePlayers) { Player p = Bukkit.getPlayer(u); if (p != null) p.sendActionBar(Component.text("§4§l急速: " + rushHourSeconds + "s")); }
            rushHourSeconds--; checkRushHourWin();
        } }.runTaskTimer(plugin, 0, 20);
    }

    private void checkRushHourWin() { int free = 0; UUID du = null; for (UUID u : alivePlayers) { PlayerData pd = playerDataMap.get(u); if (pd == null) continue; if (pd.getRole() == Role.DELIVERYMAN) du = u; boolean sw = false; for (UUID o : alivePlayers) { PlayerData od = playerDataMap.get(o); if (od != null && u.equals(od.getSwallowedTarget())) { sw = true; break; } } if (!sw) free++; } if (free <= 1 && du != null) { broadcastToAll(Constants.PREFIX + "§e送货员最后自由者！胜利！"); if (rushHourTask != null) rushHourTask.cancel(); endGame(); } }

    public void endGame() {
        cancelTimer(); if (scoreboardTask != null) { scoreboardTask.cancel(); scoreboardTask = null; } if (invisibleHandsTask != null) { invisibleHandsTask.cancel(); invisibleHandsTask = null; } if (bombTickTask != null) { bombTickTask.cancel(); bombTickTask = null; } if (rushHourTask != null) { rushHourTask.cancel(); rushHourTask = null; } if (eventManager != null) { eventManager.stopAll(); eventManager = null; } if (scoreboardManager != null) { scoreboardManager.remove(); scoreboardManager = null; } if (deathManager != null) { deathManager.removeAllCorpses(); deathManager = null; } if (gameMapManager != null) { gameMapManager.revertCoalBlocks(); }
        stateMachine.forcePhase(GamePhase.GAME_OVER); playerDataMap.clear(); lobbyPlayers.clear(); readyPlayers.clear(); alivePlayers.clear(); deadPlayers.clear(); imitatorPlayers.clear(); door1Hiders.clear(); door2Hiders.clear(); totalTasksCompleted = 0; hasDeliveryman = false; meetingManager = null; votingManager = null;
        for (Player p : Bukkit.getOnlinePlayers()) { p.setInvisible(false); p.setCollidable(true); p.setSilent(false); p.setWalkSpeed(0.2f); p.getInventory().clear(); p.setGameMode(GameMode.ADVENTURE); var lobby = plugin.getLobbyManager(); if (lobby != null) lobby.teleportToLobby(p); }
        stateMachine.forcePhase(GamePhase.LOBBY);
    }

    public void triggerPowerOutage() { if (eventManager == null || stateMachine.getCurrentPhase() != GamePhase.FREE_ACTION) return; if (eventManager.hasActiveEvent(FloodingEvent.class)) { broadcastMessage(Constants.PREFIX + "§c水阀事件进行中，不能同时触发停电"); return; } if (eventManager.hasActiveEvent(PowerOutageEvent.class)) return; eventManager.addEvent(new PowerOutageEvent(0)); }
    public void triggerFlooding() { if (eventManager == null || stateMachine.getCurrentPhase() != GamePhase.FREE_ACTION) return; if (eventManager.hasActiveEvent(PowerOutageEvent.class)) { broadcastMessage(Constants.PREFIX + "§c停电事件进行中，不能同时触发水阀"); return; } if (eventManager.hasActiveEvent(FloodingEvent.class)) return; eventManager.addEvent(new FloodingEvent(plugin.getConfigManager().getGameConfig().floodingSeconds())); }

    public void handleDeath(UUID victimUuid) { alivePlayers.remove(victimUuid); deadPlayers.add(victimUuid); PlayerData pd = playerDataMap.get(victimUuid); if (pd != null) { pd.setAlive(false); pd.setHasBomb(false); pd.setBombExpireTime(0); if (pd.getSwallowedTarget() != null) { UUID sw = pd.getSwallowedTarget(); Player sp = Bukkit.getPlayer(sw); if (sp != null && alivePlayers.contains(sw)) { sp.setInvisible(false); sp.setSpectatorTarget(null); broadcastMessage(Constants.PREFIX + "§e" + sp.getName() + " 掉出！"); } pd.setSwallowedTarget(null); } } exitDoorHideout(victimUuid); Player victim = Bukkit.getPlayer(victimUuid); if (victim != null) { if (deathManager != null) deathManager.spawnCorpse(victim, victim.getLocation()); victim.setGameMode(GameMode.SPECTATOR); victim.setInvisible(true); victim.setCollidable(false); victim.setSilent(true); Role.giveDeathBeacon(victim); } }
    public void eliminateByVote(UUID u) { handleDeath(u); Player p = Bukkit.getPlayer(u); broadcastMessage(Constants.PREFIX + "§c" + (p != null ? p.getName() : "玩家") + " 被淘汰！"); }
    public void cancelTimer() { if (currentTimer != null && !currentTimer.isCancelled()) { currentTimer.cancel(); currentTimer = null; } }

    public void addLobbyPlayer(UUID uuid) { if (!isActive()) { lobbyPlayers.add(uuid); playerDataMap.putIfAbsent(uuid, new PlayerData(uuid)); } }
    public void removeLobbyPlayer(UUID uuid) { lobbyPlayers.remove(uuid); readyPlayers.remove(uuid); if (!isActive()) playerDataMap.remove(uuid); }
    public boolean setReady(UUID uuid) { if (!lobbyPlayers.contains(uuid) || readyPlayers.contains(uuid) || isActive()) return false; readyPlayers.add(uuid); if (readyPlayers.size() >= plugin.getConfigManager().getGameConfig().minPlayers() && lobbyPlayers.size() >= plugin.getConfigManager().getGameConfig().minPlayers()) start(); return true; }
    public boolean isReady(UUID u) { return readyPlayers.contains(u); }
    public int getReadyCount() { return readyPlayers.size(); }
    public boolean isLobbyOpen() { return !isActive(); }

    public boolean isInLobby(UUID u) { return lobbyPlayers.contains(u); }
    public boolean isAlive(UUID u) { return alivePlayers.contains(u); }
    public boolean isDead(UUID u) { return deadPlayers.contains(u); }
    public boolean isImitator(UUID u) { return imitatorPlayers.contains(u); }
    public List<UUID> getImitatorPlayers() { return Collections.unmodifiableList(imitatorPlayers); }
    public boolean hasDeliveryman() { return hasDeliveryman; }
    public Set<UUID> getLobbyPlayers() { return Collections.unmodifiableSet(lobbyPlayers); }
    public Set<UUID> getAlivePlayers() { return Collections.unmodifiableSet(alivePlayers); }
    public Set<UUID> getDeadPlayers() { return Collections.unmodifiableSet(deadPlayers); }
    public int getLobbySize() { return lobbyPlayers.size(); }
    public int getAliveCount() { return alivePlayers.size(); }
    public PlayerData getPlayerData(UUID u) { return playerDataMap.get(u); }
    public ImitatorGamePlugin getPlugin() { return plugin; }
    public MeetingManager getMeetingManager() { return meetingManager; }
    public VotingManager getVotingManager() { return votingManager; }
    public TimedEventManager getEventManager() { return eventManager; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public DeathManager getDeathManager() { return deathManager; }
    public GameMapManager getGameMapManager() { return gameMapManager; }
    public int getTotalTasksCompleted() { return totalTasksCompleted; }
    public int getTargetTotalTasks() { return TARGET_TASKS; }
    public void setTargetTotalTasks(int v) {} // stub for TaskManager compat
    public void incrementTotalTasksCompleted() { totalTasksCompleted++; } // stub
    public List<TimedGameEvent> getActiveEvents() { return eventManager != null ? eventManager.getActiveEvents() : List.of(); }
    public void broadcastMessage(String msg) { for (UUID u : alivePlayers) { Player p = Bukkit.getPlayer(u); if (p != null) p.sendMessage(msg); } for (UUID u : deadPlayers) { Player p = Bukkit.getPlayer(u); if (p != null) p.sendMessage("§7[旁观] " + msg); } }
    public void broadcastToAll(String msg) { Bukkit.broadcast(Component.text(msg)); }
}
