package fr.onishintan.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.onishintan.model.*;

/** The minion effects of MockCards by id: Banzai and their targets, Seppuku, passives, auras, Rokurokubi, Yogake. */
class MinionEffectTest {

    private Player me;        // Samurai, plays first
    private Player enemy;     // Ninja
    private GameEngine engine;

    private static MinionCard card(String id, int ki, int atk, int hp, Keyword... keywords) {
        EnumSet<Keyword> set = EnumSet.noneOf(Keyword.class);
        for (Keyword k : keywords) set.add(k);
        return new MinionCard(id, id, "試", "", ki, Rarity.COMMON, HeroClass.NEUTRAL, "", "", atk, hp, set);
    }

    private static MinionCard card(String id, int atk, int hp, Keyword... keywords) {
        return card(id, 1, atk, hp, keywords);
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

    /** Plays a minion card of mine with that id from my hand. */
    private boolean play(MinionCard c, int slot, Combatant target) {
        me.addToHand(c);
        return engine.playCard(c, slot, target);
    }

    @BeforeEach
    void setUp() {
        me = player("Pierre", HeroClass.SAMURAI);
        enemy = player("Kayne", HeroClass.NINJA);
        engine = new GameEngine(me, enemy, new Random(42));
        engine.setCatalog(List.of(card("koi", 2, 2), card("sakana-uri", 5, 5), card("kitsune-no-tsukai", 5, 5, Keyword.TORII),
                new CurseCard("tatari", "Tatari", "祟り", "", 3, Rarity.COMMON, HeroClass.ONI, "", "", CurseZone.DECK, 5, 0)));
        engine.startMatch();
        engine.beginFirstTurn();
        me.gainTemporaryKi(30);
    }

    // ---------- Banzai targets ----------

    @Test
    void aBanzaiWithLegalTargetsNeedsOneAndHitsIt() {
        Minion e = place(enemy, 0, card("e", 1, 10));
        MinionCard yumihiki = card("yumihiki", 4, 3);
        assertEquals(SpellTarget.CHARACTER, engine.banzaiTarget(yumihiki));
        me.addToHand(yumihiki);
        assertFalse(engine.playCard(yumihiki, 0, null), "a target is needed");
        assertTrue(engine.playCard(yumihiki, 0, e));
        assertEquals(6, e.getHealth());
    }

    @Test
    void aBanzaiWithNoLegalTargetIsPlayedWithoutOne() {
        place(enemy, 0, card("full", 1, 10));                    // not damaged: Shinigami has nothing to kill
        MinionCard shinigami = card("shinigami", 10, 11);
        assertTrue(engine.banzaiTargets(shinigami).isEmpty());
        assertTrue(play(shinigami, 0, null));
    }

    @Test
    void healBanzais() {
        me.getHero().takeDamage(30);
        assertTrue(play(card("unsui", 2, 3), 0, me.getHero()));
        assertEquals(76, me.getHero().getHealth());
        assertTrue(play(card("yamabushi", 5, 6), 1, me.getHero()));
        assertEquals(88, me.getHero().getHealth());
        assertTrue(play(card("amaterasu-omikami", 17, 17), 2, null));
        assertEquals(100, me.getHero().getHealth());
    }

    @Test
    void shikeSilencesAnEnemyMinionForGood() {
        Minion hatamoto = place(enemy, 0, card("hatamoto", 5, 7, Keyword.SHIRODU));
        assertTrue(play(card("shike", 10, 12), 0, hatamoto));
        assertFalse(hatamoto.hasKeyword(Keyword.SHIRODU));

        engine.endTurn();
        engine.endTurn();                                        // his turn ended: no armour from a silenced Hatamoto
        assertEquals(0, enemy.getHero().getArmour());
    }

    @Test
    void attackChangingBanzais() {
        Minion e = place(enemy, 0, card("e", 7, 10));
        assertTrue(play(card("maiko", 3, 5), 0, e));
        assertEquals(4, e.getAttack());
        assertTrue(play(card("onnagata", 2, 6), 1, e));
        assertEquals(4, me.getSide().getAt(1).getAttack());
    }

    @Test
    void boardBuffBanzais() {
        Minion a = place(me, 0, card("a", 1, 1));
        assertTrue(play(card("taiko-uchi", 6, 6), 1, null));
        assertEquals(3, a.getAttack());
        assertEquals(6, me.getSide().getAt(1).getAttack(), "not itself");
        assertTrue(play(card("shogun", 12, 12), 2, null));
        assertEquals(7, a.getAttack());
        assertEquals(5, a.getHealth());
        assertEquals(16, me.getSide().getAt(2).getAttack(), "itself too");
    }

    @Test
    void weaponBanzaisAndSeppuku() {
        assertTrue(play(card("daimyo", 9, 9), 0, null));
        assertEquals(8, me.getHero().getWeapon().getAttack());
        assertTrue(play(card("katana-kaji", 5, 6), 1, null));
        assertEquals(11, me.getHero().getWeapon().getAttack());
        assertTrue(play(card("takemikazuchi", 10, 9), 2, null));
        assertEquals(10, me.getHero().getWeapon().getAttack());
        assertEquals(2, me.getHero().getWeapon().getDurability());

        engine.destroy(place(me, 3, card("jujutsu-shihan", 6, 7)));
        assertEquals(15, me.getHero().getWeapon().getAttack(), "Honjō Masamune");
    }

    @Test
    void tokenEffectsUseTheCatalog() {
        int hand = me.getHand().size();
        assertTrue(play(card("sakana-uri", 5, 5), 0, null));
        assertEquals(hand + 1, me.getHand().size(), "Sakana uri left, a Koi came");
        assertEquals("koi", me.getHand().get(me.getHand().size() - 1).getId());

        engine.destroy(place(me, 1, card("sendaiku", 4, 6)));
        assertEquals("sakana-uri", me.getSide().getAt(1).getSource().getId());
    }

    @Test
    void monogoiDrawsTwoOnAnEmptyHand() {
        for (Card c : List.copyOf(me.getHand())) me.removeFromHand(c);
        assertTrue(play(card("monogoi", 2, 3), 0, null));
        assertEquals(2, me.getHand().size());
    }

    @Test
    void boardClearBanzais() {
        Minion small = place(enemy, 0, card("s", 3, 5));
        Minion big = place(enemy, 1, card("b", 9, 20));
        assertTrue(play(card("nue", 9, 9), 0, null));
        assertEquals(-1, enemy.getSide().slotOf(small));
        assertEquals(14, big.getHealth());

        Minion mine = place(me, 1, card("m", 2, 2));
        assertTrue(play(card("enma", 13, 14), 2, null));
        assertEquals(-1, me.getSide().slotOf(mine));
        assertNotNull(me.getSide().getAt(0), "Nue (9 ATK) survives Enma");
        assertTrue(enemy.getSide().slotOf(big) >= 0, "9 ATK is above 8");
    }

    @Test
    void jorogumoPutsAMinionToSleepUntilTheEndOfItsOwnersTurn() {
        Minion e = place(enemy, 0, card("e", 3, 9));
        assertTrue(play(card("jorogumo", 6, 10), 0, e));
        engine.endTurn();
        assertFalse(e.canAttack(), "asleep on his turn");
        engine.endTurn();
        engine.endTurn();
        assertTrue(e.canAttack(), "awake the turn after");
    }

    @Test
    void kitsuneTransformsAndFujinBounces() {
        MinionCard tora = card("tora", 10, 6);
        Minion e = place(enemy, 0, tora);
        assertTrue(play(card("kitsune-no-bakashiya", 3, 2, Keyword.SHINOBI), 0, e));
        assertSame(tora, me.getSide().getAt(0).getSource());

        int hand = enemy.getHand().size();
        assertTrue(play(card("fujin", 10, 10), 1, e));
        assertEquals(-1, enemy.getSide().slotOf(e));
        assertEquals(hand + 1, enemy.getHand().size());
    }

    @Test
    void raijinDealsTwelveSplitAmongEnemies() {
        Minion e = place(enemy, 0, card("e", 1, 20));
        assertTrue(play(card("raijin", 8, 8), 0, null));
        int dealt = (100 - enemy.getHero().getHealth()) + (20 - e.getHealth());
        assertEquals(12, dealt);
    }

    @Test
    void tsukuyomiOkuninushiAndIzanami() {
        Minion a = place(me, 0, card("a", 1, 10));
        a.takeDamage(5);
        assertTrue(play(card("tsukuyomi", 12, 10, Keyword.SHINOBI), 1, null));
        assertTrue(a.isHidden());
        assertTrue(play(card("okuninushi", 11, 12), 2, null));
        assertEquals(10, a.getHealth());

        MinionCard cheap = card("cheap", 1, 1, 1);
        MinionCard mid = card("mid", 5, 1, 1);
        MinionCard dear = card("dear", 8, 1, 1);
        for (MinionCard c : List.of(cheap, mid, dear)) engine.destroy(place(me, 6, c));
        assertTrue(play(card("izanami", 11, 11), 3, null));
        List<String> back = me.getSide().getMinions().stream().map(m -> m.getSource().getId()).toList();
        assertTrue(back.contains("dear") && back.contains("mid") && !back.contains("cheap"));
    }

    @Test
    void kijiCopiesACardFromTheOpponentsHand() {
        int hand = me.getHand().size();
        assertTrue(play(card("kiji", 3, 3), 0, null));
        assertEquals(hand + 1, me.getHand().size());
        Card copied = me.getHand().get(me.getHand().size() - 1);
        assertTrue(enemy.getHand().contains(copied));
    }

    // ---------- Seppuku ----------

    @Test
    void seppukuEffects() {
        me.getHero().takeDamage(20);
        engine.destroy(place(me, 0, card("inasaku-noka", 2, 2)));
        assertEquals(88, me.getHero().getHealth());

        MinionCard noppera = card("noppera-bo", 4, 1, Keyword.SHINOBI);
        int hand = me.getHand().size();
        engine.destroy(place(me, 1, noppera));
        assertEquals(hand + 1, me.getHand().size());
        assertTrue(me.getHand().contains(noppera));

        engine.destroy(place(me, 2, card("koi", 2, 2)));
        assertEquals(hand + 2, me.getHand().size());

        int deck = enemy.getDeck().size();
        engine.destroy(place(me, 3, card("oiwa", 9, 9)));
        assertEquals(deck + 2, enemy.getDeck().size());
    }

    // ---------- passives ----------

    @Test
    void endOfTurnPassives() {
        me.getHero().takeDamage(20);
        place(me, 0, card("jushoku", 5, 7));
        place(me, 1, card("komuso", 2, 3, Keyword.SHINOBI));
        place(me, 2, card("hatamoto", 5, 7, Keyword.SHIRODU));
        Minion hurt = place(me, 3, card("goze", 3, 6));
        hurt.takeDamage(4);
        place(me, 4, card("inari-okami", 10, 11));

        engine.endTurn();
        assertEquals(90, me.getHero().getHealth());           // 80 + 6 + 4
        assertEquals(3, me.getHero().getArmour());
        assertEquals(5, hurt.getHealth());
        assertEquals("kitsune-no-tsukai", me.getSide().getAt(5).getSource().getId());
    }

    @Test
    void hakahoriGrowsWhenAFriendlyMinionDies() {
        Minion h = place(me, 0, card("hakahori", 4, 6));
        engine.destroy(place(me, 1, card("a", 1, 1)));
        assertEquals(6, h.getAttack());
    }

    @Test
    void roninAndJoninAuras() {
        Minion ronin = place(me, 0, card("ronin", 7, 7));
        engine.getMinionEffects().refreshAuras();
        assertEquals(13, ronin.getAttack());
        Minion genin = place(me, 1, card("genin", 3, 2, Keyword.SHINOBI));
        engine.getMinionEffects().refreshAuras();
        assertEquals(7, ronin.getAttack(), "not alone any more");

        place(me, 2, card("jonin", 8, 7, Keyword.SHINOBI));
        engine.getMinionEffects().refreshAuras();
        assertEquals(6, genin.getAttack());
        assertEquals(8, me.getSide().getAt(2).getAttack(), "only the other Shinobi");
    }

    @Test
    void tennoCutsYourOtherMinions() {
        Card inHand = me.getHand().get(0);                     // 1 Ki
        place(me, 0, card("tenno", 10, 15, 16));
        assertEquals(0, engine.costOf(me, inHand));
    }

    @Test
    void rokurokubiAttacksPastShiroduAndTorii() {
        Minion roku = place(me, 0, card("rokurokubi", 6, 5, Keyword.TATAKAE));
        place(enemy, 0, card("wall", 1, 5, Keyword.SHIRODU));
        Minion torii = place(enemy, 1, card("t", 1, 6, Keyword.TORII));
        assertTrue(engine.legalTargets(roku).contains(torii));
        assertTrue(engine.attack(roku, torii));
        assertEquals(-1, enemy.getSide().slotOf(torii));
    }

    @Test
    void fumaKotaroYogakeMakesTheEnemyMinionsFightEachOther() {
        Minion fuma = place(me, 0, card("fuma-kotaro", 9, 6, Keyword.SHINOBI, Keyword.TATAKAE));
        Minion a = place(enemy, 0, card("a", 3, 20));
        Minion b = place(enemy, 1, card("b", 4, 20));
        assertTrue(engine.attack(fuma, enemy.getHero()));
        assertTrue(a.getHealth() < 20 && b.getHealth() < 20, "they hit each other");
        assertFalse(fuma.isHidden());
    }
        /** Two hidden Fuma face to face: the enemy Fuma forced to fight does not answer with its own Yogake (it looped forever). */
    @Test
    void twoFumaKotaroDoNotLoop() {
        Minion fuma = place(me, 0, card("fuma-kotaro", 9, 6, Keyword.SHINOBI, Keyword.TATAKAE));
        place(me, 1, card("mine", 2, 20));
        Minion theirFuma = place(enemy, 0, card("fuma-kotaro", 9, 20, Keyword.SHINOBI));
        Minion b = place(enemy, 1, card("b", 4, 20));
        assertTrue(engine.attack(fuma, enemy.getHero()));
        assertTrue(theirFuma.getHealth() < 20 && b.getHealth() < 20, "the enemy minions hit each other once");
        assertEquals(20, me.getSide().getAt(1).getHealth(), "no Yogake came back on my side");
    }
}