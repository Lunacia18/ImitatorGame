package com.imitatorgame.meeting;

import com.imitatorgame.game.GameSession;
import com.imitatorgame.game.PlayerData;
import com.imitatorgame.util.Constants;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class VotingManager {

    private final GameSession session;
    private final Map<UUID, UUID> votes = new LinkedHashMap<>();
    private final Set<UUID> skippedVotes = new HashSet<>();
    private int votingSeconds;
    private boolean votingOpen = false;

    public VotingManager(GameSession session) {
        this.session = session;
    }

    public void startVoting() {
        votes.clear();
        skippedVotes.clear();
        votingSeconds = session.getPlugin().getConfigManager().getGameConfig().votingSeconds();
        votingOpen = true;

        session.broadcastMessage(Constants.PREFIX + "§e投票阶段开始！使用 §6/vote <玩家名> §e或 §6/vote skip §e弃权");
        session.broadcastMessage(Constants.PREFIX + "§e你有 §6" + votingSeconds + " §e秒投票");

        new BukkitRunnable() {
            int remaining = votingSeconds;

            @Override
            public void run() {
                if (remaining <= 0) {
                    this.cancel();
                    session.endMeeting();
                    return;
                }
                if (remaining <= 5) {
                    session.broadcastMessage(Constants.PREFIX + "§e投票剩余 §6" + remaining + " §e秒");
                }
                remaining--;
            }
        }.runTaskTimer(session.getPlugin(), 0, 20);
    }

    public boolean castVote(UUID voterUuid, UUID targetUuid) {
        if (!votingOpen) return false;
        PlayerData pd = session.getPlayerData(voterUuid);
        if (pd == null || pd.hasVoted()) return false;

        votes.put(voterUuid, targetUuid);
        pd.setHasVoted(true);
        pd.setVoteTarget(targetUuid);

        Player voter = Bukkit.getPlayer(voterUuid);
        Player target = Bukkit.getPlayer(targetUuid);
        if (voter != null) {
            voter.sendMessage(Constants.PREFIX + "§a你投票给了 " + (target != null ? target.getName() : "未知玩家"));
        }
        checkAllVoted();
        return true;
    }

    public boolean skipVote(UUID voterUuid) {
        if (!votingOpen) return false;
        PlayerData pd = session.getPlayerData(voterUuid);
        if (pd == null || pd.hasVoted()) return false;

        skippedVotes.add(voterUuid);
        pd.setHasVoted(true);
        pd.setSkippedVote(true);

        Player voter = Bukkit.getPlayer(voterUuid);
        if (voter != null) {
            voter.sendMessage(Constants.PREFIX + "§a你选择了弃权");
        }
        checkAllVoted();
        return true;
    }

    private void checkAllVoted() {
        long totalVoted = votes.size() + skippedVotes.size();
        if (totalVoted >= session.getAliveCount()) {
            session.cancelTimer();
            session.endMeeting();
        }
    }

    public void processResults() {
        votingOpen = false;

        Map<UUID, Integer> voteCounts = new LinkedHashMap<>();
        for (UUID target : votes.values()) {
            voteCounts.merge(target, 1, Integer::sum);
        }

        if (voteCounts.isEmpty() && skippedVotes.isEmpty()) {
            session.broadcastMessage(Constants.PREFIX + "§e无人投票。");
            return;
        }

        if (voteCounts.isEmpty()) {
            session.broadcastMessage(Constants.PREFIX + "§e全员弃权，无人被淘汰。");
            return;
        }

        int maxVotes = Collections.max(voteCounts.values());
        int skipCount = skippedVotes.size();

        long playersWithMaxVotes = voteCounts.values().stream().filter(v -> v == maxVotes).count();

        if (maxVotes <= skipCount) {
            session.broadcastMessage(Constants.PREFIX + "§e最高票(" + maxVotes + ")未超过弃权数(" + skipCount + ")，无人被淘汰。");
            return;
        }

        if (playersWithMaxVotes > 1) {
            session.broadcastMessage(Constants.PREFIX + "§e存在平票，无人被淘汰。");
            return;
        }

        UUID eliminated = voteCounts.entrySet().stream()
                .filter(e -> e.getValue() == maxVotes)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        if (eliminated != null) {
            session.eliminateByVote(eliminated);
        }
    }

    public boolean isVotingOpen() { return votingOpen; }
}
