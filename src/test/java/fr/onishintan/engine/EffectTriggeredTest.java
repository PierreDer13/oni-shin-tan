package fr.onishintan.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.onishintan.model.*;

/** EFFECT_TRIGGERED is published just before a triggered effect acts, with what triggered it as source. */
class EffectTriggeredTest {

    private Player me;        // Samurai, plays first
    private Player enemy;     // Ninja
    private GameEngine engine;
    private final List<GameEvent> log = new ArrayList<>();

    private static MinionCard card(String id, int atk, int hp) {
        return new MinionCard(id, id, "試", "", 1, Rarity.COMMON, HeroClass.NEUTRAL, "", "", atk, hp, EnumSet.noneOf(Keyword.class));
    }

    private static Player player(String name, HeroClass cls) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < Deck.SIZE; i++) cards.add(card(name + "-" + i, 1, 1));
        return new Player(name, new Hero(cls), new Deck(cards));
    }

    private boolean play(Card c, int slot) {
        me.addToHand(c);
        return engine.playCard(c, slot, null);
    }

    private int indexOf(GameEvent.Type type, int from) {
        for (int i = from; i < log.size(); i++) if (log.get(i).type() == type) return i;
        return -1;
    }

    private List<GameEvent> triggered() {
        return log.stream().filter(e -> e.type() == GameEvent.Type.EFFECT_TRIGGERED).toList();
    }

    @BeforeEach
    void setUp() {
        me = player("Pierre", HeroClass.SAMURAI);
        enemy = player("Kayne", HeroClass.NINJA);
        engine = new GameEngine(me, enemy, new Random(42));
        engine.setCatalog(List.of(card("koi", 2, 2), card("sakana-uri", 5, 5)));
        engine.startMatch();
        engine.beginFirstTurn();
        me.gainTemporaryKi(30);
        engine.addListener((e, s) -> log.add(e));
    }

    @Test
    void seppukuIsAnnouncedAfterTheDeathAndBeforeItsDraw() {
        assertTrue(play(card("koi", 2, 2), 0));
        Minion koi = me.getSide().getAt(0);
        log.clear();
        engine.destroy(koi);
        int died = indexOf(GameEvent.Type.MINION_DIED, 0);
        int fx = indexOf(GameEvent.Type.EFFECT_TRIGGERED, 0);
        int draw = indexOf(GameEvent.Type.CARD_DRAWN, 0);
        assertTrue(died >= 0 && died < fx && fx < draw, "MINION_DIED, then EFFECT_TRIGGERED, then the draw");
        assertSame(koi, log.get(fx).source());
        assertSame(me, log.get(fx).target());
    }

    @Test
    void aBanzaiIsNotAnnounced() {
        assertTrue(play(card("sakana-uri", 5, 5), 0));
        assertTrue(triggered().isEmpty(), "CARD_PLAYED already announces a Banzai");
    }

    @Test
    void aMinionWithoutEffectIsNotAnnounced() {
        assertTrue(play(card("plain", 1, 1), 0));
        engine.destroy(me.getSide().getAt(0));
        engine.endTurn();
        assertTrue(triggered().isEmpty());
    }

    @Test
    void heiwaNoToriiIsAnnouncedBeforeItsEndOfTurnHeal() {
        assertTrue(play(new ShrineCard("heiwa-no-torii", "Heiwa", "社", "", 1, Rarity.COMMON, HeroClass.KANNUSHI, "", "", 2), 0));
        assertTrue(play(card("plain", 1, 5), 1));
        Minion m = me.getSide().getAt(1);
        m.takeDamage(2);
        log.clear();
        engine.endTurn();
        int fx = indexOf(GameEvent.Type.EFFECT_TRIGGERED, 0);
        assertTrue(fx >= 0);
        assertTrue(log.get(fx).source() instanceof Shrine);
        assertTrue(fx < indexOf(GameEvent.Type.HEAL, 0));
    }

    @Test
    void anOathRewardIsAnnouncedWithItsOath() {
        Oath peace = new Oath(OathType.PEACE);
        for (int i = 0; i < 6; i++) peace.addStack();
        me.setOath(peace);
        me.getHero().takeDamage(10);
        log.clear();
        engine.endTurn();
        int fx = indexOf(GameEvent.Type.EFFECT_TRIGGERED, 0);
        assertTrue(fx >= 0);
        assertSame(peace, log.get(fx).source());
        assertSame(me, log.get(fx).target());
        assertTrue(fx < indexOf(GameEvent.Type.HEAL, 0), "the banner comes before the heal");
    }

    // ---------- fatigue (decided 18 Nov) ----------

    private void emptyDeck(Player p) {
        while (!p.getDeck().isEmpty()) p.getDeck().draw();
    }

    @Test
    void fatigueDeals1Then2Then3() {
        emptyDeck(enemy);
        int hp = enemy.getHero().getHealth();
        log.clear();
        engine.endTurn();                                  // the enemy draws from an empty deck
        assertEquals(hp - 1, enemy.getHero().getHealth());
        int f = indexOf(GameEvent.Type.FATIGUE, 0);
        assertTrue(f >= 0 && f < indexOf(GameEvent.Type.DAMAGE, f));
        assertEquals(1, log.get(f).amount());
        assertSame(enemy, log.get(f).source());
        assertEquals(2, engine.nextFatigue(enemy));
        engine.endTurn();
        engine.endTurn();
        assertEquals(hp - 3, enemy.getHero().getHealth(), "then 2");
        engine.endTurn();
        engine.endTurn();
        assertEquals(hp - 6, enemy.getHero().getHealth(), "then 3");
    }

    @Test
    void fatigueCanEndTheMatch() {
        emptyDeck(enemy);
        enemy.getHero().takeDamage(enemy.getHero().getHealth() - 1);
        engine.endTurn();
        assertTrue(engine.getState().isOver());
        assertSame(me, engine.getState().getWinner().orElseThrow());
    }

    @Test
    void aFullHandWithCardsLeftStillBurnsWithoutFatigue() {
        while (enemy.getHand().size() < Player.MAX_HAND) enemy.addToHand(card("filler", 1, 1));
        int hp = enemy.getHero().getHealth();
        log.clear();
        engine.endTurn();
        assertEquals(hp, enemy.getHero().getHealth());
        assertTrue(indexOf(GameEvent.Type.CARD_BURNED, 0) >= 0);
        assertEquals(-1, indexOf(GameEvent.Type.FATIGUE, 0));
    }
}