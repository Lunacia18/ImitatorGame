package com.imitatorgame.role;

import com.imitatorgame.config.GameConfig;

import java.util.*;

public class RoleAssignment {

    private final GameConfig config;

    public RoleAssignment(GameConfig config) {
        this.config = config;
    }

    private static final Role[] DETECTIVE_ROLES = {
            Role.DETECTIVE, Role.SHERIFF, Role.HUNTER,
            Role.SENTRY, Role.LOCKSMITH, Role.SPICE_MASTER
    };

    private static final Role[] IMITATOR_ROLES = {
            Role.MASTER_THIEF, Role.CHANGELING, Role.CONSPIRATOR, Role.PYROTECHNICIAN
    };

    private static final Role[] MYSTERY_ROLES = {
            Role.FOOL, Role.VAGABOND, Role.DELIVERYMAN
    };

    public Map<UUID, Role> assign(List<UUID> players) {
        Map<UUID, Role> result = new LinkedHashMap<>();
        List<UUID> shuffled = new ArrayList<>(players);
        Collections.shuffle(shuffled, new Random());

        int detCount = config.detectiveCount();
        int imiCount = config.imitatorCount();
        int mysCount = config.mysteryGuestCount();

        List<Role> detRoles = new ArrayList<>(Arrays.asList(DETECTIVE_ROLES));
        Collections.shuffle(detRoles);
        List<Role> imiRoles = new ArrayList<>(Arrays.asList(IMITATOR_ROLES));
        Collections.shuffle(imiRoles);
        List<Role> mysRoles = new ArrayList<>(Arrays.asList(MYSTERY_ROLES));
        Collections.shuffle(mysRoles);

        int idx = 0;
        for (int i = 0; i < detCount && idx < shuffled.size(); i++) {
            result.put(shuffled.get(idx), detRoles.get(i % detRoles.size()));
            idx++;
        }
        for (int i = 0; i < imiCount && idx < shuffled.size(); i++) {
            result.put(shuffled.get(idx), imiRoles.get(i % imiRoles.size()));
            idx++;
        }
        for (int i = 0; i < mysCount && idx < shuffled.size(); i++) {
            result.put(shuffled.get(idx), mysRoles.get(i % mysRoles.size()));
            idx++;
        }
        for (; idx < shuffled.size(); idx++) {
            result.put(shuffled.get(idx), Role.DETECTIVE);
        }

        return result;
    }
}
