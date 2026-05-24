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
            player.sendMessage(Constants.PREFIX + "§7等待开始。输入 §e/ig start §7或等待自动开始");
            return;
        }

        // Auto-join if game is in lobby phase
        if (session.getPhase() == GamePhase.LOBBY && session.isLobbyOpen()) {
            session.addLobbyPlayer(player.getUniqueId());
            int lobbySize = session.getLobbySize();
            int min = plugin.getConfigManager().getGameConfig().minPlayers();
            player.sendMessage(Constants.PREFIX + "§a已自动加入大厅！(§6" + lobbySize + "§a/" + min + ")");
            player.sendMessage(Constants.PREFIX + "§e输入 §6/ready §e准备！满人自动开始");
        } else if (session.isActive()) {
            // Game in progress — let them spectate
            player.sendMessage(Constants.PREFIX + "§7游戏进行中，你将以旁观者身份进入");
            player.setGameMode(org.bukkit.GameMode.SPECTATOR);
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

        if (session.isActive() && session.isAlive(player.getUniqueId())) {
            session.handleDeath(player.getUniqueId());
            session.broadcastMessage(Constants.PREFIX + "§7" + player.getName() + " 断开了连接");

            // If no alive players remain, end the game so lobby reopens
            if (session.getAliveCount() == 0) {
                plugin.getLogger().info("所有存活玩家断开，自动结束游戏");
                plugin.getGameManager().stopGame();
            }
        }
    }
}
