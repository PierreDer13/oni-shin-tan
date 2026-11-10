package fr.onishintan.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One player's side of the board: 7 fixed slots, minions never shift. */
public class BoardSide {
    public static final int MAX = 7;

    private final Minion[] slots = new Minion[MAX];

    /** Puts a minion in a free slot. */
    public void place(Minion m, int slot) {
        if (slot < 0 || slot >= MAX) {
            throw new IllegalArgumentException("Invalid slot: " + slot);
        }
        if (slots[slot] != null) {
            throw new IllegalStateException("Slot " + slot + " is already occupied");
        }
        slots[slot] = m;
    }

    public Minion getAt(int slot) {
        return (slot < 0 || slot >= MAX) ? null : slots[slot];
    }

    public boolean isFree(int slot) {
        return slot >= 0 && slot < MAX && slots[slot] == null;
    }

    /** Slot index of this minion, or -1 if it is not on this side. */
    public int slotOf(Minion m) {
        for (int i = 0; i < MAX; i++) {
            if (slots[i] == m) return i;
        }
        return -1;
    }

    /** Leftmost free slot, or -1 if the side is full. */
    public int firstFree() {
        for (int i = 0; i < MAX; i++) {
            if (slots[i] == null) return i;
        }
        return -1;
    }

    /** Minions directly left and right of the slot (empty neighbours ignored). */
    public List<Minion> adjacent(int slot) {
        List<Minion> result = new ArrayList<>(2);
        Minion left = getAt(slot - 1);
        Minion right = getAt(slot + 1);
        if (left != null) result.add(left);
        if (right != null) result.add(right);
        return result;
    }

    public boolean remove(Minion m) {
        int slot = slotOf(m);
        if (slot < 0) return false;
        slots[slot] = null;
        return true;
    }

    /** Minions with Shirodu: enemies must attack these first. */
    public List<Minion> shirodus() {
        return getMinions().stream().filter(m -> m.hasKeyword(Keyword.SHIRODU)).toList();
    }

    /** Removes dead minions, leaving their slots empty (no shifting). */
    public List<Minion> removeDead() {
        List<Minion> dead = new ArrayList<>();
        for (int i = 0; i < MAX; i++) {
            if (slots[i] != null && slots[i].isDead()) {
                dead.add(slots[i]);
                slots[i] = null;
            }
        }
        return dead;
    }

    public boolean isFull() { return firstFree() == -1; }

    public int size() {
        int count = 0;
        for (Minion m : slots) if (m != null) count++;
        return count;
    }

    /** Minions left to right, empty slots skipped. */
    public List<Minion> getMinions() {
        List<Minion> list = new ArrayList<>();
        for (Minion m : slots) if (m != null) list.add(m);
        return Collections.unmodifiableList(list);
    }
}