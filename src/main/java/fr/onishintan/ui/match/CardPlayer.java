package fr.onishintan.ui.match;

import fr.onishintan.model.Card;
import fr.onishintan.model.Combatant;

import java.util.List;

/** What the match tells the board about playing a card from the hand: its legal targets, how many to pick, and the play itself. */
public interface CardPlayer {
    /** The characters this card may be aimed at now; empty when it is played without a target. */
    List<Combatant> targets(Card card);

    /** How many targets to pick when targets() is not empty: 2 for Kettō, otherwise 1. */
    int picks(Card card);

    /** Plays the card on that slot with those targets (null when not needed). True if the engine accepted it. */
    boolean play(Card card, int slot, Combatant first, Combatant second);

    /** What this hand card costs now: its printed Ki minus Yōi and the Oath discounts. */
    int cost(Card card);

    /** True if the engine would accept this card now (Ki, a legal target, a weapon for Iaidō, room on the board…). */
    boolean canPlay(Card card);

    /** What to add to this hand card's damage or healing number now (Ryū, Oaths, Chikara), shown in green. */
    int bonus(Card card);

    /** The characters this attacker may hit now (the engine's rule: Shirodu first, no hidden Shinobi, Stealth…). */
    List<Combatant> attackTargets(Combatant attacker);

    /** True once the active player's hero power was used this turn (Kuji-in gives it back). */
    boolean powerUsed();
}
