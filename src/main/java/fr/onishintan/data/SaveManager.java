package fr.onishintan.data;

import fr.onishintan.adventure.AdventureMode;
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
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * Save files of the game, in a folder of the player's home (~/.onishintan by default):
 * decks.properties (the decks built in the editor), settings.properties (music and effects volumes)
 * collection.properties (the kami won at the end of an Adventure) and profile.properties
 * (name, Rei, wins and losses per class, the hero picked for each class).
 * Only ids and plain values are written: the UI turns card ids back into cards with its catalog.
 * A missing or broken file gives the defaults and never crashes the game; a save that fails
 * returns false and leaves the previous file untouched (written to a .tmp file, then moved).
 */
public class SaveManager {
    public static final String DECKS_FILE = "decks.properties";
    public static final String SETTINGS_FILE = "settings.properties";
    public static final String COLLECTION_FILE = "collection.properties";
    public static final String PROFILE_FILE = "profile.properties";
    public static final String RUN_FILE = "run.properties";
    public static final String PROGRESS_FILE = "progress.properties";

    /** The 4 playable classes: their Adventures, in NORMAL and HARDCORE, unlock Yamata no Orochi (8 in all). */
    public static final List<HeroClass> ADVENTURE_CLASSES =
            List.of(HeroClass.SAMURAI, HeroClass.NINJA, HeroClass.KANNUSHI, HeroClass.ONI);

    public static final double DEFAULT_WIDTH = 1280;
    public static final double DEFAULT_HEIGHT = 720;
    public static final double MIN_WIDTH = 800;
    public static final double MIN_HEIGHT = 450;

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

    /**
     * The Profile page: the player's name, his Rei points and his wins and losses per class
     * (a class never played counts 0). Records are immutable: withResult, withUsername and withRei give a new one.
     */
    public record Stats(String username, int rei, Map<HeroClass, Integer> wins, Map<HeroClass, Integer> losses) {
        public Stats {
            username = username == null ? "" : username.trim();
            rei = Math.max(0, rei);
            wins = counts(wins);
            losses = counts(losses);
        }

        /** Nothing played yet: no name, 0 Rei, 0 wins, 0 losses. */
        public static Stats defaults() { return new Stats("", 0, Map.of(), Map.of()); }

        public int winsOf(HeroClass c) { return wins.getOrDefault(c, 0); }

        public int lossesOf(HeroClass c) { return losses.getOrDefault(c, 0); }

        public int playedOf(HeroClass c) { return winsOf(c) + lossesOf(c); }

        public int totalWins() { return wins.values().stream().mapToInt(Integer::intValue).sum(); }

        public int totalLosses() { return losses.values().stream().mapToInt(Integer::intValue).sum(); }

        /** Every match counted: wins + losses (a draw counts as neither). */
        public int played() { return totalWins() + totalLosses(); }

        /** The same stats with one more win or loss for that class (call it at the end of a match). */
        public Stats withResult(HeroClass c, boolean won) {
            if (c == null) return this;
            Map<HeroClass, Integer> w = new EnumMap<>(HeroClass.class);
            Map<HeroClass, Integer> l = new EnumMap<>(HeroClass.class);
            w.putAll(wins);
            l.putAll(losses);
            (won ? w : l).merge(c, 1, Integer::sum);
            return new Stats(username, rei, w, l);
        }

        public Stats withUsername(String name) { return new Stats(name, rei, wins, losses); }

        public Stats withRei(int points) { return new Stats(username, points, wins, losses); }

        private static Map<HeroClass, Integer> counts(Map<HeroClass, Integer> m) {
            Map<HeroClass, Integer> copy = new EnumMap<>(HeroClass.class);
            if (m != null) {
                m.forEach((c, n) -> { if (c != null && n != null && n > 0) copy.put(c, n); });
            }
            return Collections.unmodifiableMap(copy);
        }
    }

    /**
     * The Adventure in progress, saved after each battle and each bonus, cleared at the end or after a defeat.
     * won = battles won so far, picked = ids of the bonuses taken, wounds = HP lost (hardcore keeps them).
     */
    public record SavedRun(HeroClass heroClass, AdventureMode mode, int won, List<String> picked, int wounds) {
        public SavedRun {
            if (heroClass == null || mode == null) throw new IllegalArgumentException("A run needs a class and a mode");
            won = Math.max(0, won);
            wounds = Math.max(0, wounds);
            picked = picked == null ? List.of() : List.copyOf(picked);
        }
    }

    /** The game window: its size (at least MIN_WIDTH x MIN_HEIGHT) and whether it is fullscreen. */
    public record Window(double width, double height, boolean fullscreen) {
        public Window {
            width = Double.isFinite(width) ? Math.max(MIN_WIDTH, width) : DEFAULT_WIDTH;
            height = Double.isFinite(height) ? Math.max(MIN_HEIGHT, height) : DEFAULT_HEIGHT;
        }

        public static Window defaults() { return new Window(DEFAULT_WIDTH, DEFAULT_HEIGHT, false); }
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

    /** The saved window, or Window.defaults() (1280 x 720, not fullscreen). */
    public Window loadWindow() {
        Properties p = read(SETTINGS_FILE);
        return new Window(doubleOf(p.getProperty("window.width"), DEFAULT_WIDTH),
                doubleOf(p.getProperty("window.height"), DEFAULT_HEIGHT),
                Boolean.parseBoolean(p.getProperty("window.fullscreen", "false").trim()));
    }

    /** Saves the window; the volumes stay as they are. */
    public boolean saveWindow(Window w) {
        Window window = w == null ? Window.defaults() : w;
        Properties p = read(SETTINGS_FILE);
        p.setProperty("window.width", String.valueOf(window.width()));
        p.setProperty("window.height", String.valueOf(window.height()));
        p.setProperty("window.fullscreen", String.valueOf(window.fullscreen()));
        return write(SETTINGS_FILE, p, "Oni Shin Tan - settings");
    }

    // ---------- Adventure in progress ----------

    /** The run to resume, or empty: none saved, or a broken file (unknown class or mode). */
    public Optional<SavedRun> loadRun() {
        Properties p = read(RUN_FILE);
        HeroClass heroClass = classOf(p.getProperty("class"));
        AdventureMode mode = modeOf(p.getProperty("mode"));
        if (heroClass == null || mode == null) return Optional.empty();
        return Optional.of(new SavedRun(heroClass, mode, intOf(p.getProperty("won"), 0),
                idsOf(p.getProperty("picked")), intOf(p.getProperty("wounds"), 0)));
    }

    /** Replaces the saved run (after each battle and each bonus). */
    public boolean saveRun(SavedRun run) {
        if (run == null) return clearRun();
        Properties p = new Properties();
        p.setProperty("class", run.heroClass().name());
        p.setProperty("mode", run.mode().name());
        p.setProperty("won", String.valueOf(run.won()));
        p.setProperty("picked", String.join(",", run.picked()));
        p.setProperty("wounds", String.valueOf(run.wounds()));
        return write(RUN_FILE, p, "Oni Shin Tan - Adventure in progress");
    }

    /** Forgets the run (Adventure finished or lost); true if there is none saved anymore. */
    public boolean clearRun() {
        try {
            Files.deleteIfExists(dir.resolve(RUN_FILE));
            return true;
        } catch (IOException e) {
            System.err.println("Could not clear the run: " + e.getMessage());
            return false;
        }
    }

    // ---------- Adventures finished, Yamata no Orochi ----------

    /** Records an Adventure won with that class in that mode; true if it is saved, already there included. */
    public boolean markCompleted(HeroClass heroClass, AdventureMode mode) {
        if (heroClass == null || mode == null) return false;
        Properties p = read(PROGRESS_FILE);
        p.setProperty(completedKey(heroClass, mode), "true");
        return write(PROGRESS_FILE, p, "Oni Shin Tan - progress");
    }

    public boolean isCompleted(HeroClass heroClass, AdventureMode mode) {
        if (heroClass == null || mode == null) return false;
        return Boolean.parseBoolean(read(PROGRESS_FILE).getProperty(completedKey(heroClass, mode), "false").trim());
    }

    /** Yamata no Orochi is open once the 4 classes have finished their Adventure in NORMAL and in HARDCORE. */
    public boolean orochiUnlocked() {
        Properties p = read(PROGRESS_FILE);
        for (HeroClass c : ADVENTURE_CLASSES) {
            for (AdventureMode m : AdventureMode.values()) {
                if (!Boolean.parseBoolean(p.getProperty(completedKey(c, m), "false").trim())) return false;
            }
        }
        return true;
    }

    /** How many times Yamata no Orochi was beaten. */
    public int loadOrochiKills() {
        return intOf(read(PROGRESS_FILE).getProperty("orochi.kills"), 0);
    }

    /** One more win against Yamata no Orochi. */
    public boolean addOrochiKill() {
        Properties p = read(PROGRESS_FILE);
        p.setProperty("orochi.kills", String.valueOf(intOf(p.getProperty("orochi.kills"), 0) + 1));
        return write(PROGRESS_FILE, p, "Oni Shin Tan - progress");
    }

    private static String completedKey(HeroClass c, AdventureMode m) {
        return "completed." + c.name() + "." + m.name();
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

    // ---------- profile ----------

    /** The saved Profile stats, or Stats.defaults(). A broken number counts 0. */
    public Stats loadStats() {
        Properties p = read(PROFILE_FILE);
        Map<HeroClass, Integer> wins = new EnumMap<>(HeroClass.class);
        Map<HeroClass, Integer> losses = new EnumMap<>(HeroClass.class);
        for (HeroClass c : HeroClass.values()) {
            wins.put(c, intOf(p.getProperty("wins." + c.name()), 0));
            losses.put(c, intOf(p.getProperty("losses." + c.name()), 0));
        }
        return new Stats(p.getProperty("username", ""), intOf(p.getProperty("rei"), 0), wins, losses);
    }

    /** Saves the Profile stats; the heroes picked per class stay as they are. */
    public boolean saveStats(Stats s) {
        Stats stats = s == null ? Stats.defaults() : s;
        Properties p = read(PROFILE_FILE);
        p.setProperty("username", stats.username());
        p.setProperty("rei", String.valueOf(stats.rei()));
        for (HeroClass c : HeroClass.values()) {
            p.setProperty("wins." + c.name(), String.valueOf(stats.winsOf(c)));
            p.setProperty("losses." + c.name(), String.valueOf(stats.lossesOf(c)));
        }
        return write(PROFILE_FILE, p, "Oni Shin Tan - profile");
    }

    /** The hero picked for each class (its id, e.g. HeroChoice's); a class with none picked is missing. */
    public Map<HeroClass, String> loadHeroes() {
        Properties p = read(PROFILE_FILE);
        Map<HeroClass, String> heroes = new EnumMap<>(HeroClass.class);
        for (HeroClass c : HeroClass.values()) {
            String id = p.getProperty("hero." + c.name());
            if (id != null && !id.isBlank()) heroes.put(c, id.trim());
        }
        return Collections.unmodifiableMap(heroes);
    }

    /** Replaces the heroes picked per class; the Profile stats stay as they are. */
    public boolean saveHeroes(Map<HeroClass, String> heroes) {
        Properties p = read(PROFILE_FILE);
        for (HeroClass c : HeroClass.values()) p.remove("hero." + c.name());
        if (heroes != null) {
            heroes.forEach((c, id) -> { if (c != null && id != null && !id.isBlank()) p.setProperty("hero." + c.name(), id.trim()); });
        }
        return write(PROFILE_FILE, p, "Oni Shin Tan - profile");
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

    private static AdventureMode modeOf(String text) {
        if (text == null) return null;
        try {
            return AdventureMode.valueOf(text.trim());
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