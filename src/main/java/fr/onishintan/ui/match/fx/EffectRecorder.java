package fr.onishintan.ui.match.fx;

import fr.onishintan.engine.GameEngine;
import fr.onishintan.engine.event.GameEvent;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Notes what an engine action did that the table can show (damage, healing, deaths, a trap going off),
 * so the board can replay it on the cards still on screen before it redraws.
 */
public final class EffectRecorder {
    private static final Set<GameEvent.Type> SHOWN = EnumSet.of(
            GameEvent.Type.DAMAGE, GameEvent.Type.HEAL, GameEvent.Type.MINION_DIED, GameEvent.Type.MINION_REMOVED,
            GameEvent.Type.TRAP_TRIGGERED, GameEvent.Type.CARD_ADDED, GameEvent.Type.ATTACK,
            GameEvent.Type.EFFECT_TRIGGERED, GameEvent.Type.FATIGUE, GameEvent.Type.SILENCED, GameEvent.Type.CARD_PLAYED);

    private final List<GameEvent> events = new ArrayList<>();
    private boolean recording;

    public EffectRecorder(GameEngine engine) {
        // addListener gives each event before what it causes (a Seppuku's death before its draw), with the rules already applied.
        engine.addListener((event, state) -> {
            if (recording && SHOWN.contains(event.type())) {
                events.add(event);
            }
        });
    }

    /** Starts noting events (just before calling the engine). */
    public void start() {
        events.clear();
        recording = true;
    }

    /** Stops noting and gives what was noted, in order. */
    public List<GameEvent> stop() {
        recording = false;
        return new ArrayList<>(events);
    }

    /** True between start() and stop(): the replay will show what happens now. */
    public boolean recording() {
        return recording;
    }
}
