package fr.onishintan.engine.oath;

import fr.onishintan.model.Player;
import fr.onishintan.engine.GameState;
import fr.onishintan.engine.turn.TurnTracker;

/** Checks one Oath at the end of its owner's turn (GDD, Chosen vs unable). */
@FunctionalInterface
public interface OathRule {

    enum Verdict {
        KEPT,      // +1 stack
        UNABLE,    // no legal way to keep it: no stack, the Oath holds
        BROKEN     // broken on purpose
    }

    Verdict check(Player owner, TurnTracker turn, GameState state);
}