package fr.onishintan.engine.event;

import fr.onishintan.engine.GameState;

/** Implemented by the UI (MatchController) to react to the engine. */
@FunctionalInterface
public interface GameListener {
    void onEvent(GameEvent event, GameState state);
}