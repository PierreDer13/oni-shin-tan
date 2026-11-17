package fr.onishintan.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.onishintan.ai.AIController;
import fr.onishintan.model.*;

/** The mulligan: once per player, before the first turn, chosen cards replaced by new ones. */
class MulliganTest {

    private Player me;        // plays first, 4 cards
    private Player enemy;     // 5 cards
    private GameEngine engine;
    private final List<GameEvent> events = new ArrayList<>();

    private static MinionCard minionCard(String id, int cost) {
        return new MinionCard(id, id, "試", "てすと", cost, Rarity.COMMON, HeroClass.NEUTRAL, "", "", 2, 2,
                EnumSet.noneOf(Keyword.class));
    }

    private static Player player(String name, HeroClass cls) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < Deck.SIZE; i++) cards.add(minionCard(name + "-" + i, 1 + i % 8));
        return new Player(name, new Hero(cls), new Deck(cards));
    }

    @BeforeEach
    void setUp() {
        me = player("Pierre", HeroClass.SAMURAI);
        enemy = player("Kayne", HeroClass.NINJA);
        engine = new GameEngine(me, enemy, new Random(42));
        engine.addListener((e, s) -> events.add(e));
        engine.startMatch();
    }

    @Test
    void chosenCardsAreReplacedAndGoBackIntoTheDeck() {
        List<Card> hand = new ArrayList<>(me.getHand());
        assertEquals(4, hand.size());
        List<Card> back = List.of(hand.get(0), hand.get(2));
        List<Card> fresh = engine.mulligan(me, back);

        assertEquals(2, fresh.size());
        assertEquals(4, me.getHand().size());
        assertTrue(me.getHand().containsAll(fresh));
        assertTrue(me.getHand().contains(hand.get(1)));
        assertTrue(me.getHand().contains(hand.get(3)));
        for (Card c : back) {
            assertFalse(me.getHand().contains(c), "sent back, not drawn again");
            assertTrue(me.getDeck().getCards().contains(c));
        }
        assertEquals(Deck.SIZE - 4, me.getDeck().size());

        GameEvent e = events.get(events.size() - 1);
        assertEquals(GameEvent.Type.MULLIGAN_DONE, e.type());
        assertSame(me, e.source());
        assertEquals(2, e.amount());
        assertEquals(fresh, e.target());
    }

    @Test
    void onlyOncePerPlayerAndBeforeTheFirstTurn() {
        assertFalse(engine.hasMulliganed(me));
        assertTrue(engine.mulligan(me, List.of()).isEmpty());          // keep the hand: counts as done
        assertTrue(engine.hasMulliganed(me));
        assertTrue(engine.mulligan(me, List.of(me.getHand().get(0))).isEmpty());

        engine.beginFirstTurn();
        Card c = enemy.getHand().get(0);
        assertTrue(engine.mulligan(enemy, List.of(c)).isEmpty());
        assertTrue(enemy.getHand().contains(c));
    }

    @Test
    void cardsNotInTheHandAreIgnored() {
        Card stranger = minionCard("stranger", 3);
        Card mine = enemy.getHand().get(0);
        List<Card> fresh = engine.mulligan(enemy, List.of(stranger, mine, mine));
        assertEquals(1, fresh.size());
        assertEquals(5, enemy.getHand().size());
    }

    @Test
    void theAiSendsBackItsExpensiveCards() {
        AIController ai = new AIController(engine, enemy, AIController.Difficulty.NORMAL, new Random(1));
        List<Card> back = ai.mulliganChoice();
        for (Card c : enemy.getHand()) assertEquals(engine.costOf(enemy, c) > AIController.MULLIGAN_MAX_KI, back.contains(c));
        engine.mulligan(enemy, back);
        assertEquals(5, enemy.getHand().size());
    }
}