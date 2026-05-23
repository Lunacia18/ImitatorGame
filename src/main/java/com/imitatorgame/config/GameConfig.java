package com.imitatorgame.config;

public record GameConfig(
        int minPlayers,
        int maxPlayers,
        int detectiveCount,
        int imitatorCount,
        int mysteryGuestCount,
        int lobbyWaitSeconds,
        int roleRevealSeconds,
        int discussionSeconds,
        int votingSeconds,
        int resultSeconds,
        int powerOutageSeconds,
        int floodingSeconds,
        int hunterMaxUses,
        int conspiratorMaxGuesses,
        int masterThiefInvisibilitySeconds,
        int masterThiefCooldownSeconds,
        int disguiseDurationSeconds,
        int disguiseCooldownSeconds,
        int spyTrackerDurationSeconds,
        int spyTrackerCooldownSeconds,
        int vagabondMinInteractions,
        int deliverymanTargets,
        int bombFuseSeconds,
        int bombRadius,
        int imitatorEventCooldownSeconds,
        int tasksPerPlayer
) {

    public static GameConfig defaults() {
        return new GameConfig(
                10, 12, 6, 2, 2,
                60, 5, 30, 20, 10, 60, 75,
                1, 2, 10, 30, 30, 45, 60, 20,
                5, 10, 5, 3, 90, 5
        );
    }
}
