package com.imitatorgame.command;

import com.imitatorgame.ImitatorGamePlugin;
import com.imitatorgame.game.GamePhase;
import com.imitatorgame.game.PlayerData;
import com.imitatorgame.meeting.VotingManager;
import com.imitatorgame.util.Constants;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class VoteCommand implements CommandExecutor {

    private final ImitatorGamePlugin plugin;

    public VoteCommand(ImitatorGamePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c此命令只能由玩家执行");
            return true;
        }

        var session = plugin.getGameManager().getCurrentSession();
        if (session == null || session.getPhase() != GamePhase.MEETING_VOTING) {
            player.sendMessage(Constants.PREFIX + "§c当前不在投票阶段");
            return true;
        }

        PlayerData pd = session.getPlayerData(player.getUniqueId());
        if (pd == null || !pd.isAlive()) {
            player.sendMessage(Constants.PREFIX + "§c已死亡的玩家不能投票");
            return true;
        }

        VotingManager vm = session.getVotingManager();
        if (vm == null || !vm.isVotingOpen()) {
            player.sendMessage(Constants.PREFIX + "§c投票已结束");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Constants.PREFIX + "§c用法: /vote <玩家名> 或 /vote skip");
            return true;
        }

        if (args[0].equalsIgnoreCase("skip")) {
            vm.skipVote(player.getUniqueId());
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(Constants.PREFIX + "§c找不到该玩家");
            return true;
        }

        PlayerData targetData = session.getPlayerData(target.getUniqueId());
        if (targetData == null || !targetData.isAlive()) {
            player.sendMessage(Constants.PREFIX + "§c该玩家已死亡，不能投票给他们");
            return true;
        }

        vm.castVote(player.getUniqueId(), target.getUniqueId());
        return true;
    }
}
