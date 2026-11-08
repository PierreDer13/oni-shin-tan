package fr.onishintan.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.onishintan.model.*;

class TurnManagerTest {

    private Player first;
    private Player second;
    private GameState state;
    private TurnManager tm;

    // ---------- helpers: the only lines that depend on constructors ----------

    private static Card card(String owner, int i) {
        return new MinionCard(owner + "-" + i, "Test " + i, "試", "てすと", 1,
                Rarity.COMMON, HeroClass.NEUTRAL, "", "",
                4, 4, EnumSet.noneOf(Keyword.class));
    }

    private static Player player(String name, HeroClass cls) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < Deck.SIZE; i++)
            cards.add(card(name, i));
        return new Player(name, new Hero(cls), new Deck(cards));
    }

    @BeforeEach
    void setUp() {
        Random rng = new Random(42);
        first = player("Pierre", HeroClass.SAMURAI);
        second = player("Kayne", HeroClass.NINJA);
        state = new GameState(first, second);
        tm = new TurnManager(state, rng);
    }

    // ---------- start of match ----------

    @Test
    void startMatchDealsOpeningHands() {
        tm.startMatch();
        assertEquals(TurnManager.OPENING_HAND_FIRST, first.getHand().size()); // 6
        assertEquals(TurnManager.OPENING_HAND_SECOND, second.getHand().size()); // 7
        assertEquals(Deck.SIZE - 6, first.getDeck().size());
        assertEquals(Deck.SIZE - 7, second.getDeck().size());
    }

    @Test
    void startMatchTwiceThrows() {
        tm.startMatch();
        assertThrows(IllegalStateException.class, () -> tm.startMatch());
    }

    @Test
    void startTurnBeforeMatchThrows() {
        assertThrows(IllegalStateException.class, () -> tm.startTurn());
    }

    @Test
    void endTurnBeforeMatchThrows() {
        assertThrows(IllegalStateException.class, () -> tm.endTurn());
    }

    // ---------- turns and Ki ----------

    @Test
    void firstTurnFirstPlayerHasOneKiAndDraws() {
        tm.startMatch();
        tm.startTurn();
        assertSame(first, state.active());
        assertEquals(1, first.getKi());
        assertEquals(7, first.getHand().size()); // 6 + 1 draw
    }

    @Test
    void secondPlayerGetsBonusKiOnFirstTurnOnly() {
        tm.startMatch();
        tm.startTurn(); // turn 1: first
        tm.endTurn(); // turn 2: second
        assertSame(second, state.active());
        assertEquals(2, second.getKi()); // 1 + 1 bonus
        assertEquals(1, second.getMaxKi());
        assertEquals(8, second.getHand().size()); // 7 + 1 draw

        tm.endTurn(); // turn 3: first
        tm.endTurn(); // turn 4: second again
        assertEquals(2, second.getKi()); // max 2, no bonus anymore
    }

    @Test
    void endTurnPassesToTheOpponent() {
        tm.startMatch();
        tm.startTurn();
        assertSame(first, state.active());
        tm.endTurn();
        assertSame(second, state.active());
        tm.endTurn();
        assertSame(first, state.active());
        assertEquals(2, first.getKi());
    }

    @Test
    void kiStopsGrowingAt12() {
        tm.startMatch();
        tm.startTurn();
        for (int i = 0; i < 30; i++)
            tm.endTurn();
        assertEquals(Player.KI_CAP, state.active().getMaxKi());
    }

    // ---------- swap / trade ----------

    @Test
    void swapKeepsHandAndDeckSizes() {
        tm.startMatch();
        tm.startTurn();
        int hand = first.getHand().size();
        int deck = first.getDeck().size();

        assertTrue(tm.swapCard(first.getHand().get(0)));
        assertEquals(hand, first.getHand().size());
        assertEquals(deck, first.getDeck().size());
    }

    @Test
    void swapOnlyOncePerTurn() {
        tm.startMatch();
        tm.startTurn();
        assertTrue(tm.swapCard(first.getHand().get(0)));
        assertTrue(tm.isSwapUsedThisTurn());
        assertFalse(tm.swapCard(first.getHand().get(0)));
    }

    @Test
    void swapIsAvailableAgainNextTurn() {
        tm.startMatch();
        tm.startTurn();
        tm.swapCard(first.getHand().get(0));
        tm.endTurn();
        assertFalse(tm.isSwapUsedThisTurn());
        assertTrue(tm.swapCard(second.getHand().get(0)));
    }

    @Test
    void cannotSwapOpponentsCard() {
        tm.startMatch();
        tm.startTurn(); // first is active
        assertFalse(tm.swapCard(second.getHand().get(0)));
        assertFalse(tm.isSwapUsedThisTurn()); // a failed swap does not count
    }

    @Test
    void swapBeforeMatchThrows() {
        assertThrows(IllegalStateException.class,
                () -> tm.swapCard(first.getDeck().draw().orElseThrow()));
    }
}