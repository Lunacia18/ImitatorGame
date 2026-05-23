package com.imitatorgame.meeting;

import com.imitatorgame.game.GameSession;
import com.imitatorgame.util.Constants;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class MeetingManager {

    private final GameSession session;
    private UUID reporterUuid;
    private UUID victimUuid;
    private int discussionSeconds;

    public MeetingManager(GameSession session) {
        this.session = session;
    }

    public void startMeeting(Player reporter, UUID victimUuid) {
        this.reporterUuid = reporter != null ? reporter.getUniqueId() : null;
        this.victimUuid = victimUuid;
        this.discussionSeconds = session.getPlugin().getConfigManager().getGameConfig().discussionSeconds();

        String reporterName = reporter != null ? reporter.getName() : "系统";
        if (victimUuid != null) {
            Player victim = Bukkit.getPlayer(victimUuid);
            String victimName = victim != null ? victim.getName() : "玩家";
            session.broadcastMessage(Constants.PREFIX + "§c" + reporterName + " 发现了 " + victimName + " 的尸体！");
        } else {
            session.broadcastMessage(Constants.PREFIX + "§c" + reporterName + " 通过电台召开了紧急会议！");
        }

        teleportToMeetingRoom();

        session.broadcastMessage(Constants.PREFIX + "§e讨论阶段开始！你有 §6" + discussionSeconds + " §e秒讨论。");

        new BukkitRunnable() {
            int remaining = discussionSeconds;

            @Override
            public void run() {
                if (remaining <= 0) {
                    this.cancel();
                    session.startVoting();
                    return;
                }
                if (remaining <= 5 || remaining == 10 || remaining == 20) {
                    session.broadcastMessage(Constants.PREFIX + "§e讨论剩余 §6" + remaining + " §e秒");
                }
                remaining--;
            }
        }.runTaskTimer(session.getPlugin(), 0, 20);
    }

    private void teleportToMeetingRoom() {
        var meetingLoc = session.getPlugin().getConfigManager().getMapConfig().getMeetingRoom();
        for (UUID uuid : session.getAlivePlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && meetingLoc != null) {
                p.teleport(meetingLoc);
                p.setGameMode(GameMode.ADVENTURE);
                p.setWalkSpeed(0);
            }
        }
    }

    public UUID getReporterUuid() { return reporterUuid; }
    public UUID getVictimUuid() { return victimUuid; }
}
