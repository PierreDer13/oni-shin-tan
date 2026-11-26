package fr.onishintan.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.onishintan.model.*;
import fr.onishintan.engine.event.GameEvent;

/** The family effects of cards.json by id: the 6 Traps, the 6 Shrine auras, the 6 Curses, Gokitō's draw and Rite stacking. */
class FamilyEffectTest {

    private Player me;        // Kannushi, plays first
    private Player enemy;     // Ninja
    private GameEngine engine;

    private static MinionCard card(String id, int atk, int hp, Keyword... keywords) {
        EnumSet<Keyword> set = EnumSet.noneOf(Keyword.class);
        for (Keyword k : keywords) set.add(k);
        return new MinionCard(id, id, "試", "", 1, Rarity.COMMON, HeroClass.NEUTRAL, "", "", atk, hp, set);
    }

    private static Player player(String name, HeroClass cls) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < Deck.SIZE; i++) cards.add(card(name + "-" + i, 1, 1));
        return new Player(name, new Hero(cls), new Deck(cards));
    }

    private static Minion place(Player p, int slot, MinionCard c) {
        Minion m = new Minion(c);
        p.getSide().place(m, slot);
        return m;
    }

    private static TrapCard trap(String id, TrapTrigger when) {
        return new TrapCard(id, id, "罠", "", 2, Rarity.COMMON, HeroClass.NINJA, "", "", when);
    }

    private static ShrineCard shrine(String id, int turns) {
        return new ShrineCard(id, id, "社", "", 1, Rarity.COMMON, HeroClass.KANNUSHI, "", "", turns);
    }

    private static CurseCard curse(String id, CurseZone zone, int copies, int removal) {
        return new CurseCard(id, id, "呪", "", 2, Rarity.COMMON, HeroClass.ONI, "", "", zone, copies, removal);
    }

    private static RiteCard rite(String id, int ancestors, Set<Keyword> now, int atk, int hp, Set<Keyword> bonus) {
        return new RiteCard(id, id, "儀", "", 1, Rarity.COMMON, HeroClass.SAMURAI, "", "", ancestors, now, atk, hp, bonus);
    }

    /** Plays one of my cards from my hand. */
    private boolean play(Card c, int slot, Combatant target) {
        me.addToHand(c);
        return engine.playCard(c, slot, target);
    }

    /** Ends my turn and the enemy's: my next turn starts. */
    private void round() {
        engine.endTurn();
        engine.endTurn();
        me.gainTemporaryKi(30);
    }

    @BeforeEach
    void setUp() {
        me = player("Pierre", HeroClass.KANNUSHI);
        enemy = player("Kayne", HeroClass.NINJA);
        engine = new GameEngine(me, enemy, new Random(42));
        engine.startMatch();
        engine.beginFirstTurn();
        me.gainTemporaryKi(30);
    }

    // ---------- Traps (set by the enemy, sprung by me) ----------

    @Test
    void kawarimiLogTakesTheHit() {
        Minion target = place(enemy, 0, card("target", 1, 5));
        Minion attacker = place(me, 0, card("attacker", 6, 6));
        attacker.onTurnStart();
        enemy.setTrap(trap("kawarimi", TrapTrigger.ON_ENEMY_ATTACK_MINION));

        assertTrue(engine.attack(attacker, target));

        assertEquals(5, target.getHealth(), "the target is untouched");
        Minion log = enemy.getSide().getMinions().stream().filter(m -> m != target).findFirst().orElseThrow();
        assertEquals("kawarimi-log", log.getSource().getId());
        assertEquals(3, log.getHealth());
    }

    @Test
    void kawarimiIsHeardBeforeTheAttackWhichTargetsTheLog() {
        Minion target = place(enemy, 0, card("target", 1, 5));
        Minion attacker = place(me, 0, card("attacker", 6, 6));
        attacker.onTurnStart();
        enemy.setTrap(trap("kawarimi", TrapTrigger.ON_ENEMY_ATTACK_MINION));
        List<GameEvent> heard = new ArrayList<>();
        engine.addListener((e, s) -> heard.add(e));

        assertTrue(engine.attack(attacker, target));

        List<GameEvent.Type> types = heard.stream().map(GameEvent::type).toList();
        int trap = types.indexOf(GameEvent.Type.TRAP_TRIGGERED);
        int log = types.indexOf(GameEvent.Type.MINION_SUMMONED);
        int attack = types.indexOf(GameEvent.Type.ATTACK);
        assertTrue(trap >= 0 && trap < log && log < attack, "TRAP_TRIGGERED, then the log, then ATTACK");
        assertSame(heard.get(log).target(), heard.get(attack).target(), "ATTACK.target() is the log, the real target");
    }

    @Test
    void makibishiHitsTheSummonedMinion() {
        enemy.setTrap(trap("makibishi", TrapTrigger.ON_ENEMY_SUMMON));
        assertTrue(play(card("big", 2, 10), 0, null));
        assertEquals(4, me.getSide().getAt(0).getHealth());
    }

    @Test
    void oyasumiPutsTwoOfTheCastersMinionsToSleep() {
        Minion a = place(me, 0, card("a", 2, 2)), b = place(me, 1, card("b", 2, 2)), c = place(me, 2, card("c", 2, 2));
        enemy.setTrap(trap("oyasumi", TrapTrigger.ON_ENEMY_SPELL));
        SpellCard spell = new SpellCard("plain", "plain", "術", "", 1, Rarity.COMMON, HeroClass.KANNUSHI, "", "");

        assertTrue(play(spell, 0, null));

        long asleep = List.of(a, b, c).stream().filter(m -> m.hasStatus(Status.SLEEP)).count();
        assertEquals(2, asleep);
        engine.endTurn();
        assertEquals(2, List.of(a, b, c).stream().filter(m -> m.hasStatus(Status.SLEEP)).count(), "asleep during my turn: they sleep through my next one too");
        engine.endTurn();                        // my next turn
        List.of(a, b, c).forEach(Minion::onTurnStart);
        assertEquals(1, List.of(a, b, c).stream().filter(Minion::canAttack).count(), "the two sleepers still cannot attack");
        engine.endTurn();
        assertTrue(List.of(a, b, c).stream().noneMatch(m -> m.hasStatus(Status.SLEEP)), "they wake at the end of my next turn");
    }

    @Test
    void otoshianaCancelsTheAttackOnTheHero() {
        Minion attacker = place(me, 0, card("attacker", 6, 6));
        attacker.onTurnStart();
        int hp = enemy.getHero().getHealth();
        enemy.setTrap(trap("otoshiana", TrapTrigger.ON_ENEMY_ATTACK_HERO));

        assertTrue(engine.attack(attacker, enemy.getHero()));

        assertEquals(hp, enemy.getHero().getHealth());
        assertTrue(attacker.hasStatus(Status.SLEEP));
        assertFalse(attacker.canAttack());
        engine.endTurn();
        engine.endTurn();                        // my next turn: still asleep
        assertTrue(attacker.hasStatus(Status.SLEEP));
        assertFalse(attacker.canAttack());
        engine.endTurn();
        assertFalse(attacker.hasStatus(Status.SLEEP), "awake after the end of my next turn");
    }

    @Test
    void shikomiAndonKillsTheAttackerFirst() {
        Minion target = place(enemy, 0, card("target", 1, 5));
        Minion attacker = place(me, 0, card("attacker", 6, 14));
        attacker.onTurnStart();
        enemy.setTrap(trap("shikomi-andon", TrapTrigger.ON_ENEMY_ATTACK_MINION));

        assertFalse(engine.attack(attacker, target));

        assertTrue(attacker.isDead());
        assertEquals(5, target.getHealth());
    }

    @Test
    void dondengaeshiCopiesTheSummonedMinion() {
        enemy.setTrap(trap("dondengaeshi", TrapTrigger.ON_ENEMY_SUMMON));
        assertTrue(play(card("oni", 5, 5), 3, null));
        assertEquals("oni", enemy.getSide().getMinions().get(0).getSource().getId());
    }

    // ---------- Shrines (mine) ----------

    @Test
    void heiwaGivesHealthAndHealsAtTheEndOfMyTurn() {
        Minion m = place(me, 1, card("m", 1, 5));
        assertTrue(play(shrine("heiwa-no-torii", 2), 0, null));
        assertEquals(8, m.getHealth());
        m.takeDamage(6);                         // 2 / 8
        engine.endTurn();
        assertEquals(5, m.getHealth(), "healed 3 at the end of my turn");
    }

    @Test
    void auraEndsWhenTheShrineExpires() {
        Minion m = place(me, 1, card("m", 1, 5));
        assertTrue(play(shrine("meiji-otorii", 1), 0, null));
        assertEquals(4, m.getAttack());
        engine.endTurn();                        // still standing during the enemy's turn
        assertEquals(4, m.getAttack());
        engine.endTurn();                        // my next turn starts: its one turn is used up, gone
        assertEquals(1, m.getAttack());
    }

    @Test
    void meijiHealsAfterAttacking() {
        Minion m = place(me, 1, card("m", 1, 10));
        Minion e = place(enemy, 0, card("e", 4, 20));
        m.onTurnStart();
        assertTrue(play(shrine("meiji-otorii", 2), 0, null));
        List<GameEvent> heard = new ArrayList<>();
        engine.addListener((ev, s) -> heard.add(ev));
        assertTrue(engine.attack(m, e));
        assertEquals(9, m.getHealth(), "10 - 4 + 3");
        assertEquals(16, e.getHealth(), "1 + 3 ATK from Meiji");
        List<GameEvent.Type> types = heard.stream().map(GameEvent::type).toList();
        int fx = types.indexOf(GameEvent.Type.EFFECT_TRIGGERED);
        assertTrue(fx >= 0 && fx < types.indexOf(GameEvent.Type.HEAL), "the Shrine is announced before its heal");
        assertTrue(heard.get(fx).source() instanceof Shrine);
    }

    @Test
    void itsukushimaGivesToriiAtTheEndOfMyTurn() {
        Minion m = place(me, 1, card("m", 1, 5));
        assertTrue(play(shrine("itsukushima", 3), 2, null));
        engine.endTurn();
        assertEquals(1, m.getToriiStacks());
    }

    @Test
    void kumanoLendsShiroduAndTakesItBack() {
        Minion m = place(me, 1, card("m", 1, 5));
        Minion guard = place(me, 3, card("guard", 1, 5, Keyword.SHIRODU));
        assertTrue(play(shrine("kumano-hongu-taisha", 1), 2, null));
        assertEquals(3, m.getAttack());
        assertEquals(9, m.getHealth());
        assertTrue(m.hasKeyword(Keyword.SHIRODU));
        engine.endTurn();
        engine.endTurn();                        // my next turn starts: the Shrine expires
        assertFalse(m.hasKeyword(Keyword.SHIRODU));
        assertTrue(guard.hasKeyword(Keyword.SHIRODU), "a Shirodu of its own stays");
    }

    @Test
    void motonosumiGivesTatakaeToAMinionPlayedNextToIt() {
        assertTrue(play(shrine("motonosumi-shrine", 2), 0, null));
        assertTrue(play(card("fresh", 2, 2), 1, null));
        Minion m = me.getSide().getAt(1);
        assertEquals(5, m.getAttack());
        assertTrue(m.canAttack(), "Tatakae: attacks the turn it is played");
    }

    @Test
    void motonosumiReachesTwoSlotsOnEachSideOnly() {
        Minion[] at = new Minion[7];
        for (int slot : new int[] {0, 1, 2, 4, 5, 6}) at[slot] = place(me, slot, card("m" + slot, 1, 1));
        assertTrue(play(shrine("motonosumi-shrine", 2), 3, null));
        for (int slot : new int[] {1, 2, 4, 5}) {
            assertEquals(4, at[slot].getAttack(), "slot " + slot + " is within 2 slots");
            assertTrue(at[slot].hasKeyword(Keyword.TATAKAE), "slot " + slot);
        }
        for (int slot : new int[] {0, 6}) {
            assertEquals(1, at[slot].getAttack(), "slot " + slot + " is 3 slots away");
            assertFalse(at[slot].hasKeyword(Keyword.TATAKAE), "slot " + slot);
        }
    }

    @Test
    void otherShrinesStillReachOnlyTheirNeighbours() {
        Minion near = place(me, 2, card("near", 1, 1)), far = place(me, 1, card("far", 1, 1));
        assertTrue(play(shrine("meiji-otorii", 2), 3, null));
        assertEquals(4, near.getAttack());
        assertEquals(1, far.getAttack());
    }

    @Test
    void fushimiInariSavesFromASingleHitOnTheOpponentsTurn() {
        Minion m = place(me, 1, card("m", 1, 5));
        assertTrue(play(shrine("fushimi-inari", 3), 0, null));
        engine.endTurn();                        // enemy's turn
        Minion e = place(enemy, 0, card("e", 10, 10));
        e.onTurnStart();
        assertTrue(engine.attack(e, m));
        assertEquals(1, m.getHealth());
    }

    @Test
    void fushimiInariDoesNothingOnMyTurn() {
        Minion m = place(me, 1, card("m", 1, 5));
        Minion e = place(enemy, 0, card("e", 10, 10));
        assertTrue(play(shrine("fushimi-inari", 3), 0, null));
        engine.dealDamage(null, m, 9);
        assertTrue(m.isDead() || me.getSide().slotOf(m) < 0);
        assertFalse(e.isDead());
    }

    // ---------- Curses (given to the enemy) ----------

    @Test
    void bakenekoMakesTheHoldersMinionsCostMore() {
        CurseCard b = curse("bakeneko", CurseZone.HAND, 1, 2);
        MinionCard m = card("m", 1, 1);
        assertTrue(play(b, 0, null));
        assertEquals(2, engine.costOf(enemy, m));
        assertEquals(1, engine.costOf(me, m));
    }

    @Test
    void hinoeumaHitsTheHolderAtTheEndOfHisTurn() {
        assertTrue(play(curse("hinoeuma", CurseZone.HAND, 1, 3), 0, null));
        int hp = enemy.getHero().getHealth();
        engine.endTurn();
        engine.endTurn();
        assertEquals(hp - 8, enemy.getHero().getHealth());
    }

    @Test
    void tatariHitsWhenDrawn() {
        CurseCard t = curse("tatari", CurseZone.DECK, 1, 0);
        int hp = enemy.getHero().getHealth();
        enemy.getDeck().insertRandom(t, new Random(1));
        while (enemy.getDeck().getCards().contains(t)) engine.draw(enemy);
        assertEquals(hp - 5, enemy.getHero().getHealth());
        assertFalse(enemy.getHand().contains(t));
    }

    @Test
    void gagozeHitsARandomMinionOfTheVictim() {
        Minion e = place(enemy, 0, card("e", 1, 10));
        CurseCard g = curse("gagoze", CurseZone.DECK, 1, 0);
        enemy.getDeck().insertRandom(g, new Random(1));
        while (enemy.getDeck().getCards().contains(g)) engine.draw(enemy);
        assertEquals(6, e.getHealth());
    }

    @Test
    void gakiTakesOneKiAtTheStartOfTheHoldersTurn() {
        assertTrue(play(curse("gaki", CurseZone.HAND, 1, 5), 0, null));
        engine.endTurn();
        assertEquals(1, enemy.getKi(), "2 Ki on his first turn (second player +1), minus 1");
    }

    @Test
    void gokiburiEatsTheCardOnItsLeft() {
        Card left = enemy.getHand().get(enemy.getHand().size() - 1);
        int size = enemy.getHand().size();
        assertTrue(play(curse("gokiburi", CurseZone.HAND, 1, 3), 0, null));
        engine.endTurn();                         // draws come at the start of his turn, after the Curse in the hand
        int drawn = enemy.getHand().size() - size - 1;
        engine.endTurn();                         // end of his turn: the Curse eats
        assertFalse(enemy.getHand().contains(left));
        assertEquals(size + drawn, enemy.getHand().size());
    }

    // ---------- Rites ----------

    @Test
    void gokitoAncestorsDrawAndRitesStack() {
        RiteCard gokito = rite("rite-of-gokito", 1, Set.of(), 0, 0, Set.of(Keyword.BANZAI));
        RiteCard ujigami = rite("rite-of-ujigami-mairi", 1, Set.of(Keyword.TORII), 0, 0, Set.of(Keyword.TORII));
        assertTrue(play(gokito, 0, null));
        int hand = me.getHand().size();

        assertTrue(play(ujigami, 1, null));       // after Gokitō: Banzai, draw a card

        assertEquals(hand + 1, me.getHand().size());
        assertEquals(1, me.getSide().getAt(1).getToriiStacks());

        assertTrue(play(gokito, 2, null));        // a second Gokitō: later Ancestors draw once per Gokitō
        hand = me.getHand().size();
        assertTrue(play(rite("plain-rite", 1, Set.of(), 0, 0, Set.of()), 3, null));
        assertEquals(hand + 2, me.getHand().size());
        assertEquals(1, me.getSide().getAt(3).getToriiStacks(), "Ujigami-mairi's lasting Torii");
    }
}