package fr.onishintan.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.onishintan.engine.event.GameEvent.Type;
import fr.onishintan.model.*;

import fr.onishintan.engine.event.GameEvent;
class GameEngineTest {

    private Player first;
    private Player second;
    private GameEngine engine;
    private final List<GameEvent> events = new ArrayList<>();

    private static Card card(String owner, int i) {
        return new MinionCard(owner + "-" + i, "Test " + i, "試", "てすと", 1,
                Rarity.COMMON, HeroClass.NEUTRAL, "", "",
                4, 4, EnumSet.noneOf(Keyword.class));
    }

    private static Player player(String name, HeroClass cls) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < Deck.SIZE; i++) cards.add(card(name, i));
        return new Player(name, new Hero(cls), new Deck(cards));
    }

    private List<Type> types() {
        return events.stream().map(GameEvent::type).toList();
    }

    @BeforeEach
    void setUp() {
        first = player("Pierre", HeroClass.SAMURAI);
        second = player("Kayne", HeroClass.NINJA);
        engine = new GameEngine(first, second, new Random(42));
        engine.addListener((e, s) -> events.add(e));
    }

    @Test
    void startMatchPublishesMatchStarted() {
        engine.startMatch();
        assertEquals(List.of(Type.MATCH_STARTED), types());
    }

    @Test
    void firstTurnPublishesDrawThenTurnStarted() {
        engine.startMatch();
        events.clear();
        engine.beginFirstTurn();
        assertEquals(List.of(Type.CARD_DRAWN, Type.TURN_STARTED), types());
        assertSame(first, engine.active());
    }

    @Test
    void endTurnPublishesEndDrawStart() {
        engine.startMatch();
        engine.beginFirstTurn();
        events.clear();
        engine.endTurn();
        assertEquals(List.of(Type.TURN_ENDED, Type.CARD_DRAWN, Type.TURN_STARTED), types());
        assertSame(second, engine.active());
    }

    @Test
    void tradePublishesCardTradedOnlyWhenAllowed() {
        engine.startMatch();
        engine.beginFirstTurn();
        events.clear();
        assertTrue(engine.trade(first.getHand().get(0)));
        assertFalse(engine.trade(first.getHand().get(0)));   // once per turn
        assertEquals(List.of(Type.CARD_TRADED), types());
    }

    @Test
    void canPlayChecksHandAndKi() {
        engine.startMatch();
        engine.beginFirstTurn();                              // 1 Ki, all cards cost 1
        assertTrue(engine.canPlay(first.getHand().get(0)));
        assertFalse(engine.canPlay(second.getHand().get(0))); // not my card
        assertFalse(engine.canPlay(null));
    }

    @Test
    void attackWithNothingIsRefused() {
        engine.startMatch();
        engine.beginFirstTurn();
        assertFalse(engine.attack(null, null));
        assertFalse(engine.useHeroPower(null));   // no target: refused, nothing paid   
        }

    @Test
    void bothHeroesDeadIsADraw() {
        engine.startMatch();
        engine.beginFirstTurn();
        first.getHero().takeDamage(Hero.START_HEALTH);
        second.getHero().takeDamage(Hero.START_HEALTH);

        engine.checkGameOver();
        GameEvent last = events.get(events.size() - 1);
        assertEquals(Type.GAME_OVER, last.type());
        assertNull(last.target());                            // draw
    }

    @Test
    void oneHeroDeadGivesTheOtherTheWin() {
        engine.startMatch();
        engine.beginFirstTurn();
        second.getHero().takeDamage(Hero.START_HEALTH);

        engine.checkGameOver();
        GameEvent last = events.get(events.size() - 1);
        assertEquals(Type.GAME_OVER, last.type());
        assertSame(first, last.target());
    }
    @Test
    void concedeGivesTheOpponentTheWin() {
        engine.startMatch();
        engine.beginFirstTurn();
        events.clear();

        assertTrue(engine.concede(first));
        assertEquals(List.of(Type.GAME_OVER), types());
        assertSame(first, events.get(0).source());            // who gave up
        assertSame(second, events.get(0).target());           // who won
        assertSame(second, engine.getState().getWinner().orElseThrow());
    }

    @Test
    void concedeTwiceIsRefused() {
        engine.startMatch();
        engine.beginFirstTurn();
        engine.concede(first);
        events.clear();

        assertFalse(engine.concede(second));
        assertTrue(events.isEmpty());
        assertSame(second, engine.getState().getWinner().orElseThrow());
    }

    @Test
    void concedeStopsBothClocks() {
        long[] now = {0};
        engine.setTimeSource(() -> now[0]);
        engine.startMatch();
        engine.swearOath(first, OathType.HONOR);
        engine.swearOath(second, OathType.SILENCE);
        engine.beginFirstTurn();
        now[0] = 10_000;
        assertTrue(first.getClock().isRunning());

        engine.concede(first);
        assertFalse(first.getClock().isRunning());
        assertFalse(second.getClock().isRunning());

        engine.resumeClock();                                 // the veil closes after the match
        assertFalse(first.getClock().isRunning());
        assertEquals(ChessClock.BANK_MS - 10_000, engine.remainingMs(first));
    }

}