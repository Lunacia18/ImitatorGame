package com.imitatorgame.event;

import com.imitatorgame.game.GameSession;
import com.imitatorgame.role.Faction;
import com.imitatorgame.util.Constants;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PowerOutageEvent implements TimedGameEvent {

    private boolean resolved;

    public PowerOutageEvent(int durationSeconds) {
        // duration ignored — unlimited until fixed
    }

    @Override
    public void onActivate(GameSession session) {
        session.broadcastMessage(Constants.PREFIX + "§8§l停电！右键熔炉30次修复！");
        int longTicks = 99999 * 20;
        for (var uuid : session.getAlivePlayers()) {
            var pd = session.getPlayerData(uuid);
            if (pd == null || pd.getFaction() == Faction.IMITATOR) continue;
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, longTicks, 0, false, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, longTicks, 0, false, false, false));
                p.playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1f, 0.5f);
            }
        }
    }

    @Override
    public void onTick(GameSession session) {} // No auto-expire

    @Override
    public void onDeactivate(GameSession session) {
        for (var uuid : session.getAlivePlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.removePotionEffect(PotionEffectType.BLINDNESS);
                p.removePotionEffect(PotionEffectType.DARKNESS);
                p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);
            }
        }
    }

    @Override public boolean isExpired() { return false; } // never expires
    @Override public boolean isResolved() { return resolved; }

    public void fix(GameSession session) {
        if (resolved) return;
        resolved = true;
        session.broadcastMessage(Constants.PREFIX + "§a电力已修复！");
        onDeactivate(session);
    }
}
