package fr.onishintan.ui;

import fr.onishintan.model.Minion;

/** A minion on the board: its card, with the current ATK and HP instead of the printed ones. */
public class MinionView extends CardView {
    private final Minion minion;

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
}