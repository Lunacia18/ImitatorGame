package com.imitatorgame.command;

import com.imitatorgame.ImitatorGamePlugin;
import com.imitatorgame.game.GamePhase;
import com.imitatorgame.util.Constants;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ImitatorGameCommand implements CommandExecutor, TabCompleter {

    private final ImitatorGamePlugin plugin;

    public ImitatorGameCommand(ImitatorGamePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "join" -> handleJoin(sender);
            case "leave" -> handleLeave(sender);
            case "start" -> handleStart(sender);
            case "stop" -> handleStop(sender);
            case "list" -> handleList(sender);
            case "reload" -> handleReload(sender);
            case "setspawn" -> handleSetSpawn(sender, args);
            case "addtask" -> handleAddTask(sender, args);
            case "adddoor" -> handleAddDoor(sender);
            default -> { sendHelp(sender); yield true; }
        };
    }

    private void sendHelp(CommandSender sender) {
        sender.sendRichMessage("<gold>=== 模仿者游戏帮助 ===</gold>");
        sender.sendRichMessage("<yellow>/ig join</yellow> <gray>- 加入游戏大厅</gray>");
        sender.sendRichMessage("<yellow>/ig leave</yellow> <gray>- 离开游戏大厅</gray>");
        sender.sendRichMessage("<yellow>/ig start</yellow> <gray>- 手动开始游戏</gray>");
        sender.sendRichMessage("<yellow>/ig stop</yellow> <gray>- 强制结束游戏</gray>");
        sender.sendRichMessage("<yellow>/ig list</yellow> <gray>- 查看玩家列表</gray>");
        sender.sendRichMessage("<yellow>/ig reload</yellow> <gray>- 重载配置</gray>");
        sender.sendRichMessage("<yellow>/ig setspawn <lobby|meeting|game></yellow> <gray>- 设置生成点</gray>");
        sender.sendRichMessage("<yellow>/ig addtask <type> <room></yellow> <gray>- 添加任务位置</gray>");
        sender.sendRichMessage("<yellow>/ig adddoor</yellow> <gray>- 添加秘密通道门</gray>");
    }

    private boolean handleJoin(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c此命令只能由玩家执行");
            return true;
        }
        var session = plugin.getGameManager().getCurrentSession();
        if (session == null) {
            player.sendMessage(Constants.PREFIX + "§c当前没有游戏会话，请先使用 /ig start 开始一局");
            return true;
        }
        if (session.isActive()) {
            player.sendMessage(Constants.PREFIX + "§c游戏已在进行中，无法加入");
            return true;
        }
        session.addLobbyPlayer(player.getUniqueId());
        player.sendMessage(Constants.PREFIX + "§a你已加入游戏大厅！(" + session.getLobbySize() + " 人)");
        return true;
    }

    private boolean handleLeave(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c此命令只能由玩家执行");
            return true;
        }
        var session = plugin.getGameManager().getCurrentSession();
        if (session == null) {
            player.sendMessage(Constants.PREFIX + "§c你不在游戏大厅中");
            return true;
        }
        session.removeLobbyPlayer(player.getUniqueId());
        player.sendMessage(Constants.PREFIX + "§a你已离开游戏大厅");
        return true;
    }

    private boolean handleStart(CommandSender sender) {
        if (!sender.hasPermission("imitatorgame.admin")) {
            sender.sendMessage("§c你没有权限");
            return true;
        }
        var session = plugin.getGameManager().getCurrentSession();
        if (session != null && session.isActive()) {
            sender.sendMessage(Constants.PREFIX + "§c游戏已在运行中");
            return true;
        }
        sender.sendMessage(Constants.PREFIX + "§a正在创建新游戏...");
        if (!plugin.getGameManager().startNewGame()) {
            sender.sendMessage(Constants.PREFIX + "§c无法开始游戏，玩家不足");
        }
        return true;
    }

    private boolean handleStop(CommandSender sender) {
        if (!sender.hasPermission("imitatorgame.admin")) {
            sender.sendMessage("§c你没有权限");
            return true;
        }
        if (plugin.getGameManager().stopGame()) {
            sender.sendMessage(Constants.PREFIX + "§a游戏已强制结束");
        } else {
            sender.sendMessage(Constants.PREFIX + "§c没有正在运行的游戏");
        }
        return true;
    }

    private boolean handleList(CommandSender sender) {
        var session = plugin.getGameManager().getCurrentSession();
        if (session == null) {
            sender.sendMessage(Constants.PREFIX + "§7当前没有游戏会话");
            return true;
        }
        if (session.isActive()) {
            sender.sendMessage(Constants.PREFIX + "§6存活玩家: " + session.getAliveCount() + " | 死亡: " + session.getDeadPlayers().size());
        } else {
            sender.sendMessage(Constants.PREFIX + "§e大厅玩家(" + session.getLobbySize() + "人):");
            for (var uuid : session.getLobbyPlayers()) {
                var p = org.bukkit.Bukkit.getPlayer(uuid);
                if (p != null) sender.sendMessage("§7- " + p.getName());
            }
        }
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("imitatorgame.admin")) {
            sender.sendMessage("§c你没有权限");
            return true;
        }
        plugin.getConfigManager().reload();
        sender.sendMessage(Constants.PREFIX + "§a配置已重载");
        return true;
    }

    private boolean handleSetSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c此命令只能由玩家执行");
            return true;
        }
        if (!sender.hasPermission("imitatorgame.admin")) {
            sender.sendMessage("§c你没有权限");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("§c用法: /ig setspawn <lobby|meeting|game> [index]");
            return true;
        }
        var config = plugin.getConfigManager().getConfig();
        var loc = player.getLocation();
        return switch (args[1].toLowerCase()) {
            case "lobby" -> {
                config.set("map.lobby-spawn", serializeLoc(loc));
                plugin.getConfigManager().saveConfig();
                player.sendMessage(Constants.PREFIX + "§a大厅生成点已设置");
                yield true;
            }
            case "meeting" -> {
                config.set("map.meeting-room", serializeLoc(loc));
                plugin.getConfigManager().saveConfig();
                player.sendMessage(Constants.PREFIX + "§a会议室已设置");
                yield true;
            }
            case "game" -> {
                int idx = args.length > 2 ? Integer.parseInt(args[2]) : 0;
                List<Object> spawns = (List<Object>) config.getList("map.game-spawns", new ArrayList<>());
                while (spawns.size() <= idx) spawns.add(serializeLoc(loc));
                spawns.set(idx, serializeLoc(loc));
                config.set("map.game-spawns", spawns);
                plugin.getConfigManager().saveConfig();
                player.sendMessage(Constants.PREFIX + "§a游戏生成点 #" + idx + " 已设置");
                yield true;
            }
            default -> {
                player.sendMessage("§c无效类型");
                yield true;
            }
        };
    }

    private boolean handleAddTask(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c此命令只能由玩家执行");
            return true;
        }
        if (!sender.hasPermission("imitatorgame.admin")) return true;
        if (args.length < 2) {
            sender.sendMessage("§c用法: /ig addtask <PRESS_BUTTON|FLIP_LEVER|STEP_ON_PLATE|INTERACT_BLOCK> [room]");
            return true;
        }
        String type = args[1].toUpperCase();
        String room = args.length > 2 ? args[2] : "";
        var config = plugin.getConfigManager().getConfig();
        List<Map<String, Object>> tasks = (List<Map<String, Object>>) config.getList("map.tasks", new ArrayList<>());
        Map<String, Object> entry = new java.util.LinkedHashMap<>();
        entry.put("location", serializeLoc(player.getLocation()));
        entry.put("type", type);
        entry.put("target-block", "STONE_BUTTON");
        entry.put("room", room);
        tasks.add(entry);
        config.set("map.tasks", tasks);
        plugin.getConfigManager().saveConfig();
        player.sendMessage(Constants.PREFIX + "§a任务位置 #" + (tasks.size() - 1) + " 已添加");
        return true;
    }

    @SuppressWarnings("unchecked")
    private boolean handleAddDoor(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c此命令只能由玩家执行");
            return true;
        }
        if (!sender.hasPermission("imitatorgame.admin")) return true;
        var config = plugin.getConfigManager().getConfig();
        List<Map<String, Object>> doors = (List<Map<String, Object>>) config.getList("map.iron-doors", new ArrayList<>());
        doors.add(serializeLoc(player.getLocation()));
        config.set("map.iron-doors", doors);
        plugin.getConfigManager().saveConfig();
        player.sendMessage(Constants.PREFIX + "§a秘密通道门已添加");
        return true;
    }

    private Map<String, Object> serializeLoc(org.bukkit.Location loc) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("world", loc.getWorld().getName());
        map.put("x", loc.getX());
        map.put("y", loc.getY());
        map.put("z", loc.getZ());
        map.put("yaw", (double) loc.getYaw());
        map.put("pitch", (double) loc.getPitch());
        return map;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("join", "leave", "start", "stop", "list", "reload", "setspawn", "addtask", "adddoor");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("setspawn")) {
            return List.of("lobby", "meeting", "game");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("addtask")) {
            return List.of("PRESS_BUTTON", "FLIP_LEVER", "STEP_ON_PLATE", "INTERACT_BLOCK");
        }
        return List.of();
    }
}
