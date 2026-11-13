package fr.onishintan.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import fr.onishintan.model.*;

/** The rules of the 10 other Oaths (Allegiance and Silence are in OathTest). */
class OathRulesTest {

    private Player me;        // plays first
    private Player enemy;
    private GameEngine engine;

    private static MinionCard card(String id, int ki, int atk, int hp) {
        return new MinionCard(id, id, "試", "てすと", ki, Rarity.COMMON,
                HeroClass.NEUTRAL, "", "", atk, hp, EnumSet.noneOf(Keyword.class));
    }

    private static Player player(String name, HeroClass cls) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < Deck.SIZE; i++) cards.add(card(name + "-" + i, 1, 1, 1));
        return new Player(name, new Hero(cls), new Deck(cards));
    }

    /** I swear the Oath, then my turn 1 starts: 1 Ki, 1-Ki minions in hand, empty boards. */
    private void start(HeroClass mine, OathType oath) {
        me = player("Pierre", mine);
        enemy = player("Kayne", HeroClass.NINJA);
        engine = new GameEngine(me, enemy, new Random(42));
        engine.startMatch();
        assertTrue(engine.swearOath(me, oath));
        engine.beginFirstTurn();
    }

    /** A minion already on the board that can attack this turn. */
    private Minion ready(Player owner, int slot, int atk, int hp) {
        Minion m = new Minion(card("ready-" + slot, 1, atk, hp));
        owner.getSide().place(m, slot);
        m.onTurnStart();
        return m;
    }

    private void assertKept(int stacks) {
        assertTrue(me.getOath().isSworn());
        assertEquals(stacks, me.getOath().getStacks());
    }

    private void assertUnable() { assertKept(0); }

    private void assertBroken() {
        assertEquals(Oath.State.OATHBREAKER, me.getOath().getState());
    }

    // ---------- Honor ----------

    @Test
    void honorKeptWhenAMinionHitsTheEnemyHero() {
        start(HeroClass.SAMURAI, OathType.HONOR);
        assertTrue(engine.attack(ready(me, 0, 2, 2), enemy.getHero()));
        engine.endTurn();
        assertKept(1);
    }

    @Test
    void honorBrokenWhenAReadyMinionLeavesTheHeroAlone() {
        start(HeroClass.SAMURAI, OathType.HONOR);
        ready(me, 0, 2, 2);
        engine.endTurn();
        assertBroken();
    }

    @Test
    void honorUnableWithoutAMinionThatCanAttack() {
        start(HeroClass.SAMURAI, OathType.HONOR);
        engine.endTurn();
        assertUnable();
    }

    // ---------- Justice ----------

    /** My turn 1 (nothing to avenge), then an enemy minion hits my hero; back to my turn 2. */
    private Minion enemyHitsMyHero() {
        start(HeroClass.SAMURAI, OathType.JUSTICE);
        engine.endTurn();
        Minion attacker = ready(enemy, 0, 1, 5);
        assertTrue(engine.attack(attacker, me.getHero()));
        engine.endTurn();
        return attacker;
    }

    @Test
    void justiceKeptWhenTheAttackerIsHitBack() {
        Minion attacker = enemyHitsMyHero();
        assertTrue(engine.attack(ready(me, 0, 2, 2), attacker));
        engine.endTurn();
        assertKept(2);
    }

    @Test
    void justiceBrokenWhenTheAttackerIsLeftAlone() {
        enemyHitsMyHero();
        ready(me, 0, 2, 2);                     // could hit it back, but does not
        engine.endTurn();
        assertBroken();
    }

    // ---------- Ninja ----------

    @Test
    void deceptionUnableWithoutATrapToSet() {
        start(HeroClass.NINJA, OathType.DECEPTION);
        engine.endTurn();
        assertUnable();
    }

    @Test
    void disciplineBrokenWithKiLeftAndACardToPlay() {
        start(HeroClass.NINJA, OathType.DISCIPLINE);
        engine.endTurn();
        assertBroken();
    }

    @Test
    void disciplineKeptWithZeroKi() {
        start(HeroClass.NINJA, OathType.DISCIPLINE);
        me.spendKi(me.getKi());
        engine.endTurn();
        assertKept(1);
    }

    // ---------- Kannushi ----------

    @Test
    void purityUnableWithADamagedMinion() {
        start(HeroClass.KANNUSHI, OathType.PURITY);
        ready(me, 0, 2, 4).takeDamage(1);
        engine.endTurn();
        assertUnable();
    }

    @Test
    void peaceBrokenWhenHittingTheHeroPastEnemyMinions() {
        start(HeroClass.KANNUSHI, OathType.PEACE);
        ready(enemy, 0, 1, 5);                  // no Shirodu: the hero can still be hit
        assertTrue(engine.attack(ready(me, 0, 2, 2), enemy.getHero()));
        engine.endTurn();
        assertBroken();
    }

    @Test
    void peaceKeptWhenTheEnemyBoardIsEmpty() {
        start(HeroClass.KANNUSHI, OathType.PEACE);
        assertTrue(engine.attack(ready(me, 0, 2, 2), enemy.getHero()));
        engine.endTurn();
        assertKept(1);
    }

    @Test
    void divinityKeptBeforeTurnThree() {
        start(HeroClass.KANNUSHI, OathType.DIVINITY);
        engine.endTurn();
        assertKept(1);
    }

    // ---------- Oni ----------

    @Test
    void havocKeptAfterThreeDamage() {
        start(HeroClass.ONI, OathType.HAVOC);
        assertTrue(engine.attack(ready(me, 0, 3, 3), enemy.getHero()));
        engine.endTurn();
        assertKept(1);
    }

    @Test
    void havocBrokenWhenAMinionCouldStillHit() {
        start(HeroClass.ONI, OathType.HAVOC);
        ready(me, 0, 1, 1);
        engine.endTurn();
        assertBroken();
    }

    @Test
    void descensionBrokenWhenABigMinionWasAffordable() {
        start(HeroClass.ONI, OathType.DESCENSION);
        me.addToHand(card("big", 1, 5, 1));
        engine.endTurn();
        assertBroken();
    }

    @Test
    void descensionKeptWhenABigMinionIsPlayed() {
        start(HeroClass.ONI, OathType.DESCENSION);
        Card big = card("big", 1, 5, 1);
        me.addToHand(big);
        assertTrue(engine.playCard(big, 0, null));
        engine.endTurn();
        assertKept(1);
    }

    @Test
    void corruptionUnableWithoutACurse() {
        start(HeroClass.ONI, OathType.CORRUPTION);
        engine.endTurn();
        assertUnable();
    }
}