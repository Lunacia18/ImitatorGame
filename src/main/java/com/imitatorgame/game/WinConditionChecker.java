package com.imitatorgame.game;

import com.imitatorgame.role.Faction;
import com.imitatorgame.role.Role;
import com.imitatorgame.util.Constants;
import org.bukkit.Bukkit;
import java.util.UUID;

public class WinConditionChecker {

    private final GameSession session;

    public WinConditionChecker(GameSession session) {
        this.session = session;
    }

    public boolean checkAllConditions() {
        if (checkMysteryWins()) return true;
        if (checkRushHourTrigger()) return true;
        // Normal wins are blocked if deliveryman exists
        if (session.hasDeliveryman()) return false;
        if (checkDetectiveWin()) return true;
        if (checkImitatorWin()) return true;
        return false;
    }

    private boolean checkDetectiveWin() {
        if (session.getTargetTotalTasks() > 0 && session.getTotalTasksCompleted() >= session.getTargetTotalTasks()) {
            announceWin("侦探团", "§b所有演绎任务已完成！侦探团获胜！", Faction.DETECTIVE);
            return true;
        }
        boolean allImitatorsDead = session.getImitatorPlayers().stream().noneMatch(session::isAlive);
        if (allImitatorsDead && !session.getImitatorPlayers().isEmpty()) {
            announceWin("侦探团", "§b所有模仿者已被淘汰！侦探团获胜！", Faction.DETECTIVE);
            return true;
        }
        return false;
    }

    private boolean checkImitatorWin() {
        long imitatorsAlive = session.getImitatorPlayers().stream().filter(session::isAlive).count();
        if (imitatorsAlive > 0 && imitatorsAlive * 2 >= session.getAliveCount()) {
            announceWin("模仿者", "§c模仿者已占据多数！模仿者获胜！", Faction.IMITATOR);
            return true;
        }
        return false;
    }

    private boolean checkMysteryWins() {
        for (UUID uuid : session.getAlivePlayers()) {
            PlayerData pd = session.getPlayerData(uuid);
            if (pd == null) continue;
            if (pd.getRole() == Role.VAGABOND
                    && pd.getVagabondInteractions().size() >= session.getPlugin().getConfigManager().getGameConfig().vagabondMinInteractions()) {
                announcePersonalWin(uuid, "流浪汉");
                return true;
            }
        }
        for (UUID uuid : session.getDeadPlayers()) {
            PlayerData pd = session.getPlayerData(uuid);
            if (pd == null) continue;
            if (pd.getRole() == Role.FOOL && pd.hasVoted() && pd.getVoteTarget() != null) {
                announcePersonalWin(uuid, "愚人");
                return true;
            }
        }
        return false;
    }

    private boolean checkRushHourTrigger() {
        if (!session.hasDeliveryman()) return false;
        // Count free (not swallowed) alive non-spectator players
        int free = 0;
        for (UUID u : session.getAlivePlayers()) {
            boolean swallowed = false;
            for (UUID o : session.getAlivePlayers()) {
                PlayerData od = session.getPlayerData(o);
                if (od != null && u.equals(od.getSwallowedTarget())) { swallowed = true; break; }
            }
            if (!swallowed) free++;
        }
        if (free <= 3) {
            // Check if deliveryman is among free
            boolean deliveryFree = false;
            for (UUID u : session.getAlivePlayers()) {
                PlayerData pd = session.getPlayerData(u);
                if (pd == null || pd.getRole() != Role.DELIVERYMAN) continue;
                boolean sw = false;
                for (UUID o : session.getAlivePlayers()) {
                    PlayerData od = session.getPlayerData(o);
                    if (od != null && u.equals(od.getSwallowedTarget())) { sw = true; break; }
                }
                if (!sw) deliveryFree = true;
            }
            if (deliveryFree) {
                session.startRushHour();
                return true;
            }
        }
        return false;
    }

    private void announceWin(String winner, String msg, Faction faction) {
        session.broadcastToAll(Constants.PREFIX + msg);
        for (UUID u : session.getAlivePlayers()) {
            PlayerData pd = session.getPlayerData(u);
            if (pd == null) continue;
            var p = Bukkit.getPlayer(u);
            if (p != null) p.sendMessage(pd.getFaction() == faction ? Constants.PREFIX + "§a§l你赢了！"
                    : Constants.PREFIX + "§c§l你输了！");
        }
        session.getPlugin().getGameManager().stopGame();
    }

    private void announcePersonalWin(UUID uuid, String role) {
        session.broadcastToAll(Constants.PREFIX + "§e" + role + " 达成个人胜利！");
        var w = Bukkit.getPlayer(uuid);
        if (w != null) w.sendMessage(Constants.PREFIX + "§a§l个人目标达成！");
        session.getPlugin().getGameManager().stopGame();
    }
}
