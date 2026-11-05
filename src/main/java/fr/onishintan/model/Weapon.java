package fr.onishintan.model;

/** A weapon equipped by a hero. Each attack costs 1 durability. */
public class Weapon {
    private final WeaponCard source;
    private int attack;
    private int durability;

    public Weapon(WeaponCard source) {
        this.source = source;
        this.attack = source.getAttack();
        this.durability = source.getDurability();
    }

    /** Called after the hero attacks with it. */
    public void use() { durability--; }

    public boolean isBroken() { return durability <= 0; }

    public void addAttack(int n) { attack = Math.max(0, attack + n); }     // Weaponsmith
    public void addDurability(int n) { durability += n; }                   // Chiburi

    public WeaponCard getSource() { return source; }
    public int getAttack() { return attack; }
    public int getDurability() { return durability; }

    @Override
    public String toString() { return source.getNameRomaji() + " " + attack + "/" + durability; }
}