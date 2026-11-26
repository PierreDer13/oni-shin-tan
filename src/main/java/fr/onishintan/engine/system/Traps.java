package fr.onishintan.engine.system;

import fr.onishintan.engine.GameEngine;
import fr.onishintan.engine.effect.EffectContext;
import fr.onishintan.engine.effect.Trigger;
import fr.onishintan.engine.event.GameEvent;
import fr.onishintan.model.Card;
import fr.onishintan.model.Combatant;
import fr.onishintan.model.Player;
import fr.onishintan.model.TrapCard;
import fr.onishintan.model.TrapTrigger;

import java.util.List;
import java.util.Optional;

/** The face-down Traps: set off by an enemy action, stolen (Exposed), set for free (Deception 9). */
public class Traps {

    private final GameEngine engine;

    public Traps(GameEngine engine) {
        this.engine = engine;
    }

    /** One trap per action: the first of the owner's traps waiting for it goes off, then is gone. */
    public void trigger(Player owner, TrapTrigger when, Combatant target) {
        Optional<TrapCard> trap = owner.takeTrap(when);
        if (trap.isEmpty()) return;
        engine.publish(GameEvent.of(GameEvent.Type.TRAP_TRIGGERED, owner, trap.get()));
        engine.getCardEffects().get(trap.get().getId(), Trigger.TRAP)
                .ifPresent(effect -> engine.applyAs(effect, new EffectContext(engine, owner, null, target)));
        engine.combat().removeDead();
        engine.checkGameOver();
    }

    /** Deception 9: a second Trap set for free, from the hand or the deck (not a card played, no TurnTracker count). */
    public void setExtraTrap(Player p, TrapCard trap) {
        if (!p.removeFromHand(trap)) p.getDeck().remove(trap);
        if (!p.setTrap(trap)) return;
        engine.publish(new GameEvent(GameEvent.Type.TRAP_SET, p, trap, p.trapCount()));
    }

    /** Exposed: every Trap of from, set or in hand, goes to thief (a copy he already has set, or a full hand, loses it). */
    public void stealTraps(Player from, Player thief) {
        int stolen = 0;
        for (TrapCard t : from.takeAllTraps()) {
            if (thief.setTrap(t)) stolen++;
        }
        for (Card c : List.copyOf(from.getHand())) {
            if (c instanceof TrapCard t) {
                from.removeFromHand(t);
                if (engine.addToHand(thief, t)) stolen++;
            }
        }
        engine.publish(new GameEvent(GameEvent.Type.TRAPS_STOLEN, from, thief, stolen));
    }
}
