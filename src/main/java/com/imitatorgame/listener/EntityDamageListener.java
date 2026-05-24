package com.imitatorgame.listener;

import com.imitatorgame.ImitatorGamePlugin;
import com.imitatorgame.game.GameSession;
import com.imitatorgame.game.PlayerData;
import com.imitatorgame.role.Role;
import com.imitatorgame.util.Constants;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public class EntityDamageListener implements Listener {

    private final ImitatorGamePlugin plugin;

    public EntityDamageListener(ImitatorGamePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        GameSession session = plugin.getGameManager().getCurrentSession();
        if (session == null || !session.isActive()) return;
        if (!session.isAlive(victim.getUniqueId()) || !session.isAlive(attacker.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        PlayerData apd = session.getPlayerData(attacker.getUniqueId());
        if (apd == null) return;

        // All PvP during game: cancel vanilla damage, handle via role logic
        event.setDamage(0);
        event.setCancelled(true);

        ItemStack held = attacker.getInventory().getItemInMainHand();
        String tag = Role.getItemTag(held);

        // One-hit kill: imitator knife, sheriff sword, hunter sword
        boolean canOneHit = "imitator_knife".equals(tag)
                || apd.getRole() == Role.SHERIFF
                || apd.getRole() == Role.HUNTER;

        if (canOneHit) {
            if (apd.isOnCooldown()) {
                attacker.sendMessage(Constants.PREFIX + "§c冷却中！还需 "
                        + String.format("%.1f", apd.getCooldownRemainingSeconds()) + " 秒");
                return;
            }
            if (!apd.canUseGlobal()) {
                attacker.sendMessage(Constants.PREFIX + "§c技能次数已用完");
                return;
            }
            apd.tryUseGlobal();
            apd.markAbilityUsed();

            // Sheriff risk check
            if (apd.getRole() == Role.SHERIFF) {
                PlayerData vpd = session.getPlayerData(victim.getUniqueId());
                if (vpd != null && vpd.getFaction() == com.imitatorgame.role.Faction.IMITATOR) {
                    session.handleDeath(victim.getUniqueId());
                } else {
                    session.handleDeath(attacker.getUniqueId());
                }
            } else {
                session.handleDeath(victim.getUniqueId());
            }
            return;
        }

        // Deliveryman eat on left-click too
        if (apd.getRole() == Role.DELIVERYMAN) {
            handleDeliverymanEat(attacker, victim, apd, session);
        }
    }

    private void handleDeliverymanEat(Player player, Player target, PlayerData pd, GameSession session) {
        if (pd.isOnCooldown()) {
            player.sendMessage(Constants.PREFIX + "§c冷却中");
            return;
        }
        pd.setSwallowedTarget(target.getUniqueId());
        pd.incrementEatUseCount();
        pd.setAbilityCooldownMillis(pd.getDeliverymanCooldown());
        pd.markAbilityUsed();
        player.sendMessage(Constants.PREFIX + "§a吞下 " + target.getName());
        target.sendMessage(Constants.PREFIX + "§e被送货员吞入腹中...会议时死亡");
        target.setSpectatorTarget(player);
        target.setInvisible(true);
    }
}
