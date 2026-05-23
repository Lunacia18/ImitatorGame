package com.imitatorgame.listener;

import com.imitatorgame.ImitatorGamePlugin;
import com.imitatorgame.game.GamePhase;
import com.imitatorgame.game.GameSession;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public class VoteGuiListener implements Listener {

    private final ImitatorGamePlugin plugin;

    public VoteGuiListener(ImitatorGamePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        GameSession session = plugin.getGameManager().getCurrentSession();
        if (session == null) return;

        // Only handle during voting phase
        if (session.getPhase() != GamePhase.MEETING_VOTING) return;

        Inventory inv = event.getInventory();
        if (session.getMeetingManager() == null) return;

        event.setCancelled(true); // prevent moving items

        if (event.getCurrentItem() == null) return;

        session.getMeetingManager().handleVoteClick(player, event.getSlot(), inv);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        GameSession session = plugin.getGameManager().getCurrentSession();
        if (session == null) return;
        if (session.getPhase() == GamePhase.MEETING_VOTING) {
            event.setCancelled(true);
        }
    }
}
