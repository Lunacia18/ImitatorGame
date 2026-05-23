package com.imitatorgame.game;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public class GameStateMachine {

    private GamePhase currentPhase = GamePhase.LOBBY;
    private final Map<GamePhase, Set<GamePhase>> validTransitions = new EnumMap<>(GamePhase.class);

    public GameStateMachine() {
        validTransitions.put(GamePhase.LOBBY, Set.of(GamePhase.STARTING));
        validTransitions.put(GamePhase.STARTING, Set.of(GamePhase.ROLE_REVEAL, GamePhase.LOBBY));
        validTransitions.put(GamePhase.ROLE_REVEAL, Set.of(GamePhase.FREE_ACTION));
        validTransitions.put(GamePhase.FREE_ACTION, Set.of(GamePhase.MEETING_DISCUSSION, GamePhase.GAME_OVER));
        validTransitions.put(GamePhase.MEETING_DISCUSSION, Set.of(GamePhase.MEETING_VOTING));
        validTransitions.put(GamePhase.MEETING_VOTING, Set.of(GamePhase.MEETING_RESULT));
        validTransitions.put(GamePhase.MEETING_RESULT, Set.of(GamePhase.FREE_ACTION, GamePhase.GAME_OVER));
        validTransitions.put(GamePhase.GAME_OVER, Set.of(GamePhase.LOBBY));
    }

    public GamePhase getCurrentPhase() {
        return currentPhase;
    }

    public boolean transitionTo(GamePhase target) {
        Set<GamePhase> allowed = validTransitions.get(currentPhase);
        if (allowed != null && allowed.contains(target)) {
            currentPhase = target;
            return true;
        }
        return false;
    }

    public void forcePhase(GamePhase phase) {
        currentPhase = phase;
    }

    public boolean isInGame() {
        return currentPhase != GamePhase.LOBBY && currentPhase != GamePhase.GAME_OVER;
    }

    public boolean isMeeting() {
        return currentPhase == GamePhase.MEETING_DISCUSSION
                || currentPhase == GamePhase.MEETING_VOTING
                || currentPhase == GamePhase.MEETING_RESULT;
    }
}
