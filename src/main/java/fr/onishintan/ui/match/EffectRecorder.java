package fr.onishintan.ui.match;

import fr.onishintan.engine.EventBus;
import fr.onishintan.engine.GameEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Notes what an engine action did that the table can show (damage, healing, deaths, a trap going off),
 * so the board can replay it on the cards still on screen before it redraws.
 */
final class EffectRecorder {
    private static final GameEvent.Type[] SHOWN = {
            GameEvent.Type.DAMAGE, GameEvent.Type.HEAL, GameEvent.Type.MINION_DIED, GameEvent.Type.TRAP_TRIGGERED, GameEvent.Type.CARD_ADDED };

    private final List<GameEvent> events = new ArrayList<>();
    private boolean recording;

    EffectRecorder(EventBus bus) {
        for (GameEvent.Type type : SHOWN) {
            bus.subscribe(type, event -> {
                if (recording) {
                    events.add(event);
                }
            });
        }
    }

    /** Starts noting events (just before calling the engine). */
    void start() {
        events.clear();
        recording = true;
    }

    /** Stops noting and gives what was noted, in order. */
    List<GameEvent> stop() {
        recording = false;
        return new ArrayList<>(events);
    }
}
