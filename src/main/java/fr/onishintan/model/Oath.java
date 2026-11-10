package fr.onishintan.model;

import java.util.Objects;

/**
 * A sworn Oath (GDD, Oaths): 0 to 9 stacks, then on a break 2 turns of
 * Oathbreaker, after which the player may swear the same Oath again from 0.
 * The rules that keep or break it live in the engine (OathRules).
 */
public class Oath {
    public static final int MAX_STACKS = 9;
    public static final int OATHBREAKER_TURNS = 2;

    public enum State { SWORN, OATHBREAKER, LAPSED }

    private final OathType type;
    private State state = State.SWORN;
    private int stacks;
    private int oathbreakerTurnsLeft;
    private boolean revealed;   // Ninja only: hidden until 3 stacks or a break

    public Oath(OathType type) {
        this.type = Objects.requireNonNull(type, "type");
        this.revealed = type.getHeroClass() != HeroClass.NINJA;
    }

    /** +1 stack (cap 9). Returns false if the Oath is not sworn or already full. */
    public boolean addStack() {
        if (state != State.SWORN || stacks >= MAX_STACKS) return false;
        stacks++;
        if (stacks >= 3) revealed = true;
        return true;
    }

    /** Breaks the Oath. Returns the stacks lost (added to the break damage). */
    public int breakOath() {
        if (state != State.SWORN) throw new IllegalStateException("Oath not sworn: " + state);
        int lost = stacks;
        stacks = 0;
        state = State.OATHBREAKER;
        oathbreakerTurnsLeft = OATHBREAKER_TURNS;
        revealed = true;
        return lost;
    }

    /** End of one of the owner's turns. Returns true when Oathbreaker just ended. */
    public boolean tickOathbreaker() {
        if (state != State.OATHBREAKER) return false;
        oathbreakerTurnsLeft--;
        if (oathbreakerTurnsLeft > 0) return false;
        state = State.LAPSED;
        return true;
    }

    /** Swear the same Oath again, from 0 stacks, once Oathbreaker is over. */
    public void reswear() {
        if (state != State.LAPSED) throw new IllegalStateException("Cannot re-swear now: " + state);
        state = State.SWORN;
    }

    public OathType getType() { return type; }
    public State getState() { return state; }
    public int getStacks() { return stacks; }
    public int getOathbreakerTurnsLeft() { return oathbreakerTurnsLeft; }
    public boolean isSworn() { return state == State.SWORN; }
    /** What the opponent may see: false shows only "Ninja Oath sworn". */
    public boolean isRevealed() { return revealed; }
}