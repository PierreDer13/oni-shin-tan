package fr.onishintan.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.onishintan.model.*;

import fr.onishintan.engine.event.GameEvent;
import fr.onishintan.engine.turn.TurnManager;
/** Yamata no Orochi as a boss: full opening hand, his mythic every 5 turns from turn 10, Yatsu-kubi. */
class OrochiBossTest {

    private Player me;
    private Player orochi;
    private GameEngine engine;
    private final List<GameEvent> heard = new ArrayList<>();

    private static final MinionCard MYTHIC = new MinionCard(GameEngine.OROCHI_MYTHIC_ID, "Yamata no Orochi", "八岐大蛇", "",
            10, Rarity.MYTHIC, HeroClass.NEUTRAL, "", "", 14, 18, EnumSet.of(Keyword.BANZAI));

    private static MinionCard card(String id, int atk, int hp) {
        return new MinionCard(id, id, "試", "", 1, Rarity.COMMON, HeroClass.NEUTRAL, "", "", atk, hp, EnumSet.noneOf(Keyword.class));
    }

    private static Deck deck(String name, int size) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < size; i++) cards.add(card(name + "-" + i, 1, 1));
        return new Deck(cards);
    }

    @BeforeEach
    void setUp() {
        me = new Player("Pierre", new Hero(HeroClass.SAMURAI), deck("me", Deck.SIZE));
        orochi = new Player("Yamata no Orochi", new Hero(HeroClass.OROCHI, 200), deck("orochi", 100));
        engine = new GameEngine(me, orochi, new Random(7));
        engine.setCatalog(List.of(MYTHIC));
        for (GameEvent.Type t : List.of(GameEvent.Type.CARD_PLAYED, GameEvent.Type.MINION_SUMMONED, GameEvent.Type.MINION_REMOVED)) {
            engine.getBus().subscribe(t, heard::add);
        }
        engine.startMatch();
        engine.beginFirstTurn();
    }

    /** Ends turns until it is Orochi's turn of that round. */
    private void toOrochiTurn(int round) {
        while (!(engine.getState().getRound() == round && engine.getState().active() == orochi)) engine.endTurn();
    }

    private long mythics() {
        return orochi.getSide().getMinions().stream().filter(m -> GameEngine.OROCHI_MYTHIC_ID.equals(m.getSource().getId())).count();
    }

    @Test
    void orochiStartsWithAFullHand() {
        assertEquals(Player.MAX_HAND, orochi.getHand().size());
        assertEquals(TurnManager.OPENING_HAND_FIRST + 1, me.getHand().size());   // my first turn's draw
    }

    @Test
    void mythicTurnsAreTenFifteenTwenty() {
        for (int r = 1; r < 30; r++) {
            assertEquals(r >= 10 && r % 5 == 0, GameEngine.isOrochiMythicTurn(r), "round " + r);
        }
    }

    @Test
    void mythicArrivesOnTurnTenPaysTenKiAndItsBanzaiHitsEveryEnemy() {
        toOrochiTurn(9);
        assertEquals(0, mythics(), "not before turn 10");
        Minion mine = new Minion(card("mine", 1, 20));
        me.getSide().place(mine, 0);
        int handBefore = orochi.getHand().size();

        toOrochiTurn(10);
        assertEquals(1, mythics());
        assertEquals(0, orochi.getKi(), "it took his 10 Ki");
        assertEquals(handBefore, orochi.getHand().size(), "it never went through his hand");
        assertEquals(Hero.START_HEALTH - 4, me.getHero().getHealth());
        assertEquals(16, mine.getHealth());
        assertTrue(heard.stream().anyMatch(e -> e.type() == GameEvent.Type.CARD_PLAYED && e.source() == orochi
                && e.target() == MYTHIC && e.amount() == 10));
        assertTrue(heard.stream().anyMatch(e -> e.type() == GameEvent.Type.MINION_SUMMONED && e.source() == orochi));
    }

    @Test
    void mythicComesWithoutKiAndEvenWhenOneIsAlreadyThere() {
        toOrochiTurn(10);
        assertEquals(0, orochi.getKi());
        engine.orochiMythic(orochi);                 // same turn, no Ki left
        assertEquals(2, mythics());
        assertTrue(heard.stream().anyMatch(e -> e.type() == GameEvent.Type.CARD_PLAYED && e.target() == MYTHIC && e.amount() == 0));
    }

    @Test
    void mythicReplacesTheWeakestMinionOnAFullSide() {
        toOrochiTurn(9);
        Minion weakest = null;
        for (int slot = 0; slot < BoardSide.MAX; slot++) {
            Minion m = new Minion(card("o" + slot, 3, slot == 4 ? 1 : 5));
            if (slot == 4) weakest = m;
            orochi.getSide().place(m, slot);
        }
        toOrochiTurn(10);
        assertEquals(1, mythics());
        assertEquals(-1, orochi.getSide().slotOf(weakest));
        assertEquals(GameEngine.OROCHI_MYTHIC_ID, orochi.getSide().getAt(4).getSource().getId());
        final Minion gone = weakest;
        assertTrue(heard.stream().anyMatch(e -> e.type() == GameEvent.Type.MINION_REMOVED && e.target() == gone));
    }

    @Test
    void mythicHealsEightAtTheEndOfHisTurn() {
        toOrochiTurn(10);
        Minion dragon = orochi.getSide().getMinions().stream()
                .filter(m -> GameEngine.OROCHI_MYTHIC_ID.equals(m.getSource().getId())).findFirst().orElseThrow();
        dragon.takeDamage(10);
        engine.endTurn();
        assertEquals(16, dragon.getHealth());
    }

    @Test
    void noMythicForAnyoneElse() {
        toOrochiTurn(10);
        assertTrue(me.getSide().getMinions().stream().noneMatch(m -> GameEngine.OROCHI_MYTHIC_ID.equals(m.getSource().getId())));
        engine.endTurn();                            // my turn 11, then...
        engine.orochiMythic(me);
        assertTrue(me.getSide().getMinions().isEmpty());
    }

    @Test
    void yatsuKubiDealsEightDamageSpreadOverTheEnemies() {
        toOrochiTurn(3);
        Minion a = new Minion(card("a", 1, 20)), b = new Minion(card("b", 1, 20));
        me.getSide().place(a, 0);
        me.getSide().place(b, 1);
        int kiBefore = orochi.getKi();
        assertTrue(engine.useHeroPower(null));
        assertEquals(kiBefore - GameEngine.HERO_POWER_COST, orochi.getKi());
        int dealt = (Hero.START_HEALTH - me.getHero().getHealth()) + (20 - a.getHealth()) + (20 - b.getHealth());
        assertEquals(GameEngine.YATSU_KUBI_HITS, dealt);
        assertFalse(engine.useHeroPower(null), "once per turn");
    }
}
