package fr.onishintan.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.onishintan.engine.GameEvent.Type;
import fr.onishintan.model.*;

/** Oaths: swearing, stacks and rewards, breaking, Oathbreaker, re-swearing. */
class OathTest {

    private Player me;        // Samurai, plays first
    private Player enemy;     // Ninja
    private GameEngine engine;
    private final List<GameEvent> events = new ArrayList<>();

    private static MinionCard minionCard(String id) {
        return new MinionCard(id, id, "試", "てすと", 1,
                Rarity.COMMON, HeroClass.NEUTRAL, "", "", 1, 1, EnumSet.noneOf(Keyword.class));
    }

    private static Player player(String name, HeroClass cls) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < Deck.SIZE; i++) cards.add(minionCard(name + "-" + i));
        return new Player(name, new Hero(cls), new Deck(cards));
    }

    /** Ends my turn and the enemy's: back to me. */
    private void fullRound() {
        engine.endTurn();
        engine.endTurn();
    }

    private void putTwoMinions() {
        me.getSide().place(new Minion(minionCard("a")), 0);
        me.getSide().place(new Minion(minionCard("b")), 1);
    }

    @BeforeEach
    void setUp() {
        me = player("Pierre", HeroClass.SAMURAI);
        enemy = player("Kayne", HeroClass.NINJA);
        engine = new GameEngine(me, enemy, new Random(42));
        engine.startMatch();
        engine.addListener((e, s) -> events.add(e));
    }

    @Test
    void anOathOfAnotherClassCannotBeSworn() {
        assertFalse(engine.swearOath(me, OathType.SILENCE));
        assertTrue(engine.swearOath(me, OathType.ALLEGIANCE));
        assertFalse(engine.swearOath(me, OathType.HONOR));   // only one Oath
    }

    @Test
    void keepingTheOathGivesOneStackPerTurnAndARewardAtThree() {
        engine.swearOath(me, OathType.ALLEGIANCE);
        engine.beginFirstTurn();
        putTwoMinions();

        for (int i = 0; i < 3; i++) fullRound();

        assertEquals(3, me.getOath().getStacks());
        assertTrue(events.stream().anyMatch(e -> e.type() == Type.OATH_REWARD && e.amount() == 3));
    }

    @Test
    void breakingDealsBasePlusStacksAndStartsOathbreaker() {
        engine.swearOath(me, OathType.ALLEGIANCE);
        engine.beginFirstTurn();               // 1 Ki, 1-Ki minions in hand, empty board

        engine.endTurn();                      // could have played a minion: broken

        Oath oath = me.getOath();
        assertEquals(Oath.State.OATHBREAKER, oath.getState());
        assertEquals(0, oath.getStacks());
        GameEvent broken = events.stream().filter(e -> e.type() == Type.OATH_BROKEN).findFirst().orElseThrow();
        assertEquals(OathType.ALLEGIANCE.getBreakBase(), broken.amount());
    }

    @Test
    void oathbreakerLastsTwoOwnTurnsThenTheOathCanBeSwornAgain() {
        engine.swearOath(me, OathType.ALLEGIANCE);
        engine.beginFirstTurn();
        engine.endTurn();                      // break
        engine.endTurn();                      // enemy

        assertFalse(engine.reswearOath(me));
        fullRound();                           // Oathbreaker 1
        fullRound();                           // Oathbreaker 2

        assertEquals(Oath.State.LAPSED, me.getOath().getState());
        assertTrue(engine.reswearOath(me));
        assertEquals(0, me.getOath().getStacks());
    }

    @Test
    void unableToKeepGivesNoStackButTheOathHolds() {
        engine.swearOath(me, OathType.ALLEGIANCE);
        engine.beginFirstTurn();
        me.spendKi(me.getKi());                // no Ki left: no minion could be played

        engine.endTurn();

        assertTrue(me.getOath().isSworn());
        assertEquals(0, me.getOath().getStacks());
    }

    @Test
    void stacksStopAtNineAndANinjaOathShowsAtThree() {
        Oath oath = new Oath(OathType.SILENCE);
        assertFalse(oath.isRevealed());
        for (int i = 0; i < 12; i++) oath.addStack();
        assertEquals(Oath.MAX_STACKS, oath.getStacks());
        assertTrue(oath.isRevealed());
    }
}