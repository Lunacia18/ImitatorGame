package com.imitatorgame.listener;

import com.imitatorgame.ImitatorGamePlugin;
import com.imitatorgame.game.GamePhase;
import com.imitatorgame.game.GameSession;
import com.imitatorgame.util.Constants;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {

    private final ImitatorGamePlugin plugin;

    public PlayerConnectionListener(ImitatorGamePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        var lobby = plugin.getLobbyManager();

        // Teleport to void lobby immediately
        if (lobby != null) {
            lobby.teleportToLobby(player);
        }

        var session = plugin.getGameManager().getCurrentSession();
        if (session == null) {
            player.sendMessage(Constants.PREFIX + "§7服务器正在等待管理员开始游戏。输入 §e/ig start §7开始！");
            return;
        }

        if (session.getPhase() == GamePhase.LOBBY) {
            player.sendMessage(Constants.PREFIX + "§e游戏大厅已开放！输入 §6/ig join §e加入");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        var player = event.getPlayer();
        var session = plugin.getGameManager().getCurrentSession();
        if (session == null) return;

        if (!session.isActive() && session.isInLobby(player.getUniqueId())) {
            session.removeLobbyPlayer(player.getUniqueId());
        }

        if (session.isActive()) {
            session.handleDeath(player.getUniqueId());
            session.broadcastMessage(Constants.PREFIX + "§7" + player.getName() + " 断开了连接");
        }
    }
}
