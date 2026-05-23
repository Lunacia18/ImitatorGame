package com.imitatorgame.command;

import com.imitatorgame.ImitatorGamePlugin;
import com.imitatorgame.game.GamePhase;
import com.imitatorgame.util.Constants;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RadioCommand implements CommandExecutor {

    private final ImitatorGamePlugin plugin;

    public RadioCommand(ImitatorGamePlugin plugin) {
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
            player.sendMessage(Constants.PREFIX + "§c当前无法使用电台");
            return true;
        }

        var pd = session.getPlayerData(player.getUniqueId());
        if (pd == null || !pd.isAlive()) {
            player.sendMessage(Constants.PREFIX + "§c已死亡的玩家不能使用电台");
            return true;
        }

        if (pd.hasUsedRadio()) {
            player.sendMessage(Constants.PREFIX + "§c你已使用过电台，每局游戏只能使用一次");
            return true;
        }

        Location radioLoc = plugin.getConfigManager().getMapConfig().getRadioLocation();
        if (radioLoc != null && player.getLocation().distance(radioLoc) > 5) {
            player.sendMessage(Constants.PREFIX + "§c你离电台太远，请靠近餐厅的电台");
            return true;
        }

        pd.setUsedRadio(true);
        session.callMeeting(player, null);
        return true;
    }
}
