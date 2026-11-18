package fr.onishintan.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.onishintan.adventure.Upgrade;
import fr.onishintan.model.*;

/** The Adventure upgrades: Inochi (+30 max HP), Chikara (+3 ATK, +3 spell damage), Hayasa (-1 Ki, min 1). */
class UpgradeTest {

    private Player me;        // Samurai, plays first
    private Player enemy;     // Ninja
    private GameEngine engine;

    private static MinionCard minionCard(String id, int cost, int atk, int hp) {
        return new MinionCard(id, id, "試", "てすと", cost, Rarity.COMMON, HeroClass.NEUTRAL, "", "", atk, hp,
                EnumSet.noneOf(Keyword.class));
    }

    private static Player player(String name, HeroClass cls) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < Deck.SIZE; i++) cards.add(minionCard(name + "-" + i, 1, 1, 1));
        return new Player(name, new Hero(cls), new Deck(cards));
    }

    @BeforeEach
    void setUp() {
        me = player("Pierre", HeroClass.SAMURAI);
        enemy = player("Kayne", HeroClass.NINJA);
        engine = new GameEngine(me, enemy, new Random(42));
    }

    private void start() {
        engine.startMatch();
        engine.beginFirstTurn();
        me.gainTemporaryKi(30);
    }

    @Test
    void noUpgradesOutsideTheAdventure() {
        start();
        assertTrue(engine.upgradesOf(me).isEmpty());
        assertEquals(Hero.START_HEALTH, me.getHero().getMaxHealth());
        MinionCard c = minionCard("plain", 4, 5, 5);
        me.addToHand(c);
        assertEquals(4, engine.costOf(me, c));
    }

    @Test
    void inochiAddsThirtyToMaxAndCurrentHp() {
        engine.setUpgrades(me, Set.of(Upgrade.INOCHI));
        engine.setUpgrades(me, Set.of(Upgrade.INOCHI));      // given twice, counted once
        start();
        assertEquals(130, me.getHero().getMaxHealth());
        assertEquals(130, me.getHero().getHealth());
        assertEquals(100, enemy.getHero().getMaxHealth());
        me.getHero().takeDamage(50);
        engine.heal(me.getHero(), 100);
        assertEquals(130, me.getHero().getHealth());
    }

    @Test
    void chikaraGivesThreeAtkToMinionsAndWeapons() {
        engine.setUpgrades(me, Set.of(Upgrade.CHIKARA));
        start();
        MinionCard c = minionCard("plain", 2, 5, 5);
        me.addToHand(c);
        assertTrue(engine.playCard(c, 0, null));
        Minion m = me.getSide().getMinions().get(0);
        assertEquals(8, m.getAttack());
        assertEquals(5, m.getHealth());

        engine.equip(me, new WeaponCard("blade", "blade", "刀", "", 1, Rarity.COMMON, HeroClass.SAMURAI, "", "", 5, 2));
        assertEquals(8, me.getHero().getAttack());
    }

    @Test
    void chikaraAddsThreeToSpellDamageOnly() {
        engine.setUpgrades(me, Set.of(Upgrade.CHIKARA));
        start();
        Minion target = new Minion(minionCard("wall", 1, 1, 20));
        enemy.getSide().place(target, 0);

        engine.dealDamage(null, target, 4);                  // not a spell: no bonus
        assertEquals(16, target.getHealth());

        engine.equip(me, new WeaponCard("blade", "blade", "刀", "", 1, Rarity.COMMON, HeroClass.SAMURAI, "", "", 2, 2));
        SpellCard iaido = new SpellCard("iaido", "iaido", "術", "", 1, Rarity.COMMON, HeroClass.SAMURAI, "", "");
        me.addToHand(iaido);
        assertTrue(engine.playCard(iaido, 0, null));         // weapon 2+3 ATK, spell +3
        assertEquals(16 - 8, target.getHealth());
    }

    @Test
    void chikaraBonusShowsOnDamageSpellsInHand() {
        engine.setUpgrades(me, Set.of(Upgrade.CHIKARA));
        start();
        SpellCard bolt = new SpellCard("bolt", "bolt", "術", "", 2, Rarity.COMMON, HeroClass.NEUTRAL, "Deal 10 damage to a minion", "");
        assertEquals(3, engine.bonusOf(me, bolt));
        assertEquals(0, engine.bonusOf(enemy, bolt));
    }

    @Test
    void hayasaTakesOneKiOffDownToOne() {
        engine.setUpgrades(me, Set.of(Upgrade.HAYASA));
        start();
        MinionCard four = minionCard("four", 4, 5, 5);
        MinionCard one = minionCard("one", 1, 2, 2);
        me.addToHand(four);
        me.addToHand(one);
        assertEquals(3, engine.costOf(me, four));
        assertEquals(1, engine.costOf(me, one));
        int ki = me.getKi();
        assertTrue(engine.playCard(four, 0, null));
        assertEquals(ki - 3, me.getKi());
    }

    @Test
    void labelsMatchTheAdventureScreen() {
        assertEquals(Upgrade.INOCHI, Upgrade.fromLabel("命 Inochi"));
        assertEquals(Upgrade.HAYASA, Upgrade.fromLabel("hayasa"));
        assertNull(Upgrade.fromLabel("nothing"));
    }

    @Test
    void chikaraStaysWithItsOwnerWhenYourSpellSetsOffAnEnemyTrap() {
        engine.setUpgrades(me, Set.of(Upgrade.CHIKARA));
        start();
        TrapCard makibishi = new TrapCard("makibishi", "Makibishi", "撒菱", "", 2, Rarity.COMMON, HeroClass.NINJA,
                "The enemy summons a minion: deal 6 damage to it", "", TrapTrigger.ON_ENEMY_SUMMON);
        assertTrue(enemy.setTrap(makibishi));
        List<Integer> hits = new ArrayList<>();
        engine.getBus().subscribe(GameEvent.Type.DAMAGE, e -> hits.add(e.amount()));
        SpellCard kagura = new SpellCard("kagura", "Kagura", "神楽", "", 1, Rarity.COMMON, HeroClass.KANNUSHI, "", "");
        me.addToHand(kagura);
        assertTrue(engine.playCard(kagura, 0, null));      // the first Miko sets off the enemy's Makibishi
        assertFalse(hits.isEmpty());
        assertTrue(hits.get(0) <= 6, "the enemy's trap must not get your +3 spell damage, hit " + hits.get(0));
    }

}
