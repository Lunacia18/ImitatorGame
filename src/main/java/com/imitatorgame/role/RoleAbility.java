package com.imitatorgame.role;

import com.imitatorgame.game.GameSession;
import com.imitatorgame.game.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public interface RoleAbility {

    default boolean onInteract(Player player, PlayerData data, PlayerInteractEvent event) {
        return false;
    }

    default boolean onInteractEntity(Player player, PlayerData data, Player target, PlayerInteractEntityEvent event) {
        return false;
    }

    default void onMeetingStart(Player player, PlayerData data) {
    }

    default void onMeetingEnd(Player player, PlayerData data) {
    }

    default long getCooldownMillis() {
        return 0;
    }
}
