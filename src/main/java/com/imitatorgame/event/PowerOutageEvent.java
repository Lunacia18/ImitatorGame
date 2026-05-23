package com.imitatorgame.event;

import com.imitatorgame.game.GameSession;
import com.imitatorgame.game.PlayerData;
import com.imitatorgame.role.Faction;
import com.imitatorgame.util.Constants;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PowerOutageEvent implements TimedGameEvent {

    private int remainingTicks;
    private boolean resolved;
    private final int durationTicks;

    public PowerOutageEvent(int durationSeconds) {
        this.durationTicks = durationSeconds * Constants.TICKS_PER_SECOND;
        this.remainingTicks = this.durationTicks;
    }

    @Override
    public void onActivate(GameSession session) {
        session.broadcastMessage(Constants.PREFIX + "§8§l停电了！非模仿者玩家视野受限！");
        session.broadcastMessage(Constants.PREFIX + "§7前往配电室修复电力...");

        for (var uuid : session.getAlivePlayers()) {
            PlayerData pd = session.getPlayerData(uuid);
            if (pd == null || pd.getFaction() == Faction.IMITATOR) continue;
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,
                        durationTicks, 0, false, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS,
                        durationTicks, 0, false, false, false));
                p.playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.5f);
            }
        }
    }

    @Override
    public void onTick(GameSession session) {
        remainingTicks--;
        if (remainingTicks <= 0 && !resolved) {
            session.broadcastMessage(Constants.PREFIX + "§a电力自动恢复");
        }
    }

    @Override
    public void onDeactivate(GameSession session) {
        for (var uuid : session.getAlivePlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.removePotionEffect(PotionEffectType.BLINDNESS);
                p.removePotionEffect(PotionEffectType.DARKNESS);
                p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
            }
        }
    }

    @Override
    public boolean isExpired() {
        return remainingTicks <= 0;
    }

    @Override
    public boolean isResolved() {
        return resolved;
    }

    public void fix(GameSession session) {
        if (resolved) return;
        resolved = true;
        session.broadcastMessage(Constants.PREFIX + "§a电力已修复！视野恢复正常。");
        onDeactivate(session);
    }

    public boolean tryFix(Player player, GameSession session) {
        Location fixLoc = session.getPlugin().getConfigManager().getMapConfig().getPowerOutageFix();
        if (fixLoc != null && player.getLocation().distance(fixLoc) > 5) {
            player.sendMessage(Constants.PREFIX + "§c你离配电室太远了");
            return false;
        }
        fix(session);
        return true;
    }
}
