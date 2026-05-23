package com.imitatorgame.event;

import com.imitatorgame.game.GameSession;

public interface TimedGameEvent {
    void onActivate(GameSession session);
    void onTick(GameSession session);
    void onDeactivate(GameSession session);
    boolean isExpired();
    boolean isResolved();
}
