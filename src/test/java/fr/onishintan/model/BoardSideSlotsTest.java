package fr.onishintan.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.EnumSet;

class BoardSideSlotsTest {

    /** A 1 Ki neutral minion with the given stats and a unique id. */
    private Minion minion(String id, int atk, int hp) {
        return new Minion(new MinionCard("test-" + id, "Test " + id, "試", "てすと", 1,
                Rarity.COMMON, HeroClass.NEUTRAL, "", "",
                atk, hp, EnumSet.noneOf(Keyword.class)));
    }

    @Test
    void minionStaysInItsSlotWhenNeighbourDies() {
        BoardSide side = new BoardSide();
        Minion left = minion("left", 1, 1);
        Minion right = minion("right", 1, 3);
        side.place(left, 2);
        side.place(right, 3);

        left.takeDamage(5);
        side.removeDead();

        assertTrue(side.isFree(2));
        assertSame(right, side.getAt(3));
        assertEquals(3, side.slotOf(right));
    }

    @Test
    void placingOnOccupiedSlotIsRefused() {
        BoardSide side = new BoardSide();
        side.place(minion("a", 1, 1), 4);

        assertThrows(IllegalStateException.class,
                () -> side.place(minion("b", 1, 1), 4));
    }

    @Test
    void adjacentReturnsOnlyOccupiedNeighbours() {
        BoardSide side = new BoardSide();
        Minion a = minion("a", 1, 1);
        Minion b = minion("b", 1, 1);
        side.place(a, 0);
        side.place(b, 1);

        assertEquals(java.util.List.of(b), side.adjacent(0));
        assertEquals(java.util.List.of(a), side.adjacent(1));
        assertTrue(side.adjacent(6).isEmpty());
    }
}