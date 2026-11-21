package fr.onishintan.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import fr.onishintan.model.*;

import fr.onishintan.engine.event.GameEvent;
/** Rites: 4/4 Ancestors now, lasting bonus on the Ancestors summoned later. */
class RiteTest {

    private Player me;
    private Player enemy;
    private GameEngine engine;
    private final List<GameEvent.Type> seen = new ArrayList<>();

    private static Player player(String name, HeroClass cls) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < Deck.SIZE; i++) {
            cards.add(new MinionCard(name + "-" + i, "m", "試", "てすと", 1, Rarity.COMMON,
                    HeroClass.NEUTRAL, "", "", 1, 1, EnumSet.noneOf(Keyword.class)));
        }
        return new Player(name, new Hero(cls), new Deck(cards));
    }

    private static RiteCard rite(int ancestors, Set<Keyword> now, int atk, int hp, Set<Keyword> bonus) {
        return new RiteCard("test-rite", "Gishiki", "儀式", "ぎしき", 1, Rarity.COMMON,
                HeroClass.SAMURAI, "", "", ancestors, now, atk, hp, bonus);
    }

    private void start() {
        me = player("Pierre", HeroClass.SAMURAI);
        enemy = player("Kayne", HeroClass.NINJA);
        engine = new GameEngine(me, enemy, new Random(42));
        engine.addListener((e, s) -> seen.add(e.type()));
        engine.startMatch();
        engine.beginFirstTurn();
        me.gainTemporaryKi(10);
    }

    private void play(RiteCard r, int slot) {
        me.addToHand(r);
        assertTrue(engine.playCard(r, slot, null));
    }

    @Test
    void riteSummonsAPlainAncestorOnTheChosenSlot() {
        start();
        play(rite(1, Set.of(), 1, 1, Set.of()), 3);
        Minion a = me.getSide().getAt(3);
        assertNotNull(a);
        assertEquals(RiteCard.ANCESTOR_ATK, a.getAttack());
        assertEquals(RiteCard.ANCESTOR_HP, a.getHealth());
        assertTrue(seen.contains(GameEvent.Type.RITE_PERFORMED));
    }

    @Test
    void lastingBonusOnlyAppliesToLaterAncestorsAndStacks() {
        start();
        play(rite(1, Set.of(), 1, 1, Set.of()), 0);     // 4/4
        play(rite(1, Set.of(), 1, 1, Set.of()), 1);     // 5/5
        play(rite(1, Set.of(), 1, 1, Set.of()), 2);     // 6/6
        assertEquals(4, me.getSide().getAt(0).getAttack());
        assertEquals(5, me.getSide().getAt(1).getHealth());
        assertEquals(6, me.getSide().getAt(2).getAttack());
    }

    @Test
    void nowKeywordsAndLastingKeywordsAddUp() {
        start();
        play(rite(1, Set.of(Keyword.TORII), 0, 0, Set.of(Keyword.TORII)), 0);
        play(rite(1, Set.of(Keyword.TORII), 0, 0, Set.of(Keyword.TORII)), 1);
        assertEquals(1, me.getSide().getAt(0).getToriiStacks());
        assertEquals(2, me.getSide().getAt(1).getToriiStacks());
    }

    @Test
    void severalAncestorsFillFreeSlotsAndAFullBoardIsRefused() {
        start();
        play(rite(4, Set.of(), 0, 0, Set.of()), 2);     // 2, 3, 4, 6: board full        assertNotNull(me.getSide().getAt(5));
        assertNotNull(me.getSide().getAt(0));
        assertNotNull(me.getSide().getAt(1));
        play(rite(3, Set.of(), 0, 0, Set.of()), 2);     // 2, 3, 4: board full
        assertTrue(me.getSide().isFull());
        RiteCard last = rite(1, Set.of(), 0, 0, Set.of());
        me.addToHand(last);
        int ki = me.getKi();
        assertFalse(engine.playCard(last, 0, null));
        assertEquals(ki, me.getKi());
    }
}