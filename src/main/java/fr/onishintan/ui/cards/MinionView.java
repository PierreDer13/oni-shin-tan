package fr.onishintan.ui.cards;

import fr.onishintan.model.Minion;

/** A minion on the board: its card, with the current ATK and HP instead of the printed ones. */
public class MinionView extends CardView {
    private final Minion minion;
    /** Oishi Kuranosuke seen by his opponent: his ATK and HP show as "?". */
    private boolean hideStats;

    /** A minion whose stats the viewer may not see (hideStats: "?" instead of ATK and HP). */
    public MinionView(Minion minion, double width, boolean hideStats) {
        this(minion, width);
        this.hideStats = hideStats;
        render();
    }

    public MinionView(Minion minion, double width) {
        super(minion.getSource(), width);
        this.minion = minion;
        render();
    }

    @Override
    protected int[] stats() {
        return minion == null ? super.stats() : new int[] { minion.getAttack(), minion.getHealth() };
    }

    @Override
    protected boolean damaged() { return minion != null && minion.getHealth() < minion.getMaxHealth(); }

    public Minion getMinion() { return minion; }

    @Override
    protected boolean onBoard() { return minion != null; }

    @Override
    protected boolean hiddenStats() { return hideStats; }
}