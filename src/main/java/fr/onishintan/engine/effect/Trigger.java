package fr.onishintan.engine.effect;

/** When a card effect runs. */
public enum Trigger {
    BANZAI,      // played from hand
    SEPPUKU,     // the minion dies
    TURN_START,  // start of its owner's turn
    TURN_END,    // end of its owner's turn (Hatamoto)
    TRAP,        // a set trap goes off
    CURSE        // a Curse acts: end of the holder's turn (hand) or when drawn (deck); owner = the cursed player
}