package fr.onishintan.ui.app;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

/** The two volumes the player sets in Settings (0 = silent, 1 = full); Music and Sound read them. Kept while the game runs. */
public final class AudioSettings {
    /** Background music volume (starts at 35 %, as before). */
    public static final DoubleProperty MUSIC = new SimpleDoubleProperty(0.35);
    /** Sound effects volume: clicks, shōji doors, combat. */
    public static final DoubleProperty EFFECTS = new SimpleDoubleProperty(1.0);

    private AudioSettings() {
        // Only static volumes: nobody creates an AudioSettings.
    }
}