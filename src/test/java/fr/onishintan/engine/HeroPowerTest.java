package fr.onishintan.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import fr.onishintan.model.*;

/** Hero powers: Shuriken, Miko, Jikininki, Katana ladder, once per turn. */
class HeroPowerTest {

    private Player me;
    private Player enemy;
    private GameEngine engine;

    private static Player player(String name, HeroClass cls) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < Deck.SIZE; i++) {
            cards.add(new MinionCard(name + "-" + i, "m", "試", "てすと", 1, Rarity.COMMON,
                    HeroClass.NEUTRAL, "", "", 1, 1, EnumSet.noneOf(Keyword.class)));
        }
        return new Player(name, new Hero(cls), new Deck(cards));
    }

    private void start(HeroClass mine) {
        me = player("Pierre", mine);
        enemy = player("Kayne", HeroClass.NINJA);
        engine = new GameEngine(me, enemy, new Random(42));
        engine.startMatch();
        engine.beginFirstTurn();
        me.gainTemporaryKi(10);
    }

    @Test
    void shurikenDeals6OncePerTurn() {
        start(HeroClass.NINJA);
        int ki = me.getKi();
        assertTrue(engine.useHeroPower(enemy.getHero()));
        assertEquals(Hero.START_HEALTH - GameEngine.SHURIKEN_DAMAGE, enemy.getHero().getHealth());
        assertEquals(ki - GameEngine.HERO_POWER_COST, me.getKi());
        assertFalse(engine.useHeroPower(enemy.getHero()));
    }

    @Test
    void shurikenNeedsATarget() {
        start(HeroClass.NINJA);
        int ki = me.getKi();
        assertFalse(engine.useHeroPower(null));
        assertEquals(ki, me.getKi());
        assertFalse(engine.isHeroPowerUsed());
    }

    @Test
    void mikoSummonsA3x3() {
        start(HeroClass.KANNUSHI);
        assertTrue(engine.useHeroPower(null));
        Minion miko = me.getSide().getAt(0);
        assertNotNull(miko);
        assertEquals(3, miko.getAttack());
        assertEquals(3, miko.getHealth());
    }

    /** Uses the Kannushi power, then a full round back to my turn with Ki. */
    private void mikoTurn() {
        assertTrue(engine.useHeroPower(null));
        engine.endTurn();
        engine.endTurn();
        me.gainTemporaryKi(10);
    }

    @Test
    void mikoTokenUsesTheMikoArtId() {
        start(HeroClass.KANNUSHI);
        assertTrue(engine.useHeroPower(null));
        assertEquals("miko", me.getSide().getAt(0).getSource().getId());
    }

    @Test
    void everyThirdMikoCallGivesEveryMikoPlusOne() {
        start(HeroClass.KANNUSHI);
        mikoTurn();
        mikoTurn();
        assertEquals(3, me.getSide().getAt(0).getAttack());     // 2 calls: no bonus yet
        mikoTurn();                                              // 3rd call: all +1/+1
        for (int slot = 0; slot < 3; slot++) {
            assertEquals(4, me.getSide().getAt(slot).getAttack());
            assertEquals(4, me.getSide().getAt(slot).getHealth());
        }
        assertTrue(engine.useHeroPower(null));                   // 4th: arrives as a 4/4
        assertEquals(4, me.getSide().getAt(3).getAttack());
        assertEquals(4, me.getSide().getAt(3).getHealth());
    }

    @Test
    void aMikoCardPlayedLaterGetsTheBonus() {
        start(HeroClass.KANNUSHI);
        mikoTurn();
        mikoTurn();
        mikoTurn();
        Card mikoCard = new MinionCard("miko", "Miko", "巫女", "みこ", 1, Rarity.COMMON,
                HeroClass.KANNUSHI, "", "", 3, 3, EnumSet.noneOf(Keyword.class));
        me.removeFromHand(me.getHand().get(0));                  // room in a full hand
        me.addToHand(mikoCard);
        assertTrue(engine.playCard(mikoCard, 5, null));
        assertEquals(4, me.getSide().getAt(5).getAttack());
    }

    @Test
    void jikininkiDrainsAndHeals() {
        start(HeroClass.ONI);
        me.getHero().takeDamage(10);
        assertTrue(engine.useHeroPower(enemy.getHero()));
        assertEquals(96, enemy.getHero().getHealth());
        assertEquals(94, me.getHero().getHealth());
    }

    @Test
    void katanaEquipsTheChosenBlade() {
        start(HeroClass.SAMURAI);
        int ki = me.getKi();
        assertFalse(engine.useHeroPower(enemy.getHero()));   // Samurai goes through useKatana
        assertTrue(engine.useKatana(KatanaTier.KATANA));
        assertEquals(8, me.getHero().getAttack());
        assertEquals(ki - KatanaTier.KATANA.getKiCost(), me.getKi());
        assertFalse(engine.useKatana(KatanaTier.BOKKEN));     // once per turn
    }

    @Test
    void katanaOnlyForSamurai() {
        start(HeroClass.NINJA);
        assertFalse(engine.useKatana(KatanaTier.BOKKEN));
    }

    @Test
    void powerResetsNextTurn() {
        start(HeroClass.NINJA);
        assertTrue(engine.useHeroPower(enemy.getHero()));
        engine.endTurn();
        engine.endTurn();
        me.gainTemporaryKi(10);
        assertTrue(engine.useHeroPower(enemy.getHero()));
    }
}