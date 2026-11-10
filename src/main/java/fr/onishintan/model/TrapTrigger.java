package fr.onishintan.model;

/** What sets off a hidden trap. Always an action of the trap owner's enemy. */
public enum TrapTrigger {
    ON_ENEMY_ATTACK_HERO,
    ON_ENEMY_ATTACK_MINION,
    ON_ENEMY_SUMMON,
    ON_ENEMY_SPELL
}