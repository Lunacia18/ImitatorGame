package com.imitatorgame.listener;

import com.imitatorgame.ImitatorGamePlugin;
import com.imitatorgame.game.GameSession;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockProtectListener implements Listener {

    private final ImitatorGamePlugin plugin;

    public BlockProtectListener(ImitatorGamePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("imitatorgame.admin")) return;
        GameSession session = plugin.getGameManager().getCurrentSession();
        if (session != null && session.isActive()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("imitatorgame.admin")) return;
        GameSession session = plugin.getGameManager().getCurrentSession();
        if (session != null && session.isActive()) {
            event.setCancelled(true);
        }
    }
}
