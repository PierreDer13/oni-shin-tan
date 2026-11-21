package fr.onishintan.engine.system;

import fr.onishintan.engine.GameEngine;
import fr.onishintan.engine.GameState;
import fr.onishintan.engine.effect.EffectContext;
import fr.onishintan.engine.effect.Trigger;
import fr.onishintan.engine.event.GameEvent;
import fr.onishintan.model.Card;
import fr.onishintan.model.CurseCard;
import fr.onishintan.model.CurseZone;
import fr.onishintan.model.Player;

import java.util.List;

/** The Oni's Curses: given to the hand or shuffled into the deck, fired at the end of the holder's turn or when drawn. */
public class Curses {

    private final GameEngine engine;
    /** The holder of the hand Curse acting right now (fireCurse), null otherwise. */
    private Player cursed;

    public Curses(GameEngine engine) {
        this.engine = engine;
    }

    private GameState state() {
        return engine.getState();
    }

    /** The holder of the Curse acting right now, null when no Curse is acting. */
    public Player cursed() {
        return cursed;
    }

    public void giveCurse(Player from, Player to, CurseCard curse) {
        if (curse.getZone() == CurseZone.HAND) {
            if (!to.receiveCurse(curse)) return;          // hand full: the Curse is lost
        } else {
            for (int i = 0; i < curse.getCopies(); i++) to.getDeck().insertRandom(curse, engine.rng());
        }
        engine.publish(new GameEvent(GameEvent.Type.CURSE_GIVEN, from, curse, curse.getCopies()));
    }

    /** The active player pays the removal cost to get rid of a Curse in his hand. Refused = nothing paid. */
    public boolean removeCurse(CurseCard curse) {
        Player p = state().active();
        if (state().isOver() || !p.isHeldCurse(curse) || !p.canAfford(curse.getRemovalCost())) return false;
        p.spendKi(curse.getRemovalCost());
        p.removeCurse(curse);
        engine.publish(GameEvent.of(GameEvent.Type.CURSE_REMOVED, p, curse));
        return true;
    }

    /** End of the holder's turn (and Corruption 9 at every turn start and end): every hand Curse he holds acts, left to right. */
    public void runHandCurses(Player holder) {
        for (Card c : List.copyOf(holder.getHand())) {
            if (c instanceof CurseCard curse && holder.isHeldCurse(curse)) fireCurse(holder, curse);
            if (state().isOver()) return;
        }
    }

    /** A deck Curse acts as soon as it is drawn, then is gone. */
    public void onCurseDrawn(Player p, Card card) {
        if (!(card instanceof CurseCard curse) || curse.getZone() != CurseZone.DECK) return;
        p.removeFromHand(curse);
        fireCurse(p, curse);
    }

    private void fireCurse(Player victim, CurseCard curse) {
        engine.publish(GameEvent.of(GameEvent.Type.CURSE_TRIGGERED, victim, curse));
        Player before = cursed;
        cursed = victim;                      // its damage is the cursing player's (Corruption 3)
        engine.getCardEffects().get(curse.getId(), Trigger.CURSE)
                .ifPresent(effect -> effect.apply(new EffectContext(engine, victim, null, null)));
        cursed = before;
        engine.combat().removeDead();
        engine.checkGameOver();
    }

    /** The deck Curses of the catalog, sorted by id (Oiwa picks one). */
    public List<CurseCard> deckCurses() {
        return engine.catalog().values().stream().filter(c -> c instanceof CurseCard cc && cc.getZone() == CurseZone.DECK)
                .map(c -> (CurseCard) c).sorted(java.util.Comparator.comparing(Card::getId)).toList();
    }

    /** Oiwa: one copy of a deck Curse shuffled into to's deck. */
    public void giveDeckCurse(Player from, Player to, CurseCard curse) {
        to.getDeck().insertRandom(curse, engine.rng());
        engine.publish(new GameEvent(GameEvent.Type.CURSE_GIVEN, from, curse, 1));
    }

    /** Exorcism: every Curse the holder was given is thrown away for free. */
    public void purgeCurses(Player holder) {
        for (Card c : List.copyOf(holder.getHand())) {
            if (c instanceof CurseCard curse && holder.isHeldCurse(curse)) {
                holder.removeCurse(curse);
                engine.publish(GameEvent.of(GameEvent.Type.CURSE_REMOVED, holder, curse));
            }
        }
    }
}
