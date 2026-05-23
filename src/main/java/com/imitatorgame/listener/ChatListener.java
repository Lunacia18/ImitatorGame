package com.imitatorgame.listener;

import com.imitatorgame.ImitatorGamePlugin;
import com.imitatorgame.game.GamePhase;
import com.imitatorgame.game.GameSession;
import com.imitatorgame.game.PlayerData;
import com.imitatorgame.role.Faction;
import com.imitatorgame.util.Constants;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {

    private final ImitatorGamePlugin plugin;

    public ChatListener(ImitatorGamePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        GameSession session = plugin.getGameManager().getCurrentSession();
        if (session == null || !session.isActive()) return;

        GamePhase phase = session.getPhase();
        boolean isMeeting = phase == GamePhase.MEETING_DISCUSSION
                || phase == GamePhase.MEETING_VOTING
                || phase == GamePhase.MEETING_RESULT;

        if (!isMeeting) {
            PlayerData pd = session.getPlayerData(player.getUniqueId());
            if (pd == null) return;
            if (pd.isAlive()) {
                event.setCancelled(true);
                player.sendMessage(Constants.PREFIX + "§c你只能在会议阶段发言");
            } else {
                event.setCancelled(true);
                for (var deadUuid : session.getDeadPlayers()) {
                    var deadPlayer = org.bukkit.Bukkit.getPlayer(deadUuid);
                    if (deadPlayer != null) {
                        deadPlayer.sendMessage("§7[死亡] " + player.getName() + ": §7" +
                                net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                                        .plainText().serialize(event.message()));
                    }
                }
            }
            return;
        }

        PlayerData pd = session.getPlayerData(player.getUniqueId());
        if (pd == null || !pd.isAlive()) {
            event.setCancelled(true);
            return;
        }

        event.renderer((source, sourceDisplayName, message, viewer) -> {
            PlayerData sourceData = session.getPlayerData(source.getUniqueId());
            String factionColor = sourceData != null ? sourceData.getFaction().getColorCode() : "§7";
            return Component.text(factionColor + source.getName() + "§7: ")
                    .append(message);
        });
    }
}
