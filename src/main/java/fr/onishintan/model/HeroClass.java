package fr.onishintan.model;

/** The four playable classes, plus NEUTRAL for cards any deck can use. */
public enum HeroClass {
    NINJA("Ninja", "忍者"),
    SAMURAI("Samurai", "侍"),
    KANNUSHI("Kannushi", "神主"),
    ONI("Oni", "鬼"),
    NEUTRAL("Neutral", "中立");

    private final String displayName;
    private final String kanji;

    HeroClass(String displayName, String kanji) {
        this.displayName = displayName;
        this.kanji = kanji;
    }

    public String getDisplayName() { return displayName; }
    public String getKanji() { return kanji; }
}