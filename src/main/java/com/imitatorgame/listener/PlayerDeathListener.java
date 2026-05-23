package com.imitatorgame.listener;

import com.imitatorgame.ImitatorGamePlugin;
import com.imitatorgame.game.GamePhase;
import com.imitatorgame.game.GameSession;
import com.imitatorgame.game.PlayerData;
import com.imitatorgame.role.Role;
import com.imitatorgame.util.Constants;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {

    private final ImitatorGamePlugin plugin;

    public PlayerDeathListener(ImitatorGamePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getPlayer();
        GameSession session = plugin.getGameManager().getCurrentSession();
        if (session == null || !session.isActive()) return;
        if (!session.isAlive(victim.getUniqueId())) return;

        event.setDeathMessage(null);

        PlayerData pd = session.getPlayerData(victim.getUniqueId());
        Location deathLoc = victim.getLocation();

        spawnCorpse(victim, deathLoc);

        session.handleDeath(victim.getUniqueId());

        if (pd != null && pd.getRole() == Role.SENTRY) {
            session.broadcastMessage(Constants.PREFIX + "§c哨兵被击杀！自动召开紧急会议！");
            session.callMeeting(null, victim.getUniqueId());
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        GameSession session = plugin.getGameManager().getCurrentSession();
        if (session == null || !session.isActive()) return;

        if (session.isInMeeting()) {
            event.setCancelled(true);
        }
    }

    private void spawnCorpse(Player victim, Location loc) {
        loc.getWorld().spawn(loc, ArmorStand.class, stand -> {
            stand.setVisible(false);
            stand.setMarker(true);
            stand.setGravity(false);
            stand.customName(net.kyori.adventure.text.Component.text("§c" + victim.getName() + " 的尸体"));
            stand.setCustomNameVisible(true);
            stand.getPersistentDataContainer().set(
                    Constants.CORPSE_KEY,
                    org.bukkit.persistence.PersistentDataType.STRING,
                    victim.getUniqueId().toString()
            );
        });
    }
}
