package com.imitatorgame.game;

import com.imitatorgame.role.Faction;
import com.imitatorgame.role.Role;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

public class PlayerData {

    private final UUID uuid;
    private Player player;
    private Role role = Role.NONE;
    private Faction faction = Faction.NONE;
    private boolean alive = true;
    private boolean hasVoted;
    private UUID voteTarget;
    private boolean skippedVote;
    private int taskProgress;
    private int totalTasks;
    private Location preMeetingPosition;
    private int abilityUsesRemaining;
    private boolean usedRadio;
    private final Set<UUID> vagabondInteractions = new HashSet<>();
    private int deliveryProgress;
    private boolean disguiseActive;

    // Cooldown system
    private long lastAbilityUseTime; // epoch millis
    private long abilityCooldownMillis; // configured per-role
    private int globalUsesRemaining = -1; // -1 = unlimited

    // Deliveryman eat target: UUID of swallowed player
    private UUID swallowedTarget;

    // Pyrotechnician bomb
    private boolean hasBomb; // is holding the bomb item
    private long bombExpireTime; // epoch millis when bomb explodes
    private boolean bombVisibleToAll; // swapped at 11s

    // Imitator knife
    private boolean hasKnife;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    // Basic getters/setters
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

    // Cooldowns
    public long getLastAbilityUseTime() { return lastAbilityUseTime; }
    public void setLastAbilityUseTime(long v) { lastAbilityUseTime = v; }
    public long getAbilityCooldownMillis() { return abilityCooldownMillis; }
    public void setAbilityCooldownMillis(long v) { abilityCooldownMillis = v; }

    public boolean isOnCooldown() {
        if (abilityCooldownMillis <= 0) return false;
        return System.currentTimeMillis() - lastAbilityUseTime < abilityCooldownMillis;
    }

    public long getCooldownRemainingMillis() {
        if (abilityCooldownMillis <= 0) return 0;
        long remaining = abilityCooldownMillis - (System.currentTimeMillis() - lastAbilityUseTime);
        return Math.max(0, remaining);
    }

    public double getCooldownRemainingSeconds() {
        return getCooldownRemainingMillis() / 1000.0;
    }

    public void markAbilityUsed() {
        this.lastAbilityUseTime = System.currentTimeMillis();
    }

    public int getGlobalUsesRemaining() { return globalUsesRemaining; }
    public void setGlobalUsesRemaining(int v) { globalUsesRemaining = v; }

    public boolean canUseGlobal() {
        return globalUsesRemaining == -1 || globalUsesRemaining > 0;
    }

    public boolean tryUseGlobal() {
        if (globalUsesRemaining == -1) return true; // unlimited
        if (globalUsesRemaining <= 0) return false;
        globalUsesRemaining--;
        return true;
    }

    // Deliveryman eat
    public UUID getSwallowedTarget() { return swallowedTarget; }
    public void setSwallowedTarget(UUID v) { swallowedTarget = v; }

    // Pyro bomb
    public boolean hasBomb() { return hasBomb; }
    public void setHasBomb(boolean v) { hasBomb = v; }
    public long getBombExpireTime() { return bombExpireTime; }
    public void setBombExpireTime(long v) { bombExpireTime = v; }
    public boolean isBombVisibleToAll() { return bombVisibleToAll; }
    public void setBombVisibleToAll(boolean v) { bombVisibleToAll = v; }

    // Knife
    public boolean hasKnife() { return hasKnife; }
    public void setHasKnife(boolean v) { hasKnife = v; }

    // Reset
    public void resetForNewRound() {
        hasVoted = false;
        voteTarget = null;
        skippedVote = false;
        preMeetingPosition = null;
    }

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
        lastAbilityUseTime = 0;
        abilityCooldownMillis = 0;
        globalUsesRemaining = -1;
        swallowedTarget = null;
        hasBomb = false;
        bombExpireTime = 0;
        bombVisibleToAll = false;
        hasKnife = false;
    }
}
