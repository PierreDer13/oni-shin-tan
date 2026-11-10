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

/** Traps: set face down, one copy each, go off on the enemy's action. */
class TrapTest {

    private Player me;
    private Player enemy;
    private GameEngine engine;
    private final List<GameEvent> events = new ArrayList<>();

    private static MinionCard minionCard(String id) {
        return new MinionCard(id, id, "試", "てすと", 1,
                Rarity.COMMON, HeroClass.NEUTRAL, "", "", 1, 1, EnumSet.noneOf(Keyword.class));
    }

    private static TrapCard trap(String id, TrapTrigger when) {
        return new TrapCard(id, id, "罠", "わな", 1,
                Rarity.COMMON, HeroClass.NINJA, "", "", when);
    }

    private static Player player(String name, HeroClass cls) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < Deck.SIZE; i++) cards.add(minionCard(name + "-" + i));
        return new Player(name, new Hero(cls), new Deck(cards));
    }

    private List<Type> types() {
        return events.stream().map(GameEvent::type).toList();
    }

    @BeforeEach
    void setUp() {
        me = player("Pierre", HeroClass.NINJA);
        enemy = player("Kayne", HeroClass.NINJA);
        engine = new GameEngine(me, enemy, new Random(42));
        engine.startMatch();
        engine.beginFirstTurn();               // turn 1: me active, 1 Ki
        engine.addListener((e, s) -> events.add(e));
    }

    @Test
    void settingATrapIsAnnouncedWithoutCardPlayed() {
        TrapCard t = trap("kunai-trap", TrapTrigger.ON_ENEMY_SUMMON);
        me.addToHand(t);

        assertTrue(engine.playCard(t, -1, null));

        assertEquals(0, me.getKi());
        assertFalse(me.getHand().contains(t));
        assertEquals(1, me.trapCount());
        assertEquals(List.of(Type.TRAP_SET), types());
        assertSame(t, events.get(0).target(), "the owner's UI needs the card; the enemy's shows it face down");
        assertFalse(types().contains(Type.CARD_PLAYED), "CARD_PLAYED would reveal the trap");
        assertEquals(1, engine.getTurnTracker().getTrapsSet());
        assertEquals(1, engine.getTurnTracker().getCardsPlayed());
    }

    @Test
    void sameTrapCannotBeSetTwice() {
        assertTrue(me.setTrap(trap("kunai-trap", TrapTrigger.ON_ENEMY_SUMMON)));
        TrapCard copy = trap("kunai-trap", TrapTrigger.ON_ENEMY_SUMMON);
        me.addToHand(copy);

        assertFalse(engine.playCard(copy, -1, null));
        assertEquals(1, me.getKi(), "nothing spent on a refused trap");
        assertTrue(me.getHand().contains(copy));
    }

    @Test
    void enemyTrapGoesOffOnSummon() {
        TrapCard t = trap("kunai-trap", TrapTrigger.ON_ENEMY_SUMMON);
        enemy.setTrap(t);
        Minion[] hit = new Minion[1];
        engine.getCardEffects().register("kunai-trap", Trigger.TRAP,
                ctx -> hit[0] = (Minion) ctx.target());

        assertTrue(engine.playCard(me.getHand().get(0), 0, null));

        assertTrue(types().contains(Type.TRAP_TRIGGERED));
        assertSame(me.getSide().getMinions().get(0), hit[0]);
        assertEquals(0, enemy.trapCount(), "a trap goes off once");
    }

    @Test
    void trapWithAnotherTriggerStaysSet() {
        enemy.setTrap(trap("smoke", TrapTrigger.ON_ENEMY_SPELL));

        assertTrue(engine.playCard(me.getHand().get(0), 0, null));

        assertFalse(types().contains(Type.TRAP_TRIGGERED));
        assertEquals(1, enemy.trapCount());
    }
}