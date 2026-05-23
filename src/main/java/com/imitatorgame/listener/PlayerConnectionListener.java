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

        // Auto-join if game is in lobby phase
        if (session.getPhase() == GamePhase.LOBBY) {
            session.addLobbyPlayer(player.getUniqueId());
            player.sendMessage(Constants.PREFIX + "§a你已自动加入游戏大厅！(" + session.getLobbySize() + " 人)");
            player.sendMessage(Constants.PREFIX + "§7等待管理员输入 §e/ig start §7开始游戏");
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
