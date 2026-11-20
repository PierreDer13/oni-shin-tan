package fr.onishintan.data;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import fr.onishintan.model.Card;
import fr.onishintan.model.CurseCard;
import fr.onishintan.model.RiteCard;
import fr.onishintan.model.ShrineCard;
import fr.onishintan.model.TrapCard;
import fr.onishintan.adventure.AdventureMode;
import fr.onishintan.ai.AIController.Difficulty;
import fr.onishintan.model.Deck;
import fr.onishintan.model.HeroClass;
import fr.onishintan.model.Player;
import fr.onishintan.model.Rarity;

/** The Adventure decks are legal 40-card decks built from the real card list, and the runs follow the GDD order. */
class AdventureDecksTest {

    private static final List<HeroClass> CLASSES = List.of(HeroClass.SAMURAI, HeroClass.NINJA, HeroClass.KANNUSHI, HeroClass.ONI);
    /** The run rewards and the mythic: earned in the Adventure, never in its decks. */
    private static final Set<String> REWARDS = Set.of("tsukuyomi", "takemikazuchi", "okuninushi", "izanami", "amaterasu-omikami");

    private final AdventureDecks adventure = new AdventureDecks(CardRepository.all());

    @Test
    void playerDecksAreLegal() {
        for (HeroClass c : CLASSES) {
            Deck deck = new Deck(adventure.playerDeck(c));
            assertEquals(List.of(), deck.validate(c), c + " player deck");
            assertNoReward(adventure.playerDeck(c), c + " player deck");
        }
    }

    @Test
    void everyEnemyDeckIsLegalForItsClass() {
        for (HeroClass you : CLASSES) {
            for (int battle = 0; battle < AdventureDecks.BATTLES; battle++) {
                HeroClass foe = AdventureDecks.opponent(you, battle);
                List<Card> cards = adventure.enemyDeck(you, battle);
                assertEquals(List.of(), new Deck(cards).validate(foe), you + " battle " + battle);
                assertNoReward(cards, you + " battle " + battle);
            }
        }
    }

    @Test
    void eachRunMeetsTheThreeOtherClasses() {
        for (HeroClass you : CLASSES) {
            Set<HeroClass> foes = new HashSet<>(AdventureDecks.opponents(you));
            assertEquals(3, foes.size());
            assertFalse(foes.contains(you));
            assertFalse(foes.contains(HeroClass.NEUTRAL));
        }
        assertEquals(List.of(HeroClass.KANNUSHI, HeroClass.ONI, HeroClass.NINJA), AdventureDecks.opponents(HeroClass.SAMURAI));
    }

    @Test
    void legendariesOnlyOnMountFuji() {
        for (HeroClass you : CLASSES) {
            assertEquals(0, legendaries(adventure.enemyDeck(you, 0)), "Town");
            assertEquals(0, legendaries(adventure.enemyDeck(you, 1)), "Ocean");
            assertTrue(legendaries(adventure.enemyDeck(you, 2)) >= 3, "Mount Fuji");
        }
    }

    /** The same enemy class is met in battle 1, 2 or 3 depending on your class: its deck is heavier the later the battle. */
    @Test
    void battlesGetHarder() {
        for (HeroClass foe : CLASSES) {
            double[] ki = new double[AdventureDecks.BATTLES];
            for (HeroClass you : CLASSES) {
                for (int battle = 0; battle < AdventureDecks.BATTLES; battle++) {
                    if (AdventureDecks.opponent(you, battle) == foe) ki[battle] = averageKi(adventure.enemyDeck(you, battle));
                }
            }
            assertTrue(ki[0] > 0 && ki[0] < ki[1] && ki[1] < ki[2], foe + ": " + ki[0] + " / " + ki[1] + " / " + ki[2]);
        }
    }

    @Test
    void boardsAndPlayers() {
        assertEquals(List.of("town", "ship", "fuji"),
                List.of(AdventureDecks.board(0), AdventureDecks.board(1), AdventureDecks.board(2)));
        Player foe = adventure.enemy(HeroClass.NINJA, 0);
        assertEquals("Oni", foe.getName());
        assertEquals(HeroClass.ONI, foe.getHero().getHeroClass());
        assertEquals(Deck.SIZE, foe.getDeck().size());
        Player me = adventure.hero("Pierre", HeroClass.NINJA);
        assertEquals(Deck.SIZE, me.getDeck().size());
    }

    @Test
    void wrongInputsAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> AdventureDecks.board(3));
        assertThrows(IllegalArgumentException.class, () -> AdventureDecks.opponent(HeroClass.SAMURAI, -1));
        assertThrows(IllegalArgumentException.class, () -> adventure.playerDeck(HeroClass.NEUTRAL));
        AdventureDecks empty = new AdventureDecks(List.of());
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> empty.playerDeck(HeroClass.ONI));
        assertTrue(e.getMessage().contains("hakahori"));
    }

    private static void assertNoReward(List<Card> cards, String what) {
        for (Card c : cards) assertFalse(REWARDS.contains(c.getId()), what + " holds the reward " + c.getId());
    }

    private static long legendaries(List<Card> cards) {
        return cards.stream().filter(c -> c.getRarity() == Rarity.LEGENDARY || c.getRarity() == Rarity.MYTHIC).count();
    }

    private static double averageKi(List<Card> cards) {
        return cards.stream().mapToInt(Card::getKiCost).average().orElse(0);
    }

    // ---------- Yamata no Orochi ----------

    @Test
    void orochiDeckHasHundredKnownCardsWithTheAnnouncedCurve() {
        List<Card> deck = adventure.orochiDeck();
        assertEquals(100, deck.size());
        int[] curve = new int[11];
        for (Card c : deck) {
            assertNotNull(CardRepository.byId(c.getId()), "unknown id " + c.getId());
            assertTrue(c.getKiCost() >= 1 && c.getKiCost() <= 10, c.getId() + " costs " + c.getKiCost());
            curve[c.getKiCost()]++;
        }
        assertArrayEquals(new int[] { 0, 8, 12, 14, 14, 13, 13, 15, 7, 2, 2 }, curve);
    }

    @Test
    void orochiDeckHasNoKamiNoAmaterasuNoFamilyCard() {
        for (Card c : adventure.orochiDeck()) {
            assertFalse(CardRepository.isReward(c.getId()), c.getId() + " is a reward card");
            assertFalse(c instanceof TrapCard || c instanceof ShrineCard || c instanceof CurseCard || c instanceof RiteCard,
                    c.getId() + " is a family card");
            assertNotEquals(Rarity.MYTHIC, c.getRarity(), c.getId() + " is a mythic");
        }
    }

    @Test
    void orochiIsHisOwnClassWithTwoHundredHpAndHundredCards() {
        Player orochi = adventure.orochi();
        assertEquals(HeroClass.OROCHI, orochi.getHero().getHeroClass());
        assertEquals(AdventureDecks.OROCHI_HEALTH, orochi.getHero().getHealth());
        assertEquals(200, orochi.getHero().getMaxHealth());
        assertEquals(100, orochi.getDeck().size());
        assertFalse(orochi.getDeck().isLegal(HeroClass.OROCHI));   // 100 cards: never validated, and it does not block the Deck
    }

    @Test
    void orochiIsNoAdventureClass() {
        assertThrows(IllegalArgumentException.class, () -> AdventureDecks.opponents(HeroClass.OROCHI));
        assertThrows(IllegalArgumentException.class, () -> adventure.playerDeck(HeroClass.OROCHI));
        assertFalse(SaveManager.ADVENTURE_CLASSES.contains(HeroClass.OROCHI));
        for (AdventureMode m : AdventureMode.values()) {
            for (int battle = 0; battle < AdventureDecks.BATTLES; battle++) {
                assertNotEquals(Difficulty.EXTREME, m.difficulty(battle));
            }
        }
    }
}