package com.imitatorgame.listener;

import com.imitatorgame.ImitatorGamePlugin;
import com.imitatorgame.event.*;
import com.imitatorgame.game.*;
import com.imitatorgame.role.*;
import com.imitatorgame.util.Constants;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import java.util.*;

public class PlayerInteractListener implements Listener {

    private final ImitatorGamePlugin plugin;
    private int furnaceFixClicks, craftingFixClicks;

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

        // Event item triggers (air or block)
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            String tag = Role.getItemTag(player.getInventory().getItemInMainHand());
            if ("power_outage".equals(tag)) { session.triggerPowerOutage(); return; }
            if ("flooding".equals(tag)) { session.triggerFlooding(); return; }
        }

        if (clicked == null) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        // Cartography table → meeting
        if (clicked.getType() == Material.CARTOGRAPHY_TABLE) {
            session.callMeeting(player, null);
            return;
        }

        // Bone block
        if (clicked.getType() == Material.BONE_BLOCK && session.getDeathManager() != null) {
            UUID v = session.getDeathManager().getVictimFromBlock(clicked);
            if (v != null) { session.callMeeting(player, v); return; }
        }

        // Iron doors → hideout
        if (clicked.getType() == Material.IRON_DOOR) {
            handleIronDoor(player, clicked, session, pd);
            return;
        }

        // Lodestone task
        if (clicked.getType() == Material.LODESTONE) {
            handleLodestoneTask(player, pd, session);
            return;
        }

        // Event fixes
        if (clicked.getType() == Material.CRAFTING_TABLE) { handleCraftingTableFix(player, session); return; }
        if (clicked.getType() == Material.FURNACE) { handleFurnaceFix(player, session); return; }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
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

        if (pd.getRole() == Role.DELIVERYMAN) { handleDeliverymanEat(player, target, pd, session); return; }
        // Anyone holding the bomb can pass it
        if ("pyro_bomb".equals(Role.getItemTag(player.getInventory().getItemInMainHand()))) {
            handleBombPass(player, target, pd, session); return;
        }
        handleRoleAbility(player, target, session);
    }

    // Shift to exit door hideout
    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        GameSession session = plugin.getGameManager().getCurrentSession();
        if (session == null || !session.isActive()) return;
        if (!event.isSneaking()) return;
        session.exitDoorHideout(player.getUniqueId());
    }

    // --- Lodestone task ---
    private void handleLodestoneTask(Player player, PlayerData pd, GameSession session) {
        if (pd.getFaction() != Faction.DETECTIVE) { player.sendMessage(Constants.PREFIX + "§7只有侦探团能做任务"); return; }
        if (session.completeDetectiveTask(player.getUniqueId(), player.getLocation())) {
            player.sendMessage(Constants.PREFIX + "§a任务完成！§7(总进度: " + session.getTotalTasksCompleted() + "/" + session.getTargetTotalTasks() + ")");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        } else {
            player.sendMessage(Constants.PREFIX + "§c任务冷却中，请等5秒");
        }
    }

    // --- Iron door hideout ---
    private void handleIronDoor(Player player, Block door, GameSession session, PlayerData pd) {
        var mm = session.getGameMapManager();
        if (mm == null) return;
        int doorNum = 0;
        if (door.getLocation().distance(mm.getDoor1Loc()) < 3) doorNum = 1;
        else if (door.getLocation().distance(mm.getDoor2Loc()) < 3) doorNum = 2;
        if (doorNum == 0) return;

        // Locksmith: left-click with string = lock
        if (pd.getRole() == Role.LOCKSMITH && player.isSneaking()) {
            session.lockDoor(doorNum, player);
            return;
        }

        session.enterDoorHideout(player, doorNum);
    }

    // --- Event fixes ---
    private void handleCraftingTableFix(Player player, GameSession session) {
        var em = session.getEventManager();
        if (em == null || !em.hasActiveEvent(PowerOutageEvent.class)) return;
        // Glow the block
        if (session.getGameMapManager() != null) session.getGameMapManager().setBlockGlowing(player.getLocation(), true);
        craftingFixClicks++;
        player.sendActionBar(Component.text("§e恢复电力: " + craftingFixClicks + "/25"));
        if (craftingFixClicks >= 25) { craftingFixClicks = 0; for (TimedGameEvent e : new ArrayList<>(session.getActiveEvents())) if (e instanceof PowerOutageEvent poe) { poe.fix(session); break; } }
    }

    private void handleFurnaceFix(Player player, GameSession session) {
        var em = session.getEventManager();
        if (em == null || !em.hasActiveEvent(FloodingEvent.class)) return;
        if (session.getGameMapManager() != null) session.getGameMapManager().setBlockGlowing(player.getLocation(), true);
        furnaceFixClicks++;
        player.sendActionBar(Component.text("§e关闭水阀: " + furnaceFixClicks + "/25"));
        if (furnaceFixClicks >= 25) { furnaceFixClicks = 0; for (TimedGameEvent e : new ArrayList<>(session.getActiveEvents())) if (e instanceof FloodingEvent fe) { fe.tryFixValve(1, player, session); break; } }
    }

    // --- Misc ---
    private void handleDeadReturn(Player player, GameSession session) {
        if ("return_lobby".equals(Role.getItemTag(player.getInventory().getItemInMainHand()))) {
            player.setInvisible(false); player.setCollidable(true); player.setSilent(false);
            player.setSpectatorTarget(null); player.getInventory().clear();
            var lobby = plugin.getLobbyManager(); if (lobby != null) lobby.teleportToLobby(player);
        }
    }

    private void handleDeliverymanEat(Player player, Player target, PlayerData pd, GameSession session) {
        if (pd.isOnCooldown()) { player.sendMessage(Constants.PREFIX + "§c冷却中"); return; }
        pd.setSwallowedTarget(target.getUniqueId()); pd.incrementEatUseCount();
        pd.setAbilityCooldownMillis(pd.getDeliverymanCooldown()); pd.markAbilityUsed();
        target.setInvisible(true); target.setSpectatorTarget(player);
        player.sendMessage(Constants.PREFIX + "§a吞下 " + target.getName());
        target.sendMessage(Constants.PREFIX + "§e被吞入腹中...会议时死亡");
    }

    private void handleBombPass(Player player, Player target, PlayerData pd, GameSession session) {
        // Remove bomb from passer's inventory
        for (ItemStack i : player.getInventory().getContents())
            if (i != null && "pyro_bomb".equals(Role.getItemTag(i))) player.getInventory().remove(i);
        // Clear passer flags
        pd.setHasBomb(false); pd.setBombExpireTime(0); pd.setBombVisibleToAll(false);
        // Give to target with fresh 20s timer
        PlayerData td = session.getPlayerData(target.getUniqueId());
        td.setHasBomb(true); td.setBombExpireTime(System.currentTimeMillis() + 20_000); td.setBombVisibleToAll(false);
        target.getInventory().addItem(Role.createFixedItem(Material.TNT, "§4§l定时炸弹",
                List.of("§720秒倒计时", "§7<11s他人可见烟花", "§7右键他人可传递"), "pyro_bomb"));
        player.sendMessage(Constants.PREFIX + "§c炸弹传给 " + target.getName() + "！");
        target.sendMessage(Constants.PREFIX + "§4你被塞了炸弹！20秒后爆炸！右键可传给他人");
    }

    private void handleRoleAbility(Player player, Player target, GameSession session) {
        PlayerData pd = session.getPlayerData(player.getUniqueId());
        if (pd == null) return;
        switch (pd.getRole()) {
            case DETECTIVE -> { if (!checkCd(player, pd)) return; PlayerData td = session.getPlayerData(target.getUniqueId()); player.sendMessage(Constants.PREFIX + "§a" + target.getName() + " 阵营: " + (td != null ? td.getFaction().getDisplayName() : "?")); }
            case SPICE_MASTER -> { if (!checkCd(player, pd)) return; target.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.GLOWING, 1200, 0, false, false)); player.sendMessage(Constants.PREFIX + "§a标记 " + target.getName()); }
            case MASTER_THIEF -> { if (!checkCd(player, pd)) return; player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY, 200, 0, false, false)); player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 200, 1, false, false)); player.sendMessage(Constants.PREFIX + "§a隐身+加速 10秒"); }
            case CHANGELING -> { if (!checkCd(player, pd)) return; if (pd.getRecordedTarget() == null) { pd.setRecordedTarget(target.getUniqueId()); player.sendMessage(Constants.PREFIX + "§a记录 " + target.getName()); } else { Player rec = Bukkit.getPlayer(pd.getRecordedTarget()); if (rec != null) { player.setPlayerListName(rec.getName()); player.displayName(Component.text(rec.getName())); player.sendMessage(Constants.PREFIX + "§a变装30秒"); Bukkit.getScheduler().runTaskLater(plugin, () -> { player.setPlayerListName(player.getName()); player.displayName(null); pd.setRecordedTarget(null); }, 20 * 30); } } }
            case PYROTECHNICIAN -> { if (!checkCd(player, pd)) return; if (pd.hasBomb()) { player.sendMessage(Constants.PREFIX + "§c已有炸弹"); return; } pd.setHasBomb(true); pd.setBombExpireTime(System.currentTimeMillis() + 20_000); player.getInventory().addItem(Role.createFixedItem(Material.TNT, "§4§l定时炸弹", List.of("§7右键他人传递"), "pyro_bomb")); player.sendMessage(Constants.PREFIX + "§c炸弹制造完成！"); }
            case VAGABOND -> { pd.getVagabondInteractions().add(target.getUniqueId()); player.sendMessage(Constants.PREFIX + "§a互动 " + target.getName()); }
            case LOCKSMITH -> { player.sendMessage(Constants.PREFIX + "§a潜行+右键铁门可锁定15秒"); }
        }
    }

    private boolean checkCd(Player player, PlayerData pd) { if (pd.isOnCooldown()) { player.sendMessage(Constants.PREFIX + "§c冷却中 " + String.format("%.1f", pd.getCooldownRemainingSeconds()) + "s"); return false; } if (!pd.canUseGlobal()) { player.sendMessage(Constants.PREFIX + "§c次数用完"); return false; } pd.tryUseGlobal(); pd.markAbilityUsed(); return true; }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) { GameSession s = plugin.getGameManager().getCurrentSession(); if (s == null || !s.isActive()) return; if (!event.getPlayer().hasPermission("imitatorgame.admin") && Role.isFixedItem(event.getItemDrop().getItemStack())) event.setCancelled(true); }
}
