package com.imitatorgame.meeting;

import com.imitatorgame.game.GameSession;
import com.imitatorgame.game.PlayerData;
import com.imitatorgame.util.Constants;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.*;

public class MeetingManager {

    private final GameSession session;
    private UUID reporterUuid;
    private UUID victimUuid;
    private int discussionSeconds;
    private boolean votingOpen;
    private final Map<UUID, UUID> votes = new LinkedHashMap<>();
    private final Set<UUID> skipped = new HashSet<>();
    private Inventory voteGui;

    public MeetingManager(GameSession session) {
        this.session = session;
    }

    public void startMeeting(Player reporter, UUID victimUuid) {
        this.reporterUuid = reporter != null ? reporter.getUniqueId() : null;
        this.victimUuid = victimUuid;
        this.discussionSeconds = session.getPlugin().getConfigManager().getGameConfig().discussionSeconds();
        this.votes.clear();
        this.skipped.clear();
        this.votingOpen = false;

        // Announce with death info
        String reporterName = reporter != null ? reporter.getName() : "哨兵被动";
        if (victimUuid != null) {
            Player victim = Bukkit.getPlayer(victimUuid);
            String victimName = victim != null ? victim.getName() : "玩家";
            session.broadcastMessage(Constants.PREFIX + "§c" + reporterName + " 发现了 §4"
                    + victimName + " §c的尸体！紧急会议召开！");
        } else {
            session.broadcastMessage(Constants.PREFIX + "§c" + reporterName + " 通过电台召开了紧急会议！");
        }

        teleportToMeetingRoom();

        session.broadcastMessage(Constants.PREFIX + "§e讨论阶段开始！你有 §6" + discussionSeconds + " §e秒讨论");

        new BukkitRunnable() {
            int remaining = discussionSeconds;

            @Override
            public void run() {
                if (remaining <= 0) {
                    this.cancel();
                    startVoting();
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
        Location meetingLoc = session.getPlugin().getConfigManager().getMapConfig().getMeetingRoom();
        if (meetingLoc == null) return;

        for (UUID uuid : session.getAlivePlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                session.getPlayerData(uuid).setPreMeetingPosition(p.getLocation());
                p.teleport(meetingLoc);
                p.setGameMode(GameMode.ADVENTURE);
                p.setWalkSpeed(0);
            }
        }
        // Also teleport dead players (spectators) to watch
        for (UUID uuid : session.getDeadPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                Location watchLoc = meetingLoc.clone().add(0, 3, 0);
                p.teleport(watchLoc);
            }
        }
    }

    private void startVoting() {
        votingOpen = true;
        session.getPlugin().getGameManager().getCurrentSession()
                .getStateMachine().forcePhase(com.imitatorgame.game.GamePhase.MEETING_VOTING);

        int aliveCount = session.getAliveCount();
        int guiSize = Math.max(9, ((aliveCount + 1) / 9 + 1) * 9);
        voteGui = Bukkit.createInventory(null, Math.min(guiSize, 54),
                Component.text("投票 - 选择要淘汰的玩家"));

        List<UUID> aliveList = new ArrayList<>(session.getAlivePlayers());
        for (int i = 0; i < aliveList.size() && i < 44; i++) {
            UUID uuid = aliveList.get(i);
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            head.editMeta(meta -> {
                if (meta instanceof SkullMeta skull) {
                    skull.setOwningPlayer(p);
                }
                meta.displayName(Component.text("§e" + p.getName()));
                meta.lore(List.of(Component.text("§7点击投票给 " + p.getName())));
            });
            voteGui.setItem(i, head);
        }

        // Skip button at last slot
        ItemStack skip = new ItemStack(Material.BARRIER);
        skip.editMeta(meta -> {
            meta.displayName(Component.text("§7跳过投票"));
            meta.lore(List.of(Component.text("§7点击弃权，不投给任何人")));
        });
        voteGui.setItem(Math.min(guiSize, 54) - 1, skip);

        // Open GUI for all alive players
        for (UUID uuid : session.getAlivePlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.openInventory(voteGui);
        }

        session.broadcastMessage(Constants.PREFIX + "§e投票阶段开始！点击背包中的玩家头颅投票");
        session.broadcastMessage(Constants.PREFIX + "§e§l未在时间内投票视为弃权！");

        int votingSeconds = session.getPlugin().getConfigManager().getGameConfig().votingSeconds();
        new BukkitRunnable() {
            int remaining = votingSeconds;

            @Override
            public void run() {
                if (remaining <= 0) {
                    this.cancel();
                    processResults();
                    return;
                }
                if (remaining <= 5) {
                    session.broadcastMessage(Constants.PREFIX + "§e投票剩余 §6" + remaining + " §e秒");
                }
                remaining--;
            }
        }.runTaskTimer(session.getPlugin(), 0, 20);
    }

    public void handleVoteClick(Player voter, int slot, Inventory inv) {
        if (!votingOpen || !inv.equals(voteGui)) return;
        if (!session.isAlive(voter.getUniqueId())) return;
        PlayerData pd = session.getPlayerData(voter.getUniqueId());
        if (pd == null || pd.hasVoted()) return;

        int skipSlot = Math.min(voteGui.getSize(), 54) - 1;
        if (slot == skipSlot) {
            // Skip
            skipped.add(voter.getUniqueId());
            pd.setHasVoted(true);
            pd.setSkippedVote(true);
            voter.closeInventory();
            voter.sendMessage(Constants.PREFIX + "§a你选择了弃权");
        } else {
            ItemStack clicked = inv.getItem(slot);
            if (clicked == null || clicked.getType() != Material.PLAYER_HEAD) return;
            // Find the target player by matching skull owner
            for (UUID uuid : session.getAlivePlayers()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null) continue;
                if (clicked.getItemMeta() instanceof SkullMeta skull
                        && p.getUniqueId().equals(skull.getOwningPlayer() != null
                        ? skull.getOwningPlayer().getUniqueId() : null)) {
                    // match by name fallback
                    String displayName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                            .plainText().serialize(clicked.getItemMeta().displayName());
                    if (displayName.contains(p.getName())) {
                        votes.put(voter.getUniqueId(), uuid);
                        pd.setHasVoted(true);
                        pd.setVoteTarget(uuid);
                        voter.closeInventory();
                        voter.sendMessage(Constants.PREFIX + "§a你投票给了 " + p.getName());
                        break;
                    }
                }
            }
        }

        // Check if all voted
        if (votes.size() + skipped.size() >= session.getAliveCount()) {
            session.cancelTimer();
            processResults();
        }
    }

    private void processResults() {
        votingOpen = false;
        // Close all open vote GUIs
        for (UUID uuid : session.getAlivePlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.closeInventory();
        }

        // Auto-skip anyone who didn't vote
        for (UUID uuid : session.getAlivePlayers()) {
            PlayerData pd = session.getPlayerData(uuid);
            if (pd != null && !pd.hasVoted()) {
                skipped.add(uuid);
                pd.setHasVoted(true);
                pd.setSkippedVote(true);
            }
        }

        Map<UUID, Integer> voteCounts = new LinkedHashMap<>();
        for (UUID target : votes.values()) {
            voteCounts.merge(target, 1, Integer::sum);
        }

        String resultTitle;
        String resultSubtitle;

        if (voteCounts.isEmpty()) {
            resultTitle = "§e全员弃权";
            resultSubtitle = "§7无人被淘汰";
        } else {
            int maxVotes = Collections.max(voteCounts.values());
            int skipCount = skipped.size();
            long playersWithMax = voteCounts.values().stream().filter(v -> v == maxVotes).count();

            if (maxVotes <= skipCount) {
                resultTitle = "§e票数不足";
                resultSubtitle = "§7最高票(" + maxVotes + ") ≤ 弃权(" + skipCount + ")，无人淘汰";
            } else if (playersWithMax > 1) {
                resultTitle = "§6平票";
                resultSubtitle = "§7无人被淘汰";
            } else {
                UUID eliminated = voteCounts.entrySet().stream()
                        .filter(e -> e.getValue() == maxVotes)
                        .map(Map.Entry::getKey)
                        .findFirst().orElse(null);
                if (eliminated != null) {
                    Player target = Bukkit.getPlayer(eliminated);
                    String name = target != null ? target.getName() : "玩家";
                    session.eliminateByVote(eliminated);
                    resultTitle = "§c" + name + " 被淘汰";
                    resultSubtitle = "§7获得了 " + maxVotes + " 票";
                } else {
                    resultTitle = "§e无人被淘汰";
                    resultSubtitle = "";
                }
            }
        }

        // Show result to EVERYONE including spectators via Title
        Title title = Title.title(
                Component.text(resultTitle),
                Component.text(resultSubtitle),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(500))
        );
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(title);
        }

        // Also broadcast in chat
        session.broadcastMessage(Constants.PREFIX + resultTitle + " " + resultSubtitle);

        // Transition to result phase
        session.getStateMachine().forcePhase(com.imitatorgame.game.GamePhase.MEETING_RESULT);
        int resultSeconds = session.getPlugin().getConfigManager().getGameConfig().resultSeconds();
        new BukkitRunnable() {
            int remaining = resultSeconds;

            @Override
            public void run() {
                if (remaining <= 0) {
                    this.cancel();
                    session.checkWinAndResume();
                    return;
                }
                remaining--;
            }
        }.runTaskTimer(session.getPlugin(), 0, 20);
    }

    public boolean isVotingOpen() { return votingOpen; }
    public UUID getReporterUuid() { return reporterUuid; }
    public UUID getVictimUuid() { return victimUuid; }

    public String getVictimName() {
        if (victimUuid == null) return "无";
        Player p = Bukkit.getPlayer(victimUuid);
        return p != null ? p.getName() : "玩家";
    }

    public String getReporterName() {
        if (reporterUuid == null) return "系统";
        Player p = Bukkit.getPlayer(reporterUuid);
        return p != null ? p.getName() : "玩家";
    }
}
