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

import fr.onishintan.adventure.AdventureMode;
import fr.onishintan.data.SaveManager.DeckData;
import fr.onishintan.data.SaveManager.SavedRun;
import fr.onishintan.data.SaveManager.Window;
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

    // ---------- run in progress ----------

    @Test
    void noRunSavedMeansEmpty() {
        assertTrue(new SaveManager(dir).loadRun().isEmpty());
    }

    @Test
    void runRoundTripsAndClears() {
        SaveManager save = new SaveManager(dir);
        SavedRun run = new SavedRun(HeroClass.ONI, AdventureMode.HARDCORE, 2, List.of("inochi", "kiai"), 7);
        assertTrue(save.saveRun(run));
        assertEquals(run, new SaveManager(dir).loadRun().orElseThrow());

        assertTrue(save.clearRun());
        assertTrue(save.loadRun().isEmpty());
        assertTrue(save.clearRun());                     // nothing to clear is fine
    }

    @Test
    void runWithoutBonusesRoundTrips() {
        SaveManager save = new SaveManager(dir);
        SavedRun run = new SavedRun(HeroClass.NINJA, AdventureMode.NORMAL, 0, List.of(), 0);
        save.saveRun(run);
        assertEquals(run, save.loadRun().orElseThrow());
    }

    @Test
    void brokenRunFileGivesNoRun() throws IOException {
        Files.writeString(dir.resolve(SaveManager.RUN_FILE), "class=DRAGON\nmode=NORMAL\nwon=1\n");
        assertTrue(new SaveManager(dir).loadRun().isEmpty());
        Files.writeString(dir.resolve(SaveManager.RUN_FILE), "class=ONI\nmode=EASY\n");
        assertTrue(new SaveManager(dir).loadRun().isEmpty());
    }

    @Test
    void brokenRunNumbersCountZero() throws IOException {
        Files.writeString(dir.resolve(SaveManager.RUN_FILE), "class=SAMURAI\nmode=NORMAL\nwon=abc\nwounds=-5\n");
        SavedRun run = new SaveManager(dir).loadRun().orElseThrow();
        assertEquals(0, run.won());
        assertEquals(0, run.wounds());
        assertEquals(List.of(), run.picked());
    }

    // ---------- Adventures finished, Orochi ----------

    @Test
    void orochiNeedsTheEightAdventures() {
        SaveManager save = new SaveManager(dir);
        assertFalse(save.orochiUnlocked());
        for (HeroClass c : SaveManager.ADVENTURE_CLASSES) {
            for (AdventureMode m : AdventureMode.values()) {
                assertFalse(save.orochiUnlocked(), "unlocked too early, before " + c + " " + m);
                assertFalse(save.isCompleted(c, m));
                assertTrue(save.markCompleted(c, m));
                assertTrue(save.isCompleted(c, m));
            }
        }
        assertTrue(new SaveManager(dir).orochiUnlocked());
    }

    @Test
    void markingTwiceStaysCompleted() {
        SaveManager save = new SaveManager(dir);
        save.markCompleted(HeroClass.KANNUSHI, AdventureMode.NORMAL);
        save.markCompleted(HeroClass.KANNUSHI, AdventureMode.NORMAL);
        assertTrue(save.isCompleted(HeroClass.KANNUSHI, AdventureMode.NORMAL));
        assertFalse(save.isCompleted(HeroClass.KANNUSHI, AdventureMode.HARDCORE));
        assertFalse(save.markCompleted(null, AdventureMode.NORMAL));
    }

    @Test
    void orochiKillsCount() {
        SaveManager save = new SaveManager(dir);
        assertEquals(0, save.loadOrochiKills());
        save.addOrochiKill();
        save.addOrochiKill();
        assertEquals(2, new SaveManager(dir).loadOrochiKills());
    }

    // ---------- window ----------

    @Test
    void windowDefaultsThenRoundTripsWithoutTouchingVolumes() {
        SaveManager save = new SaveManager(dir);
        assertEquals(Window.defaults(), save.loadWindow());

        save.saveVolumes(new Volumes(0.5, 0.25));
        assertTrue(save.saveWindow(new Window(1600, 900, true)));
        assertEquals(new Window(1600, 900, true), new SaveManager(dir).loadWindow());
        assertEquals(new Volumes(0.5, 0.25), save.loadVolumes());
    }

    @Test
    void brokenOrTinyWindowIsFixed() throws IOException {
        Files.writeString(dir.resolve(SaveManager.SETTINGS_FILE), "window.width=abc\nwindow.height=10\nwindow.fullscreen=yes\n");
        Window w = new SaveManager(dir).loadWindow();
        assertEquals(SaveManager.DEFAULT_WIDTH, w.width());
        assertEquals(SaveManager.MIN_HEIGHT, w.height());
        assertFalse(w.fullscreen());
    }
}