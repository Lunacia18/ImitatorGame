package com.imitatorgame.listener;

import com.imitatorgame.ImitatorGamePlugin;
import com.imitatorgame.game.GameSession;
import com.imitatorgame.map.LobbyManager;
import com.imitatorgame.util.Constants;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class DimensionGuardListener implements Listener {

    private final ImitatorGamePlugin plugin;

    public DimensionGuardListener(ImitatorGamePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        GameSession session = plugin.getGameManager().getCurrentSession();
        LobbyManager lobby = plugin.getLobbyManager();

        // Always cancel portal travel during any game-related state
        if (lobby != null && lobby.isInLobbyWorld(player)) {
            event.setCancelled(true);
            player.sendMessage(Constants.PREFIX + "§c大厅世界不能使用传送门");
            return;
        }

        if (session != null && session.isActive() && session.isAlive(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(Constants.PREFIX + "§c游戏进行中不能前往其他维度");
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        GameSession session = plugin.getGameManager().getCurrentSession();
        LobbyManager lobby = plugin.getLobbyManager();

        // Block dimension-changing teleports
        World from = event.getFrom().getWorld();
        World to = event.getTo() != null ? event.getTo().getWorld() : null;
        if (to == null || from.equals(to)) return; // same world, allow

        PlayerTeleportEvent.TeleportCause cause = event.getCause();

        // Allow plugin-initiated teleports
        if (cause == PlayerTeleportEvent.TeleportCause.PLUGIN
                || cause == PlayerTeleportEvent.TeleportCause.COMMAND
                || cause == PlayerTeleportEvent.TeleportCause.UNKNOWN) {
            return;
        }

        // Block spectators from leaving the game world
        if (player.getGameMode() == GameMode.SPECTATOR && session != null && session.isActive()) {
            // Allow spectators to move within the game world
            if (session.getPlugin().getConfigManager().getMapConfig().getGameSpawns().stream()
                    .anyMatch(loc -> loc.getWorld() != null && loc.getWorld().equals(to))) {
                return;
            }
            if (lobby != null && lobby.getLobbyWorld() != null && to.equals(lobby.getLobbyWorld())) {
                return;
            }
        }

        // Block dimension change for lobby players
        if (lobby != null && lobby.getLobbyWorld() != null && from.equals(lobby.getLobbyWorld())) {
            event.setCancelled(true);
            player.sendMessage(Constants.PREFIX + "§c大厅中不能前往其他维度");
            return;
        }

        // Block dimension change for game players
        if (session != null && session.isActive()) {
            event.setCancelled(true);
            player.sendMessage(Constants.PREFIX + "§c游戏进行中不能前往其他维度");
        }
    }
}
