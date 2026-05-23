package com.imitatorgame.command;

import com.imitatorgame.ImitatorGamePlugin;
import com.imitatorgame.game.GamePhase;
import com.imitatorgame.game.GameSession;
import com.imitatorgame.util.Constants;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReadyCommand implements CommandExecutor {

    private final ImitatorGamePlugin plugin;

    public ReadyCommand(ImitatorGamePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("此命令只能由玩家执行");
            return true;
        }

        GameSession session = plugin.getGameManager().getCurrentSession();
        if (session == null) {
            player.sendMessage(Constants.PREFIX + "§c没有活跃的游戏会话");
            return true;
        }

        if (session.getPhase() != GamePhase.LOBBY) {
            player.sendMessage(Constants.PREFIX + "§c游戏已在进行中");
            return true;
        }

        if (!session.isInLobby(player.getUniqueId())) {
            player.sendMessage(Constants.PREFIX + "§c你不在游戏大厅中");
            return true;
        }

        if (session.isReady(player.getUniqueId())) {
            player.sendMessage(Constants.PREFIX + "§e你已经准备过了！");
            return true;
        }

        if (session.setReady(player.getUniqueId())) {
            int ready = session.getReadyCount();
            int total = session.getLobbySize();
            int min = plugin.getConfigManager().getGameConfig().minPlayers();

            session.broadcastToAll(Constants.PREFIX + "§a" + player.getName()
                    + " §e已准备！(§6" + ready + "§e/§6" + total + "§e)");

            if (ready >= min) {
                session.broadcastToAll(Constants.PREFIX + "§6准备人数达标！游戏即将开始...");
            }
        }

        return true;
    }
}
