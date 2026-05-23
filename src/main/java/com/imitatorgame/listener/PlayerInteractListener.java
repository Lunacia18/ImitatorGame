package com.imitatorgame.listener;

import com.imitatorgame.ImitatorGamePlugin;
import com.imitatorgame.event.FloodingEvent;
import com.imitatorgame.event.PowerOutageEvent;
import com.imitatorgame.event.TimedGameEvent;
import com.imitatorgame.game.GamePhase;
import com.imitatorgame.game.GameSession;
import com.imitatorgame.game.PlayerData;
import com.imitatorgame.role.Faction;
import com.imitatorgame.role.Role;
import com.imitatorgame.util.Constants;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import java.util.*;

public class PlayerInteractListener implements Listener {

    private final ImitatorGamePlugin plugin;
    private int furnaceClicks, craftingClicks;

    public PlayerInteractListener(ImitatorGamePlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        GameSession session = plugin.getGameManager().getCurrentSession();
        if (session == null) return;
        Block clicked = event.getClickedBlock();

        // Dead → return beacon
        if (session.isActive() && session.isDead(player.getUniqueId())) {
            event.setCancelled(true);
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
                handleDeadReturn(player, session);
            return;
        }

        GamePhase phase = session.getPhase();
        if (phase != GamePhase.FREE_ACTION && phase != GamePhase.RUSH_HOUR) return;
        if (!session.isAlive(player.getUniqueId())) return;
        PlayerData pd = session.getPlayerData(player.getUniqueId());
        if (pd == null) return;

        // Event item triggers
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            String tag = Role.getItemTag(player.getInventory().getItemInMainHand());
            if ("power_outage".equals(tag)) { session.triggerPowerOutage(); return; }
            if ("flooding".equals(tag)) { session.triggerFlooding(); return; }
        }

        if (clicked == null) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        // Bone block corpse → meeting
        if (clicked.getType() == Material.BONE_BLOCK && session.getDeathManager() != null) {
            UUID v = session.getDeathManager().getVictimFromBlock(clicked);
            if (v != null) { session.callMeeting(player, v); return; }
        }

        // Iron door
        if (clicked.getType() == Material.IRON_DOOR) { handleIronDoor(player, clicked, session); return; }

        // Event fixes: blackout = crafting table 25x, water valve = furnace 25x
        if (clicked.getType() == Material.CRAFTING_TABLE) { handleCraftingTableFix(player, session); return; }
        if (clicked.getType() == Material.FURNACE) { handleFurnaceFix(player, session); return; }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        GameSession session = plugin.getGameManager().getCurrentSession();
        if (session == null) return;
        if (session.isActive() && session.isDead(player.getUniqueId())) { event.setCancelled(true); handleDeadReturn(player, session); return; }

        GamePhase phase = session.getPhase();
        if (phase != GamePhase.FREE_ACTION && phase != GamePhase.RUSH_HOUR) return;
        if (!session.isAlive(player.getUniqueId())) return;
        if (!(event.getRightClicked() instanceof Player target)) return;
        if (!session.isAlive(target.getUniqueId())) return;

        PlayerData pd = session.getPlayerData(player.getUniqueId());
        if (pd == null) return;

        // Deliveryman eat (right-click)
        if (pd.getRole() == Role.DELIVERYMAN) { handleDeliverymanEat(player, target, pd, session); return; }

        // Pyro bomb pass
        if (pd.getRole() == Role.PYROTECHNICIAN && pd.hasBomb()
                && "pyro_bomb".equals(Role.getItemTag(player.getInventory().getItemInMainHand()))) {
            handleBombPass(player, target, pd, session); return;
        }

        handleRoleAbility(player, target, session);
    }

    // --- Dead return ---
    private void handleDeadReturn(Player player, GameSession session) {
        if ("return_lobby".equals(Role.getItemTag(player.getInventory().getItemInMainHand()))) {
            player.setInvisible(false); player.setCollidable(true); player.setSilent(false);
            player.setSpectatorTarget(null); player.getInventory().clear();
            var lobby = plugin.getLobbyManager();
            if (lobby != null) lobby.teleportToLobby(player);
        }
    }

    // --- Deliveryman eat (unlimited) ---
    private void handleDeliverymanEat(Player player, Player target, PlayerData pd, GameSession session) {
        if (pd.isOnCooldown()) { player.sendMessage(Constants.PREFIX + "§c冷却中"); return; }
        pd.setSwallowedTarget(target.getUniqueId());
        pd.incrementEatUseCount();
        pd.setAbilityCooldownMillis(pd.getDeliverymanCooldown());
        pd.markAbilityUsed();
        target.setInvisible(true);
        target.setSpectatorTarget(player);
        player.sendMessage(Constants.PREFIX + "§a吞下 " + target.getName());
        target.sendMessage(Constants.PREFIX + "§e被送货员吞入腹中...会议时死亡");
    }

    // --- Pyro bomb pass ---
    private void handleBombPass(Player player, Player target, PlayerData pd, GameSession session) {
        pd.setHasBomb(false); pd.setBombExpireTime(0); pd.setBombVisibleToAll(false);
        for (ItemStack i : player.getInventory().getContents())
            if (i != null && "pyro_bomb".equals(Role.getItemTag(i))) player.getInventory().remove(i);
        PlayerData td = session.getPlayerData(target.getUniqueId());
        td.setHasBomb(true); td.setBombExpireTime(System.currentTimeMillis() + 20_000); td.setBombVisibleToAll(false);
        target.getInventory().addItem(Role.createFixedItem(Material.TNT, "§4§l定时炸弹",
                List.of("§720秒倒计时", "§7<11s他人可见", "§7右键他人传递"), "pyro_bomb"));
        player.sendMessage(Constants.PREFIX + "§c炸弹传给 " + target.getName());
        target.sendMessage(Constants.PREFIX + "§4被塞了炸弹！20秒后爆炸！");
    }

    // --- Power outage fix = crafting table 25x ---
    private void handleCraftingTableFix(Player player, GameSession session) {
        var em = session.getEventManager();
        if (em == null || !em.hasActiveEvent(PowerOutageEvent.class)) return;
        craftingClicks++;
        player.sendActionBar(Component.text("§e恢复电力: " + craftingClicks + "/25"));
        if (craftingClicks >= 25) {
            craftingClicks = 0;
            for (TimedGameEvent e : new ArrayList<>(session.getActiveEvents()))
                if (e instanceof PowerOutageEvent poe) { poe.fix(session); break; }
        }
    }

    // --- Water valve fix = furnace 25x ---
    private void handleFurnaceFix(Player player, GameSession session) {
        var em = session.getEventManager();
        if (em == null || !em.hasActiveEvent(FloodingEvent.class)) return;
        furnaceClicks++;
        player.sendActionBar(Component.text("§e关闭水阀: " + furnaceClicks + "/25"));
        if (furnaceClicks >= 25) {
            furnaceClicks = 0;
            for (TimedGameEvent e : new ArrayList<>(session.getActiveEvents()))
                if (e instanceof FloodingEvent fe) { fe.tryFixValve(1, player, session); break; }
        }
    }

    // --- Iron door ---
    private void handleIronDoor(Player player, Block door, GameSession session) {
        PlayerData pd = session.getPlayerData(player.getUniqueId());
        if (pd == null || (pd.getFaction() != Faction.IMITATOR && pd.getRole() != Role.LOCKSMITH)) return;
        if (door.getBlockData() instanceof org.bukkit.block.data.type.Door dd) { dd.setOpen(!dd.isOpen()); door.setBlockData(dd); }
    }

    // --- Role abilities (right-click) ---
    private void handleRoleAbility(Player player, Player target, GameSession session) {
        PlayerData pd = session.getPlayerData(player.getUniqueId());
        if (pd == null) return;
        switch (pd.getRole()) {
            case DETECTIVE -> {
                if (!checkCd(player, pd)) return;
                PlayerData td = session.getPlayerData(target.getUniqueId());
                player.sendMessage(Constants.PREFIX + "§a" + target.getName() + " 阵营: " + (td != null ? td.getFaction().getDisplayName() : "?"));
            }
            case SPICE_MASTER -> {
                if (!checkCd(player, pd)) return;
                target.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.GLOWING, 1200, 0, false, false));
                player.sendMessage(Constants.PREFIX + "§a标记 " + target.getName());
            }
            case MASTER_THIEF -> {
                if (!checkCd(player, pd)) return;
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY, 200, 0, false, false));
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 200, 1, false, false));
                player.sendMessage(Constants.PREFIX + "§a隐身+加速 10秒");
            }
            case CHANGELING -> {
                if (!checkCd(player, pd)) return;
                if (pd.getRecordedTarget() == null) {
                    pd.setRecordedTarget(target.getUniqueId());
                    player.sendMessage(Constants.PREFIX + "§a记录 " + target.getName() + "，再次右键变装30秒");
                } else {
                    Player rec = Bukkit.getPlayer(pd.getRecordedTarget());
                    if (rec != null) {
                        player.setPlayerListName(rec.getName()); player.displayName(Component.text(rec.getName()));
                        player.sendMessage(Constants.PREFIX + "§a变装为 " + rec.getName() + "！30秒");
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            player.setPlayerListName(player.getName()); player.displayName(null);
                            pd.setRecordedTarget(null); player.sendMessage(Constants.PREFIX + "§e变装结束");
                        }, 20 * 30);
                    }
                }
            }
            case PYROTECHNICIAN -> {
                if (!checkCd(player, pd)) return;
                if (pd.hasBomb()) { player.sendMessage(Constants.PREFIX + "§c已有炸弹"); return; }
                pd.setHasBomb(true); pd.setBombExpireTime(System.currentTimeMillis() + 20_000); pd.setBombVisibleToAll(false);
                player.getInventory().addItem(Role.createFixedItem(Material.TNT, "§4§l定时炸弹",
                        List.of("§7右键他人传递", "§720秒倒计时"), "pyro_bomb"));
                player.sendMessage(Constants.PREFIX + "§c炸弹制造完成！右键他人传递");
            }
            case VAGABOND -> {
                pd.getVagabondInteractions().add(target.getUniqueId());
                player.sendMessage(Constants.PREFIX + "§a互动 " + target.getName() + " (" + pd.getVagabondInteractions().size() + ")");
            }
        }
    }

    private boolean checkCd(Player player, PlayerData pd) {
        if (pd.isOnCooldown()) { player.sendMessage(Constants.PREFIX + "§c冷却中！还需 " + String.format("%.1f", pd.getCooldownRemainingSeconds()) + " 秒"); return false; }
        if (!pd.canUseGlobal()) { player.sendMessage(Constants.PREFIX + "§c次数已用完"); return false; }
        pd.tryUseGlobal(); pd.markAbilityUsed(); return true;
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        GameSession session = plugin.getGameManager().getCurrentSession();
        if (session == null || !session.isActive()) return;
        if (!event.getPlayer().hasPermission("imitatorgame.admin") && Role.isFixedItem(event.getItemDrop().getItemStack()))
            event.setCancelled(true);
    }
}
