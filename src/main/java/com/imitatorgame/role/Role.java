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
    NONE("无身份", "等待分配身份", Faction.NONE, Material.AIR, 0, -1, false, false),

    DETECTIVE("侦探", "右键调查阵营", Faction.DETECTIVE, Material.COMPASS, 24_000, 2, false, false),
    SHERIFF("治安官", "右键击杀，杀错自己死", Faction.DETECTIVE, Material.GOLDEN_SWORD, 24_000, -1, false, false),
    HUNTER("猎人", "右键击杀", Faction.DETECTIVE, Material.DIAMOND_SWORD, 24_000, 1, false, false),
    SENTRY("哨兵", "被击杀时强制拉起尸体开会", Faction.DETECTIVE, Material.AIR, 0, -1, false, false),
    LOCKSMITH("锁匠", "开关秘密通道铁门", Faction.DETECTIVE, Material.STRING, 0, -1, false, false),
    SPICE_MASTER("香料师", "右键标记追踪60秒", Faction.DETECTIVE, Material.BLAZE_POWDER, 24_000, -1, false, false),

    MASTER_THIEF("神偷", "隐身+加速 | 信标=停电 | 水桶=水淹", Faction.IMITATOR, Material.POTION, 24_000, -1, true, true),
    CHANGELING("千面人", "伪装外观 | 信标=停电 | 水桶=水淹", Faction.IMITATOR, Material.PLAYER_HEAD, 24_000, -1, true, true),
    CONSPIRATOR("阴谋家", "会议猜身份 | 信标=停电 | 水桶=水淹", Faction.IMITATOR, Material.WRITABLE_BOOK, 24_000, -1, true, true),
    PYROTECHNICIAN("烟火师", "造炸弹(10s CD)→塞给别人→倒计时→煤炭块 | 信标=停电 | 水桶=水淹",
            Faction.IMITATOR, Material.TNT, 24_000, -1, true, false), // no knife

    FOOL("愚人", "被投票淘汰则个人胜利", Faction.MYSTERY_GUEST, Material.AIR, 0, -1, false, false),
    VAGABOND("流浪汉", "与足够多玩家互动获胜", Faction.MYSTERY_GUEST, Material.PAPER, 0, -1, false, false),
    DELIVERYMAN("送货员", "右键将玩家吞入腹中，开会时才死亡", Faction.MYSTERY_GUEST, Material.NAME_TAG, 0, -1, false, false);

    private static final NamespacedKey ITEM_TAG = new NamespacedKey("imitatorgame", "fixed_item");

    private final String displayName;
    private final String description;
    private final Faction faction;
    private final Material abilityMaterial;
    private final long cooldownMillis;
    private final int globalUses; // -1 = unlimited
    private final boolean hasEventItems;
    private final boolean hasKnife;

    Role(String displayName, String description, Faction faction, Material abilityMaterial,
         long cooldownMillis, int globalUses, boolean hasEventItems, boolean hasKnife) {
        this.displayName = displayName;
        this.description = description;
        this.faction = faction;
        this.abilityMaterial = abilityMaterial;
        this.cooldownMillis = cooldownMillis;
        this.globalUses = globalUses;
        this.hasEventItems = hasEventItems;
        this.hasKnife = hasKnife;
    }

    public String getDisplayName() { return faction.getColorCode() + displayName; }
    public String getRawName() { return displayName; }
    public String getDescription() { return description; }
    public Faction getFaction() { return faction; }
    public Material getAbilityMaterial() { return abilityMaterial; }
    public long getCooldownMillis() { return cooldownMillis; }
    public int getGlobalUses() { return globalUses; }
    public boolean hasEventItems() { return hasEventItems; }
    public boolean hasKnife() { return hasKnife; }

    public boolean isDetective() { return faction == Faction.DETECTIVE; }
    public boolean isImitator() { return faction == Faction.IMITATOR; }
    public boolean isMysteryGuest() { return faction == Faction.MYSTERY_GUEST; }

    public void giveAbilityItems(Player player) {
        // Main ability item (skip for NONE, FOOL, SENTRY which are passive)
        if (this != NONE && this != FOOL && this != SENTRY) {
            ItemStack item = createFixedItem(abilityMaterial, getDisplayName(),
                    List.of("§7" + description, "§8CD: " + (cooldownMillis / 1000) + "s"), this.name());
            player.getInventory().addItem(item);
        }

        // Imitator event items
        if (hasEventItems) {
            player.getInventory().addItem(createFixedItem(Material.BEACON, "§8§l停电装置",
                    List.of("§7右键触发停电事件"), "power_outage"));
            player.getInventory().addItem(createFixedItem(Material.WATER_BUCKET, "§1§l水淹装置",
                    List.of("§7右键触发水淹事件"), "flooding"));
        }

        // Imitator knife (iron sword, one-hit kill)
        if (hasKnife) {
            player.getInventory().addItem(createFixedItem(Material.IRON_SWORD, "§c§l模仿者之刃",
                    List.of("§7一击毙命", "§8CD: 24s"), "imitator_knife"));
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
        String tag = getItemTag(item);
        if (tag == null) return NONE;
        try { return Role.valueOf(tag); } catch (IllegalArgumentException e) { return NONE; }
    }

    public static void giveDeathBeacon(Player player) {
        ItemStack beacon = createFixedItem(Material.BEACON, "§a§l回到大厅",
                List.of("§7右键返回虚空大厅"), "return_lobby");
        player.getInventory().setItem(0, beacon);
    }
}
