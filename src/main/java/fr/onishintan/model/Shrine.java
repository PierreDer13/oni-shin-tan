package fr.onishintan.model;

import java.util.Objects;

/** A Shrine on the board: no ATK or HP, cannot be attacked, gone after its duration. */
public class Shrine implements BoardEntity {
    private final ShrineCard source;
    private int turnsLeft;

    public Shrine(ShrineCard source) {
        this.source = Objects.requireNonNull(source);
        this.turnsLeft = source.getDuration();
    }

    /** End of the owner's turn: one turn less. */
    public void tick() { if (turnsLeft > 0) turnsLeft--; }
    /** Stays n more turns (Divinity 3). */
    public void extend(int n) { if (n > 0) turnsLeft += n; }
    public boolean isExpired() { return turnsLeft <= 0; }
    public int getTurnsLeft() { return turnsLeft; }
    public ShrineCard getSource() { return source; }

    @Override
    public String toString() { return source.getNameRomaji() + " (" + turnsLeft + ")"; }
}