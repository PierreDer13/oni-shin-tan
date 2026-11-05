package fr.onishintan.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One player's half of the board: up to 7 minions, left to right. */
public class BoardSide {
    public static final int MAX = 7;

    private final List<Minion> minions = new ArrayList<>();

    /** Places a minion at a slot (0 = leftmost); clamped to the end of the row. */
    public void place(Minion m, int slot) {
        if (isFull()) throw new IllegalStateException("Board is full (" + MAX + " minions)");
        int i = Math.max(0, Math.min(slot, minions.size()));
        minions.add(i, m);
    }

    public boolean remove(Minion m) { return minions.remove(m); }

    /** Minions with Shirodu: enemies must attack these first. */
    public List<Minion> shirodus() {
        return minions.stream().filter(m -> m.hasKeyword(Keyword.SHIRODU)).toList();
    }

    /** Removes dead minions and returns them (the engine then runs their Seppuku). */
    public List<Minion> removeDead() {
        List<Minion> dead = minions.stream().filter(Minion::isDead).toList();
        minions.removeAll(dead);
        return dead;
    }

    public boolean isFull() { return minions.size() >= MAX; }
    public int size() { return minions.size(); }
    public List<Minion> getMinions() { return Collections.unmodifiableList(minions); }
}