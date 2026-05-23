package com.imitatorgame.event;

import com.imitatorgame.game.GameSession;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TimedEventManager {

    private final GameSession session;
    private final List<TimedGameEvent> activeEvents = new ArrayList<>();
    private BukkitTask tickTask;

    public TimedEventManager(GameSession session) {
        this.session = session;
    }

    public void startTicking() {
        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                List<TimedGameEvent> toRemove = new ArrayList<>();
                for (TimedGameEvent event : activeEvents) {
                    event.onTick(session);
                    if (event.isExpired() || event.isResolved()) {
                        toRemove.add(event);
                    }
                }
                activeEvents.removeAll(toRemove);
            }
        }.runTaskTimer(session.getPlugin(), 0, 1);
    }

    public void addEvent(TimedGameEvent event) {
        event.onActivate(session);
        activeEvents.add(event);
    }

    public void deactivateEvent(TimedGameEvent event) {
        event.onDeactivate(session);
        activeEvents.remove(event);
    }

    public void stopAll() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        for (TimedGameEvent event : new ArrayList<>(activeEvents)) {
            event.onDeactivate(session);
        }
        activeEvents.clear();
    }

    public boolean hasActiveEvent(Class<?> type) {
        return activeEvents.stream().anyMatch(type::isInstance);
    }

    public List<TimedGameEvent> getActiveEvents() {
        return Collections.unmodifiableList(activeEvents);
    }
}
