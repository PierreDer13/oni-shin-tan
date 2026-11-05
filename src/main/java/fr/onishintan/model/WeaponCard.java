package fr.onishintan.model;

/** A weapon card: ATK and durability (Katana ladder, Honjō Masamune, Futsu-no-mitama). */
public class WeaponCard extends Card {
    private final int attack;
    private final int durability;

    public WeaponCard(String id, String nameRomaji, String nameKanji, String nameKana,
                      int kiCost, Rarity rarity, HeroClass heroClass,
                      String effectText, String effectTextJp,
                      int attack, int durability) {
        super(id, nameRomaji, nameKanji, nameKana, kiCost, rarity, heroClass, effectText, effectTextJp);
        if (attack < 0) throw new IllegalArgumentException("ATK must be >= 0: " + attack);
        if (durability < 1) throw new IllegalArgumentException("Durability must be >= 1: " + durability);
        this.attack = attack;
        this.durability = durability;
    }

    public int getAttack() { return attack; }
    public int getDurability() { return durability; }
}