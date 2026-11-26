package fr.onishintan.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.onishintan.model.*;
import fr.onishintan.engine.effect.Trigger;
import fr.onishintan.engine.oath.OathRewards;

/** The passive rewards of the 3 and 6 stack tiers (GDD Oath table), lost when the Oath breaks. */
class OathRewardTest {

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

    // ---------- Samurai ----------

    @Test
    void honorThreeGivesYourMinionsTwoAttackUntilTheBreak() {
        sworn(me, OathType.HONOR, 3);
        assertTrue(engine.playCard(me.getHand().get(0), 0, null));
        Minion m = me.getSide().getAt(0);
        assertEquals(3, m.getAttack());

        me.getOath().breakOath();
        rewards.refreshAuras();
        assertEquals(1, m.getAttack());
    }

    @Test
    void honorSixHealsYourHeroWhenAMinionHitsTheEnemyHero() {
        sworn(me, OathType.HONOR, 6);
        Minion m = place(me, 0, minionCard("tatakae", 1, 1, Keyword.TATAKAE));
        me.getHero().takeDamage(10);

        assertTrue(engine.attack(m, enemy.getHero()));
        assertEquals(93, me.getHero().getHealth());
    }

    @Test
    void allegianceThreeBuffsAFriendlyMinionAtTheEndOfTurn() {
        sworn(me, OathType.ALLEGIANCE, 3);
        Minion m = place(me, 0, minionCard("a", 1, 1));

        rewards.endTurn(me);
        assertEquals(3, m.getAttack());
        assertEquals(3, m.getHealth());
    }

    @Test
    void allegianceSixSummonsARetainerOncePerTurn() {
        sworn(me, OathType.ALLEGIANCE, 6);
        Minion m = place(me, 2, minionCard("a", 1, 1));

        engine.destroy(m);
        Minion retainer = me.getSide().getAt(0);
        assertNotNull(retainer);
        assertSame(OathRewards.RETAINER, retainer.getSource());
        assertEquals(5, retainer.getAttack());
        assertEquals(5, retainer.getHealth());

        engine.destroy(retainer);
        assertEquals(0, me.getSide().size());
    }

    @Test
    void justiceThreeGivesOneKiNextTurnPerEnemyMinionKilled() {
        sworn(me, OathType.JUSTICE, 3);
        engine.destroy(place(enemy, 0, minionCard("e", 1, 1)));
        assertEquals(1, rewards.kiNextTurn(me));

        engine.endTurn();
        engine.endTurn();                      // my turn 2: 2 Ki + 1
        assertEquals(3, me.getKi());
    }

    @Test
    void justiceSixDoublesDamageOnMinionsThatHitYourHeroLastTurn() {
        Minion em = place(enemy, 0, minionCard("e", 2, 10));
        engine.endTurn();                      // enemy turn: his minion can attack
        assertTrue(engine.attack(em, me.getHero()));
        engine.endTurn();                      // my turn 2

        sworn(me, OathType.JUSTICE, 6);
        engine.dealDamage(null, em, 3);
        assertEquals(4, em.getHealth());
    }

    // ---------- Ninja ----------

    @Test
    void silenceThreeMakesNinjaCardsCostOneLessDownToOne() {
        sworn(me, OathType.SILENCE, 3);
        assertEquals(2, engine.costOf(me, costed("n3", 3, HeroClass.NINJA, 1)));
        assertEquals(1, engine.costOf(me, costed("n1", 1, HeroClass.NINJA, 1)));
        assertEquals(3, engine.costOf(me, costed("x3", 3, HeroClass.NEUTRAL, 1)));

        me.getOath().breakOath();
        assertEquals(3, engine.costOf(me, costed("n3", 3, HeroClass.NINJA, 1)));
    }

    @Test
    void silenceSixDrawsWhenTwoCardsOrFewerWerePlayed() {
        sworn(me, OathType.SILENCE, 6);
        int hand = me.getHand().size();
        rewards.endTurn(me);
        assertEquals(hand + 1, me.getHand().size());
    }

    @Test
    void deceptionThreeAndDescensionThreeCutTheirCards() {
        sworn(me, OathType.DECEPTION, 3);
        assertEquals(1, engine.costOf(me, trap("t", 2)));

        sworn(me, OathType.DESCENSION, 3);
        MinionCard big = costed("big", 2, HeroClass.ONI, 5);
        assertEquals(2, engine.costOf(me, costed("small", 2, HeroClass.ONI, 4)));
        assertEquals(1, engine.costOf(me, big));

        me.addToHand(big);                     // 1 Ki: playable thanks to the discount
        assertTrue(engine.playCard(big, 0, null));
        assertEquals(0, me.getKi());
    }

    @Test
    void deceptionSixDrawsAndGivesKiWhenTheEnemySetsOffATrap() {
        sworn(me, OathType.DECEPTION, 6);
        me.setTrap(trap("t", 1));
        engine.endTurn();
        int hand = me.getHand().size();

        assertTrue(engine.playCard(enemy.getHand().get(0), 0, null));   // his summon sets my Trap off
        assertEquals(hand + 1, me.getHand().size());
        assertEquals(1, rewards.kiNextTurn(me));
    }

    @Test
    void disciplineGivesKiAndCardsForTheKiSpent() {
        sworn(me, OathType.DISCIPLINE, 6);
        engine.getTurnTracker().addKiSpent(6);
        int hand = me.getHand().size();

        rewards.endTurn(me);
        assertEquals(1, rewards.kiNextTurn(me));          // Discipline 3: 4+ spent
        assertEquals(hand + 1, me.getHand().size());      // Discipline 6: one card per 6
    }

    // ---------- Kannushi ----------

    @Test
    void purityHealsTwoMoreAndDrawsOnceForAFullHeal() {
        sworn(me, OathType.PURITY, 6);
        Minion m = place(me, 1, minionCard("p", 1, 10));
        int hand = me.getHand().size();

        m.takeDamage(5);
        engine.heal(m, 3);                     // 3 + 2: back to full
        assertEquals(10, m.getHealth());
        assertEquals(hand + 1, me.getHand().size());

        m.takeDamage(5);
        engine.heal(m, 5);
        assertEquals(10, m.getHealth());
        assertEquals(hand + 1, me.getHand().size(), "once per turn");
    }

    @Test
    void peaceThreeHealthAuraEndsWithoutKillingADamagedMinion() {
        sworn(me, OathType.PEACE, 3);
        assertTrue(engine.playCard(me.getHand().get(0), 0, null));
        Minion m = me.getSide().getAt(0);
        assertEquals(3, m.getMaxHealth());

        m.takeDamage(2);
        me.getOath().breakOath();
        rewards.refreshAuras();
        assertEquals(1, m.getMaxHealth());
        assertEquals(1, m.getHealth());
        assertFalse(m.isDead());
    }

    @Test
    void peaceSixHealsYourHeroAtTheEndOfTurn() {
        sworn(me, OathType.PEACE, 6);
        me.getHero().takeDamage(10);
        engine.endTurn();
        assertEquals(96, me.getHero().getHealth());
    }

    @Test
    void divinityThreeShrinesLastOneMoreTurn() {
        sworn(me, OathType.DIVINITY, 3);
        ShrineCard card = new ShrineCard("s", "s", "社", "", 1, Rarity.COMMON, HeroClass.KANNUSHI, "", "", 3);
        me.addToHand(card);

        assertTrue(engine.playCard(card, 1, null));
        assertEquals(4, me.getSide().shrines().get(0).getTurnsLeft());
    }

    @Test
    void divinitySixHealsThreeAndBuffsAMinionAtTheEndOfTurn() {
        sworn(me, OathType.DIVINITY, 6);
        Minion m = place(me, 0, minionCard("d", 1, 1));
        me.getHero().takeDamage(10);

        rewards.endTurn(me);
        assertEquals(93, me.getHero().getHealth());
        assertEquals(2, m.getAttack());
        assertEquals(2, m.getHealth());
    }

    // ---------- Oni ----------

    @Test
    void havocAddsOneToEffectDamageAndDrawsOnceAtSixDealt() {
        sworn(me, OathType.HAVOC, 6);
        Minion em = place(enemy, 0, minionCard("e", 1, 20));
        int hand = me.getHand().size();

        engine.dealDamage(null, em, 3);        // 4 dealt
        assertEquals(hand, me.getHand().size());
        engine.dealDamage(null, em, 2);        // 7 dealt: draw
        engine.dealDamage(null, em, 1);        // 9 dealt: no second draw
        assertEquals(11, em.getHealth());
        assertEquals(hand + 1, me.getHand().size());

        engine.dealDamage(me.getOath(), me.getHero(), 5);   // an Oath's own damage gets no bonus
        assertEquals(95, me.getHero().getHealth());
    }

    @Test
    void descensionSixBigMinionsGoThroughShiroduAndTorii() {
        sworn(me, OathType.DESCENSION, 6);
        Minion big = place(me, 0, minionCard("big", 5, 5, Keyword.TATAKAE));
        place(enemy, 0, minionCard("wall", 1, 1, Keyword.SHIRODU));
        Minion torii = place(enemy, 1, minionCard("torii", 1, 3, Keyword.TORII));

        assertTrue(engine.legalTargets(big).contains(torii));
        assertTrue(engine.attack(big, torii));
        assertEquals(-1, enemy.getSide().slotOf(torii));
    }

    @Test
    void corruptionThreeCursesDealOneMore() {
        sworn(me, OathType.CORRUPTION, 3);
        CurseCard c = curse("kuro");
        engine.getCardEffects().register("kuro", Trigger.CURSE,
                ctx -> ctx.engine().dealDamage(null, ctx.owner().getHero(), 3));
        enemy.receiveCurse(c);

        engine.endTurn();
        engine.endTurn();                      // end of his turn: the Curse in his hand acts
        assertEquals(96, enemy.getHero().getHealth());
    }

    @Test
    void corruptionSixDrawsWhenTheEnemyRemovesACurse() {
        sworn(me, OathType.CORRUPTION, 6);
        CurseCard c = curse("kuro");
        enemy.receiveCurse(c);
        engine.endTurn();
        int hand = me.getHand().size();

        assertTrue(engine.removeCurse(c));     // 2 Ki on his turn 1
        assertEquals(hand + 1, me.getHand().size());
    }
}