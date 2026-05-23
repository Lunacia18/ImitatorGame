package com.imitatorgame.ui;

import com.imitatorgame.game.GameSession;
import com.imitatorgame.game.PlayerData;
import com.imitatorgame.role.Faction;
import com.imitatorgame.util.Constants;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

public class ScoreboardManager {

    private final GameSession session;
    private Scoreboard scoreboard;
    private Objective objective;

    public ScoreboardManager(GameSession session) {
        this.session = session;
    }

    public void createScoreboard() {
        var manager = Bukkit.getScoreboardManager();
        if (manager == null) return;
        scoreboard = manager.getNewScoreboard();
        objective = scoreboard.registerNewObjective("imitatorgame", Criteria.DUMMY,
                net.kyori.adventure.text.Component.text(Constants.SCOREBOARD_TITLE));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        for (var uuid : session.getAlivePlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.setScoreboard(scoreboard);
        }
        for (var uuid : session.getDeadPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.setScoreboard(scoreboard);
        }
    }

    public void update() {
        if (scoreboard == null || objective == null) return;

        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        int score = 10;
        objective.getScore("§7==============").setScore(score--);

        String aliveLine = "§a存活: " + session.getAliveCount() + "/" +
                (session.getAliveCount() + session.getDeadPlayers().size());
        objective.getScore(aliveLine).setScore(score--);

        if (session.getTargetTotalTasks() > 0) {
            String taskLine = "§e任务: " + session.getTotalTasksCompleted() +
                    "/" + session.getTargetTotalTasks();
            objective.getScore(taskLine).setScore(score--);
        }

        objective.getScore("§7--------------").setScore(score--);

        long detCount = session.getAlivePlayers().stream()
                .map(session::getPlayerData)
                .filter(pd -> pd != null && pd.getFaction() == Faction.DETECTIVE)
                .count();
        long imiCount = session.getAlivePlayers().stream()
                .map(session::getPlayerData)
                .filter(pd -> pd != null && pd.getFaction() == Faction.IMITATOR)
                .count();
        long mysCount = session.getAlivePlayers().stream()
                .map(session::getPlayerData)
                .filter(pd -> pd != null && pd.getFaction() == Faction.MYSTERY_GUEST)
                .count();

        objective.getScore("§b侦探: " + detCount).setScore(score--);
        objective.getScore("§c模仿者: " + imiCount).setScore(score--);
        objective.getScore("§e神秘客: " + mysCount).setScore(score--);
        objective.getScore("§7==============").setScore(score);

        String phase = switch (session.getPhase()) {
            case LOBBY -> "§e大厅等待中";
            case STARTING -> "§6即将开始...";
            case ROLE_REVEAL -> "§d身份揭示";
            case FREE_ACTION -> "§a自由行动";
            case RUSH_HOUR -> "§4急速时刻";
            case MEETING_DISCUSSION -> "§b会议讨论中";
            case MEETING_VOTING -> "§c投票中";
            case MEETING_RESULT -> "§6会议结果";
            case GAME_OVER -> "§4游戏结束";
        };
        objective.getScore("").setScore(-1);
        objective.getScore(phase).setScore(-1);
    }

    public void assignToPlayer(Player player) {
        if (scoreboard != null) player.setScoreboard(scoreboard);
    }

    public void remove() {
        var manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            var main = manager.getMainScoreboard();
            for (var uuid : session.getAlivePlayers()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) p.setScoreboard(main);
            }
            for (var uuid : session.getDeadPlayers()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) p.setScoreboard(main);
            }
        }
        scoreboard = null;
        objective = null;
    }
}
