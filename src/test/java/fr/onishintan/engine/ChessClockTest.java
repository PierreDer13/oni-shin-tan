package fr.onishintan.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import fr.onishintan.model.*;

/** Chess clock: 7 min bank, +2 s per turn, timeout = half HP, +30 s, turn over. */
class ChessClockTest {

    private final long[] time = {0};
    private Player me;
    private Player enemy;
    private GameEngine engine;

    private static Player player(String name) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < Deck.SIZE; i++) {
            cards.add(new MinionCard(name + "-" + i, "m", "試", "てすと", 1, Rarity.COMMON,
                    HeroClass.NEUTRAL, "", "", 1, 1, EnumSet.noneOf(Keyword.class)));
        }
        return new Player(name, new Hero(HeroClass.NINJA), new Deck(cards));
    }

    private void start() {
        me = player("Pierre");
        enemy = player("Kayne");
        engine = new GameEngine(me, enemy, new Random(42));
        engine.setTimeSource(() -> time[0]);
        engine.startMatch();
        engine.beginFirstTurn();
    }

    @Test
    void clockOnlyRunsWhenStarted() {
        ChessClock c = new ChessClock();
        assertEquals(ChessClock.BANK_MS, c.remaining(5_000));
        c.start(1_000);
        assertEquals(ChessClock.BANK_MS - 4_000, c.remaining(5_000));
        c.pause(5_000);
        assertEquals(ChessClock.BANK_MS - 4_000, c.remaining(60_000));
    }

    @Test
    void endTurnAddsTwoSeconds() {
        start();
        time[0] = 10_000;
        engine.endTurn();
        assertEquals(ChessClock.BANK_MS - 10_000 + ChessClock.INCREMENT_MS, engine.remainingMs(me));
    }

    @Test
    void onlyTheActiveClockRuns() {
        start();
        engine.endTurn();                       // at t = 0, enemy's turn starts
        time[0] = 20_000;
        assertEquals(ChessClock.BANK_MS + ChessClock.INCREMENT_MS, engine.remainingMs(me));
        assertEquals(ChessClock.BANK_MS - 20_000, engine.remainingMs(enemy));
    }

    @Test
    void timeoutTakesHalfHpAddsThirtySecondsAndEndsTurn() {
        start();
        assertFalse(engine.checkClock());
        int hp = me.getHero().getHealth();
        time[0] = ChessClock.BANK_MS;
        assertTrue(engine.checkClock());
        assertEquals(hp - (hp + 1) / 2, me.getHero().getHealth());
        assertEquals(ChessClock.TIMEOUT_BONUS_MS + ChessClock.INCREMENT_MS, engine.remainingMs(me));
        assertEquals(ChessClock.BANK_MS, engine.remainingMs(enemy));   // his turn, t = 0 for him
    }
}