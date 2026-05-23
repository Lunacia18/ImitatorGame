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
        if (checkDetectiveWin()) return true;
        if (checkImitatorWin()) return true;
        if (checkMysteryWins()) return true;
        return false;
    }

    private boolean checkDetectiveWin() {
        int totalTasks = session.getTargetTotalTasks();
        int completed = session.getTotalTasksCompleted();
        if (totalTasks > 0 && completed >= totalTasks) {
            announceWin("侦探团", "§b所有演绎任务已完成！侦探团获胜！", Faction.DETECTIVE);
            return true;
        }

        boolean allImitatorsDead = session.getImitatorPlayers().stream()
                .noneMatch(session::isAlive);
        if (allImitatorsDead && session.getImitatorPlayers().size() > 0) {
            announceWin("侦探团", "§b所有模仿者已被淘汰！侦探团获胜！", Faction.DETECTIVE);
            return true;
        }
        return false;
    }

    private boolean checkImitatorWin() {
        long imitatorsAlive = session.getImitatorPlayers().stream()
                .filter(session::isAlive)
                .count();
        int totalAlive = session.getAliveCount();
        if (imitatorsAlive > 0 && imitatorsAlive * 2 >= totalAlive) {
            announceWin("模仿者", "§c模仿者已占据多数！模仿者获胜！", Faction.IMITATOR);
            return true;
        }
        return false;
    }

    private boolean checkMysteryWins() {
        for (UUID uuid : session.getAlivePlayers()) {
            PlayerData pd = session.getPlayerData(uuid);
            if (pd == null) continue;

            if (pd.getRole() == Role.FOOL && !pd.isAlive()) {
                if (pd.getVoteTarget() != null || pd.skippedVote()) continue;
                announcePersonalWin(uuid, "愚人", "§e愚人被投票淘汰，达成个人胜利！");
                return true;
            }

            if (pd.getRole() == Role.VAGABOND) {
                int minInteractions = session.getPlugin().getConfigManager()
                        .getGameConfig().vagabondMinInteractions();
                if (pd.getVagabondInteractions().size() >= minInteractions) {
                    announcePersonalWin(uuid, "流浪汉", "§e流浪汉与足够多玩家互动，达成个人胜利！");
                    return true;
                }
            }

            if (pd.getRole() == Role.DELIVERYMAN) {
                int targets = session.getPlugin().getConfigManager().getGameConfig().deliverymanTargets();
                if (pd.getDeliveryProgress() >= Math.min(targets, session.getAliveCount() + session.getDeadPlayers().size())) {
                    announcePersonalWin(uuid, "送货员", "§e送货员完成所有投递，达成个人胜利！");
                    return true;
                }
            }
        }
        return false;
    }

    public boolean checkFloodingImitatorWin() {
        long imitatorsAlive = session.getImitatorPlayers().stream()
                .filter(session::isAlive)
                .count();
        if (imitatorsAlive > 0) {
            announceWin("模仿者", "§c水淹事件到期！模仿者获胜！", Faction.IMITATOR);
            return true;
        }
        return false;
    }

    private void announceWin(String winnerName, String message, Faction winningFaction) {
        session.broadcastToAll(Constants.PREFIX + message);
        for (UUID uuid : session.getAlivePlayers()) {
            PlayerData pd = session.getPlayerData(uuid);
            if (pd == null) continue;
            var player = Bukkit.getPlayer(uuid);
            if (player != null) {
                if (pd.getFaction() == winningFaction) {
                    player.sendMessage(Constants.PREFIX + "§a§l你赢了！");
                } else if (pd.getFaction() == Faction.MYSTERY_GUEST) {
                    player.sendMessage(Constants.PREFIX + "§e你的个人目标未能达成...");
                } else {
                    player.sendMessage(Constants.PREFIX + "§c§l你输了！");
                }
            }
        }
        session.getPlugin().getGameManager().stopGame();
    }

    private void announcePersonalWin(UUID uuid, String roleName, String message) {
        session.broadcastToAll(Constants.PREFIX + message);
        var winner = Bukkit.getPlayer(uuid);
        if (winner != null) {
            winner.sendMessage(Constants.PREFIX + "§a§l个人目标达成！你赢了！");
        }
        for (UUID otherUuid : session.getAlivePlayers()) {
            if (otherUuid.equals(uuid)) continue;
            var p = Bukkit.getPlayer(otherUuid);
            if (p != null) {
                p.sendMessage(Constants.PREFIX + "§c神秘客个人胜利！游戏结束！");
            }
        }
        session.getPlugin().getGameManager().stopGame();
    }
}
