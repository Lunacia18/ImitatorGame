package com.imitatorgame.role;

import java.util.*;

public class AbilityRegistry {

    private static final Map<Role, List<RoleAbility>> registry = new EnumMap<>(Role.class);

    static {
        for (Role role : Role.values()) {
            registry.put(role, new ArrayList<>());
        }
    }

    public static void register(Role role, RoleAbility ability) {
        registry.computeIfAbsent(role, k -> new ArrayList<>()).add(ability);
    }

    public static List<RoleAbility> get(Role role) {
        return Collections.unmodifiableList(registry.getOrDefault(role, List.of()));
    }

    public static boolean hasAbility(Role role) {
        return !registry.getOrDefault(role, List.of()).isEmpty();
    }
}
