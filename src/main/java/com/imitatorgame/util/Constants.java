package com.imitatorgame.util;

import org.bukkit.NamespacedKey;

public final class Constants {

    public static final String PLUGIN_NAME = "ImitatorGame";
    public static final String PREFIX = "§8[§6模仿者游戏§8]§r ";

    public static final NamespacedKey ABILITY_KEY = new NamespacedKey("imitatorgame", "ability");
    public static final NamespacedKey CORPSE_KEY = new NamespacedKey("imitatorgame", "corpse_uuid");

    public static final int TICKS_PER_SECOND = 20;
    public static final int BODY_DESPAWN_TICKS = 1200;

    public static final String SCOREBOARD_TITLE = "§6§l模仿者游戏";
    public static final String LOBBY_TITLE = "§6§l游戏大厅";

    private Constants() {
    }
}
