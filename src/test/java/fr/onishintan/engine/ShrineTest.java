package fr.onishintan.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import fr.onishintan.model.*;

/** Shrines: take a slot, are not minions, leave after their duration. */
class ShrineTest {

    private Player me;
    private Player enemy;
    private GameEngine engine;

    private static Player player(String name) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < Deck.SIZE; i++) {
            cards.add(new MinionCard(name + "-" + i, "m", "試", "てすと", 1, Rarity.COMMON,
                    HeroClass.NEUTRAL, "", "", 1, 1, EnumSet.noneOf(Keyword.class)));
        }
        return new Player(name, new Hero(HeroClass.KANNUSHI), new Deck(cards));
    }

    private static ShrineCard shrine(int duration) {
        return new ShrineCard("test-shrine", "Jinja", "神社", "じんじゃ", 1, Rarity.COMMON,
                HeroClass.KANNUSHI, "", "", duration);
    }

    private void start() {
        me = player("Pierre");
        enemy = player("Kayne");
        engine = new GameEngine(me, enemy, new Random(42));
        engine.startMatch();
        engine.beginFirstTurn();
        me.gainTemporaryKi(10);
    }

    @Test
    void shrineTakesASlotButIsNotAMinion() {
        BoardSide side = new BoardSide();
        Shrine s = new Shrine(shrine(2));
        side.place(s, 3);
        assertFalse(side.isFree(3));
        assertNull(side.getAt(3));
        assertSame(s, side.entityAt(3));
        assertEquals(0, side.size());
        assertEquals(List.of(s), side.shrines());
    }

    @Test
    void shrineIsPlayedOnAFreeSlotOnly() {
        start();
        ShrineCard card = shrine(2);
        me.addToHand(card);
        assertTrue(engine.playCard(card, 2, null));
        assertTrue(me.getSide().entityAt(2) instanceof Shrine);

        ShrineCard second = shrine(2);
        me.addToHand(second);
        assertFalse(engine.playCard(second, 2, null));     // slot taken: nothing spent
        assertTrue(me.getHand().contains(second));
    }

    @Test
    void shrineExpiresAtTheEndOfItsOwnersLastTurn() {
        start();
        ShrineCard card = shrine(2);
        me.addToHand(card);
        engine.playCard(card, 0, null);
        engine.endTurn();                                  // my turn 1 ends: 1 left
        assertEquals(1, me.getSide().shrines().get(0).getTurnsLeft());
        engine.endTurn();                                  // enemy's turn: no tick for me
        assertEquals(1, me.getSide().shrines().get(0).getTurnsLeft());
        engine.endTurn();                                  // my turn 2 ends: gone
        assertTrue(me.getSide().shrines().isEmpty());
        assertTrue(me.getSide().isFree(0));
    }

    @Test
    void shrineExpiredEventIsPublished() {
        start();
        List<GameEvent.Type> seen = new ArrayList<>();
        engine.addListener((e, s) -> seen.add(e.type()));
        ShrineCard card = shrine(1);
        me.addToHand(card);
        engine.playCard(card, 4, null);
        engine.endTurn();
        assertTrue(seen.contains(GameEvent.Type.SHRINE_PLACED));
        assertTrue(seen.contains(GameEvent.Type.SHRINE_EXPIRED));
    }
}