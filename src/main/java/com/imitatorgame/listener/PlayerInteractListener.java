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
import com.imitatorgame.task.TaskInstance;
import com.imitatorgame.util.Constants;
import org.bukkit.Material;
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

import java.util.UUID;

public class PlayerInteractListener implements Listener {

    private final ImitatorGamePlugin plugin;

    public PlayerInteractListener(ImitatorGamePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        GameSession session = plugin.getGameManager().getCurrentSession();
        if (session == null) return;

        Block clicked = event.getClickedBlock();

        // Dead player: right-click air/block with return beacon
        if (session.isActive() && session.isDead(player.getUniqueId())) {
            event.setCancelled(true);
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                handleDeadReturn(player, session);
            }
            return;
        }

        // Alive players only in FREE_ACTION
        if (session.getPhase() != GamePhase.FREE_ACTION) return;
        if (!session.isAlive(player.getUniqueId())) return;

        PlayerData pd = session.getPlayerData(player.getUniqueId());
        if (pd == null) return;

        // Event item triggers (right-click air or block)
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack held = player.getInventory().getItemInMainHand();
            String tag = Role.getItemTag(held);
            if (tag != null) {
                switch (tag) {
                    case "power_outage" -> {
                        if (!checkCooldown(player, pd, session)) return;
                        session.triggerPowerOutage();
                        return;
                    }
                    case "flooding" -> {
                        if (!checkCooldown(player, pd, session)) return;
                        session.triggerFlooding();
                        return;
                    }
                    case "imitator_knife" -> {
                        player.sendMessage(Constants.PREFIX + "§c刀需要右键玩家使用");
                        return;
                    }
                }
            }
        }

        if (clicked == null) return;

        // Bone block corpse
        if (clicked.getType() == Material.BONE_BLOCK && session.getDeathManager() != null) {
            UUID victimUuid = session.getDeathManager().getVictimFromBlock(clicked);
            if (victimUuid != null && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                session.callMeeting(player, victimUuid);
                session.getDeathManager().removeCorpse(victimUuid);
                // Revert coal blocks after meeting
                if (session.getGameMapManager() != null) {
                    session.getGameMapManager().revertCoalBlocks();
                }
                return;
            }
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (clicked.getType() == Material.IRON_DOOR) {
                handleIronDoor(player, clicked, session);
                return;
            }
            handleEventFix(player, session);
            handleTaskInteraction(player, clicked, session);
        }
        if (event.getAction() == Action.PHYSICAL && clicked.getType() == Material.STONE_PRESSURE_PLATE) {
            handleTaskInteraction(player, clicked, session);
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        GameSession session = plugin.getGameManager().getCurrentSession();
        if (session == null) return;

        // Dead player
        if (session.isActive() && session.isDead(player.getUniqueId())) {
            event.setCancelled(true);
            handleDeadReturn(player, session);
            return;
        }

        if (session.getPhase() != GamePhase.FREE_ACTION) return;
        if (!session.isAlive(player.getUniqueId())) return;
        if (!(event.getRightClicked() instanceof Player target)) return;
        if (!session.isAlive(target.getUniqueId())) return;

        PlayerData pd = session.getPlayerData(player.getUniqueId());
        if (pd == null) return;

        // Deliveryman: right-click player to swallow them
        if (pd.getRole() == Role.DELIVERYMAN) {
            handleDeliverymanEat(player, target, pd, session);
            return;
        }

        // Pyrotechnician: right-click with bomb to pass it
        if (pd.getRole() == Role.PYROTECHNICIAN) {
            ItemStack held = player.getInventory().getItemInMainHand();
            String tag = Role.getItemTag(held);
            if ("pyro_bomb".equals(tag) && pd.hasBomb()) {
                handleBombPass(player, target, pd, session);
                return;
            }
        }

        // Pyro: right-click air to create bomb (handled in onInteract with pyro ability item)
        // Imitator knife: right-click player to kill
        ItemStack held = player.getInventory().getItemInMainHand();
        String tag = Role.getItemTag(held);

        if ("imitator_knife".equals(tag)) {
            if (!checkCooldown(player, pd, session)) return;
            session.handleDeath(target.getUniqueId());
            player.sendMessage(Constants.PREFIX + "§c你击杀了 " + target.getName());
            target.sendMessage(Constants.PREFIX + "§4你被模仿者击杀了！");
            return;
        }

        handleRoleAbility(player, target, session);
    }

    // --- Cooldown check ---

    private boolean checkCooldown(Player player, PlayerData pd, GameSession session) {
        if (pd.isOnCooldown()) {
            double sec = pd.getCooldownRemainingSeconds();
            player.sendMessage(Constants.PREFIX + "§c冷却中！还需 " + String.format("%.1f", sec) + " 秒");
            return false;
        }
        if (!pd.canUseGlobal()) {
            player.sendMessage(Constants.PREFIX + "§c你的技能使用次数已用完");
            return false;
        }
        pd.tryUseGlobal();
        pd.markAbilityUsed();
        return true;
    }

    // --- Deliveryman eat ---

    private void handleDeliverymanEat(Player player, Player target, PlayerData pd, GameSession session) {
        if (pd.getSwallowedTarget() != null) {
            player.sendMessage(Constants.PREFIX + "§c你已经吞下了一个人！");
            return;
        }
        pd.setSwallowedTarget(target.getUniqueId());
        player.sendMessage(Constants.PREFIX + "§a你吞下了 " + target.getName() + "！会议时才会死亡");
        target.sendMessage(Constants.PREFIX + "§e你被送货员吞入腹中...会议召开时才会死亡");

        // Target's POV becomes the deliveryman's
        target.setSpectatorTarget(player);
    }

    // --- Pyro bomb ---

    private void handleBombPass(Player player, Player target, PlayerData pd, GameSession session) {
        if (!pd.hasBomb()) {
            player.sendMessage(Constants.PREFIX + "§c你没有炸弹可传递");
            return;
        }
        // Pass the bomb
        pd.setHasBomb(false);
        pd.setBombExpireTime(0);
        pd.setBombVisibleToAll(false);
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && "pyro_bomb".equals(Role.getItemTag(item))) {
                player.getInventory().remove(item);
            }
        }

        PlayerData targetData = session.getPlayerData(target.getUniqueId());
        targetData.setHasBomb(true);
        targetData.setBombExpireTime(System.currentTimeMillis() + 20_000); // 20s
        targetData.setBombVisibleToAll(false);

        ItemStack bomb = Role.createFixedItem(Material.TNT, "§4§l定时炸弹",
                java.util.List.of("§7倒计时20秒", "§7剩余11秒时他人可见", "§7右键他人可传递"), "pyro_bomb");
        target.getInventory().addItem(bomb);

        player.sendMessage(Constants.PREFIX + "§c炸弹已传给 " + target.getName());
        target.sendMessage(Constants.PREFIX + "§4你被塞了一个炸弹！20秒后爆炸！");
    }

    // --- Dead return beacon ---

    private void handleDeadReturn(Player player, GameSession session) {
        ItemStack held = player.getInventory().getItemInMainHand();
        String tag = Role.getItemTag(held);
        if ("return_lobby".equals(tag)) {
            player.setInvisible(false);
            player.setCollidable(true);
            player.setSilent(false);
            player.setSpectatorTarget(null);
            player.getInventory().clear();
            var lobby = plugin.getLobbyManager();
            if (lobby != null) lobby.teleportToLobby(player);
            player.sendMessage(Constants.PREFIX + "§a你已返回大厅");
        }
    }

    // --- Task interaction ---

    private void handleTaskInteraction(Player player, Block block, GameSession session) {
        var tm = session.getTaskManager();
        if (tm == null) return;
        TaskInstance task = tm.getTaskAt(block.getLocation());
        if (task == null) return;
        PlayerData pd = session.getPlayerData(player.getUniqueId());
        if (pd == null || pd.getFaction() != Faction.DETECTIVE) return;
        if (tm.completeTask(player.getUniqueId(), task)) {
            player.sendMessage(Constants.PREFIX + "§a任务完成！(" + pd.getTaskProgress() + "/" + pd.getTotalTasks() + ")");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            if (session.getTargetTotalTasks() > 0 && session.getTotalTasksCompleted() >= session.getTargetTotalTasks()) {
                session.broadcastMessage(Constants.PREFIX + "§b所有任务已完成！");
                new com.imitatorgame.game.WinConditionChecker(session).checkAllConditions();
            }
        }
    }

    // --- Iron door ---

    private void handleIronDoor(Player player, Block door, GameSession session) {
        PlayerData pd = session.getPlayerData(player.getUniqueId());
        if (pd == null) return;
        if (pd.getFaction() != Faction.IMITATOR && pd.getRole() != Role.LOCKSMITH) {
            player.sendMessage(Constants.PREFIX + "§c你没有权限打开秘密通道门");
            return;
        }
        if (door.getBlockData() instanceof org.bukkit.block.data.type.Door doorData) {
            doorData.setOpen(!doorData.isOpen());
            door.setBlockData(doorData);
        }
    }

    // --- Event fix ---

    private void handleEventFix(Player player, GameSession session) {
        var em = session.getEventManager();
        if (em == null) return;
        var cfg = plugin.getConfigManager().getMapConfig();
        for (TimedGameEvent e : new java.util.ArrayList<>(session.getActiveEvents())) {
            if (e instanceof PowerOutageEvent poe) { poe.tryFix(player, session); return; }
            if (e instanceof FloodingEvent fe) {
                if (player.getLocation().distance(cfg.getFloodingFix1()) < 5) fe.tryFixValve(1, player, session);
                else if (player.getLocation().distance(cfg.getFloodingFix2()) < 5) fe.tryFixValve(2, player, session);
                return;
            }
        }
    }

    // --- Role abilities ---

    private void handleRoleAbility(Player player, Player target, GameSession session) {
        PlayerData pd = session.getPlayerData(player.getUniqueId());
        if (pd == null) return;

        switch (pd.getRole()) {
            case DETECTIVE -> {
                if (!checkCooldown(player, pd, session)) return;
                PlayerData td = session.getPlayerData(target.getUniqueId());
                player.sendMessage(Constants.PREFIX + "§a" + target.getName() + " 的阵营: "
                        + (td != null ? td.getFaction().getDisplayName() : "§7未知"));
            }
            case SHERIFF -> {
                if (!checkCooldown(player, pd, session)) return;
                PlayerData td = session.getPlayerData(target.getUniqueId());
                if (td != null && td.getFaction() == Faction.IMITATOR) {
                    session.handleDeath(target.getUniqueId());
                    player.sendMessage(Constants.PREFIX + "§a击杀了模仿者！");
                } else {
                    session.handleDeath(player.getUniqueId());
                    player.sendMessage(Constants.PREFIX + "§c对方无辜！治安官自我牺牲...");
                }
            }
            case HUNTER -> {
                if (!checkCooldown(player, pd, session)) return;
                session.handleDeath(target.getUniqueId());
                player.sendMessage(Constants.PREFIX + "§a击杀了 " + target.getName());
            }
            case SPICE_MASTER -> {
                if (!checkCooldown(player, pd, session)) return;
                target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.GLOWING, 1200, 0, false, false));
                player.sendMessage(Constants.PREFIX + "§a已标记 " + target.getName());
            }
            case MASTER_THIEF -> {
                if (!checkCooldown(player, pd, session)) return;
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.INVISIBILITY, 200, 0, false, false));
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.SPEED, 200, 1, false, false));
                player.sendMessage(Constants.PREFIX + "§a隐身+加速 10秒");
            }
            case CHANGELING -> {
                if (!checkCooldown(player, pd, session)) return;
                player.sendMessage(Constants.PREFIX + "§a伪装为 " + target.getName());
            }
            case PYROTECHNICIAN -> {
                if (!checkCooldown(player, pd, session)) return;
                // Create a bomb item
                if (pd.hasBomb()) {
                    player.sendMessage(Constants.PREFIX + "§c你已有一个炸弹！");
                    return;
                }
                pd.setHasBomb(true);
                pd.setBombExpireTime(System.currentTimeMillis() + 20_000);
                pd.setBombVisibleToAll(false);
                ItemStack bomb = Role.createFixedItem(Material.TNT, "§4§l定时炸弹",
                        java.util.List.of("§7倒计时20秒", "§7右键他人可传递"), "pyro_bomb");
                player.getInventory().addItem(bomb);
                player.sendMessage(Constants.PREFIX + "§c炸弹已制造！20秒后爆炸，右键他人可传递");
            }
            case VAGABOND -> {
                pd.getVagabondInteractions().add(target.getUniqueId());
                player.sendMessage(Constants.PREFIX + "§a互动 " + target.getName() + " ("
                        + pd.getVagabondInteractions().size() + " 次)");
            }
            case DELIVERYMAN -> {
                // Already handled above
            }
            default -> player.sendMessage(Constants.PREFIX + "§7能力暂不可用");
        }
    }

    // --- Prevent dropping fixed items ---

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        GameSession session = plugin.getGameManager().getCurrentSession();
        if (session == null || !session.isActive()) return;
        if (!player.hasPermission("imitatorgame.admin") && Role.isFixedItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            player.sendMessage(Constants.PREFIX + "§c此物品无法丢弃");
        }
    }
}
