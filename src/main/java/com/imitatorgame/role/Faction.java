package com.imitatorgame.role;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public enum Faction {
    DETECTIVE("侦探团", "§b"),
    IMITATOR("模仿者", "§c"),
    MYSTERY_GUEST("神秘客", "§e"),
    NONE("无", "§7");

    private final String displayName;
    private final String colorCode;

    Faction(String displayName, String colorCode) {
        this.displayName = displayName;
        this.colorCode = colorCode;
    }

    public String getDisplayName() { return colorCode + displayName; }
    public String getColorCode() { return colorCode; }
    public String getRawName() { return displayName; }

    public Component toComponent() {
        return Component.text(displayName).color(TextColor.fromHexString(getHexColor()));
    }

    private String getHexColor() {
        return switch (this) {
            case DETECTIVE -> "#55FFFF";
            case IMITATOR -> "#FF5555";
            case MYSTERY_GUEST -> "#FFFF55";
            default -> "#AAAAAA";
        };
    }
}
