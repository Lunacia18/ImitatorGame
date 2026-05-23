package com.imitatorgame.listener;

import com.imitatorgame.ImitatorGamePlugin;
import com.imitatorgame.game.GamePhase;
import com.imitatorgame.game.GameSession;
import com.imitatorgame.util.Constants;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerRespawnListener implements Listener {

    private final ImitatorGamePlugin plugin;

    public PlayerRespawnListener(ImitatorGamePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        GameSession session = plugin.getGameManager().getCurrentSession();
        if (session == null) return;

        GamePhase phase = session.getPhase();

        // During active game, dead players stay in the game world as spectators
        if (session.isActive() && session.isDead(player.getUniqueId())) {
            event.setRespawnLocation(player.getLocation()); // stay where they died
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.setGameMode(GameMode.SPECTATOR);
                player.sendMessage(Constants.PREFIX + "§7你已死亡，正在旁观游戏");
            });
            return;
        }

        // If in lobby or game over, respawn at lobby platform
        if (phase == GamePhase.LOBBY || phase == GamePhase.GAME_OVER) {
            var lobby = plugin.getLobbyManager();
            if (lobby != null && lobby.getSpawnPoint() != null) {
                event.setRespawnLocation(lobby.getSpawnPoint());
            }
        }
    }
}
