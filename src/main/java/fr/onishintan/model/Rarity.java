package fr.onishintan.model;

/** Card rarity. budgetBonus is added to the stat budget of the balance model. */
public enum Rarity {
    COMMON(0, 3),
    RARE(0, 3),
    EPIC(0, 3),
    LEGENDARY(4, 1),
    MYTHIC(8, 1);

    private final int budgetBonus;
    private final int maxCopies;

    Rarity(int budgetBonus, int maxCopies) {
        this.budgetBonus = budgetBonus;
        this.maxCopies = maxCopies;
    }

    public int getBudgetBonus() { return budgetBonus; }
    /** Max copies of one card in a 40-card deck. */
    public int getMaxCopies() { return maxCopies; }
}
