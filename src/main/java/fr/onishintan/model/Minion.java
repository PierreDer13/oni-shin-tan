package fr.onishintan.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** A minion on the board: starts from its card, then gets buffed, damaged, hidden... */
public class Minion extends Combatant implements BoardEntity {
    private final MinionCard source;
    private final Set<Keyword> keywords;
    private boolean summonedThisTurn = true;
    private boolean hidden;     // Shinobi: untargetable until it attacks
    private int toriiStacks;    // each stack blocks one hit; Torii can be given several times
    private int armour;         // Yoroi: taken off before HP, after Torii

    public Minion(MinionCard source) {
        super(source.getBaseAttack(), source.getBaseHealth());
        this.source = source;
        this.keywords = source.getKeywords().isEmpty()
                ? EnumSet.noneOf(Keyword.class)
                : EnumSet.copyOf(source.getKeywords());
        this.hidden = keywords.contains(Keyword.SHINOBI);
        this.toriiStacks = keywords.contains(Keyword.TORII) ? 1 : 0;
    }

    @Override
    public int takeDamage(int amount) {
        if (amount <= 0) return 0;
        if (toriiStacks > 0) {   // one Torii stack absorbs the whole hit, whatever its size
            toriiStacks--;
            return 0;
        }
        return takeDamageIgnoringTorii(amount);
    }

    @Override
    public boolean canAttack() {
        boolean fresh = summonedThisTurn && !keywords.contains(Keyword.TATAKAE);
        return attack > 0 && !fresh && !hasAttackedThisTurn && !hasStatus(Status.SLEEP);
    }

    @Override
    public void markAttacked() {
        super.markAttacked();
        hidden = false;          // a Shinobi reveals itself when it attacks
    }

    @Override
    public void onTurnStart() {
        super.onTurnStart();
        summonedThisTurn = false;
    }

    /** Adds ATK and HP (max health too). Buffs may go past the 20 base cap. */
    public void buff(int atk, int hp) {
        attack = Math.max(0, attack + atk);
        maxHealth += hp;
        health += hp;
    }

    /** Takes back a bonus given by buff (an aura that ends): max HP drops, HP only down to the new max. */
    public void unbuff(int atk, int hp) {
        attack = Math.max(0, attack - atk);
        maxHealth -= hp;
        health = Math.min(health, maxHealth);
    }

    public int takeDamageIgnoringTorii(int amount) {
        if (amount <= 0) return 0;
        int absorbed = Math.min(armour, amount);   // armour goes before HP
        armour -= absorbed;
        return super.takeDamage(amount - absorbed);
    }

    /** Yoroi: armour on a minion, lost before its HP. */
    public void gainArmour(int n) { if (n > 0) armour += n; }

    public int getArmour() { return armour; }

    /** Kettō winner: left on half its HP, rounded up. */
    public void halveHealth() { health = (health + 1) / 2; }

    /** Sakasama: ATK and HP swapped; the new HP is also the max. 0 HP = dead. */
    public void swapStats() {
        int atk = attack;
        attack = Math.max(0, health);
        health = atk;
        maxHealth = atk;
    }

    public void giveKeyword(Keyword k) {
        keywords.add(k);
        if (k == Keyword.TORII) toriiStacks++;
        if (k == Keyword.SHINOBI) hidden = true;
    }

    /** Shike: every keyword gone (Torii stacks and Shinobi included). */
    public void clearKeywords() {
        keywords.clear();
        toriiStacks = 0;
        hidden = false;
    }

    /** Takes a keyword away (an aura or an Oath reward that ends). */
    public void removeKeyword(Keyword k) {
        keywords.remove(k);
        if (k == Keyword.TORII) toriiStacks = 0;
        if (k == Keyword.SHINOBI) hidden = false;
    }
    
    public boolean hasKeyword(Keyword k) { return keywords.contains(k); }
    public Set<Keyword> getKeywords() { return Collections.unmodifiableSet(keywords); }
    public MinionCard getSource() { return source; }
    public boolean isSummonedThisTurn() { return summonedThisTurn; }
    public boolean isHidden() { return hidden; }
    public boolean hasTorii() { return toriiStacks > 0; }
    public int getToriiStacks() { return toriiStacks; }

    @Override
    public String toString() { return source.getNameRomaji() + " " + attack + "/" + health; }
}