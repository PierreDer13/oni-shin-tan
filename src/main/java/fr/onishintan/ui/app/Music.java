package fr.onishintan.ui.app;

import javafx.scene.media.MediaPlayer;

/**
 * The background music: one track at a time, looping. Going to a screen with the same track lets it play on;
 * a different track stops the old one and starts the new one. Silent if audio is unavailable (see Sound).
 */
public final class Music {
    private static final double VOLUME = 0.35;

    /** Kept in a field so it isn't garbage-collected (that would stop the music). */
    private MediaPlayer player;
    private String track = "";

    /** Plays /sounds/<name>.mp3 in a loop, unless it is the track already playing. */
    public void play(String name) {
        if (name.equals(track)) {
            return;
        }
        stop();
        track = name;
        player = Sound.music(name, VOLUME);
    }

    /** Stops the current track. */
    private void stop() {
        if (player != null) {
            player.stop();
            player.dispose();
            player = null;
        }
    }
}
