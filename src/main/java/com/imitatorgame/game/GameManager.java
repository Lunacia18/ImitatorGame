package com.imitatorgame.game;

import com.imitatorgame.ImitatorGamePlugin;

import java.util.UUID;

public class GameManager {

    private final ImitatorGamePlugin plugin;
    private GameSession currentSession;

    public GameManager(ImitatorGamePlugin plugin) {
        this.plugin = plugin;
    }

    public GameSession getCurrentSession() {
        return currentSession;
    }

    public boolean isGameActive() {
        return currentSession != null && currentSession.isActive();
    }

    public boolean startNewGame() {
        if (isGameActive()) return false;
        currentSession = new GameSession(plugin);
        return currentSession.start();
    }

    public boolean stopGame() {
        if (currentSession == null) return false;
        currentSession.endGame();
        currentSession = null;
        return true;
    }

    public PlayerData getPlayerData(UUID uuid) {
        if (currentSession == null) return null;
        return currentSession.getPlayerData(uuid);
    }

    public GamePhase getCurrentPhase() {
        if (currentSession == null) return GamePhase.LOBBY;
        return currentSession.getPhase();
    }

    public void cleanup() {
        if (currentSession != null) {
            currentSession.endGame();
            currentSession = null;
        }
    }
}
