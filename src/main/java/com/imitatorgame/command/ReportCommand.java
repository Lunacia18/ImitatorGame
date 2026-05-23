package com.imitatorgame.command;

import com.imitatorgame.ImitatorGamePlugin;
import com.imitatorgame.game.GamePhase;
import com.imitatorgame.util.Constants;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReportCommand implements CommandExecutor {

    private final ImitatorGamePlugin plugin;

    public ReportCommand(ImitatorGamePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c此命令只能由玩家执行");
            return true;
        }

        var session = plugin.getGameManager().getCurrentSession();
        if (session == null || session.getPhase() != GamePhase.FREE_ACTION) {
            player.sendMessage(Constants.PREFIX + "§c当前无法报告尸体");
            return true;
        }

        var pd = session.getPlayerData(player.getUniqueId());
        if (pd == null || !pd.isAlive()) {
            player.sendMessage(Constants.PREFIX + "§c已死亡的玩家不能报告尸体");
            return true;
        }

        player.sendMessage(Constants.PREFIX + "§c附近没有可报告的尸体。请靠近尸体后使用此命令，或右键点击尸体。");
        return true;
    }
}
