package fr.onishintan.data;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import fr.onishintan.data.SaveManager.DeckData;
import fr.onishintan.data.SaveManager.Volumes;
import fr.onishintan.model.HeroClass;

/** SaveManager: decks, volumes and the Adventure collection written to disk and read back. */
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
}