package fr.onishintan.adventure;

import fr.onishintan.ai.AIController.Difficulty;

/** How hard an Adventure run is: the normal path, or Shura no Michi (hardcore). */
public enum AdventureMode {
    /** Easy, normal then hard AI; a bonus after win 1 and win 2; full HP each battle. */
    NORMAL("冒険 Adventure", 2, false),
    /** Shura no Michi: hard AI from the first battle, one bonus only, your wounds stay from battle to battle. */
    HARDCORE("修羅の道 Shura no Michi", 1, true);

    private final String label;
    private final int bonuses;
    private final boolean keepsWounds;

    AdventureMode(String label, int bonuses, boolean keepsWounds) {
        this.label = label;
        this.bonuses = bonuses;
        this.keepsWounds = keepsWounds;
    }

    public String getLabel() { return label; }

    /** How many bonuses the run gives (one per win, up to this). */
    public int getBonuses() { return bonuses; }

    /** True if the hero starts each battle with the HP lost in the previous ones. */
    public boolean keepsWounds() { return keepsWounds; }

    /** The AI of battle 0, 1 or 2 in this mode. */
    public Difficulty difficulty(int battle) {
        return this == HARDCORE ? Difficulty.HARD : Difficulty.forBattle(battle);
    }
}
