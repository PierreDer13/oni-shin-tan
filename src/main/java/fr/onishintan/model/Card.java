package fr.onishintan.model;

import java.util.Objects;

/** A printed card: what is written on it. Game rules live in the engine. */
public abstract class Card {
    public static final int MAX_KI = 12;

    private final String id;           // unique key, e.g. "kappa"
    private final String nameRomaji;   // "Kappa"
    private final String nameKanji;    // "河童"
    private final String nameKana;     // "かっぱ"
    private final int kiCost;
    private final Rarity rarity;
    private final HeroClass heroClass;
    private final String effectText;   // English text
    private final String effectTextJp; // vertical Japanese text on the card

    protected Card(String id, String nameRomaji, String nameKanji, String nameKana,
                   int kiCost, Rarity rarity, HeroClass heroClass,
                   String effectText, String effectTextJp) {
        if (kiCost < 0 || kiCost > MAX_KI) {
            throw new IllegalArgumentException("Ki cost must be 0-" + MAX_KI + ": " + kiCost);
        }
        this.id = Objects.requireNonNull(id, "id");
        this.nameRomaji = Objects.requireNonNull(nameRomaji, "nameRomaji");
        this.nameKanji = Objects.requireNonNull(nameKanji, "nameKanji");
        this.nameKana = nameKana == null ? "" : nameKana;
        this.kiCost = kiCost;
        this.rarity = Objects.requireNonNull(rarity, "rarity");
        this.heroClass = Objects.requireNonNull(heroClass, "heroClass");
        this.effectText = effectText == null ? "" : effectText;
        this.effectTextJp = effectTextJp == null ? "" : effectTextJp;
    }

    public String getId() { return id; }
    public String getNameRomaji() { return nameRomaji; }
    public String getNameKanji() { return nameKanji; }
    public String getNameKana() { return nameKana; }
    public int getKiCost() { return kiCost; }
    public Rarity getRarity() { return rarity; }
    public HeroClass getHeroClass() { return heroClass; }
    public String getEffectText() { return effectText; }
    public String getEffectTextJp() { return effectTextJp; }

    @Override
    public String toString() {
        return nameRomaji + " (" + kiCost + " Ki)";
    }
}