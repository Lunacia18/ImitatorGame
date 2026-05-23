package com.imitatorgame.ui;

import com.imitatorgame.util.Constants;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class MessageManager {

    private MessageManager() {
    }

    public static void gameStart() {
        Bukkit.broadcast(Component.text(Constants.PREFIX + "游戏开始！").color(TextColor.color(0x55FF55)));
    }

    public static void phaseChange(String phase) {
        Bukkit.broadcast(Component.text(Constants.PREFIX + "阶段: " + phase).color(TextColor.color(0xFFFF55)));
    }

    public static void roleReveal(Player player, String role, String faction, String description) {
        player.sendMessage(Constants.PREFIX + "§a你的身份是: " + role);
        player.sendMessage(Constants.PREFIX + "§7阵营: " + faction);
        player.sendMessage(Constants.PREFIX + "§7能力: " + description);
    }

    public static void meetingCalled(String reporter, String victim) {
        Bukkit.broadcast(Component.text(Constants.PREFIX + "紧急会议！")
                .color(TextColor.color(0xFF5555)));
    }

    public static void voteResult(String result) {
        Bukkit.broadcast(Component.text(Constants.PREFIX + result).color(TextColor.color(0xFFAA00)));
    }

    public static void playerEliminated(String name) {
        Bukkit.broadcast(Component.text(Constants.PREFIX + name + " 被淘汰了！")
                .color(TextColor.color(0xFF5555)));
    }

    public static void gameOver(String winner) {
        Bukkit.broadcast(Component.text(Constants.PREFIX + winner + " 获胜！")
                .color(TextColor.color(0xFFD700)));
    }

    public static void send(Player player, String msg) {
        player.sendMessage(Constants.PREFIX + msg);
    }

    public static void broadcast(String msg) {
        Bukkit.broadcast(Component.text(Constants.PREFIX + msg));
    }
}
