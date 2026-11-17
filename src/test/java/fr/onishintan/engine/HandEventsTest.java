package fr.onishintan.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.onishintan.model.*;

/** Cards an effect puts in the hand publish CARD_ADDED (or CARD_BURNED when full); bonusOf gives the green number of a hand card. */
class HandEventsTest {

    private Player me;        // Samurai, plays first
    private Player enemy;     // Ninja
    private GameEngine engine;
    private final List<GameEvent> added = new ArrayList<>();
    private final List<GameEvent> drawn = new ArrayList<>();
    private final List<GameEvent> burned = new ArrayList<>();

    private static MinionCard card(String id, int atk, int hp, String text) {
        return new MinionCard(id, id, "試", "", 1, Rarity.COMMON, HeroClass.NEUTRAL, text, "", atk, hp, EnumSet.noneOf(Keyword.class));
    }

    private static Player player(String name, HeroClass cls) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < Deck.SIZE; i++) cards.add(card(name + "-" + i, 1, 1, ""));
        return new Player(name, new Hero(cls), new Deck(cards));
    }

    private static void sworn(Player p, OathType type, int stacks) {
        Oath oath = new Oath(type);
        for (int i = 0; i < stacks; i++) oath.addStack();
        p.setOath(oath);
    }

    private boolean play(MinionCard c, int slot) {
        me.addToHand(c);
        return engine.playCard(c, slot, null);
    }

    @BeforeEach
    void setUp() {
        me = player("Pierre", HeroClass.SAMURAI);
        enemy = player("Kayne", HeroClass.NINJA);
        engine = new GameEngine(me, enemy, new Random(42));
        engine.setCatalog(List.of(card("koi", 2, 2, "Seppuku: draw a card"), card("sakana-uri", 5, 5, "Banzai: add a Koi to your hand")));
        engine.startMatch();
        engine.beginFirstTurn();
        me.gainTemporaryKi(30);
        engine.getBus().subscribe(GameEvent.Type.CARD_ADDED, added::add);
        engine.getBus().subscribe(GameEvent.Type.CARD_DRAWN, drawn::add);
        engine.getBus().subscribe(GameEvent.Type.CARD_BURNED, burned::add);
    }

    @Test
    void sakanaUriPublishesTheKoiItAdds() {
        assertTrue(play(card("sakana-uri", 5, 5, ""), 0));
        assertEquals(1, added.size());
        assertSame(me, added.get(0).source());
        assertEquals("koi", ((Card) added.get(0).target()).getId());
        assertTrue(me.getHand().contains((Card) added.get(0).target()));
    }

    @Test
    void koiDyingPublishesItsDrawMidTurn() {
        assertTrue(play(card("koi", 2, 2, ""), 0));
        int hand = me.getHand().size();
        engine.destroy(me.getSide().getAt(0));
        assertEquals(1, drawn.size(), "the Seppuku draw is published during the turn");
        assertSame(me, drawn.get(0).source());
        assertEquals(hand + 1, me.getHand().size());
    }

    @Test
    void aFullHandBurnsTheAddedCard() {
        while (me.getHand().size() < Player.MAX_HAND) me.addToHand(card("filler", 1, 1, ""));
        assertFalse(engine.addToHand(me, card("koi", 2, 2, "")));
        assertEquals(1, burned.size());
        assertEquals(Player.MAX_HAND, me.getHand().size());
    }

    @Test
    void noBonusWithoutANumber() {
        assertEquals(0, engine.bonusOf(me, card("x", 1, 1, "Banzai: draw a card")));
        me.getSide().place(new Minion(card(GameEngine.RYU_ID, 12, 12, "")), 3);
        assertEquals(0, engine.bonusOf(me, card("x", 1, 1, "Shinobi")));
    }

    @Test
    void ryuAddsFourToDamageAndHealing() {
        MinionCard bolt = card("bolt", 1, 1, "Banzai: deal 4 damage to a minion");
        MinionCard balm = card("balm", 1, 1, "Seppuku: heal your hero 8");
        assertEquals(0, engine.bonusOf(me, bolt));
        me.getSide().place(new Minion(card(GameEngine.RYU_ID, 12, 12, "")), 3);
        assertEquals(GameEngine.RYU_BONUS, engine.bonusOf(me, bolt));
        assertEquals(GameEngine.RYU_BONUS, engine.bonusOf(me, balm));
        assertEquals(0, engine.bonusOf(enemy, bolt), "Ryu is on my side only");
    }

    @Test
    void oathBonuses() {
        MinionCard bolt = card("bolt", 1, 1, "Banzai: deal 4 damage");
        MinionCard balm = card("balm", 1, 1, "Banzai: heal all friendly characters for 1");
        sworn(me, OathType.HAVOC, 3);
        assertEquals(1, engine.bonusOf(me, bolt));
        assertEquals(0, engine.bonusOf(me, balm));
        sworn(me, OathType.PURITY, 3);
        assertEquals(OathRewards.PURITY_HEAL_BONUS, engine.bonusOf(me, balm));
        assertEquals(0, engine.bonusOf(me, bolt));
    }

    @Test
    void corruptionCountsOnCursesOnly() {
        CurseCard tatari = new CurseCard("tatari", "Tatari", "祟り", "", 3, Rarity.COMMON, HeroClass.ONI,
                "Hand (removal 3 Ki): end of their turn, 8 damage to their hero", "", CurseZone.HAND, 1, 3);
        sworn(me, OathType.CORRUPTION, 3);
        assertEquals(1, engine.bonusOf(me, tatari));
        assertEquals(0, engine.bonusOf(me, card("bolt", 1, 1, "Banzai: deal 4 damage")));
    }
}