package fr.onishintan.model;

/** Takeda Shingen's hero power, Godai: the five great elements of the gorinto, from the base stone to the top. */
public enum GodaiElement {
    CHI("Chi", "地", "A friendly minion +4 HP and Shirodu; your hero gains 4 armour"),
    SUI("Sui", "水", "Heal a friendly character for 8, then draw a card"),
    KA("Ka", "火", "Deal 6 damage to an enemy character"),
    FU("Fu", "風", "A friendly minion +3 ATK and Tatakae; it can attack again this turn"),
    KU("Ku", "空", "Remove all effects from a minion; it goes back to its printed stats");

    private final String displayName;
    private final String kanji;
    private final String effect;

    GodaiElement(String displayName, String kanji, String effect) {
        this.displayName = displayName;
        this.kanji = kanji;
        this.effect = effect;
    }

    public String getDisplayName() { return displayName; }
    public String getKanji() { return kanji; }
    public String getEffect() { return effect; }
}
