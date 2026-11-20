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

/** The 17 plain class spells (GDD spell table, cards.json ids), their targets, and Ryū's +4. */
class SpellTest {

    private Player me;        // Samurai, plays first
    private Player enemy;     // Ninja
    private GameEngine engine;
    private final List<GameEvent> events = new ArrayList<>();

    private static MinionCard minionCard(String id, int atk, int hp, Keyword... keywords) {
        EnumSet<Keyword> set = EnumSet.noneOf(Keyword.class);
        for (Keyword k : keywords) set.add(k);
        return new MinionCard(id, id, "試", "てすと", 1, Rarity.COMMON, HeroClass.NEUTRAL, "", "", atk, hp, set);
    }

    private static Player player(String name, HeroClass cls) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < Deck.SIZE; i++) cards.add(minionCard(name + "-" + i, 1, 1));
        return new Player(name, new Hero(cls), new Deck(cards));
    }

    private static Minion place(Player p, int slot, MinionCard card) {
        Minion m = new Minion(card);
        p.getSide().place(m, slot);
        return m;
    }

    /** A spell card with that id, in my hand. */
    private SpellCard spell(String id) {
        SpellCard c = new SpellCard(id, id, "術", "", 1, Rarity.COMMON, HeroClass.NEUTRAL, "", "");
        me.addToHand(c);
        return c;
    }

    @BeforeEach
    void setUp() {
        me = player("Pierre", HeroClass.SAMURAI);
        enemy = player("Kayne", HeroClass.NINJA);
        engine = new GameEngine(me, enemy, new Random(42));
        engine.startMatch();
        engine.beginFirstTurn();
        me.gainTemporaryKi(30);
        engine.addListener((e, s) -> events.add(e));
    }

    // ---------- targets ----------

    @Test
    void spellsTellTheirTargetsAndRefuseAMissingOne() {
        Minion mine = place(me, 0, minionCard("m", 1, 5));
        place(enemy, 0, minionCard("hid", 1, 5, Keyword.SHINOBI));
        Minion seen = place(enemy, 1, minionCard("e", 1, 5));

        SpellCard harae = spell("harae");
        assertEquals(SpellTarget.CHARACTER, engine.spellTarget(harae));
        assertEquals(List.of(me.getHero(), mine, enemy.getHero(), seen), engine.spellTargets(harae));

        SpellCard taisha = spell("taisha-zukuri");
        assertEquals(List.of(mine), engine.spellTargets(taisha));
        int ki = me.getKi();
        assertFalse(engine.playCard(taisha, 0, seen), "not a friendly minion");
        assertFalse(engine.playCard(taisha, 0, null));
        assertEquals(ki, me.getKi(), "nothing spent");
    }

    // ---------- Ninja ----------

    @Test
    void kakureminoGivesAllFriendlyMinionsShinobi() {
        Minion a = place(me, 0, minionCard("a", 1, 1));
        Minion b = place(me, 1, minionCard("b", 1, 1));
        assertTrue(engine.playCard(spell("kakuremino"), 0, null));
        assertTrue(a.isHidden());
        assertTrue(b.isHidden());
    }

    @Test
    void kemuriDamaHidesAShiroduSoItStopsGuarding() {
        Minion attacker = place(me, 0, minionCard("t", 1, 5, Keyword.TATAKAE));
        Minion wall = place(enemy, 0, minionCard("w", 1, 5, Keyword.SHIRODU));
        Minion behind = place(enemy, 1, minionCard("b", 1, 5));
        assertFalse(engine.legalTargets(attacker).contains(behind));

        assertTrue(engine.playCard(spell("kemuri-dama"), 0, wall));
        assertTrue(wall.isHidden());
        assertTrue(engine.legalTargets(attacker).contains(behind));
    }

    @Test
    void kujiInRefreshesTheHeroPower() {
        assertTrue(engine.useKatana(KatanaTier.BOKKEN));
        assertTrue(engine.isHeroPowerUsed());
        assertTrue(engine.playCard(spell("kuji-in"), 0, null));
        assertFalse(engine.isHeroPowerUsed());
    }

    @Test
    void yoiCutsTheCardsInHandByOneWithNoMinimum() {
        Card inHand = me.getHand().get(0);                // 1 Ki
        assertTrue(engine.playCard(spell("yoi"), 0, null));
        assertEquals(0, engine.costOf(me, inHand));

        Card later = minionCard("later", 1, 1);
        me.addToHand(later);
        assertEquals(1, engine.costOf(me, later), "only the cards already in hand");

        int ki = me.getKi();
        assertTrue(engine.playCard(inHand, 0, null));
        assertEquals(ki, me.getKi());
    }

    // ---------- Samurai ----------

    @Test
    void chiburiNeedsAWeaponAndAddsOneDurability() {
        SpellCard chiburi = spell("chiburi");
        assertFalse(engine.canPlay(chiburi));
        me.getHero().equip(new Weapon(KatanaTier.BOKKEN.toCard()));
        assertTrue(engine.playCard(chiburi, 0, null));
        assertEquals(2, me.getHero().getWeapon().getDurability());
    }

    @Test
    void iaidoBreaksTheWeaponAndHitsEveryMinionForItsAttack() {
        me.getHero().equip(new Weapon(KatanaTier.KATANA.toCard()));     // 8 ATK
        Minion mine = place(me, 0, minionCard("m", 1, 10));
        Minion theirs = place(enemy, 0, minionCard("e", 1, 10));

        assertTrue(engine.playCard(spell("iaido"), 0, null));
        assertNull(me.getHero().getWeapon());
        assertEquals(2, mine.getHealth());
        assertEquals(2, theirs.getHealth());
    }

    @Test
    void kettoNeedsTwoMinionsAndLeavesOneOnHalfItsHealth() {
        Minion a = place(me, 0, minionCard("a", 1, 9));
        Minion b = place(enemy, 0, minionCard("b", 1, 9));
        SpellCard ketto = spell("ketto");
        assertEquals(SpellTarget.TWO_MINIONS, engine.spellTarget(ketto));
        assertFalse(engine.playCard(ketto, 0, a));

        assertTrue(engine.playCard(ketto, 0, a, b));
        boolean aLives = me.getSide().slotOf(a) >= 0;
        boolean bLives = enemy.getSide().slotOf(b) >= 0;
        assertTrue(aLives ^ bLives, "exactly one survives");
        assertEquals(5, (aLives ? a : b).getHealth());
    }

    @Test
    void yoroiGivesSixArmourToYourHeroAndAFriendlyMinion() {
        Minion m = place(me, 0, minionCard("m", 1, 5));
        assertTrue(engine.playCard(spell("yoroi"), 0, m));
        assertEquals(6, me.getHero().getArmour());
        assertEquals(6, m.getArmour());

        engine.dealDamage(null, m, 4);
        assertEquals(5, m.getHealth(), "the armour took it");
    }

    // ---------- Kannushi ----------

    @Test
    void haraeHealsFifteenAndDraws() {
        me.getHero().takeDamage(20);
        int hand = me.getHand().size();
        SpellCard harae = spell("harae");
        assertTrue(engine.playCard(harae, 0, me.getHero()));
        assertEquals(95, me.getHero().getHealth());
        assertEquals(hand + 1, me.getHand().size(), "Harae left, a card came");
    }

    @Test
    void kisekiHealsTheHurtAndBuffsTheFull() {
        Minion hurt = place(me, 0, minionCard("h", 1, 10));
        Minion full = place(me, 1, minionCard("f", 1, 10));
        hurt.takeDamage(6);

        assertTrue(engine.playCard(spell("kiseki"), 0, null));
        assertEquals(10, hurt.getHealth());
        assertEquals(1, hurt.getAttack());
        assertEquals(4, full.getAttack());
        assertEquals(13, full.getHealth());
    }

    @Test
    void senbonToriiAndTaishaZukuriGiveTorii() {
        Minion a = place(me, 0, minionCard("a", 1, 1));
        Minion b = place(me, 1, minionCard("b", 1, 1));
        assertTrue(engine.playCard(spell("senbon-torii"), 0, null));
        assertTrue(a.hasTorii());
        assertTrue(b.hasTorii());

        assertTrue(engine.playCard(spell("taisha-zukuri"), 0, a));
        assertEquals(2, a.getToriiStacks());
    }

    @Test
    void kaguraSummonsThreeMikoWithTatakae() {
        assertTrue(engine.playCard(spell("kagura"), 0, null));
        assertEquals(3, me.getSide().size());
        for (Minion m : me.getSide().getMinions()) {
            assertEquals(GameEngine.MIKO_ID, m.getSource().getId());
            assertTrue(m.canAttack(), "Tatakae");
        }
    }

    // ---------- Oni ----------

    @Test
    void kuchiyoseBringsBackADeadMinionWithItsBanzai() {
        SpellCard kuchiyose = spell("kuchiyose");
        assertFalse(engine.canPlay(kuchiyose), "nobody dead yet");

        MinionCard card = minionCard("kappa", 5, 7);
        engine.getCardEffects().register("kappa", Trigger.BANZAI, Effects.damageEnemyHero(4));
        engine.destroy(place(me, 3, card));

        assertTrue(engine.playCard(kuchiyose, 0, null));
        assertSame(card, me.getSide().getAt(0).getSource());
        assertEquals(96, enemy.getHero().getHealth(), "its Banzai ran again");
        assertFalse(engine.canPlay(spell("kuchiyose")), "the graveyard is empty again");
    }

    @Test
    void kamikakushiShufflesAMinionBackWithoutKillingIt() {
        MinionCard card = minionCard("e", 3, 3);
        Minion m = place(enemy, 0, card);
        engine.getCardEffects().register("e", Trigger.SEPPUKU, Effects.damageEnemyHero(5));
        int deck = enemy.getDeck().size();

        assertTrue(engine.playCard(spell("kamikakushi"), 0, m));
        assertEquals(-1, enemy.getSide().slotOf(m));
        assertEquals(deck + 1, enemy.getDeck().size());
        assertTrue(enemy.getDeck().getCards().contains(card));
        assertEquals(100, me.getHero().getHealth(), "no Seppuku");
        assertTrue(events.stream().anyMatch(e -> e.type() == Type.MINION_REMOVED));
    }

    @Test
    void sakasamaSwapsEnemyStatsAndTheirDeckCopies() {
        MinionCard card = minionCard("e", 2, 5);
        Minion m = place(enemy, 0, card);
        enemy.getDeck().insertRandom(card, new Random(3));

        assertTrue(engine.playCard(spell("sakasama"), 0, null));
        assertEquals(5, m.getAttack());
        assertEquals(2, m.getHealth());
        MinionCard copy = (MinionCard) enemy.getDeck().getCards().stream()
                .filter(c -> c.getId().equals("e")).findFirst().orElseThrow();
        assertEquals(5, copy.getBaseAttack());
        assertEquals(2, copy.getBaseHealth());
    }

    @Test
    void tamashiiGuiDestroysAMinionAndARandomEnemyCard() {
        Minion m = place(enemy, 0, minionCard("e", 3, 20));
        int hand = enemy.getHand().size();
        assertTrue(engine.playCard(spell("tamashii-gui"), 0, m));
        assertEquals(-1, enemy.getSide().slotOf(m));
        assertEquals(hand - 1, enemy.getHand().size());
    }

    // ---------- Ryū ----------

    @Test
    void ryuAddsFourToYourDamageHealingAndArmour() {
        place(me, 0, minionCard(GameEngine.RYU_ID, 12, 12, Keyword.TORII));
        engine.dealDamage(null, enemy.getHero(), 3);
        assertEquals(93, enemy.getHero().getHealth());

        me.getHero().takeDamage(20);
        engine.heal(me.getHero(), 5);
        assertEquals(89, me.getHero().getHealth());

        engine.gainArmour(me, 6);
        assertEquals(10, me.getHero().getArmour());
    }
}