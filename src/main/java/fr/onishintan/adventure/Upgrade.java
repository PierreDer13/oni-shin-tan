package fr.onishintan.adventure;

/**
 * The Adventure upgrades (GDD, Adventure): one picked after battle 1, one after battle 2.
 * The engine applies them with GameEngine.setUpgrades(player, upgrades).
 */
public enum Upgrade {
    /** Hero max HP +30 (100 -> 130). */
    INOCHI("命 Inochi", "Hero max HP +30"),
    /** Minions and weapons +3 ATK, spells +3 damage. */
    CHIKARA("力 Chikara", "Minions and weapons +3 ATK, spells +3 damage"),
    /** Your cards cost 1 Ki less (minimum 1). */
    HAYASA("速さ Hayasa", "Your cards cost 1 Ki less (min 1)");

    public static final int INOCHI_HP = 30;
    public static final int CHIKARA_BONUS = 3;
    /** Hayasa never takes a card below this cost. */
    public static final int HAYASA_MIN = 1;

    private final String label;
    private final String description;

    Upgrade(String label, String description) {
        this.label = label;
        this.description = description;
    }

    /** The name shown on the Adventure screen, kanji first ("命 Inochi"). */
    public String getLabel() { return label; }

    public String getDescription() { return description; }

    /** The upgrade with that label or name (case ignored), or null. */
    public static Upgrade fromLabel(String text) {
        if (text == null) return null;
        for (Upgrade u : values()) {
            if (u.label.equalsIgnoreCase(text.trim()) || u.name().equalsIgnoreCase(text.trim())) return u;
        }
        return null;
    }
}
