package com.imitatorgame.listener;

import com.imitatorgame.ImitatorGamePlugin;
import com.imitatorgame.event.FloodingEvent;
import com.imitatorgame.event.PowerOutageEvent;
import com.imitatorgame.event.TimedGameEvent;
import com.imitatorgame.game.GamePhase;
import com.imitatorgame.game.GameSession;
import com.imitatorgame.game.PlayerData;
import com.imitatorgame.role.Role;
import com.imitatorgame.task.TaskInstance;
import com.imitatorgame.util.Constants;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class PlayerInteractListener implements Listener {

    private final ImitatorGamePlugin plugin;

    public PlayerInteractListener(ImitatorGamePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        GameSession session = plugin.getGameManager().getCurrentSession();
        if (session == null || session.getPhase() != GamePhase.FREE_ACTION) return;
        if (!session.isAlive(player.getUniqueId())) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

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

    private void handleEventFix(Player player, GameSession session) {
        var eventManager = session.getEventManager();
        if (eventManager == null) return;

        var config = plugin.getConfigManager().getMapConfig();

        for (TimedGameEvent event : new java.util.ArrayList<>(session.getActiveEvents())) {
            if (event instanceof PowerOutageEvent poe) {
                poe.tryFix(player, session);
                return;
            }
            if (event instanceof FloodingEvent fe) {
                if (player.getLocation().distance(config.getFloodingFix1()) < 5) {
                    fe.tryFixValve(1, player, session);
                } else if (player.getLocation().distance(config.getFloodingFix2()) < 5) {
                    fe.tryFixValve(2, player, session);
                }
                return;
            }
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        GameSession session = plugin.getGameManager().getCurrentSession();
        if (session == null || session.getPhase() != GamePhase.FREE_ACTION) return;
        if (!session.isAlive(player.getUniqueId())) return;

        if (event.getRightClicked() instanceof ArmorStand stand) {
            String corpseUuid = stand.getPersistentDataContainer()
                    .get(Constants.CORPSE_KEY, org.bukkit.persistence.PersistentDataType.STRING);
            if (corpseUuid != null) {
                session.callMeeting(player, java.util.UUID.fromString(corpseUuid));
                stand.remove();
                return;
            }
        }

        if (event.getRightClicked() instanceof Player target) {
            if (!session.isAlive(target.getUniqueId())) return;
            handleRoleAbility(player, target, session);
        }
    }

    private void handleTaskInteraction(Player player, Block block, GameSession session) {
        var taskManager = session.getTaskManager();
        if (taskManager == null) return;

        TaskInstance task = taskManager.getTaskAt(block.getLocation());
        if (task == null) return;

        PlayerData pd = session.getPlayerData(player.getUniqueId());
        if (pd == null || pd.getFaction() != com.imitatorgame.role.Faction.DETECTIVE) return;

        if (taskManager.completeTask(player.getUniqueId(), task)) {
            player.sendMessage(Constants.PREFIX + "§a任务完成！(" + pd.getTaskProgress() + "/" + pd.getTotalTasks() + ")");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

            int total = session.getTargetTotalTasks();
            int completed = session.getTotalTasksCompleted();
            if (total > 0 && completed >= total) {
                session.broadcastMessage(Constants.PREFIX + "§b所有任务已完成！");
                new com.imitatorgame.game.WinConditionChecker(session).checkAllConditions();
            }
        }
    }

    private void handleIronDoor(Player player, Block door, GameSession session) {
        PlayerData pd = session.getPlayerData(player.getUniqueId());
        if (pd == null) return;

        boolean canOpen = pd.getFaction() == com.imitatorgame.role.Faction.IMITATOR || pd.getRole() == Role.LOCKSMITH;
        if (!canOpen) {
            player.sendMessage(Constants.PREFIX + "§c你没有权限打开秘密通道门");
            return;
        }

        if (door.getBlockData() instanceof org.bukkit.block.data.type.Door doorData) {
            doorData.setOpen(!doorData.isOpen());
            door.setBlockData(doorData);
            player.sendMessage(Constants.PREFIX + "§a秘密通道门已" + (doorData.isOpen() ? "打开" : "关闭"));
        }
    }

    private void handleRoleAbility(Player player, Player target, GameSession session) {
        PlayerData pd = session.getPlayerData(player.getUniqueId());
        if (pd == null) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        Role role = Role.fromAbilityItem(item);

        switch (pd.getRole()) {
            case DETECTIVE -> {
                player.sendMessage(Constants.PREFIX + "§a调查结果: " + target.getName() + " 的阵营是 " +
                        session.getPlayerData(target.getUniqueId()).getFaction().getDisplayName());
            }
            case SHERIFF -> {
                PlayerData targetData = session.getPlayerData(target.getUniqueId());
                if (targetData.getFaction() == com.imitatorgame.role.Faction.IMITATOR) {
                    session.handleDeath(target.getUniqueId());
                    player.sendMessage(Constants.PREFIX + "§a你成功击杀了模仿者！");
                } else {
                    session.handleDeath(player.getUniqueId());
                    player.sendMessage(Constants.PREFIX + "§c对方是无辜的！治安官自我牺牲...");
                }
            }
            case MASTER_THIEF -> {
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.INVISIBILITY,
                        200, 0, false, false));
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.SPEED,
                        200, 1, false, false));
                player.sendMessage(Constants.PREFIX + "§a隐身+加速 10秒");
            }
            case VAGABOND -> {
                pd.getVagabondInteractions().add(target.getUniqueId());
                player.sendMessage(Constants.PREFIX + "§a与 " + target.getName() + " 互动成功！(" +
                        pd.getVagabondInteractions().size() + " 次)");
            }
            case PYROTECHNICIAN -> {
                player.sendMessage(Constants.PREFIX + "§c请右键点击地面放置炸弹");
            }
            default -> {
                player.sendMessage(Constants.PREFIX + "§7你的角色能力暂时不可用");
            }
        }
    }
}
