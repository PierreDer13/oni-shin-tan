package fr.onishintan.ui.app;

import fr.onishintan.data.SaveManager;
import fr.onishintan.model.HeroClass;

/** The player's saved profile (Pierre's SaveManager): name, Rei and results per class, updated at the end of each match. */
public final class Profile {
    /** The name shown until the player types one. */
    public static final String DEFAULT_NAME = "Player";
    private static final SaveManager SAVES = new SaveManager();

    private Profile() {
        // Only static rules: nobody creates a Profile.
    }

    /** The saved stats as they are now. */
    public static SaveManager.Stats stats() {
        return SAVES.loadStats();
    }

    /** The name to show: the saved one, or "Player" before one is chosen. */
    public static String name() {
        String saved = stats().username();
        if (saved.isBlank()) {
            return DEFAULT_NAME;
        }
        return saved;
    }

    /** Keeps a new name. */
    public static void rename(String name) {
        SAVES.saveStats(stats().withUsername(name.trim()));
    }

    /** A match ended: counts it for this class; a PvP match also moves the Rei (Rei.afterWin / afterLoss), an Adventure battle doesn't. */
    public static void record(HeroClass heroClass, boolean won, boolean pvp) {
        SaveManager.Stats updated = stats().withResult(heroClass, won);
        if (pvp) {
            int rei = Rei.afterLoss(updated.rei());
            if (won) {
                rei = Rei.afterWin(updated.rei());
            }
            updated = updated.withRei(rei);
        }
        SAVES.saveStats(updated);
    }
}
