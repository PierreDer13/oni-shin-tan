package fr.onishintan.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One player's side of the board: 7 fixed slots holding minions or Shrines; nothing ever shifts. */
public class BoardSide {
    public static final int MAX = 7;

    private final BoardEntity[] slots = new BoardEntity[MAX];

    /** Puts a minion or a Shrine in a free slot. */
    public void place(BoardEntity e, int slot) {
        if (slot < 0 || slot >= MAX) {
            throw new IllegalArgumentException("Invalid slot: " + slot);
        }
        if (slots[slot] != null) {
            throw new IllegalStateException("Slot " + slot + " is already occupied");
        }
        slots[slot] = e;
    }

    /** The minion in this slot; null if empty or if a Shrine stands there. */
    public Minion getAt(int slot) {
        return entityAt(slot) instanceof Minion m ? m : null;
    }

    /** Whatever stands in this slot (Minion or Shrine), or null. */
    public BoardEntity entityAt(int slot) {
        return (slot < 0 || slot >= MAX) ? null : slots[slot];
    }

    public boolean isFree(int slot) {
        return slot >= 0 && slot < MAX && slots[slot] == null;
    }

    /** Slot index of this minion or Shrine, or -1 if it is not on this side. */
    public int slotOf(BoardEntity e) {
        for (int i = 0; i < MAX; i++) {
            if (slots[i] == e) return i;
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

    /** Minions directly left and right of the slot (empty slots and Shrines ignored). */
    public List<Minion> adjacent(int slot) {
        List<Minion> result = new ArrayList<>(2);
        Minion left = getAt(slot - 1);
        Minion right = getAt(slot + 1);
        if (left != null) result.add(left);
        if (right != null) result.add(right);
        return result;
    }

    public boolean remove(BoardEntity e) {
        int slot = slotOf(e);
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
            if (slots[i] instanceof Minion m && m.isDead()) {
                dead.add(m);
                slots[i] = null;
            }
        }
        return dead;
    }

    /** End of the owner's turn: every Shrine loses a turn; expired ones leave. Returns them. */
    public List<Shrine> tickShrines() {
        List<Shrine> expired = new ArrayList<>();
        for (int i = 0; i < MAX; i++) {
            if (slots[i] instanceof Shrine s) {
                s.tick();
                if (s.isExpired()) {
                    expired.add(s);
                    slots[i] = null;
                }
            }
        }
        return expired;
    }

    /** No free slot left (minions and Shrines both take one). */
    public boolean isFull() { return firstFree() == -1; }

    /** Number of minions (Shrines not counted). */
    public int size() {
        int count = 0;
        for (BoardEntity e : slots) if (e instanceof Minion) count++;
        return count;
    }

    /** Minions left to right, empty slots and Shrines skipped. */
    public List<Minion> getMinions() {
        List<Minion> list = new ArrayList<>();
        for (BoardEntity e : slots) if (e instanceof Minion m) list.add(m);
        return Collections.unmodifiableList(list);
    }

    /** Shrines left to right. */
    public List<Shrine> shrines() {
        List<Shrine> list = new ArrayList<>();
        for (BoardEntity e : slots) if (e instanceof Shrine s) list.add(s);
        return Collections.unmodifiableList(list);
    }
}