package fr.onishintan.engine;

/** What a plain spell or a Banzai is aimed at: GameEngine.spellTarget / banzaiTarget, then spellTargets / banzaiTargets for the legal ones. */
public enum SpellTarget {
    NONE,             // no target: drop it anywhere
    MINION,           // any minion (not a hidden enemy)
    FRIENDLY_MINION,  // one of your minions
    ENEMY_MINION,     // an enemy minion (not hidden)
    FRIENDLY_CHARACTER, // one of your minions or your hero
    CHARACTER,        // any minion or hero (not a hidden enemy, not a Stealth hero)
    TWO_MINIONS       // two different minions (Kettō): playCard(card, slot, first, second)
}