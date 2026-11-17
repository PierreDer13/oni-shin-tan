package fr.onishintan.model;

import java.util.EnumSet;
import java.util.Set;

/** Anything on the board that has ATK and HP and can fight: a minion or a hero. */
public abstract class Combatant {
    protected int attack;
    protected int health;
    protected int maxHealth;
    protected boolean hasAttackedThisTurn;
    protected final Set<Status> statuses = EnumSet.noneOf(Status.class);

    protected Combatant(int attack, int health) {
        this.attack = attack;
        this.health = health;
        this.maxHealth = health;
    }

    /**
     * Applies damage and returns the damage actually taken
     * (Torii or armour can absorb part or all of it).
     */
    public int takeDamage(int amount) {
        if (amount <= 0) return 0;
        health -= amount;
        return amount;
    }

    /** Heals up to max health; returns the HP actually restored. */
    public int heal(int amount) {
        if (amount <= 0 || isDead()) return 0;
        int restored = Math.min(amount, maxHealth - health);
        health += restored;
        return restored;
    }

    /** Destroyed outright, whatever its HP, Torii or armour (Banishment, Abyss). */
    public void destroy() { health = 0; }

    /** Inochi: raises max health and current health by n. */
    public void raiseMaxHealth(int n) {
        if (n <= 0) return;
        maxHealth += n;
        health += n;
    }
    
    public boolean isDead() { return health <= 0; }

    /** Whether this combatant may attack right now (rules differ for minions and heroes). */
    public abstract boolean canAttack();

    /** Called by the engine once this combatant has attacked. */
    public void markAttacked() { hasAttackedThisTurn = true; }

    /** Called by the engine at the start of its owner's turn. */
    public void onTurnStart() { hasAttackedThisTurn = false; }

    public void addStatus(Status s) { statuses.add(s); }
    public void removeStatus(Status s) { statuses.remove(s); }
    public boolean hasStatus(Status s) { return statuses.contains(s); }

    public int getAttack() { return attack; }
    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
    public boolean hasAttackedThisTurn() { return hasAttackedThisTurn; }
}