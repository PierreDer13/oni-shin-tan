package fr.onishintan.model;

import java.util.Objects;

/** A player's hero: 100 HP, optional armour and weapon. Powers and passives come with the engine. */
public class Hero extends Combatant {
    public static final int START_HEALTH = 100;

    private final HeroClass heroClass;
    private int armour;
    private Weapon weapon;

    public Hero(HeroClass heroClass) {
        super(0, START_HEALTH);
        if (heroClass == HeroClass.NEUTRAL) {
            throw new IllegalArgumentException("A hero cannot be NEUTRAL");
        }
        this.heroClass = Objects.requireNonNull(heroClass);
    }

    @Override
    public int takeDamage(int amount) {
        if (amount <= 0) return 0;
        int absorbed = Math.min(armour, amount);   // armour goes first
        armour -= absorbed;
        return super.takeDamage(amount - absorbed);
    }

    @Override
    public boolean canAttack() {
        return getAttack() > 0 && !hasAttackedThisTurn && !hasStatus(Status.SLEEP);
    }

    /** A hero's attack is its weapon's ATK (0 without a weapon). */
    @Override
    public int getAttack() { return weapon == null ? 0 : weapon.getAttack(); }

    /** Uses the weapon after an attack and drops it when broken. */
    @Override
    public void markAttacked() {
        super.markAttacked();
        if (weapon != null) {
            weapon.use();
            if (weapon.isBroken()) weapon = null;
        }
    }

    public void equip(Weapon w) { this.weapon = w; }        // replaces any current weapon
    public void destroyWeapon() { this.weapon = null; }     // Iaidō, Jujutsu shihan
    public void gainArmour(int n) { if (n > 0) armour += n; } // Gusoku, Hatamoto

    /** Shura no Michi: the hero starts this battle with the wounds of the last ones (never below 1 HP). */
    public void carryWounds(int wounds) {
        if (wounds <= 0) return;
        health = Math.max(1, maxHealth - wounds);
    }

    public HeroClass getHeroClass() { return heroClass; }
    public int getArmour() { return armour; }
    public Weapon getWeapon() { return weapon; }
}