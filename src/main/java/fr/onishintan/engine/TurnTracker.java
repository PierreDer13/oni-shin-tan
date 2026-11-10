package fr.onishintan.engine;

import fr.onishintan.model.Card;
import fr.onishintan.model.MinionCard;

/**
 * What the active player did this turn, read by the Oath rules at the end of it.
 * Reset at TURN_STARTED. Damage dealt and attacks on the hero come with Havoc and Honor.
 */
public class TurnTracker {
    private int cardsPlayed;
    private int minionsPlayed;
    private int kiSpent;

    void reset() {
        cardsPlayed = 0;
        minionsPlayed = 0;
        kiSpent = 0;
    }

    void cardPlayed(Card card) {
        cardsPlayed++;
        kiSpent += card.getKiCost();
        if (card instanceof MinionCard) minionsPlayed++;
    }

    public int getCardsPlayed() { return cardsPlayed; }
    public int getMinionsPlayed() { return minionsPlayed; }
    public int getKiSpent() { return kiSpent; }
}