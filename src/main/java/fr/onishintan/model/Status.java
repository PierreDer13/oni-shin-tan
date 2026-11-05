package fr.onishintan.model;

/** Temporary states on a character, outside keywords. */
public enum Status {
    SLEEP,    // cannot attack next turn (Jorōgumo, Oyasumi)
    STEALTH   // hero untargetable until its next turn (Ninja Mutsubushi)
}