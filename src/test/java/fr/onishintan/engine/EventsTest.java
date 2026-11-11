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

class EventsTest {

    private Player first;
    private Player second;
    private GameEngine engine;
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

    private Minion ready(Player owner, String id, int atk, int hp) {
        Minion m = new Minion(minionCard(id, atk, hp));
        owner.getSide().place(m, owner.getSide().firstFree());
        m.onTurnStart();
        return m;
    }

    private List<Type> types() {
        return events.stream().map(GameEvent::type).toList();
    }

    @BeforeEach
    void setUp() {
        first = player("Pierre", HeroClass.SAMURAI);
        second = player("Kayne", HeroClass.NINJA);
        engine = new GameEngine(first, second, new Random(42));
        engine.addListener((e, s) -> events.add(e));
        engine.startMatch();
        engine.beginFirstTurn();              // first is active
        events.clear();
    }

    @Test
    void busOnlyCallsHandlersOfThatType() {
        List<GameEvent> heard = new ArrayList<>();
        EventBus bus = new EventBus();
        bus.subscribe(Type.HEAL, heard::add);
        bus.publish(GameEvent.of(Type.DAMAGE));
        bus.publish(GameEvent.of(Type.HEAL));
        assertEquals(1, heard.size());
        assertEquals(Type.HEAL, heard.get(0).type());
    }

    @Test
    void seppukuRunsWhenMinionDiesInCombat() {
        engine.getCardEffects().register("ashigaru", Trigger.SEPPUKU, Effects.damageEnemyHero(2));
        Minion kappa = ready(first, "kappa", 5, 5);
        Minion ashigaru = ready(second, "ashigaru", 2, 3);

        assertTrue(engine.attack(kappa, ashigaru));
        assertEquals(Hero.START_HEALTH - 2, first.getHero().getHealth());  // Ashigaru's owner hits my hero
        assertTrue(types().contains(Type.MINION_DIED));
    }

    @Test
    void banzaiDamagesItsTarget() {
        engine.getCardEffects().register("kappa", Trigger.BANZAI, Effects.damageTarget(3));
        Minion kappa = ready(first, "kappa", 5, 5);
        Minion saru = ready(second, "saru", 2, 2);

        engine.runTrigger(Trigger.BANZAI, first, kappa, saru);
        assertFalse(second.getSide().getMinions().contains(saru));
        assertEquals(List.of(Type.DAMAGE, Type.MINION_DIED), types());
    }

    @Test
    void turnEndEffectGivesArmour() {
        engine.getCardEffects().register("hatamoto", Trigger.TURN_END, Effects.gainArmour(3));
        ready(first, "hatamoto", 2, 4);

        engine.endTurn();
        assertEquals(3, first.getHero().getArmour());
        assertTrue(types().contains(Type.ARMOUR_GAINED));
    }

    @Test
    void turnStartEffectOnlyForItsOwner() {
        engine.getCardEffects().register("scout", Trigger.TURN_START, Effects.draw(1));
        ready(second, "scout", 1, 1);

        engine.endTurn();                                  // second's turn starts
        assertEquals(TurnManager.OPENING_HAND_SECOND + TurnManager.DRAW_PER_TURN + 1, second.getHand().size());  // opening, normal draw, scout draw
        }

    @Test
    void seppukuDamageCanEndTheGame() {
        engine.getCardEffects().register("ashigaru", Trigger.SEPPUKU, Effects.damageEnemyHero(2));
        first.getHero().takeDamage(Hero.START_HEALTH - 2);  // my hero at 2 HP
        Minion kappa = ready(first, "kappa", 5, 5);
        Minion ashigaru = ready(second, "ashigaru", 2, 3);

        engine.attack(kappa, ashigaru);
        GameEvent last = events.get(events.size() - 1);
        assertEquals(Type.GAME_OVER, last.type());
        assertSame(second, last.target());
    }

    @Test
    void healAndBuffEffects() {
        first.getHero().takeDamage(10);
        engine.getCardEffects().register("miko", Trigger.BANZAI,
                Effects.healOwnHero(4).andThen(Effects.buffSelf(1, 1)));
        Minion miko = ready(first, "miko", 1, 1);

        engine.runTrigger(Trigger.BANZAI, first, miko, null);
        assertEquals(Hero.START_HEALTH - 6, first.getHero().getHealth());
        assertEquals(2, miko.getAttack());
        assertEquals(2, miko.getHealth());
    }
}