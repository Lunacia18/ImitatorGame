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
    private final Map<UUID, Long> taskCooldown = new HashMap<>();
    private static final long TASK_CD_MILLIS = 15_000;

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
            ItemStack held = player.getInventory().getItemInMainHand();
            String tag = Role.getItemTag(held);
            if (tag != null) {
                switch (tag) {
                    case "power_outage" -> { if (checkCd(player, pd)) session.triggerPowerOutage(); return; }
                    case "flooding" -> { if (checkCd(player, pd)) session.triggerFlooding(); return; }
                    case "imitator_knife" -> { player.sendMessage(Constants.PREFIX + "§c刀需要右键玩家使用"); return; }
                }
            }
        }

        if (clicked == null) return;

        // Bone block corpse → meeting
        if (clicked.getType() == Material.BONE_BLOCK && session.getDeathManager() != null) {
            UUID v = session.getDeathManager().getVictimFromBlock(clicked);
            if (v != null && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                session.callMeeting(player, v);
                return;
            }
        }

        boolean rightBlock = event.getAction() == Action.RIGHT_CLICK_BLOCK;
        if (rightBlock && clicked.getType() == Material.IRON_DOOR) { handleIronDoor(player, clicked, session); return; }
        if (rightBlock && clicked.getType() == Material.FURNACE) { handleFurnaceFix(player, session); return; }
        if (rightBlock && clicked.getType() == Material.CRAFTING_TABLE) { handleCraftingTableFix(player, session); return; }

        // Task: click lodestone
        if (rightBlock && clicked.getType() == Material.LODESTONE) {
            handleTaskLodestone(player, clicked, pd, session);
            return;
        }
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

        // Deliveryman eat
        if (pd.getRole() == Role.DELIVERYMAN) { handleDeliverymanEat(player, target, pd, session); return; }

        // Pyro bomb pass
        if (pd.getRole() == Role.PYROTECHNICIAN && pd.hasBomb()) {
            ItemStack held = player.getInventory().getItemInMainHand();
            if ("pyro_bomb".equals(Role.getItemTag(held))) { handleBombPass(player, target, pd, session); return; }
        }

        // Knife kill (one-hit)
        ItemStack held = player.getInventory().getItemInMainHand();
        if ("imitator_knife".equals(Role.getItemTag(held))) {
            if (!checkCd(player, pd)) return;
            session.handleDeath(target.getUniqueId());
            player.sendMessage(Constants.PREFIX + "§c一刀击杀了 " + target.getName());
            target.sendMessage(Constants.PREFIX + "§4被模仿者一刀毙命！");
            return;
        }

        handleRoleAbility(player, target, session);
    }

    // --- Cooldown ---
    private boolean checkCd(Player player, PlayerData pd) {
        if (pd.isOnCooldown()) {
            player.sendMessage(Constants.PREFIX + "§c冷却中！还需 " + String.format("%.1f", pd.getCooldownRemainingSeconds()) + " 秒");
            return false;
        }
        if (!pd.canUseGlobal()) { player.sendMessage(Constants.PREFIX + "§c技能次数已用完"); return false; }
        pd.tryUseGlobal(); pd.markAbilityUsed(); return true;
    }

    // --- Dead return ---
    private void handleDeadReturn(Player player, GameSession session) {
        if ("return_lobby".equals(Role.getItemTag(player.getInventory().getItemInMainHand()))) {
            player.setInvisible(false); player.setCollidable(true); player.setSilent(false);
            player.setSpectatorTarget(null); player.getInventory().clear();
            var lobby = plugin.getLobbyManager();
            if (lobby != null) lobby.teleportToLobby(player);
            player.sendMessage(Constants.PREFIX + "§a已返回大厅");
        }
    }

    // --- Deliveryman eat ---
    private void handleDeliverymanEat(Player player, Player target, PlayerData pd, GameSession session) {
        if (pd.getSwallowedTarget() != null) { player.sendMessage(Constants.PREFIX + "§c已吞一人"); return; }
        if (pd.isOnCooldown()) { player.sendMessage(Constants.PREFIX + "§c冷却中"); return; }
        pd.setSwallowedTarget(target.getUniqueId());
        pd.incrementEatUseCount();
        pd.setAbilityCooldownMillis(pd.getDeliverymanCooldown());
        pd.markAbilityUsed();
        player.sendMessage(Constants.PREFIX + "§a吞下 " + target.getName() + "！会议时死亡");
        target.sendMessage(Constants.PREFIX + "§e被送货员吞入腹中...会议召开时死亡");
        target.setSpectatorTarget(player);
    }

    // --- Pyro bomb pass ---
    private void handleBombPass(Player player, Player target, PlayerData pd, GameSession session) {
        pd.setHasBomb(false); pd.setBombExpireTime(0); pd.setBombVisibleToAll(false);
        for (ItemStack item : player.getInventory().getContents())
            if (item != null && "pyro_bomb".equals(Role.getItemTag(item))) player.getInventory().remove(item);
        PlayerData td = session.getPlayerData(target.getUniqueId());
        td.setHasBomb(true); td.setBombExpireTime(System.currentTimeMillis() + 20_000); td.setBombVisibleToAll(false);
        target.getInventory().addItem(Role.createFixedItem(Material.TNT, "§4§l定时炸弹",
                List.of("§720秒倒计时", "§7<11s他人可见", "§7右键他人传递"), "pyro_bomb"));
        player.sendMessage(Constants.PREFIX + "§c炸弹传给 " + target.getName());
        target.sendMessage(Constants.PREFIX + "§4被塞了炸弹！20秒后爆炸！");
    }

    // --- Task: lodestone 20 clicks ---
    private void handleTaskLodestone(Player player, Block block, PlayerData pd, GameSession session) {
        if (pd.getFaction() != Faction.DETECTIVE) return;
        Location loc = block.getLocation();
        if (pd.getCurrentTaskLodestone() != null && !pd.getCurrentTaskLodestone().equals(loc)) {
            pd.setTaskClickCount(0);
        }
        pd.setCurrentTaskLodestone(loc);
        pd.incrementTaskClickCount();
        if (pd.getTaskClickCount() >= 20) {
            Long last = taskCooldown.get(player.getUniqueId());
            if (last != null && System.currentTimeMillis() - last < TASK_CD_MILLIS) {
                player.sendMessage(Constants.PREFIX + "§c任务冷却中");
                return;
            }
            taskCooldown.put(player.getUniqueId(), System.currentTimeMillis());
            pd.setTaskClickCount(0);
            pd.setCurrentTaskLodestone(null);
            pd.incrementTaskProgress();
            session.incrementTotalTasksCompleted();
            player.sendMessage(Constants.PREFIX + "§a任务完成！(" + pd.getTaskProgress() + "/" + pd.getTotalTasks() + ")");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            if (session.getTargetTotalTasks() > 0 && session.getTotalTasksCompleted() >= session.getTargetTotalTasks()) {
                session.broadcastMessage(Constants.PREFIX + "§b所有任务完成！");
                new com.imitatorgame.game.WinConditionChecker(session).checkAllConditions();
            }
        } else {
            player.sendActionBar(Component.text("§e任务进度: " + pd.getTaskClickCount() + "/20"));
        }
    }

    // --- Furnace = power outage fix (30 clicks) ---
    private int furnaceClicks = 0;
    private void handleFurnaceFix(Player player, GameSession session) {
        var em = session.getEventManager();
        if (em == null || !em.hasActiveEvent(PowerOutageEvent.class)) return;
        furnaceClicks++;
        player.sendActionBar(Component.text("§e修复电力: " + furnaceClicks + "/30"));
        if (furnaceClicks >= 30) {
            furnaceClicks = 0;
            for (TimedGameEvent e : new ArrayList<>(session.getActiveEvents()))
                if (e instanceof PowerOutageEvent poe) { poe.fix(session); break; }
        }
    }

    // --- Crafting table = water valve fix (30 clicks) ---
    private int craftingClicks = 0;
    private void handleCraftingTableFix(Player player, GameSession session) {
        var em = session.getEventManager();
        if (em == null || !em.hasActiveEvent(FloodingEvent.class)) return;
        craftingClicks++;
        player.sendActionBar(Component.text("§e关闭水阀: " + craftingClicks + "/30"));
        if (craftingClicks >= 30) {
            craftingClicks = 0;
            for (TimedGameEvent e : new ArrayList<>(session.getActiveEvents()))
                if (e instanceof FloodingEvent fe) { fe.tryFixValve(1, player, session); break; }
        }
    }

    // --- Iron door ---
    private void handleIronDoor(Player player, Block door, GameSession session) {
        PlayerData pd = session.getPlayerData(player.getUniqueId());
        if (pd == null || (pd.getFaction() != Faction.IMITATOR && pd.getRole() != Role.LOCKSMITH)) return;
        if (door.getBlockData() instanceof org.bukkit.block.data.type.Door dd) {
            dd.setOpen(!dd.isOpen()); door.setBlockData(dd);
        }
    }

    // --- Role abilities ---
    private void handleRoleAbility(Player player, Player target, GameSession session) {
        PlayerData pd = session.getPlayerData(player.getUniqueId());
        if (pd == null) return;
        switch (pd.getRole()) {
            case DETECTIVE -> {
                if (!checkCd(player, pd)) return;
                PlayerData td = session.getPlayerData(target.getUniqueId());
                player.sendMessage(Constants.PREFIX + "§a" + target.getName() + " 阵营: " + (td != null ? td.getFaction().getDisplayName() : "?"));
            }
            case SHERIFF -> {
                if (!checkCd(player, pd)) return;
                PlayerData td = session.getPlayerData(target.getUniqueId());
                if (td != null && td.getFaction() == Faction.IMITATOR) {
                    session.handleDeath(target.getUniqueId()); player.sendMessage(Constants.PREFIX + "§a击杀了模仿者！");
                } else { session.handleDeath(player.getUniqueId()); player.sendMessage(Constants.PREFIX + "§c对方无辜！"); }
            }
            case HUNTER -> {
                if (!checkCd(player, pd)) return;
                session.handleDeath(target.getUniqueId()); player.sendMessage(Constants.PREFIX + "§a击杀了 " + target.getName());
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
                    player.sendMessage(Constants.PREFIX + "§a已记录 " + target.getName() + "，再次右键变装30秒");
                } else {
                    Player recorded = Bukkit.getPlayer(pd.getRecordedTarget());
                    if (recorded != null) {
                        player.setPlayerListName(recorded.getName());
                        player.displayName(Component.text(recorded.getName()));
                        player.sendMessage(Constants.PREFIX + "§a变装为 " + recorded.getName() + "！持续30秒");
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            player.setPlayerListName(player.getName());
                            player.displayName(null);
                            pd.setRecordedTarget(null);
                            player.sendMessage(Constants.PREFIX + "§e变装结束");
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

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        GameSession session = plugin.getGameManager().getCurrentSession();
        if (session == null || !session.isActive()) return;
        if (!event.getPlayer().hasPermission("imitatorgame.admin") && Role.isFixedItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }
}
