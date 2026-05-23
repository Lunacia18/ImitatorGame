package com.imitatorgame.game;

import com.imitatorgame.util.Constants;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import java.util.*;

public class DeathManager {

    private final GameSession session;
    private final Map<Location, CorpseData> corpses = new LinkedHashMap<>();

    public DeathManager(GameSession session) {
        this.session = session;
    }

    public record CorpseData(Location location, UUID victimUuid, long deathTime,
                             ArmorStand label) {}

    public void spawnCorpse(Player victim, Location deathLoc) {
        Block block = deathLoc.getBlock();
        block.setType(Material.BONE_BLOCK);

        ArmorStand label = deathLoc.getWorld().spawn(
                deathLoc.clone().add(0.5, 1.2, 0.5), ArmorStand.class, stand -> {
                    stand.setVisible(false);
                    stand.setMarker(true);
                    stand.setGravity(false);
                    stand.setCustomNameVisible(true);
                    stand.customName(net.kyori.adventure.text.Component.text(
                            "§c§l" + victim.getName() + " §7的尸体"));
                });

        Location key = block.getLocation().clone();
        corpses.put(key, new CorpseData(key, victim.getUniqueId(),
                System.currentTimeMillis(), label));

        session.broadcastMessage(Constants.PREFIX + "§c" + victim.getName() + " 倒下了...");
    }

    public UUID getVictimFromBlock(Block block) {
        Location key = block.getLocation();
        CorpseData data = corpses.get(key);
        return data != null ? data.victimUuid() : null;
    }

    public boolean isCorpseBlock(Block block) {
        return block.getType() == Material.BONE_BLOCK && getVictimFromBlock(block) != null;
    }

    public CorpseData getCorpseByVictim(UUID victimUuid) {
        return corpses.values().stream()
                .filter(c -> c.victimUuid().equals(victimUuid))
                .findFirst().orElse(null);
    }

    public void removeCorpse(UUID victimUuid) {
        CorpseData data = getCorpseByVictim(victimUuid);
        if (data != null) {
            if (data.label != null) data.label.remove();
            if (data.location.getBlock().getType() == Material.BONE_BLOCK) {
                data.location.getBlock().setType(Material.AIR);
            }
            corpses.remove(data.location);
        }
    }

    public void removeCorpseAt(Block block) {
        Location key = block.getLocation();
        CorpseData data = corpses.remove(key);
        if (data != null) {
            if (data.label != null) data.label.remove();
            if (block.getType() == Material.BONE_BLOCK) {
                block.setType(Material.AIR);
            }
        }
    }

    public void removeAllCorpses() {
        for (CorpseData data : new ArrayList<>(corpses.values())) {
            if (data.label != null) data.label.remove();
            if (data.location.getBlock().getType() == Material.BONE_BLOCK) {
                data.location.getBlock().setType(Material.AIR);
            }
        }
        corpses.clear();
    }

    public Collection<CorpseData> getAllCorpses() {
        return Collections.unmodifiableCollection(corpses.values());
    }
}
