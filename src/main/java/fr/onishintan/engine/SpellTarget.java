package fr.onishintan.engine;

/** What a plain spell is aimed at: the UI asks GameEngine.spellTarget(card), then GameEngine.spellTargets(card) for the legal ones. */
public enum SpellTarget {
    NONE,             // no target: drop it anywhere
    MINION,           // any minion (not a hidden enemy)
    FRIENDLY_MINION,  // one of your minions
    CHARACTER,        // any minion or hero (not a hidden enemy, not a Stealth hero)
    TWO_MINIONS       // two different minions (Kettō): playCard(card, slot, first, second)
}