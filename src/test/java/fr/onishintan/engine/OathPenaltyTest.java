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
import fr.onishintan.engine.oath.OathPenalties;

/** The extra penalty of each broken Oath (GDD Oath table, Break column). */
class OathPenaltyTest {

    private Player me;        // Samurai, plays first
    private Player enemy;     // Ninja
    private GameEngine engine;
    private OathPenalties penalties;
    private final List<GameEvent> events = new ArrayList<>();

    private static MinionCard minionCard(String id, int atk, int hp) {
        return new MinionCard(id, id, "試", "てすと", 1,
                Rarity.COMMON, HeroClass.NEUTRAL, "", "", atk, hp, EnumSet.noneOf(Keyword.class));
    }

    private static Player player(String name, HeroClass cls) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < Deck.SIZE; i++) cards.add(minionCard(name + "-" + i, 1, 1));
        return new Player(name, new Hero(cls), new Deck(cards));
    }

    private static Minion place(Player p, int atk, int hp, int slot) {
        Minion m = new Minion(minionCard("m" + slot, atk, hp));
        p.getSide().place(m, slot);
        return m;
    }

    private static TrapCard trap(String id) {
        return new TrapCard(id, id, "罠", "", 2, Rarity.COMMON, HeroClass.NINJA, "", "", TrapTrigger.ON_ENEMY_SUMMON);
    }

    private static ShrineCard shrine(String id) {
        return new ShrineCard(id, id, "社", "", 1, Rarity.COMMON, HeroClass.KANNUSHI, "", "", 3);
    }

    private static CurseCard curse(String id) {
        return new CurseCard(id, id, "呪", "", 1, Rarity.COMMON, HeroClass.ONI, "", "", CurseZone.HAND, 1, 2);
    }

    @BeforeEach
    void setUp() {
        me = player("Pierre", HeroClass.SAMURAI);
        enemy = player("Kayne", HeroClass.NINJA);
        engine = new GameEngine(me, enemy, new Random(42));
        engine.startMatch();
        engine.beginFirstTurn();               // my turn 1: 1 Ki
        engine.addListener((e, s) -> events.add(e));
        penalties = engine.getPenalties();
    }

    /** n of p's own turns end. */
    private void tick(Player p, int n) {
        for (int i = 0; i < n; i++) penalties.tick(p);
    }

    @Test
    void dishonourPutsTheHeroAsleepForThreeTurns() {
        me.getHero().equip(new Weapon(KatanaTier.BOKKEN.toCard()));
        penalties.apply(me, new Oath(OathType.HONOR));
        assertFalse(me.getHero().canAttack());

        tick(me, 2);
        assertFalse(me.getHero().canAttack());
        tick(me, 1);
        assertTrue(me.getHero().canAttack());
    }

    @Test
    void banishmentDestroysTheStrongestMinionWhenAllegianceBreaks() {
        engine.swearOath(me, OathType.ALLEGIANCE);
        place(me, 5, 5, 0);                    // one minion, and a 1-Ki minion could be played: broken

        engine.endTurn();

        assertEquals(Oath.State.OATHBREAKER, me.getOath().getState());
        assertTrue(me.getSide().getMinions().isEmpty());
    }

    @Test
    void persecutionGivesEnemyMinionsTwoAttackForThreeTurns() {
        Minion theirs = place(enemy, 3, 3, 0);
        penalties.apply(me, new Oath(OathType.JUSTICE));
        assertEquals(5, theirs.getAttack());

        tick(me, 2);
        assertEquals(5, theirs.getAttack());
        tick(me, 1);
        assertEquals(3, theirs.getAttack());
    }

    @Test
    void revelationShowsTheHandAndDiscardsARandomCard() {
        int before = enemy.getHand().size();
        penalties.apply(enemy, new Oath(OathType.SILENCE));

        assertEquals(before - 1, enemy.getHand().size());
        assertTrue(events.stream().anyMatch(e -> e.type() == Type.HAND_REVEALED && e.amount() == before));
        assertTrue(events.stream().anyMatch(e -> e.type() == Type.CARD_DISCARDED && e.source() == enemy));
    }

    @Test
    void exposedGivesEveryTrapToTheOpponent() {
        TrapCard set = trap("makibishi");
        TrapCard held = trap("kawarimi");
        enemy.setTrap(set);
        enemy.addToHand(held);

        penalties.apply(enemy, new Oath(OathType.DECEPTION));

        assertEquals(0, enemy.trapCount());
        assertFalse(enemy.getHand().contains(held));
        assertTrue(me.getTraps().contains(set));
        assertTrue(me.getHand().contains(held));
    }

    @Test
    void disappointmentStopsDrawsForTwoTurns() {
        int before = enemy.getHand().size();
        penalties.apply(enemy, new Oath(OathType.DISCIPLINE));
        engine.draw(enemy);
        assertEquals(before, enemy.getHand().size());

        tick(enemy, 2);
        engine.draw(enemy);
        assertEquals(before + 1, enemy.getHand().size());
    }

    @Test
    void defilementHurtsOwnMinionsAndBlocksTheirHealingForTwoTurns() {
        Minion mine = place(me, 5, 5, 0);
        penalties.apply(me, new Oath(OathType.PURITY));
        assertEquals(3, mine.getHealth());

        engine.heal(mine, 2);
        assertEquals(3, mine.getHealth());
        tick(me, 2);
        engine.heal(mine, 2);
        assertEquals(5, mine.getHealth());
    }

    @Test
    void profanityDestroysShrinesAndBlocksThemForOneTurn() {
        me.getSide().place(new Shrine(shrine("heiwa-no-torii")), 0);
        ShrineCard inHand = shrine("meiji-otorii");
        me.addToHand(inHand);

        penalties.apply(me, new Oath(OathType.DIVINITY));
        assertTrue(me.getSide().shrines().isEmpty());
        assertFalse(engine.canPlay(inHand));

        tick(me, 1);
        assertTrue(engine.canPlay(inHand));
    }

    @Test
    void failureRestoresEnemyMinionsToFullHealth() {
        Minion theirs = place(enemy, 4, 6, 0);
        theirs.takeDamage(4);

        penalties.apply(me, new Oath(OathType.HAVOC));

        assertEquals(6, theirs.getHealth());
    }

    @Test
    void exorcismThrowsAwayEnemyCursesAndBlocksCursingForTwoTurns() {
        CurseCard held = curse("bakeneko");
        enemy.receiveCurse(held);
        CurseCard mine = curse("hinoeuma");
        me.addToHand(mine);

        penalties.apply(me, new Oath(OathType.CORRUPTION));
        assertFalse(enemy.isHeldCurse(held));
        assertFalse(enemy.getHand().contains(held));
        assertFalse(engine.canPlay(mine));

        tick(me, 2);
        assertTrue(engine.canPlay(mine));
    }
}