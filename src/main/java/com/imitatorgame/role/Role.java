package com.imitatorgame.role;

import com.imitatorgame.util.Constants;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public enum Role {
    NONE("无身份", "等待分配身份", Faction.NONE, Material.AIR, false),

    DETECTIVE("侦探", "右键玩家调查阵营", Faction.DETECTIVE, Material.COMPASS, false),
    SHERIFF("治安官", "右键玩家击杀，杀错自己死", Faction.DETECTIVE, Material.GOLDEN_SWORD, false),
    HUNTER("猎人", "右键玩家击杀，仅1次", Faction.DETECTIVE, Material.DIAMOND_SWORD, false),
    SENTRY("哨兵", "死亡时自动召开紧急会议", Faction.DETECTIVE, Material.BELL, false),
    LOCKSMITH("锁匠", "右键铁门开关秘密通道", Faction.DETECTIVE, Material.STRING, false),
    SPICE_MASTER("香料师", "右键玩家标记追踪", Faction.DETECTIVE, Material.BLAZE_POWDER, false),

    MASTER_THIEF("神偷", "隐身+加速 | 信标=停电 | 水桶=水淹", Faction.IMITATOR, Material.POTION, true),
    CHANGELING("千面人", "伪装成其他玩家 | 信标=停电 | 水桶=水淹", Faction.IMITATOR, Material.PLAYER_HEAD, true),
    CONSPIRATOR("阴谋家", "会议猜身份 | 信标=停电 | 水桶=水淹", Faction.IMITATOR, Material.WRITABLE_BOOK, true),
    PYROTECHNICIAN("烟火师", "放置炸弹 | 信标=停电 | 水桶=水淹", Faction.IMITATOR, Material.TNT, true),

    FOOL("愚人", "被投票淘汰则个人胜利", Faction.MYSTERY_GUEST, Material.AIR, false),
    VAGABOND("流浪汉", "与足够多玩家互动获胜", Faction.MYSTERY_GUEST, Material.PAPER, false),
    DELIVERYMAN("送货员", "向所有玩家投递包裹获胜", Faction.MYSTERY_GUEST, Material.NAME_TAG, false);

    private static final NamespacedKey ITEM_TAG = new NamespacedKey("imitatorgame", "fixed_item");
    private static final NamespacedKey EVENT_TYPE = new NamespacedKey("imitatorgame", "event_type");

    private final String displayName;
    private final String description;
    private final Faction faction;
    private final Material abilityMaterial;
    private final boolean hasEventItems;

    Role(String displayName, String description, Faction faction, Material abilityMaterial,
         boolean hasEventItems) {
        this.displayName = displayName;
        this.description = description;
        this.faction = faction;
        this.abilityMaterial = abilityMaterial;
        this.hasEventItems = hasEventItems;
    }

    public String getDisplayName() { return faction.getColorCode() + displayName; }
    public String getRawName() { return displayName; }
    public String getDescription() { return description; }
    public Faction getFaction() { return faction; }
    public Material getAbilityMaterial() { return abilityMaterial; }

    public boolean isDetective() { return faction == Faction.DETECTIVE; }
    public boolean isImitator() { return faction == Faction.IMITATOR; }
    public boolean isMysteryGuest() { return faction == Faction.MYSTERY_GUEST; }
    public boolean hasEventItems() { return hasEventItems; }

    public void giveAbilityItems(Player player) {
        // Main ability item
        if (this != NONE && this != FOOL && this != SENTRY) {
            ItemStack item = createFixedItem(abilityMaterial, getDisplayName(),
                    List.of("§7" + description), this.name());
            player.getInventory().addItem(item);
        }

        // Imitator event items (beacon = power outage, water bucket = flooding)
        if (hasEventItems) {
            ItemStack beacon = createFixedItem(Material.BEACON, "§8§l停电装置",
                    List.of("§7右键触发停电事件", "§7使非模仿者致盲"), "power_outage");
            player.getInventory().addItem(beacon);

            ItemStack bucket = createFixedItem(Material.WATER_BUCKET, "§1§l水淹装置",
                    List.of("§7右键触发水淹事件", "§775秒倒计时，到期模仿者胜"), "flooding");
            player.getInventory().addItem(bucket);
        }
    }

    public static ItemStack createFixedItem(Material material, String name, List<String> lore, String tag) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.displayName(Component.text(name));
            meta.lore(lore.stream().map(Component::text).toList());
            meta.getPersistentDataContainer().set(ITEM_TAG, PersistentDataType.STRING, tag);
            meta.getPersistentDataContainer().set(Constants.ABILITY_KEY, PersistentDataType.STRING, tag);
        });
        return item;
    }

    public static boolean isFixedItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(ITEM_TAG, PersistentDataType.STRING);
    }

    public static String getItemTag(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(ITEM_TAG, PersistentDataType.STRING);
    }

    public static Role fromAbilityItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return NONE;
        String roleName = item.getItemMeta().getPersistentDataContainer()
                .get(Constants.ABILITY_KEY, org.bukkit.persistence.PersistentDataType.STRING);
        if (roleName == null) return NONE;
        try {
            return Role.valueOf(roleName);
        } catch (IllegalArgumentException e) {
            // Check if it's an event item (power_outage, flooding)
            return NONE;
        }
    }

    /**
     * Give dead player their spectator beacon for returning to lobby
     */
    public static void giveDeathBeacon(Player player) {
        ItemStack beacon = createFixedItem(Material.BEACON, "§a§l回到大厅",
                List.of("§7右键点击返回虚空大厅"), "return_lobby");
        player.getInventory().setItem(0, beacon);
    }
}
