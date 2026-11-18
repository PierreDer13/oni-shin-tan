package fr.onishintan.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import fr.onishintan.model.*;

/** Curses: hand Curses act while held and can be removed, deck Curses act when drawn. */
class CurseTest {

    private Player me;
    private Player enemy;
    private GameEngine engine;
    private final List<GameEvent.Type> seen = new ArrayList<>();

    private static Player player(String name, HeroClass cls, int deckSize) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < deckSize; i++) {
            cards.add(new MinionCard(name + "-" + i, "m", "試", "てすと", 1, Rarity.COMMON,
                    HeroClass.NEUTRAL, "", "", 1, 1, EnumSet.noneOf(Keyword.class)));
        }
        return new Player(name, new Hero(cls), new Deck(cards));
    }

    private static CurseCard handCurse() {
        return new CurseCard("test-hand", "Noroi", "呪い", "のろい", 2, Rarity.COMMON,
                HeroClass.ONI, "", "", CurseZone.HAND, 1, 2);
    }

    private static CurseCard deckCurse() {
        return new CurseCard("test-deck", "Noroi", "呪い", "のろい", 2, Rarity.COMMON,
                HeroClass.ONI, "", "", CurseZone.DECK, 1, 0);
    }

    /** enemyDeck = 5: his opening hand empties it, so his next draw is the Curse. */
    private void start(int enemyDeck) {
        me = player("Pierre", HeroClass.ONI, Deck.SIZE);
        enemy = player("Kayne", HeroClass.NINJA, enemyDeck);
        engine = new GameEngine(me, enemy, new Random(42));
        engine.addListener((e, s) -> seen.add(e.type()));
        engine.startMatch();
        engine.beginFirstTurn();
        me.gainTemporaryKi(10);
    }

    @Test
    void handCurseGoesToTheEnemyAndCannotBePlayedBack() {
        start(Deck.SIZE);
        CurseCard curse = handCurse();
        me.addToHand(curse);
        assertTrue(engine.playCard(curse, 0, null));
        assertTrue(enemy.getHand().contains(curse));
        assertTrue(enemy.isHeldCurse(curse));
        engine.endTurn();
        enemy.gainTemporaryKi(10);
        assertFalse(engine.playCard(curse, 0, null));
        assertTrue(enemy.getHand().contains(curse));
    }

    @Test
    void curseCannotBeTraded() {
        start(Deck.SIZE);
        CurseCard mine = handCurse();
        me.addToHand(mine);
        assertFalse(GameEngine.isTradeable(mine));
        assertFalse(engine.trade(mine));                   // yours to give: refused
        assertTrue(me.getHand().contains(mine));
        assertTrue(engine.playCard(mine, 0, null));
        engine.endTurn();
        enemy.gainTemporaryKi(10);
        assertFalse(engine.trade(mine));                   // held by the enemy: refused too
        assertTrue(enemy.getHand().contains(mine));
        assertFalse(seen.contains(GameEvent.Type.CARD_TRADED));
    }

    @Test
    void holderPaysTheRemovalCost() {
        start(Deck.SIZE);
        CurseCard curse = handCurse();
        me.addToHand(curse);
        engine.playCard(curse, 0, null);
        engine.endTurn();
        int ki = enemy.getKi();
        assertTrue(engine.removeCurse(curse));
        assertEquals(ki - curse.getRemovalCost(), enemy.getKi());
        assertFalse(enemy.getHand().contains(curse));
        assertTrue(seen.contains(GameEvent.Type.CURSE_REMOVED));
    }

    @Test
    void handCurseActsAtTheEndOfTheHoldersTurnOnly() {
        start(Deck.SIZE);
        CurseCard curse = handCurse();
        me.addToHand(curse);
        engine.playCard(curse, 0, null);
        engine.endTurn();                                    // my turn ends: nothing
        assertFalse(seen.contains(GameEvent.Type.CURSE_TRIGGERED));
        engine.endTurn();                                    // his turn ends: it acts
        assertTrue(seen.contains(GameEvent.Type.CURSE_TRIGGERED));
        assertTrue(enemy.getHand().contains(curse));        // still held
    }

    @Test
    void deckCurseIsShuffledIn() {
        start(Deck.SIZE);
        int size = enemy.getDeck().size();
        CurseCard curse = deckCurse();
        me.addToHand(curse);
        assertTrue(engine.playCard(curse, 0, null));
        assertEquals(size + curse.getCopies(), enemy.getDeck().size());
        assertFalse(enemy.getHand().contains(curse));
    }

    @Test
    void drawnDeckCurseActsThenIsGone() {
        start(5);
        assertTrue(enemy.getDeck().isEmpty());
        CurseCard curse = deckCurse();
        me.addToHand(curse);
        engine.playCard(curse, 0, null);
        engine.endTurn();                                    // he draws the Curse
        assertTrue(seen.contains(GameEvent.Type.CURSE_TRIGGERED));
        assertFalse(enemy.getHand().contains(curse));
    }
}