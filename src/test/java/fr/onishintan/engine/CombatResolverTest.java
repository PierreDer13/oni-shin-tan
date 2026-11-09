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

class CombatResolverTest {

    private Player me;        // active player (first)
    private Player enemy;
    private CombatResolver combat;
    private final List<GameEvent> events = new ArrayList<>();

    // ---------- helpers ----------

    private static MinionCard minionCard(String id, int atk, int hp, Keyword... kws) {
        EnumSet<Keyword> set = kws.length == 0 ? EnumSet.noneOf(Keyword.class) : EnumSet.of(kws[0], kws);
        return new MinionCard(id, id, "試", "てすと", 1,
                Rarity.COMMON, HeroClass.NEUTRAL, "", "", atk, hp, set);
    }

    /** A minion already on the board since last turn (no summoning sickness). */
    private Minion ready(Player owner, String id, int atk, int hp, Keyword... kws) {
        Minion m = new Minion(minionCard(id, atk, hp, kws));
        owner.getSide().place(m, owner.getSide().size());
        m.onTurnStart();
        return m;
    }

    /** A minion played this turn: still asleep. */
    private Minion fresh(Player owner, String id, int atk, int hp, Keyword... kws) {
        Minion m = new Minion(minionCard(id, atk, hp, kws));
        owner.getSide().place(m, owner.getSide().size());
        return m;
    }

    private static Weapon weapon(int atk, int durability) {
        WeaponCard card = new WeaponCard("test-blade", "Blade", "刀", "かたな", 4,
                Rarity.COMMON, HeroClass.SAMURAI, "", "", atk, durability);
        return new Weapon(card);
    }

    private static Player player(String name, HeroClass cls) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < Deck.SIZE; i++) cards.add(minionCard(name + "-" + i, 1, 1));
        return new Player(name, new Hero(cls), new Deck(cards));
    }

    private List<Type> types() {
        return events.stream().map(GameEvent::type).toList();
    }

    @BeforeEach
    void setUp() {
        me = player("Pierre", HeroClass.SAMURAI);
        enemy = player("Kayne", HeroClass.NINJA);
        GameState state = new GameState(me, enemy);
        TurnManager tm = new TurnManager(state, new Random(42));
        tm.startMatch();
        tm.startTurn();                         // turn 1: me active
        combat = new CombatResolver(state, events::add);
    }

    // ---------- trade of damage ----------

    @Test
    void minionsHitEachOtherAtOnce() {
        Minion kappa = ready(me, "kappa", 5, 5);
        Minion saru = ready(enemy, "saru", 2, 2);

        assertTrue(combat.resolve(kappa, saru));
        assertEquals(3, kappa.getHealth());                       // Kappa 5/3
        assertFalse(enemy.getSide().getMinions().contains(saru)); // Saru died and left the board
        assertEquals(List.of(Type.ATTACK, Type.DAMAGE, Type.DAMAGE, Type.MINION_DIED), types());
    }

    @Test
    void attackingTheHeroGetsNoStrikeBack() {
        Minion kappa = ready(me, "kappa", 5, 5);
        assertTrue(combat.resolve(kappa, enemy.getHero()));
        assertEquals(Hero.START_HEALTH - 5, enemy.getHero().getHealth());
        assertEquals(5, kappa.getHealth());
    }

    @Test
    void aMinionCanAttackOnlyOncePerTurn() {
        Minion kappa = ready(me, "kappa", 5, 5);
        assertTrue(combat.resolve(kappa, enemy.getHero()));
        assertFalse(combat.resolve(kappa, enemy.getHero()));
    }

    // ---------- targeting ----------

    @Test
    void visibleShiroduMustBeAttackedFirst() {
        Minion kappa = ready(me, "kappa", 5, 5);
        Minion wall = ready(enemy, "wall", 1, 6, Keyword.SHIRODU);
        ready(enemy, "saru", 2, 2);

        assertEquals(List.of(wall), combat.legalTargets(kappa));
        assertFalse(combat.resolve(kappa, enemy.getHero()));
        assertEquals(Hero.START_HEALTH, enemy.getHero().getHealth());
    }

    @Test
    void hiddenShinobiCannotBeTargeted() {
        Minion kappa = ready(me, "kappa", 5, 5);
        Minion ninja = ready(enemy, "ninja", 3, 1, Keyword.SHINOBI);

        assertTrue(ninja.isHidden());
        assertFalse(combat.legalTargets(kappa).contains(ninja));
        assertTrue(combat.legalTargets(kappa).contains(enemy.getHero()));
    }

    @Test
    void stealthHeroCannotBeTargeted() {
        Minion kappa = ready(me, "kappa", 5, 5);
        enemy.getHero().addStatus(Status.STEALTH);          // Mutsubushi
        assertFalse(combat.legalTargets(kappa).contains(enemy.getHero()));
    }

    @Test
    void cannotAttackWithTheOpponentsMinion() {
        Minion saru = ready(enemy, "saru", 2, 2);
        assertTrue(combat.legalTargets(saru).isEmpty());
        assertFalse(combat.resolve(saru, me.getHero()));
    }

    // ---------- sleep, Torii ----------

    @Test
    void freshMinionIsAsleep() {
        Minion kappa = fresh(me, "kappa", 5, 5);
        assertFalse(combat.resolve(kappa, enemy.getHero()));
        assertEquals(Hero.START_HEALTH, enemy.getHero().getHealth());
    }

    @Test
    void toriiAbsorbsTheFirstHit() {
        Minion kappa = ready(me, "kappa", 5, 5);
        Minion gate = ready(enemy, "gate", 1, 3, Keyword.TORII);

        assertTrue(combat.resolve(kappa, gate));
        assertEquals(3, gate.getHealth());                  // no damage taken
        assertFalse(gate.hasTorii());                       // Torii used up
    }

    // ---------- hero with a weapon ----------

    @Test
    void heroWithWeaponAttacksAndWeaponBreaks() {
        me.getHero().equip(weapon(8, 1));
        assertTrue(combat.resolve(me.getHero(), enemy.getHero()));

        assertEquals(Hero.START_HEALTH - 8, enemy.getHero().getHealth());
        assertNull(me.getHero().getWeapon());
        assertTrue(types().contains(Type.WEAPON_BROKEN));
    }

    @Test
    void heroAttackingAMinionTakesItsDamage() {
        me.getHero().equip(weapon(8, 1));
        Minion saru = ready(enemy, "saru", 2, 2);
        assertTrue(combat.resolve(me.getHero(), saru));
        assertEquals(Hero.START_HEALTH - 2, me.getHero().getHealth());
    }

    @Test
    void unarmedHeroCannotAttack() {
        assertFalse(combat.resolve(me.getHero(), enemy.getHero()));
    }
}