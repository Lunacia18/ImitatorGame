package com.imitatorgame.ui;

import com.imitatorgame.game.GameSession;
import com.imitatorgame.util.Constants;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

public class ScoreboardManager {

    private final GameSession session;
    private Scoreboard scoreboard;
    private Objective objective;

    public ScoreboardManager(GameSession session) { this.session = session; }

    public void createScoreboard() {
        var mgr = Bukkit.getScoreboardManager();
        if (mgr == null) return;
        scoreboard = mgr.getNewScoreboard();
        objective = scoreboard.registerNewObjective("ig", Criteria.DUMMY,
                net.kyori.adventure.text.Component.text(Constants.SCOREBOARD_TITLE));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        for (UUID u : session.getAlivePlayers()) { Player p = Bukkit.getPlayer(u); if (p != null) p.setScoreboard(scoreboard); }
        for (UUID u : session.getDeadPlayers()) { Player p = Bukkit.getPlayer(u); if (p != null) p.setScoreboard(scoreboard); }
    }

    public void update() {
        if (scoreboard == null || objective == null) return;
        for (String e : scoreboard.getEntries()) scoreboard.resetScores(e);
        int s = 5;
        objective.getScore("§7==============").setScore(s--);
        objective.getScore("§a存活: " + session.getAliveCount()).setScore(s--);
        objective.getScore("§e任务进度: " + session.getTotalTasksCompleted() + "/" + session.getTargetTotalTasks()).setScore(s--);
        objective.getScore("§7==============").setScore(s);
        String phase = switch (session.getPhase()) {
            case LOBBY -> "§e大厅";
            case STARTING -> "§6倒计时";
            case ROLE_REVEAL -> "§d身份";
            case FREE_ACTION -> "§a自由行动";
            case RUSH_HOUR -> "§4急速时刻";
            case MEETING_DISCUSSION -> "§b讨论";
            case MEETING_VOTING -> "§c投票";
            case MEETING_RESULT -> "§6结果";
            case GAME_OVER -> "§4结束";
        };
        objective.getScore("").setScore(-1);
        objective.getScore(phase).setScore(-1);
    }

    public void assignToPlayer(Player p) { if (scoreboard != null) p.setScoreboard(scoreboard); }

    public void remove() {
        var main = Bukkit.getScoreboardManager().getMainScoreboard();
        for (UUID u : session.getAlivePlayers()) { Player p = Bukkit.getPlayer(u); if (p != null) p.setScoreboard(main); }
        for (UUID u : session.getDeadPlayers()) { Player p = Bukkit.getPlayer(u); if (p != null) p.setScoreboard(main); }
        scoreboard = null; objective = null;
    }
}
