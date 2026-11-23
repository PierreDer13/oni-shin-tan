package fr.onishintan.model;

import java.util.Objects;

/** A player's hero: 100 HP, optional armour and weapon. Powers and passives come with the engine. */
public class Hero extends Combatant {
    /** Kagemusha: armed by its Banzai, used by the next lethal damage. */
    private boolean lastStand;
    /** Kagemusha: after a lethal hit, the hero can't be harmed until its owner's next turn. */
    private boolean untouchable;
    public static final int START_HEALTH = 100;

    private final HeroClass heroClass;
    private int armour;
    private Weapon weapon;

    public Hero(HeroClass heroClass) {
        this(heroClass, START_HEALTH);
    }

    /** A hero with other starting HP (Yamata no Orochi: 200). */
    public Hero(HeroClass heroClass, int startHealth) {
        super(0, startHealth);
        if (startHealth <= 0) throw new IllegalArgumentException("A hero needs HP, not " + startHealth);
        if (heroClass == HeroClass.NEUTRAL) {
            throw new IllegalArgumentException("A hero cannot be NEUTRAL");
        }
        this.heroClass = Objects.requireNonNull(heroClass);
    }

    @Override
    public int takeDamage(int amount) {
        if (amount <= 0 || untouchable) return 0;   // Kagemusha: nothing harms the hero until its owner's next turn
        int absorbed = Math.min(armour, amount);   // armour goes first
        armour -= absorbed;
        int left = amount - absorbed;
        if (lastStand && left >= health) {          // Kagemusha: lethal damage leaves the hero on 1 HP, untouchable
            lastStand = false;
            untouchable = true;
            addStatus(Status.STEALTH);              // and no enemy can target it (TurnManager lifts it on its next turn)
            return super.takeDamage(health - 1);
        }
        return super.takeDamage(left);
    }

    /** Kagemusha: the next lethal damage leaves this hero on 1 HP instead (once). */
    public void armLastStand() { lastStand = true; }

    /** True while Kagemusha's Banzai still waits for a lethal hit. */
    public boolean hasLastStand() { return lastStand; }

    /** True while Kagemusha keeps this hero from any harm (until its owner's next turn). */
    public boolean isUntouchable() { return untouchable; }

    /** Its owner's turn starts: Kagemusha's protection is over. */
    public void endUntouchable() { untouchable = false; }

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