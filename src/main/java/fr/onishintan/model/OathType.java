package fr.onishintan.model;

/**
 * The 12 Oaths of the GDD, 3 per class. breakBase is the damage the owner's hero
 * takes on a break, before the current stacks are added (GDD, Breaking).
 * Extra penalties (Asleep, discard...) come with engine-oath-rewards.
 */
public enum OathType {
    HONOR("Honor", "名誉", HeroClass.SAMURAI, 10),
    ALLEGIANCE("Allegiance", "忠義", HeroClass.SAMURAI, 6),
    JUSTICE("Justice", "正義", HeroClass.SAMURAI, 8),
    SILENCE("Silence", "沈黙", HeroClass.NINJA, 8),
    DECEPTION("Deception", "欺瞞", HeroClass.NINJA, 6),
    DISCIPLINE("Discipline", "規律", HeroClass.NINJA, 6),
    PURITY("Purity", "清浄", HeroClass.KANNUSHI, 8),
    PEACE("Peace", "平和", HeroClass.KANNUSHI, 15),
    DIVINITY("Divinity", "神性", HeroClass.KANNUSHI, 8),
    HAVOC("Havoc", "破壊", HeroClass.ONI, 10),
    DESCENSION("Descension", "降臨", HeroClass.ONI, 8),
    CORRUPTION("Corruption", "堕落", HeroClass.ONI, 8);

    private final String displayName;
    private final String kanji;
    private final HeroClass heroClass;
    private final int breakBase;

    OathType(String displayName, String kanji, HeroClass heroClass, int breakBase) {
        this.displayName = displayName;
        this.kanji = kanji;
        this.heroClass = heroClass;
        this.breakBase = breakBase;
    }

    /** "Justice" -> JUSTICE: the name OathView hands back when an Oath is sworn. */
    public static OathType fromName(String name) {
        for (OathType t : values()) {
            if (t.displayName.equalsIgnoreCase(name)) return t;
        }
        throw new IllegalArgumentException("Unknown Oath: " + name);
    }

    public String getDisplayName() { return displayName; }
    public String getKanji() { return kanji; }
    public HeroClass getHeroClass() { return heroClass; }
    public int getBreakBase() { return breakBase; }
}