package com.imitatorgame.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownTracker {

    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public boolean tryUse(UUID playerId, long cooldownMillis) {
        long now = System.currentTimeMillis();
        Long unlockTime = cooldowns.get(playerId);
        if (unlockTime != null && now < unlockTime) {
            return false;
        }
        cooldowns.put(playerId, now + cooldownMillis);
        return true;
    }

    public long getRemainingMillis(UUID playerId) {
        Long unlockTime = cooldowns.get(playerId);
        if (unlockTime == null) return 0;
        long remaining = unlockTime - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    public double getRemainingSeconds(UUID playerId) {
        return getRemainingMillis(playerId) / 1000.0;
    }

    public void reset(UUID playerId) {
        cooldowns.remove(playerId);
    }

    public void clear() {
        cooldowns.clear();
    }
}
