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

/** GameEngine.playCard: minions, weapons, Ki and board checks, events. */
class PlayCardTest {

    private Player me;        // active player (first), 1 Ki on turn 1
    private Player enemy;
    private GameEngine engine;
    private final List<GameEvent> events = new ArrayList<>();

    // ---------- helpers ----------

    private static MinionCard minionCard(String id, int ki, int atk, int hp) {
        return new MinionCard(id, id, "試", "てすと", ki,
                Rarity.COMMON, HeroClass.NEUTRAL, "", "", atk, hp, EnumSet.noneOf(Keyword.class));
    }

    private static Player player(String name, HeroClass cls) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < Deck.SIZE; i++) cards.add(minionCard(name + "-" + i, 1, 1, 1));
        return new Player(name, new Hero(cls), new Deck(cards));
    }

    private List<Type> types() {
        return events.stream().map(GameEvent::type).toList();
    }

    @BeforeEach
    void setUp() {
        me = player("Pierre", HeroClass.SAMURAI);
        enemy = player("Kayne", HeroClass.NINJA);
        engine = new GameEngine(me, enemy, new Random(42));
        engine.startMatch();
        engine.beginFirstTurn();               // turn 1: me active, 1 Ki
        engine.addListener((e, s) -> events.add(e));
    }

    // ---------- minions ----------

    @Test
    void playingAMinionSpendsKiAndPutsItOnTheBoard() {
        Card card = me.getHand().get(0);       // a 1-Ki 1/1 from the test deck
        int handBefore = me.getHand().size();

        assertTrue(engine.playCard(card, 0, null));

        assertEquals(handBefore - 1, me.getHand().size());
        assertFalse(me.getHand().contains(card));
        assertEquals(1, me.getSide().size());
        assertFalse(me.canAfford(1));          // the only Ki is spent
        assertTrue(types().indexOf(Type.CARD_PLAYED) < types().indexOf(Type.MINION_SUMMONED));
    }

    @Test
    void notEnoughKiIsRefused() {
        assertTrue(engine.playCard(me.getHand().get(0), 0, null));   // spends the 1 Ki
        Card second = me.getHand().get(0);
        int handBefore = me.getHand().size();
        events.clear();

        assertFalse(engine.playCard(second, 1, null));

        assertEquals(handBefore, me.getHand().size());
        assertEquals(1, me.getSide().size());
        assertTrue(events.isEmpty());
    }

    @Test
    void fullBoardIsRefusedWithoutSpendingKi() {
        for (int i = 0; i < BoardSide.MAX; i++) {
            me.getSide().place(new Minion(minionCard("filler-" + i, 1, 1, 1)), i);
        }
        Card card = me.getHand().get(0);

        assertFalse(engine.playCard(card, 0, null));

        assertTrue(me.canAfford(1));           // Ki not spent
        assertTrue(me.getHand().contains(card));
        assertTrue(events.isEmpty());
    }

    @Test
    void cardNotInHandIsRefused() {
        assertFalse(engine.playCard(minionCard("ghost", 1, 1, 1), 0, null));
        assertEquals(0, me.getSide().size());
    }

    // ---------- weapons ----------

    @Test
    void playingAWeaponEquipsTheHero() {
        WeaponCard bokken = new WeaponCard("bokken", "Bokken", "木刀", "ぼっけん", 1,
                Rarity.COMMON, HeroClass.SAMURAI, "", "", 3, 1);
        me.addToHand(bokken);
        
        assertTrue(engine.playCard(bokken, -1, null));

        assertNotNull(me.getHero().getWeapon());
        assertFalse(me.getHand().contains(bokken));
        assertTrue(types().contains(Type.WEAPON_EQUIPPED));
    }
}