package fr.onishintan.data;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import fr.onishintan.data.SaveManager.DeckData;
import fr.onishintan.data.SaveManager.Stats;
import fr.onishintan.data.SaveManager.Volumes;
import fr.onishintan.model.HeroClass;

/** SaveManager: decks, volumes, the Adventure collection and the Profile written to disk and read back. */
class SaveManagerTest {

    @TempDir
    Path dir;

    @Test
    void nothingSavedGivesTheDefaults() {
        SaveManager save = new SaveManager(dir.resolve("none"));
        assertTrue(save.loadDecks().isEmpty());
        assertEquals(Volumes.defaults(), save.loadVolumes());
        assertTrue(save.loadCollection().isEmpty());
    }

    @Test
    void decksComeBackInOrderWithTheirCopies() {
        SaveManager save = new SaveManager(dir);
        List<DeckData> decks = List.of(
                new DeckData("Lame du matin, été", HeroClass.SAMURAI, List.of("ronin", "ronin", "chiburi")),
                new DeckData("Ninja deck 2", HeroClass.NINJA, List.of()));
        assertTrue(save.saveDecks(decks));
        assertEquals(decks, new SaveManager(dir).loadDecks());

        assertTrue(save.saveDecks(List.of(decks.get(1))));        // a deleted deck is gone
        assertEquals(List.of(decks.get(1)), save.loadDecks());
    }

    @Test
    void volumesAreSavedAndKeptBetweenZeroAndOne() {
        SaveManager save = new SaveManager(dir);
        assertTrue(save.saveVolumes(new Volumes(0.6, 0.0)));
        assertEquals(new Volumes(0.6, 0.0), new SaveManager(dir).loadVolumes());
        assertEquals(1.0, new Volumes(3, -1).music());
        assertEquals(0.0, new Volumes(3, -1).effects());
    }

    @Test
    void rewardsAreUnlockedOnce() {
        SaveManager save = new SaveManager(dir);
        assertTrue(save.unlock("susanoo"));
        assertTrue(save.unlock("susanoo"));
        assertTrue(save.unlock("inari"));
        assertEquals(Set.of("susanoo", "inari"), new SaveManager(dir).loadCollection());
        assertTrue(save.isUnlocked("inari"));
        assertFalse(save.isUnlocked("raijin"));
        assertFalse(save.unlock(" "));
    }

    @Test
    void brokenFilesGiveTheDefaults() throws IOException {
        Files.writeString(dir.resolve(SaveManager.DECKS_FILE), "count=2\ndeck.0.class=WIZARD\ndeck.1.class=ONI\n");
        Files.writeString(dir.resolve(SaveManager.SETTINGS_FILE), "music=loud\neffects=0.5\n");
        SaveManager save = new SaveManager(dir);
        List<DeckData> decks = save.loadDecks();
        assertEquals(1, decks.size());
        assertEquals(HeroClass.ONI, decks.get(0).heroClass());
        assertEquals(new Volumes(SaveManager.DEFAULT_MUSIC, 0.5), save.loadVolumes());
    }

    @Test
    void noTemporaryFileIsLeft() {
        SaveManager save = new SaveManager(dir);
        save.saveVolumes(Volumes.defaults());
        assertFalse(Files.exists(dir.resolve(SaveManager.SETTINGS_FILE + ".tmp")));
    }

    @Test
    void noProfileGivesEmptyStatsAndNoHeroes() {
        SaveManager save = new SaveManager(dir.resolve("none"));
        assertEquals(Stats.defaults(), save.loadStats());
        assertEquals(0, save.loadStats().played());
        assertTrue(save.loadHeroes().isEmpty());
    }

    @Test
    void statsComeBackWithWinsAndLossesPerClass() {
        SaveManager save = new SaveManager(dir);
        Stats stats = Stats.defaults().withUsername(" Pierre ").withRei(120)
                .withResult(HeroClass.NINJA, true).withResult(HeroClass.NINJA, true)
                .withResult(HeroClass.NINJA, false).withResult(HeroClass.ONI, false);
        assertTrue(save.saveStats(stats));
        Stats back = new SaveManager(dir).loadStats();
        assertEquals(stats, back);
        assertEquals("Pierre", back.username());
        assertEquals(120, back.rei());
        assertEquals(2, back.winsOf(HeroClass.NINJA));
        assertEquals(3, back.playedOf(HeroClass.NINJA));
        assertEquals(0, back.playedOf(HeroClass.SAMURAI));
        assertEquals(2, back.totalWins());
        assertEquals(2, back.totalLosses());
        assertEquals(4, back.played());
    }

    @Test
    void heroesAndStatsShareTheProfileWithoutErasingEachOther() {
        SaveManager save = new SaveManager(dir);
        Map<HeroClass, String> heroes = new EnumMap<>(HeroClass.class);
        heroes.put(HeroClass.SAMURAI, "miyamoto-musashi");
        heroes.put(HeroClass.KANNUSHI, "abe-no-seimei");
        assertTrue(save.saveHeroes(heroes));
        assertTrue(save.saveStats(Stats.defaults().withResult(HeroClass.SAMURAI, true)));
        assertEquals(heroes, save.loadHeroes());                  // saving the stats kept the heroes
        assertTrue(save.saveHeroes(Map.of(HeroClass.ONI, "shuten-doji")));
        assertEquals(Map.of(HeroClass.ONI, "shuten-doji"), save.loadHeroes());   // replaced, not merged
        assertEquals(1, save.loadStats().winsOf(HeroClass.SAMURAI));             // saving the heroes kept the stats
    }

    @Test
    void brokenProfileNumbersCountZero() throws IOException {
        Files.writeString(dir.resolve(SaveManager.PROFILE_FILE), "username=Kayne\nrei=lots\nwins.NINJA=-4\nlosses.ONI=2\n");
        Stats stats = new SaveManager(dir).loadStats();
        assertEquals("Kayne", stats.username());
        assertEquals(0, stats.rei());
        assertEquals(0, stats.winsOf(HeroClass.NINJA));
        assertEquals(2, stats.lossesOf(HeroClass.ONI));
    }
}