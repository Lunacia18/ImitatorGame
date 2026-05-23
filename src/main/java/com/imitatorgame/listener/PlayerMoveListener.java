package com.imitatorgame.listener;

import com.imitatorgame.ImitatorGamePlugin;
import com.imitatorgame.game.GamePhase;
import com.imitatorgame.game.GameSession;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerMoveListener implements Listener {

    private final ImitatorGamePlugin plugin;

    public PlayerMoveListener(ImitatorGamePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        GameSession session = plugin.getGameManager().getCurrentSession();
        if (session == null || !session.isActive()) return;

        GamePhase phase = session.getPhase();
        if (phase == GamePhase.MEETING_DISCUSSION || phase == GamePhase.MEETING_VOTING
                || phase == GamePhase.MEETING_RESULT) {
            // Only freeze alive players - dead spectators can move freely
            if (!session.isAlive(player.getUniqueId())) return;
            Location from = event.getFrom();
            Location to = event.getTo();
            if (to != null && (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ())) {
                event.setCancelled(true);
            }
        }
    }
}
