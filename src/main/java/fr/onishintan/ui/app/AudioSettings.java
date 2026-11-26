package fr.onishintan.ui.app;

import fr.onishintan.data.SaveManager;

import javafx.beans.property.DoubleProperty;
import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleDoubleProperty;

/** The two volumes the player sets in Settings (0 = silent, 1 = full); Music and Sound read them. Saved on disk, so they come back next time. */
public final class AudioSettings {
    /** Reads and writes the volumes on disk; a missing or broken file gives the defaults (35 % and 100 %). */
    private static final SaveManager SAVES = new SaveManager();
    private static final SaveManager.Volumes SAVED = SAVES.loadVolumes();
    /** Background music volume, as last saved. */
    public static final DoubleProperty MUSIC = new SimpleDoubleProperty(SAVED.music());
    /** Sound effects volume (clicks, shōji doors, combat), as last saved. */
    public static final DoubleProperty EFFECTS = new SimpleDoubleProperty(SAVED.effects());

    static {
        // Every change of either volume is written to disk at once.
        InvalidationListener save = change -> SAVES.saveVolumes(new SaveManager.Volumes(MUSIC.get(), EFFECTS.get()));
        MUSIC.addListener(save);
        EFFECTS.addListener(save);
    }

    private AudioSettings() {
        // Only static volumes: nobody creates an AudioSettings.
    }
}