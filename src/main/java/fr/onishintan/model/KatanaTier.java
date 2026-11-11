package fr.onishintan.model;

/** Samurai hero power ladder (GDD): more Ki, stronger blade. */
public enum KatanaTier {
    BOKKEN("bokken", "Bokken", "木刀", "ぼっけん", 1, 3, 1),
    TANTO("tanto", "Tantō", "短刀", "たんとう", 2, 5, 1),
    KATANA("katana", "Katana", "刀", "かたな", 4, 8, 1),
    ODACHI("odachi", "Ōdachi", "大太刀", "おおだち", 6, 10, 1),
    HONJO_MASAMUNE("honjo-masamune", "Honjō Masamune", "本庄正宗", "ほんじょうまさむね", 9, 15, 1);

    private final String id, nameRomaji, nameKanji, nameKana;
    private final int kiCost, attack, durability;

    KatanaTier(String id, String romaji, String kanji, String kana, int kiCost, int attack, int durability) {
        this.id = id; this.nameRomaji = romaji; this.nameKanji = kanji; this.nameKana = kana;
        this.kiCost = kiCost; this.attack = attack; this.durability = durability;
    }

    public int getKiCost() { return kiCost; }
    public int getAttack() { return attack; }
    public int getDurability() { return durability; }

    /** The blade as a weapon card (id = image id in images/cards/). */
    public WeaponCard toCard() {
        Rarity r = this == HONJO_MASAMUNE ? Rarity.LEGENDARY : Rarity.COMMON;
        return new WeaponCard(id, nameRomaji, nameKanji, nameKana, kiCost, r, HeroClass.SAMURAI,
                "", "", attack, durability);
    }
}