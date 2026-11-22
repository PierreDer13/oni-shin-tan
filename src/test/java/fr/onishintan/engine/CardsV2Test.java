package fr.onishintan.engine;

import fr.onishintan.data.AdventureDecks;
import fr.onishintan.data.CardRepository;
import fr.onishintan.model.Card;
import fr.onishintan.model.HeroClass;
import fr.onishintan.model.KatanaTier;
import fr.onishintan.model.Keyword;
import fr.onishintan.model.Minion;
import fr.onishintan.model.MinionCard;
import fr.onishintan.model.Player;

import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The 12 cards added on 22 Nov (Cards v2): each effect played once in a real match. */
class CardsV2Test {
    private GameEngine engine;
    private Player me;
    private Player foe;

    /** A match where it is my turn 1, with plenty of Ki. */
    private void start(HeroClass mine, HeroClass theirs) {
        AdventureDecks decks = new AdventureDecks(CardRepository.all());
        me = decks.hero("Me", mine);
        foe = decks.hero("Foe", theirs);
        engine = new GameEngine(me, foe, new Random(1));
        engine.setCatalog(CardRepository.all());
        engine.startMatch();
        engine.beginFirstTurn();
        me.gainTemporaryKi(30);
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
    void shurikenUchiAddsTwoShurikenDamage() {
        start(HeroClass.NINJA, HeroClass.ONI);
        assertTrue(engine.playCard(inHand("shuriken-uchi"), 0, null));
        int before = foe.getHero().getHealth();
        engine.useHeroPower(foe.getHero());
        assertEquals(8, before - foe.getHero().getHealth());
    }

    @Test
    void kagemushaSavesTheHeroOnceUntilItsNextTurn() {
        start(HeroClass.NINJA, HeroClass.ONI);
        assertTrue(engine.playCard(inHand("kagemusha"), 0, null));
        engine.dealDamage(null, me.getHero(), 500);
        assertFalse(engine.getState().isOver());
        assertEquals(1, me.getHero().getHealth());
        engine.dealDamage(null, me.getHero(), 50);
        assertEquals(1, me.getHero().getHealth(), "untouchable until my next turn");
        engine.endTurn();
        engine.endTurn();
        assertFalse(me.getHero().isUntouchable());
        engine.dealDamage(null, me.getHero(), 5);
        assertTrue(engine.getState().isOver(), "only once per match");
    }

    @Test
    void suitonNoJutsuReturnsAFriendlyMinionToTheHand() {
        start(HeroClass.NINJA, HeroClass.ONI);
        Minion tengu = place(me, "tengu", 0);
        assertTrue(engine.playCard(inHand("suiton-no-jutsu"), -1, tengu));
        assertTrue(me.getSide().getMinions().isEmpty());
        assertTrue(me.getHand().stream().anyMatch(card -> card.getId().equals("tengu")));
    }

    @Test
    void zeniNageSpendsAllTheKiAsDamage() {
        start(HeroClass.NINJA, HeroClass.ONI);
        int ki = me.getKi();
        int before = foe.getHero().getHealth();
        assertTrue(engine.playCard(inHand("zeni-nage"), -1, null));
        assertEquals(0, me.getKi());
        assertEquals(ki, before - foe.getHero().getHealth(), "no enemy minion: every coin hits the hero");
        assertFalse(engine.canPlay(inHand("zeni-nage")), "nothing to throw at 0 Ki");
    }

    @Test
    void shishimaiGrowsWithEachMiko() {
        start(HeroClass.KANNUSHI, HeroClass.ONI);
        engine.useHeroPower(null);
        engine.endTurn();
        engine.endTurn();
        me.gainTemporaryKi(30);
        engine.useHeroPower(null);
        assertTrue(engine.playCard(inHand("shishimai"), 5, null));
        Minion shishimai = me.getSide().getAt(5);
        assertEquals(6, shishimai.getAttack());
        assertEquals(8, shishimai.getHealth());
    }

    @Test
    void kotodamaSilencesAndHeals() {
        start(HeroClass.KANNUSHI, HeroClass.ONI);
        Minion yokozuna = place(foe, "yokozuna", 0);
        engine.dealDamage(null, me.getHero(), 20);
        int before = me.getHero().getHealth();
        assertTrue(engine.playCard(inHand("kotodama"), -1, yokozuna));
        assertTrue(engine.getMinionEffects().isSilenced(yokozuna));
        assertTrue(yokozuna.getKeywords().isEmpty());
        assertEquals(before + 5, me.getHero().getHealth());
    }

    @Test
    void gashadokuroCostsLessForEachDeath() {
        start(HeroClass.ONI, HeroClass.NINJA);
        Card gashadokuro = inHand("gashadokuro");
        assertEquals(10, engine.costOf(me, gashadokuro));
        engine.destroy(place(foe, "saru", 0));
        engine.destroy(place(foe, "saru", 1));
        assertEquals(8, engine.costOf(me, gashadokuro));
    }

    @Test
    void chimatsuriSacrificesForItsAttack() {
        start(HeroClass.ONI, HeroClass.NINJA);
        Minion tora = place(me, "tora", 0);
        int before = foe.getHero().getHealth();
        assertTrue(engine.playCard(inHand("chimatsuri"), -1, tora));
        assertTrue(tora.isDead());
        assertEquals(before - 10, foe.getHero().getHealth());
    }

    @Test
    void omukadeAttacksTheTurnItIsPlayed() {
        start(HeroClass.ONI, HeroClass.NINJA);
        assertTrue(engine.playCard(inHand("omukade"), 0, null));
        int before = foe.getHero().getHealth();
        assertTrue(engine.attack(me.getSide().getAt(0), foe.getHero()));
        assertEquals(before - 16, foe.getHero().getHealth());
    }

    @Test
    void totsugekiGivesTatakaeAndFourAttack() {
        start(HeroClass.SAMURAI, HeroClass.NINJA);
        Minion farmer = place(me, "kyabetsu-noka", 0);
        int attack = farmer.getAttack();
        assertTrue(engine.playCard(inHand("totsugeki"), -1, null));
        assertEquals(attack + 4, farmer.getAttack());
        assertTrue(farmer.hasKeyword(Keyword.TATAKAE));
    }

    @Test
    void mamoriShuGivesShirodu() {
        start(HeroClass.SAMURAI, HeroClass.NINJA);
        Minion ashigaru = place(me, "ashigaru", 0);
        assertTrue(engine.playCard(inHand("mamori-shu"), 1, ashigaru));
        assertTrue(ashigaru.hasKeyword(Keyword.SHIRODU));
    }

    @Test
    void koshoStrikesWhatTheHeroStrikes() {
        start(HeroClass.SAMURAI, HeroClass.NINJA);
        place(me, "kosho", 0);
        Minion target = place(foe, "kyabetsu-noka", 0);
        engine.useKatana(KatanaTier.TANTO);
        assertTrue(engine.attack(me.getHero(), target));
        assertTrue(target.isDead(), "Tanto 5 + Kosho 3 against 6 HP");
    }
}
