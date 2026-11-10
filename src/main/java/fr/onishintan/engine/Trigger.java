package fr.onishintan.engine;

/** When a card effect runs. */
public enum Trigger {
    BANZAI,      // played from hand
    SEPPUKU,     // the minion dies
    TURN_START,  // start of its owner's turn
    TURN_END,    // end of its owner's turn (Hatamoto)
    TRAP         // a set trap goes off
}