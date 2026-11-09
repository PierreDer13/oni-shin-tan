package fr.onishintan.engine;

import fr.onishintan.model.Combatant;
import fr.onishintan.model.Minion;
import fr.onishintan.model.Player;

/** Everything an effect needs: the engine, who owns the card, the minion it comes from, and its target. */
public record EffectContext(GameEngine engine, Player owner, Minion source, Combatant target) {

    public Player enemy() {
        return engine.getState().opponentOf(owner);
    }
}