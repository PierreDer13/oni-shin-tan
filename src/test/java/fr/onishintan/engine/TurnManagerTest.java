package fr.onishintan.engine;

import fr.onishintan.model.Card;
import fr.onishintan.model.Deck;
import fr.onishintan.model.Hero;
import fr.onishintan.model.HeroClass;
import fr.onishintan.model.Minion;
import fr.onishintan.model.MinionCard;
import fr.onishintan.model.Player;
import fr.onishintan.model.Rarity;
import fr.onishintan.model.Status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TurnManagerTest {
    private static final MinionCard GENIN = new MinionCard("genin", "Genin", "下忍", "げにん",
            1, Rarity.COMMON, HeroClass.NINJA, "", "", 3, 2, Set.of());

    private Player ninja;
    private Player samurai;
    private GameState state;
    private TurnManager turns;

    private static Deck deckOf(int n) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < n; i++) cards.add(GENIN);
        return new Deck(cards);
    }

    @BeforeEach
    void setUp() {
        ninja = new Player("Pierre", new Hero(HeroClass.NINJA), deckOf(Deck.SIZE));
        samurai = new Player("Kayne", new Hero(HeroClass.SAMURAI), deckOf(Deck.SIZE));
        state = new GameState(ninja, samurai);
        turns = new TurnManager(state, new Random(42));
    }

    @Test
    void openingHandsAre6And7() {
        turns.startMatch();
        assertEquals(6, ninja.getHand().size());
        assertEquals(7, samurai.getHand().size());
        assertEquals(44, ninja.getDeck().size());
        assertEquals(43, samurai.getDeck().size());
        assertEquals(1, state.getRound());
        assertSame(ninja, state.active());
    }

    @Test
    void firstTurnGives1KiAndDraws() {
        turns.startMatch();
        turns.startTurn();
        assertEquals(1, ninja.getMaxKi());
        assertEquals(1, ninja.getKi());
        assertEquals(7, ninja.getHand().size());
    }

    @Test
    void secondPlayerGetsBonusKiOnFirstTurnOnly() {
        turns.startMatch();
        turns.startTurn();
        turns.endTurn();                         // samurai, round 1
        assertSame(samurai, state.active());
        assertEquals(1, samurai.getMaxKi());
        assertEquals(2, samurai.getKi());
        assertEquals(8, samurai.getHand().size());
        turns.endTurn();                         // ninja, round 2
        turns.endTurn();                         // samurai, round 2
        assertEquals(2, samurai.getMaxKi());
        assertEquals(2, samurai.getKi());
    }

    @Test
    void roundGoesUpAfterBothTurns() {
        turns.startMatch();
        turns.startTurn();
        turns.endTurn();
        assertEquals(1, state.getRound());
        turns.endTurn();
        assertEquals(2, state.getRound());
        assertSame(ninja, state.active());
    }

    @Test
    void spentKiIsRefilledNextTurn() {
        turns.startMatch();
        turns.startTurn();
        turns.endTurn();
        turns.endTurn();                         // ninja, max Ki 2
        ninja.spendKi(2);
        assertEquals(0, ninja.getKi());
        turns.endTurn();
        turns.endTurn();                         // ninja, max Ki 3
        assertEquals(3, ninja.getKi());
    }

    @Test
    void maxKiStopsAt12() {
        turns.startMatch();
        turns.startTurn();
        for (int i = 0; i < 40; i++) turns.endTurn();   // 21 turns each
        assertEquals(12, ninja.getMaxKi());
        assertEquals(12, samurai.getMaxKi());
    }

    @Test
    void minionCanAttackFromItsOwnersNextTurn() {
        turns.startMatch();
        turns.startTurn();
        Minion genin = new Minion(GENIN);
        ninja.getSide().place(genin, 0);
        assertFalse(genin.canAttack());          // summoned this turn, no Tatakae
        turns.endTurn();
        assertFalse(genin.canAttack());          // still the opponent's turn
        turns.endTurn();
        assertTrue(genin.canAttack());
    }

    @Test
    void stealthEndsAtOwnersNextTurn() {
        turns.startMatch();
        turns.startTurn();
        ninja.getHero().addStatus(Status.STEALTH);
        turns.endTurn();
        assertTrue(ninja.getHero().hasStatus(Status.STEALTH));
        turns.endTurn();
        assertFalse(ninja.getHero().hasStatus(Status.STEALTH));
    }

    @Test
    void swapCardOncePerTurn() {
        turns.startMatch();
        turns.startTurn();
        Card first = ninja.getHand().get(0);
        assertTrue(turns.swapCard(first));
        assertEquals(7, ninja.getHand().size());
        assertEquals(43, ninja.getDeck().size());
        assertFalse(turns.swapCard(ninja.getHand().get(0)));
        turns.endTurn();
        turns.endTurn();
        assertTrue(turns.swapCard(ninja.getHand().get(0)));
    }

    @Test
    void emptyDeckDrawsNothing() {
        ninja = new Player("Pierre", new Hero(HeroClass.NINJA), deckOf(6));
        state = new GameState(ninja, samurai);
        turns = new TurnManager(state, new Random(1));
        turns.startMatch();
        assertTrue(turns.startTurn().isEmpty());
        assertEquals(6, ninja.getHand().size());
    }

    @Test
    void heroAt0HpEndsTheMatch() {
        turns.startMatch();
        turns.startTurn();
        samurai.getHero().takeDamage(100);
        assertTrue(state.checkGameOver());
        assertSame(ninja, state.getWinner().orElseThrow());
        assertThrows(IllegalStateException.class, turns::endTurn);
    }

    @Test
    void turnBeforeStartIsRefused() {
        assertThrows(IllegalStateException.class, turns::startTurn);
    }
}