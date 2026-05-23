package com.imitatorgame.role;

import com.imitatorgame.util.Constants;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public enum Role {
    NONE("无身份", "等待分配身份", Faction.NONE, Material.AIR),

    DETECTIVE("侦探", "右键玩家调查阵营", Faction.DETECTIVE, Material.COMPASS),
    SHERIFF("治安官", "右键玩家击杀，杀错自己死", Faction.DETECTIVE, Material.GOLDEN_SWORD),
    HUNTER("猎人", "右键玩家击杀，仅1次", Faction.DETECTIVE, Material.DIAMOND_SWORD),
    SENTRY("哨兵", "死亡时自动召开紧急会议", Faction.DETECTIVE, Material.BELL),
    LOCKSMITH("锁匠", "右键铁门开关秘密通道", Faction.DETECTIVE, Material.STRING),
    SPICE_MASTER("香料师", "右键玩家标记追踪", Faction.DETECTIVE, Material.BLAZE_POWDER),

    MASTER_THIEF("神偷", "隐身+加速接近目标", Faction.IMITATOR, Material.POTION),
    CHANGELING("千面人", "伪装成其他玩家外观", Faction.IMITATOR, Material.PLAYER_HEAD),
    CONSPIRATOR("阴谋家", "会议中猜身份，猜对目标死猜错自己死", Faction.IMITATOR, Material.WRITABLE_BOOK),
    PYROTECHNICIAN("烟火师", "放置定时炸弹", Faction.IMITATOR, Material.TNT),

    FOOL("愚人", "被投票淘汰则个人胜利", Faction.MYSTERY_GUEST, Material.AIR),
    VAGABOND("流浪汉", "与足够多玩家互动获胜", Faction.MYSTERY_GUEST, Material.PAPER),
    DELIVERYMAN("送货员", "向所有玩家投递包裹获胜", Faction.MYSTERY_GUEST, Material.NAME_TAG);

    private final String displayName;
    private final String description;
    private final Faction faction;
    private final Material abilityMaterial;

    Role(String displayName, String description, Faction faction, Material abilityMaterial) {
        this.displayName = displayName;
        this.description = description;
        this.faction = faction;
        this.abilityMaterial = abilityMaterial;
    }

    public String getDisplayName() { return faction.getColorCode() + displayName; }
    public String getRawName() { return displayName; }
    public String getDescription() { return description; }
    public Faction getFaction() { return faction; }
    public Material getAbilityMaterial() { return abilityMaterial; }

    public boolean isDetective() { return faction == Faction.DETECTIVE; }
    public boolean isImitator() { return faction == Faction.IMITATOR; }
    public boolean isMysteryGuest() { return faction == Faction.MYSTERY_GUEST; }

    public void giveAbilityItems(Player player) {
        if (this == NONE || this == FOOL || this == SENTRY) return;
        ItemStack item = new ItemStack(abilityMaterial);
        item.editMeta(meta -> {
            meta.displayName(net.kyori.adventure.text.Component.text(getDisplayName()));
            meta.lore(List.of(net.kyori.adventure.text.Component.text("§7" + description)));
            meta.getPersistentDataContainer().set(
                    Constants.ABILITY_KEY,
                    org.bukkit.persistence.PersistentDataType.STRING,
                    this.name()
            );
        });
        player.getInventory().addItem(item);
    }

    public static Role fromAbilityItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return NONE;
        String roleName = item.getItemMeta().getPersistentDataContainer()
                .get(Constants.ABILITY_KEY, org.bukkit.persistence.PersistentDataType.STRING);
        if (roleName == null) return NONE;
        try {
            return Role.valueOf(roleName);
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
