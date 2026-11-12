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
        return super.takeDamage(amount);
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

    public void giveKeyword(Keyword k) {
        keywords.add(k);
        if (k == Keyword.TORII) toriiStacks++;
        if (k == Keyword.SHINOBI) hidden = true;
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