package com.imitatorgame.listener;

import com.imitatorgame.ImitatorGamePlugin;
import com.imitatorgame.game.GameSession;
import com.imitatorgame.game.PlayerData;
import com.imitatorgame.role.Role;
import com.imitatorgame.util.Constants;
import org.bukkit.Location;
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
        event.getDrops().clear(); // keep inventory
        event.setKeepInventory(true);
        PlayerData pd = session.getPlayerData(victim.getUniqueId());
        Location deathLoc = victim.getLocation();

        // Spawn bone block corpse
        if (session.getDeathManager() != null) {
            session.getDeathManager().spawnCorpse(victim, deathLoc);
        }

        session.handleDeath(victim.getUniqueId());

        // Make dead player invisible and no collision
        victim.setInvisible(true);
        victim.setCollidable(false);
        victim.setSilent(true);

        // Give lobby return beacon
        Role.giveDeathBeacon(victim);

        victim.sendMessage(Constants.PREFIX + "§7你已死亡。手持信标右键可返回大厅。");

        // Sentry triggers meeting
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
}
