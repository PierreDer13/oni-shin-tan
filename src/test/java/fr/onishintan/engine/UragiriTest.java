package fr.onishintan.engine;

import fr.onishintan.data.AdventureDecks;
import fr.onishintan.data.CardRepository;
import fr.onishintan.engine.system.HeroPowers;
import fr.onishintan.model.Card;
import fr.onishintan.model.GodaiElement;
import fr.onishintan.model.HeroClass;
import fr.onishintan.model.Keyword;
import fr.onishintan.model.Minion;
import fr.onishintan.model.MinionCard;
import fr.onishintan.model.OathType;
import fr.onishintan.model.Player;
import fr.onishintan.ai.AIController;

import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Uragiri cards (22-23 Nov): each works only while its owner is an Oathbreaker, and Takeda Shingen's Godai. */
class UragiriTest {
    private GameEngine engine;
    private Player me;
    private Player foe;

    /** A PvP match (both Oaths sworn, so the clocks run) where it is my turn 1 with plenty of Ki; broken: my Oath is broken. */
    private void start(boolean broken) {
        AdventureDecks decks = new AdventureDecks(CardRepository.all());
        me = decks.hero("Me", HeroClass.SAMURAI);
        foe = decks.hero("Foe", HeroClass.NINJA);
        engine = new GameEngine(me, foe, new Random(1));
        engine.setCatalog(CardRepository.all());
        engine.swearOath(me, OathType.HONOR);
        engine.swearOath(foe, OathType.SILENCE);
        engine.startMatch();
        engine.beginFirstTurn();
        me.gainTemporaryKi(30);
        if (broken) {
            me.getOath().breakOath();
        }
    }

    /** The card with this id, put in my hand. */
    private Card inHand(String id) {
        Card card = CardRepository.byId(id);
        me.addToHand(card);
        return card;
    }

    /** A minion of this id placed straight on p's side. */
    private static Minion place(Player p, String id, int slot) {
        Minion minion = new Minion((MinionCard) CardRepository.byId(id));
        p.getSide().place(minion, slot);
        return minion;
    }

    @Test
    void nothingHappensWhileTheOathIsSworn() {
        start(false);
        int before = foe.getHero().getHealth();
        assertTrue(engine.playCard(inHand("kunisada-chuji"), 0, null));
        assertEquals(before, foe.getHero().getHealth(), "no die roll while the Oath holds");
        assertFalse(engine.isOathbreaker(me));
    }

    @Test
    void matsunagaExplodesOnTheMinionsFacingHim() {
        start(true);
        assertTrue(engine.playCard(inHand("matsunaga-hisahide"), 3, null));
        Minion left = place(foe, "kyabetsu-noka", 2);
        Minion facing = place(foe, "kyabetsu-noka", 3);
        Minion right = place(foe, "kyabetsu-noka", 4);
        Minion far = place(foe, "kyabetsu-noka", 5);
        engine.destroy(me.getSide().getAt(3));
        assertTrue(left.isDead() && facing.isDead() && right.isDead(), "6 damage kills the three 5/6 farmers");
        assertFalse(far.isDead(), "two slots away: untouched");
    }

    @Test
    void oishiGrowsAtTheEndOfEachOfHisOwnersTurns() {
        start(true);
        assertTrue(engine.playCard(inHand("oishi-kuranosuke"), 0, null));
        Minion oishi = me.getSide().getAt(0);
        engine.endTurn();
        assertEquals(3, oishi.getAttack());
        assertEquals(3, oishi.getHealth());
        assertTrue(oishi.hasKeyword(Keyword.SHINOBI));
    }

    @Test
    void akechiTakesThirtySecondsFromTheEnemyClock() {
        start(true);
        long before = engine.remainingMs(foe);
        assertTrue(engine.playCard(inHand("akechi-mitsuhide"), 0, null));
        assertEquals(before - 30_000, engine.remainingMs(foe));
    }

    @Test
    void kunisadaRollsDamageAndKiLossButLeavesOneKi() {
        start(true);
        int before = foe.getHero().getHealth();
        assertTrue(engine.playCard(inHand("kunisada-chuji"), 0, null));
        int roll = before - foe.getHero().getHealth();
        assertTrue(roll >= 1 && roll <= 6, "a die: " + roll);
        engine.endTurn();
        int full = foe.getMaxKi() + 1;   // the second player's first turn: +1 Ki
        assertEquals(Math.max(1, full - roll), foe.getKi());
    }

    @Test
    void izanagiTakesACardFromTheEnemyHand() {
        start(true);
        assertTrue(engine.playCard(inHand("izanagi"), 0, null));
        assertEquals(me, engine.pendingSteal());
        Card taken = foe.getHand().get(0);
        int theirs = foe.getHand().size();
        assertTrue(engine.steal(taken));
        assertEquals(theirs - 1, foe.getHand().size());
        assertTrue(me.getHand().contains(taken));
        assertFalse(engine.steal(foe.getHand().get(0)), "only one card");
    }

    @Test
    void shingenTurnsTheHeroPowerIntoGodai() {
        start(true);
        assertTrue(engine.playCard(inHand("takeda-shingen"), 0, null));
        assertTrue(engine.hasGodai(me));
        assertFalse(engine.useKatana(fr.onishintan.model.KatanaTier.BOKKEN), "the katana is gone");
        int before = foe.getHero().getHealth();
        assertTrue(engine.useGodai(GodaiElement.KA, foe.getHero()));
        assertEquals(before - HeroPowers.GODAI_FIRE, foe.getHero().getHealth());
        assertFalse(engine.useGodai(GodaiElement.SUI, me.getHero()), "once per turn");
    }

    @Test
    void godaiEarthWaterWindAndVoid() {
        start(true);
        assertTrue(engine.playCard(inHand("takeda-shingen"), 0, null));
        Minion shingen = me.getSide().getAt(0);
        assertTrue(engine.useGodai(GodaiElement.CHI, shingen));
        assertTrue(shingen.hasKeyword(Keyword.SHIRODU));
        assertEquals(8 + HeroPowers.GODAI_EARTH_HP, shingen.getHealth());
        assertEquals(HeroPowers.GODAI_ARMOUR, me.getHero().getArmour());

        engine.endTurn();
        engine.endTurn();
        me.gainTemporaryKi(30);
        engine.dealDamage(null, me.getHero(), 20);
        int hp = me.getHero().getHealth();
        int cards = me.getHand().size();
        assertTrue(engine.useGodai(GodaiElement.SUI, me.getHero()));
        assertTrue(me.getHero().getHealth() > hp);
        assertEquals(cards + 1, me.getHand().size());

        engine.endTurn();
        engine.endTurn();
        me.gainTemporaryKi(30);
        assertTrue(engine.attack(shingen, foe.getHero()));
        assertFalse(shingen.canAttack());
        assertTrue(engine.useGodai(GodaiElement.FU, shingen));
        assertTrue(shingen.canAttack(), "Wind: it may strike again");
        assertEquals(7 + HeroPowers.GODAI_WIND_ATK, shingen.getAttack());

        engine.endTurn();
        engine.endTurn();
        me.gainTemporaryKi(30);
        assertTrue(engine.useGodai(GodaiElement.KU, shingen));
        assertEquals(7, shingen.getAttack(), "Void: back to its printed 7 ATK");
        assertFalse(shingen.hasKeyword(Keyword.SHIRODU));
    }

    @Test
    void theAiUsesGodaiAndStealsWithIzanagi() {
        start(true);
        AIController ai = new AIController(engine, me, AIController.Difficulty.HARD, new Random(3));
        assertTrue(engine.playCard(inHand("takeda-shingen"), 0, null));
        int theirs = foe.getHand().size();
        assertTrue(ai.play(fr.onishintan.ai.Move.play(inHand("izanagi"), 1, null, null)));
        assertEquals(theirs - 1, foe.getHand().size(), "the AI took a card");
        ai.playTurn();
        assertTrue(engine.isHeroPowerUsed() || engine.getState().active() == foe, "Godai used during its turn");
    }
}
