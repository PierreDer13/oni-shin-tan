package fr.onishintan.model;

/**
 * The four playable classes, NEUTRAL for cards any deck can use, and OROCHI: Yamata no Orochi's own class,
 * only for that bonus boss (no class passive, no cards of its own: its deck mixes the 4 classes and the neutrals).
 */
public enum HeroClass {
    NINJA("Ninja", "忍者"),
    SAMURAI("Samurai", "侍"),
    KANNUSHI("Kannushi", "神主"),
    ONI("Oni", "鬼"),
    NEUTRAL("Neutral", "中立"),
    OROCHI("Orochi", "大蛇");

    private final String displayName;
    private final String kanji;

    HeroClass(String displayName, String kanji) {
        this.displayName = displayName;
        this.kanji = kanji;
    }

    public String getDisplayName() { return displayName; }
    public String getKanji() { return kanji; }
}