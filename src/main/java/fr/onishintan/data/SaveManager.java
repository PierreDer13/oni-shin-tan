package fr.onishintan.data;

import fr.onishintan.model.HeroClass;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Save files of the game, in a folder of the player's home (~/.onishintan by default):
 * decks.properties (the decks built in the editor), settings.properties (music and effects volumes)
 * and collection.properties (the kami won at the end of an Adventure).
 * Only ids and plain values are written: the UI turns card ids back into cards with its catalog.
 * A missing or broken file gives the defaults and never crashes the game; a save that fails
 * returns false and leaves the previous file untouched (written to a .tmp file, then moved).
 */
public class SaveManager {
    public static final String DECKS_FILE = "decks.properties";
    public static final String SETTINGS_FILE = "settings.properties";
    public static final String COLLECTION_FILE = "collection.properties";

    /** Volumes used when nothing is saved yet (the same as AudioSettings). */
    public static final double DEFAULT_MUSIC = 0.35;
    public static final double DEFAULT_EFFECTS = 1.0;

    /** One saved deck: its name, its class and the ids of its cards (one id per copy). */
    public record DeckData(String name, HeroClass heroClass, List<String> cardIds) {
        public DeckData {
            name = name == null ? "" : name;
            cardIds = cardIds == null ? List.of() : List.copyOf(cardIds);
        }
    }

    /** The two volumes of the Settings, each from 0 (silent) to 1 (full). */
    public record Volumes(double music, double effects) {
        public Volumes {
            music = clamp(music);
            effects = clamp(effects);
        }

        public static Volumes defaults() { return new Volumes(DEFAULT_MUSIC, DEFAULT_EFFECTS); }

        private static double clamp(double v) { return Double.isNaN(v) ? 0 : Math.max(0, Math.min(1, v)); }
    }

    private final Path dir;

    /** Saves in ~/.onishintan. */
    public SaveManager() {
        this(defaultDir());
    }

    /** Saves in that folder (created on the first save); tests give a temporary folder. */
    public SaveManager(Path dir) {
        this.dir = dir;
    }

    public static Path defaultDir() {
        return Path.of(System.getProperty("user.home"), ".onishintan");
    }

    public Path getDir() { return dir; }

    // ---------- decks ----------

    /** Every saved deck, in the order they were saved. A deck with an unknown class is skipped. */
    public List<DeckData> loadDecks() {
        Properties p = read(DECKS_FILE);
        List<DeckData> decks = new ArrayList<>();
        int count = intOf(p.getProperty("count"), 0);
        for (int i = 0; i < count; i++) {
            HeroClass heroClass = classOf(p.getProperty("deck." + i + ".class"));
            if (heroClass == null) continue;
            String name = p.getProperty("deck." + i + ".name", heroClass.getDisplayName() + " deck");
            decks.add(new DeckData(name, heroClass, idsOf(p.getProperty("deck." + i + ".cards"))));
        }
        return decks;
    }

    /** Replaces every saved deck by these. */
    public boolean saveDecks(List<DeckData> decks) {
        Properties p = new Properties();
        List<DeckData> list = decks == null ? List.of() : decks;
        p.setProperty("count", String.valueOf(list.size()));
        for (int i = 0; i < list.size(); i++) {
            DeckData d = list.get(i);
            p.setProperty("deck." + i + ".name", d.name());
            p.setProperty("deck." + i + ".class", d.heroClass().name());
            p.setProperty("deck." + i + ".cards", String.join(",", d.cardIds()));
        }
        return write(DECKS_FILE, p, "Oni Shin Tan - decks");
    }

    // ---------- settings ----------

    /** The saved volumes, or the defaults (35 % music, 100 % effects). */
    public Volumes loadVolumes() {
        Properties p = read(SETTINGS_FILE);
        return new Volumes(doubleOf(p.getProperty("music"), DEFAULT_MUSIC), doubleOf(p.getProperty("effects"), DEFAULT_EFFECTS));
    }

    public boolean saveVolumes(Volumes v) {
        Volumes volumes = v == null ? Volumes.defaults() : v;
        Properties p = read(SETTINGS_FILE);      // keeps other settings a later version may add
        p.setProperty("music", String.valueOf(volumes.music()));
        p.setProperty("effects", String.valueOf(volumes.effects()));
        return write(SETTINGS_FILE, p, "Oni Shin Tan - settings");
    }

    // ---------- collection (Adventure rewards) ----------

    /** The ids of the reward cards won so far (the kami of each finished Adventure). */
    public Set<String> loadCollection() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(idsOf(read(COLLECTION_FILE).getProperty("unlocked"))));
    }

    public boolean isUnlocked(String cardId) {
        return cardId != null && loadCollection().contains(cardId);
    }

    /** Adds a reward card to the collection (the Claim button); true if it is saved, already there included. */
    public boolean unlock(String cardId) {
        if (cardId == null || cardId.isBlank()) return false;
        Set<String> ids = new LinkedHashSet<>(loadCollection());
        if (!ids.add(cardId.trim())) return true;
        Properties p = new Properties();
        p.setProperty("unlocked", String.join(",", ids));
        return write(COLLECTION_FILE, p, "Oni Shin Tan - collection");
    }

    // ---------- files ----------

    private Properties read(String file) {
        Properties p = new Properties();
        Path path = dir.resolve(file);
        if (!Files.isRegularFile(path)) return p;
        try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            p.load(r);
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Save file unreadable, defaults used: " + path + " (" + e.getMessage() + ")");
            return new Properties();
        }
        return p;
    }

    private boolean write(String file, Properties p, String comment) {
        Path path = dir.resolve(file);
        Path tmp = dir.resolve(file + ".tmp");
        try {
            Files.createDirectories(dir);
            try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                p.store(w, comment);
            }
            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            System.err.println("Could not save " + path + ": " + e.getMessage());
            return false;
        }
    }

    private static List<String> idsOf(String text) {
        if (text == null || text.isBlank()) return List.of();
        return Arrays.stream(text.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private static HeroClass classOf(String text) {
        if (text == null) return null;
        try {
            return HeroClass.valueOf(text.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static int intOf(String text, int fallback) {
        try {
            return text == null ? fallback : Math.max(0, Integer.parseInt(text.trim()));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static double doubleOf(String text, double fallback) {
        try {
            return text == null ? fallback : Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}