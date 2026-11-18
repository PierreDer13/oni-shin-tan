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

/** The 9 stack effects of the GDD Oath table, and what a break takes back. */
class OathCardTest {

    private Player me;        // Samurai, plays first
    private Player enemy;     // Ninja
    private GameEngine engine;
    private OathRewards rewards;

    private static MinionCard minionCard(String id, int atk, int hp, Keyword... keywords) {
        EnumSet<Keyword> set = EnumSet.noneOf(Keyword.class);
        for (Keyword k : keywords) set.add(k);
        return new MinionCard(id, id, "試", "てすと", 1,
                Rarity.COMMON, HeroClass.NEUTRAL, "", "", atk, hp, set);
    }

    private static MinionCard costed(String id, int cost, HeroClass cls, int atk) {
        return new MinionCard(id, id, "試", "", cost, Rarity.COMMON, cls, "", "", atk, 1, null);
    }

    private static Player player(String name, HeroClass cls) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < Deck.SIZE; i++) cards.add(minionCard(name + "-" + i, 1, 1));
        return new Player(name, new Hero(cls), new Deck(cards));
    }

    private static Minion place(Player p, int slot, MinionCard card) {
        Minion m = new Minion(card);
        p.getSide().place(m, slot);
        return m;
    }

    private static TrapCard trap(String id, int cost) {
        return new TrapCard(id, id, "罠", "", cost, Rarity.COMMON, HeroClass.NINJA, "", "", TrapTrigger.ON_ENEMY_SUMMON);
    }

    private static CurseCard curse(String id) {
        return new CurseCard(id, id, "呪", "", 1, Rarity.COMMON, HeroClass.ONI, "", "", CurseZone.HAND, 1, 2);
    }

    /** p's Oath of this type, already at that many stacks (the class is not checked here). */
    private static void sworn(Player p, OathType type, int stacks) {
        Oath oath = new Oath(type);
        for (int i = 0; i < stacks; i++) oath.addStack();
        p.setOath(oath);
    }

    @BeforeEach
    void setUp() {
        me = player("Pierre", HeroClass.SAMURAI);
        enemy = player("Kayne", HeroClass.NINJA);
        engine = new GameEngine(me, enemy, new Random(42));
        engine.startMatch();
        engine.beginFirstTurn();               // my turn 1: 1 Ki
        rewards = engine.getRewards();
    }

    /** p breaks his Oath now (what resolveOath publishes, without the damage and penalty). */
    private void breakOath(Player p) {
        Oath oath = p.getOath();
        oath.breakOath();
        engine.publish(new GameEvent(Type.OATH_BROKEN, p, oath, 0));
    }

    // ---------- Samurai ----------

    @Test
    void honorNineFirstMinionEachTurnGetsFourAttackAndTatakaeUntilTheBreak() {
        sworn(me, OathType.HONOR, 9);
        assertTrue(engine.playCard(me.getHand().get(0), 0, null));
        Minion first = me.getSide().getAt(0);
        assertEquals(7, first.getAttack());                // 1 + Honor 3 aura 2 + 4
        assertTrue(first.canAttack(), "Tatakae");

        me.gainTemporaryKi(1);
        assertTrue(engine.playCard(me.getHand().get(0), 1, null));
        assertEquals(3, me.getSide().getAt(1).getAttack(), "only the first minion of the turn");

        breakOath(me);
        assertEquals(1, first.getAttack());
        assertFalse(first.hasKeyword(Keyword.TATAKAE));
    }

    @Test
    void allegianceNineGivesAllMinionsTwoTwoAndShiroduWhileItHolds() {
        sworn(me, OathType.ALLEGIANCE, 9);
        Minion plain = place(me, 0, minionCard("a", 1, 1));
        Minion wall = place(me, 1, minionCard("w", 1, 1, Keyword.SHIRODU));
        rewards.refreshAuras();
        assertEquals(3, plain.getAttack());
        assertEquals(3, plain.getHealth());
        assertTrue(plain.hasKeyword(Keyword.SHIRODU));

        breakOath(me);
        assertEquals(1, plain.getAttack());
        assertFalse(plain.hasKeyword(Keyword.SHIRODU));
        assertTrue(wall.hasKeyword(Keyword.SHIRODU), "its own Shirodu stays");
    }

    @Test
    void justiceNineOathCardHitsAllEnemyMinionsForTheStrongestAttack() {
        sworn(me, OathType.JUSTICE, 8);
        engine.endTurn();                                  // kept: 9 stacks, the Oath card comes
        assertTrue(me.getHand().contains(OathRewards.JUSTICE_CARD));
        engine.endTurn();                                  // my turn 2

        place(me, 0, minionCard("s", 5, 5));
        Minion e1 = place(enemy, 0, minionCard("e1", 1, 10));
        Minion e2 = place(enemy, 1, minionCard("e2", 1, 10));
        assertTrue(engine.playCard(OathRewards.JUSTICE_CARD, 0, null));   // 0 Ki
        assertEquals(5, e1.getHealth());
        assertEquals(5, e2.getHealth());
    }

    // ---------- Ninja ----------

    @Test
    void silenceNineClonesTheFirstMinionPlayedEachTurnAtOnce() {
        sworn(me, OathType.SILENCE, 9);
        Card played = me.getHand().get(0);
        assertTrue(engine.playCard(played, 3, null));
        assertEquals(2, me.getSide().size());                       // cloned when played, not at the end of the turn
        assertSame(played, me.getSide().getAt(0).getSource());      // the clone takes the leftmost free slot

        me.gainTemporaryKi(5);
        Card second = me.getHand().get(0);
        assertTrue(engine.playCard(second, 5, null));
        assertEquals(3, me.getSide().size());                       // only the first one each turn

        rewards.endTurn(me);
        assertEquals(3, me.getSide().size());                       // nothing more at the end of the turn
    }

    @Test
    void deceptionNineSetsASecondDifferentTrapFromTheDeck() {
        sworn(me, OathType.DECEPTION, 9);
        TrapCard first = trap("t1", 1);
        TrapCard second = trap("t2", 1);
        me.addToHand(first);
        me.getDeck().insertRandom(second, new Random(1));

        assertTrue(engine.playCard(first, 0, null));
        assertEquals(2, me.trapCount());
        assertFalse(me.getDeck().getCards().contains(second));
    }

    @Test
    void disciplineNineGivesOneKiPerCardPlayed() {
        sworn(me, OathType.DISCIPLINE, 9);
        assertTrue(engine.playCard(me.getHand().get(0), 0, null));   // 1 Ki paid, 1 given back
        assertEquals(1, me.getKi());
    }

    // ---------- Kannushi ----------

    @Test
    void purityNineFullyHealsYourMinionsAtTheEndOfTurn() {
        sworn(me, OathType.PURITY, 9);
        Minion m = place(me, 0, minionCard("p", 1, 10));
        m.takeDamage(6);
        rewards.endTurn(me);
        assertEquals(10, m.getHealth());
    }

    @Test
    void peaceNineDeflectsTheFirstHitOnYourHeroEachTurn() {
        sworn(me, OathType.PEACE, 9);
        Minion e1 = place(enemy, 0, minionCard("e1", 3, 5));
        Minion e2 = place(enemy, 1, minionCard("e2", 2, 5));
        engine.endTurn();                                  // enemy turn

        assertTrue(engine.attack(e1, me.getHero()));
        assertEquals(100, me.getHero().getHealth());
        assertEquals(97, enemy.getHero().getHealth());
        assertTrue(engine.attack(e2, me.getHero()));
        assertEquals(98, me.getHero().getHealth(), "only the first hit");
    }

    @Test
    void divinityNineDoublesShrineEffectsWhileItHolds() {
        sworn(me, OathType.DIVINITY, 9);
        assertEquals(2, rewards.shrineFactor(me));
        breakOath(me);
        assertEquals(1, rewards.shrineFactor(me));
    }

    // ---------- Oni ----------

    @Test
    void havocNineWreakHavocHitsEveryEnemyAndLeavesTheHandOnABreak() {
        sworn(me, OathType.HAVOC, 9);
        me.addToHand(OathRewards.WREAK_HAVOC);
        Minion em = place(enemy, 0, minionCard("e", 1, 20));

        assertTrue(engine.playCard(OathRewards.WREAK_HAVOC, 0, null));
        assertEquals(11, em.getHealth());                  // 8 + Havoc 3
        assertEquals(91, enemy.getHero().getHealth());

        me.addToHand(OathRewards.WREAK_HAVOC);
        breakOath(me);
        assertFalse(me.getHand().contains(OathRewards.WREAK_HAVOC));
    }

    @Test
    void descensionNineDoublesTheBiggestMinionAndFollowsANewOne() {
        sworn(me, OathType.DESCENSION, 9);
        Minion big = place(me, 0, minionCard("b", 6, 4));
        Minion small = place(me, 1, minionCard("s", 2, 2));
        rewards.refreshAuras();
        assertEquals(12, big.getAttack());
        assertEquals(8, big.getHealth());
        assertEquals(2, small.getAttack());

        Minion bigger = place(me, 2, minionCard("g", 8, 1));
        rewards.refreshAuras();
        assertEquals(6, big.getAttack());
        assertEquals(4, big.getHealth());
        assertEquals(16, bigger.getAttack());
        assertEquals(2, bigger.getHealth());
    }

    @Test
    void corruptionNineCursesActAtEveryTurnStartAndEnd() {
        sworn(me, OathType.CORRUPTION, 9);
        engine.getCardEffects().register("kuro", Trigger.CURSE,
                ctx -> ctx.engine().dealDamage(null, ctx.owner().getHero(), 3));
        enemy.receiveCurse(curse("kuro"));

        engine.endTurn();                                  // end of my turn, then start of his: 2 × (3 + 1)
        assertEquals(92, enemy.getHero().getHealth());
    }
}