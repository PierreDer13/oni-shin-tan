package fr.onishintan.model;

/** The card keywords from the GDD, and Uragiri (betrayal: only while your Oath is broken). */
public enum Keyword {
    BANZAI("Banzai", "万歳"),     // effect when played from hand
    SEPPUKU("Seppuku", "切腹"),   // effect when the minion dies
    PASSIVE("Passive", "常時"),   // effect while the minion is alive
    SHIRODU("Shirodu", "盾"),     // enemies must attack it first
    SHINOBI("Shinobi", "忍び"),   // hidden and untargetable until it attacks
    TORII("Torii", "鳥居"),       // blocks the next damage, then is lost
    TATAKAE("Tatakae", "戦え"),   // can attack the turn it is played
    URAGIRI("Uragiri", "裏切り");   // its effect only works while its owner is an Oathbreaker

    private final String displayName;
    private final String kanji;

    Keyword(String displayName, String kanji) {
        this.displayName = displayName;
        this.kanji = kanji;
    }

    public String getDisplayName() { return displayName; }
    public String getKanji() { return kanji; }
}