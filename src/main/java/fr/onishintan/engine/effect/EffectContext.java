package fr.onishintan.engine.effect;

import fr.onishintan.model.Combatant;
import fr.onishintan.model.Minion;
import fr.onishintan.model.Player;

import fr.onishintan.engine.GameEngine;
/** Everything an effect needs: the engine, who owns the card, the minion it comes from, its target and a second target (Kettō). */
public record EffectContext(GameEngine engine, Player owner, Minion source, Combatant target, Combatant second) {

    public EffectContext(GameEngine engine, Player owner, Minion source, Combatant target) {
        this(engine, owner, source, target, null);
    }

    public Player enemy() {
        return engine.getState().opponentOf(owner);
    }    
}