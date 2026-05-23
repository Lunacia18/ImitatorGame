package com.imitatorgame.game;

import com.imitatorgame.role.Faction;
import com.imitatorgame.role.Role;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private Player player;
    private Role role = Role.NONE;
    private Faction faction = Faction.NONE;
    private boolean alive = true;
    private boolean hasVoted = false;
    private UUID voteTarget = null;
    private boolean skippedVote = false;
    private int taskProgress = 0;
    private int totalTasks = 0;
    private Location preMeetingPosition;
    private int abilityUsesRemaining = 0;
    private boolean usedRadio = false;
    private final Set<UUID> vagabondInteractions = new HashSet<>();
    private int deliveryProgress = 0;
    private boolean disguiseActive = false;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() { return uuid; }
    public Player getPlayer() { return player; }
    public void setPlayer(Player p) { this.player = p; }

    public Role getRole() { return role; }
    public void setRole(Role r) { this.role = r; this.faction = r.getFaction(); }

    public Faction getFaction() { return faction; }
    public void setFaction(Faction f) { this.faction = f; }

    public boolean isAlive() { return alive; }
    public void setAlive(boolean v) { alive = v; }

    public boolean hasVoted() { return hasVoted; }
    public void setHasVoted(boolean v) { hasVoted = v; }

    public UUID getVoteTarget() { return voteTarget; }
    public void setVoteTarget(UUID v) { voteTarget = v; }

    public boolean skippedVote() { return skippedVote; }
    public void setSkippedVote(boolean v) { skippedVote = v; }

    public int getTaskProgress() { return taskProgress; }
    public void setTaskProgress(int v) { taskProgress = v; }
    public void incrementTaskProgress() { taskProgress++; }

    public int getTotalTasks() { return totalTasks; }
    public void setTotalTasks(int v) { totalTasks = v; }

    public Location getPreMeetingPosition() { return preMeetingPosition; }
    public void setPreMeetingPosition(Location v) { preMeetingPosition = v; }

    public int getAbilityUsesRemaining() { return abilityUsesRemaining; }
    public void setAbilityUsesRemaining(int v) { abilityUsesRemaining = v; }
    public boolean consumeAbilityUse() {
        if (abilityUsesRemaining > 0) { abilityUsesRemaining--; return true; }
        return false;
    }

    public boolean hasUsedRadio() { return usedRadio; }
    public void setUsedRadio(boolean v) { usedRadio = v; }

    public Set<UUID> getVagabondInteractions() { return vagabondInteractions; }

    public int getDeliveryProgress() { return deliveryProgress; }
    public void setDeliveryProgress(int v) { deliveryProgress = v; }
    public void incrementDeliveryProgress() { deliveryProgress++; }

    public boolean isDisguiseActive() { return disguiseActive; }
    public void setDisguiseActive(boolean v) { disguiseActive = v; }

    public void resetForNewGame() {
        role = Role.NONE;
        faction = Faction.NONE;
        alive = true;
        hasVoted = false;
        voteTarget = null;
        skippedVote = false;
        taskProgress = 0;
        totalTasks = 0;
        preMeetingPosition = null;
        abilityUsesRemaining = 0;
        usedRadio = false;
        vagabondInteractions.clear();
        deliveryProgress = 0;
        disguiseActive = false;
    }

    public void resetForNewRound() {
        hasVoted = false;
        voteTarget = null;
        skippedVote = false;
        preMeetingPosition = null;
    }
}
