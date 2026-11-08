package fr.onishintan.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.junit.jupiter.api.Test;

class PlayerTest {

    // ---------- helpers: the only lines that depend on constructors ----------

    /** A plain 1 Ki 4/4 neutral minion card with a unique id. */
    private static Card card(int i) {
        return new MinionCard("test-" + i, "Test " + i, "試", "てすと", 1,
                Rarity.COMMON, HeroClass.NEUTRAL, "", "",
                4, 4, EnumSet.noneOf(Keyword.class));
    }

    /** A player whose deck holds n distinct cards. */
    private static Player player(int n) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < n; i++) cards.add(card(i));
        return new Player("Tester", new Hero(HeroClass.SAMURAI), new Deck(cards));
    }

    // ---------- draw ----------

    @Test
    void drawMovesTopCardFromDeckToHand() {
        Player p = player(10);
        Optional<Card> drawn = p.draw();
        assertTrue(drawn.isPresent());
        assertEquals(1, p.getHand().size());
        assertEquals(9, p.getDeck().size());
        assertSame(drawn.get(), p.getHand().get(0));
    }

    @Test
    void drawFromEmptyDeckReturnsEmpty() {
        Player p = player(0);
        assertTrue(p.draw().isEmpty());
        assertEquals(0, p.getHand().size());
    }

    @Test
    void drawWithFullHandBurnsTheCard() {
        Player p = player(Player.MAX_HAND + 3);
        for (int i = 0; i < Player.MAX_HAND; i++) p.draw();
        int deckBefore = p.getDeck().size();

        assertTrue(p.draw().isEmpty());                    // burned
        assertEquals(Player.MAX_HAND, p.getHand().size()); // hand unchanged
        assertEquals(deckBefore - 1, p.getDeck().size());  // but the card left the deck
    }

    @Test
    void handCannotBeModifiedFromOutside() {
        Player p = player(5);
        p.draw();
        assertThrows(UnsupportedOperationException.class, () -> p.getHand().clear());
    }

    // ---------- Ki ----------

    @Test
    void refillKiAddsOneMaxKiAndRefills() {
        Player p = player(5);
        p.refillKi();
        assertEquals(1, p.getMaxKi());
        assertEquals(1, p.getKi());
        p.refillKi();
        assertEquals(2, p.getMaxKi());
        assertEquals(2, p.getKi());
    }

    @Test
    void maxKiIsCappedAt12() {
        Player p = player(5);
        for (int i = 0; i < 20; i++) p.refillKi();
        assertEquals(Player.KI_CAP, p.getMaxKi());
        assertEquals(Player.KI_CAP, p.getKi());
    }

    @Test
    void spendKiLowersKi() {
        Player p = player(5);
        p.refillKi();
        p.refillKi();                 // 2 Ki
        p.spendKi(2);
        assertEquals(0, p.getKi());
    }

    @Test
    void spendingMoreKiThanAvailableThrows() {
        Player p = player(5);
        p.refillKi();                 // 1 Ki
        assertFalse(p.canAfford(2));
        assertThrows(IllegalStateException.class, () -> p.spendKi(2));
        assertEquals(1, p.getKi());   // nothing spent
    }

    @Test
    void temporaryKiIsLostAtNextRefill() {
        Player p = player(5);
        p.refillKi();                 // 1
        p.gainTemporaryKi(1);         // 2 this turn
        assertEquals(2, p.getKi());
        assertEquals(1, p.getMaxKi());
        p.refillKi();                 // next turn: back to max 2, not 3
        assertEquals(2, p.getKi());
    }

    // ---------- trade ----------

    @Test
    void tradeSendsCardBackAndDraws() {
        Player p = player(10);
        p.draw();
        p.draw();
        Card card = p.getHand().get(0);
        int handBefore = p.getHand().size();
        int deckBefore = p.getDeck().size();

        assertTrue(p.trade(card, new Random(42)));
        assertEquals(handBefore, p.getHand().size());  // one out, one in
        assertEquals(deckBefore, p.getDeck().size());  // one in, one out
    }

    @Test
    void cannotTradeCardNotInHand() {
        Player p = player(10);
        p.draw();
        Card notInHand = card(999);
        assertFalse(p.trade(notInHand, new Random(42)));
        assertEquals(1, p.getHand().size());
    }

    @Test
    void cannotTradeNull() {
        Player p = player(10);
        assertFalse(p.trade(null, new Random(42)));
    }

    @Test
    void tradeWithoutRandomWorksForTheUi() {
        Player p = player(10);
        p.draw();
        assertTrue(p.trade(p.getHand().get(0)));
        assertEquals(1, p.getHand().size());
    }

    @Test
    void tradeWithOneCardDeckCanDrawTheSameCardBack() {
        Player p = player(1);
        p.draw();                     // deck now empty
        Card card = p.getHand().get(0);
        assertTrue(p.trade(card, new Random(42)));
        assertSame(card, p.getHand().get(0));
        assertEquals(0, p.getDeck().size());
    }
}