package fr.onishintan.ui.app;

import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

/** Every sound in the game, loaded so it can never crash it: on a machine where audio doesn't work
 *  (no codec, no sound device, missing native library), these return null and the game just runs silently. */
public final class Sound {
    private Sound() {}

    /** A short effect from /sounds/<name>.wav, already warmed up so the first play has no delay; null if audio is unavailable. */
    public static AudioClip clip(String name) {
        try {
            AudioClip c = new AudioClip(Sound.class.getResource("/sounds/" + name + ".wav").toExternalForm());
            c.play(0);
            return c;
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    /** Plays an effect, if it loaded. */
    public static void play(AudioClip c) { if (c != null) c.play(); }

    /** A background track from /sounds/<name>.mp3, looping at the given volume; null if audio is unavailable.
     *  Keep the returned player in a field, or it gets garbage-collected and the music stops. */
    public static MediaPlayer music(String name, double volume) {
        try {
            MediaPlayer p = new MediaPlayer(new Media(Sound.class.getResource("/sounds/" + name + ".mp3").toExternalForm()));
            p.setCycleCount(MediaPlayer.INDEFINITE);
            p.setVolume(volume);
            p.play();
            return p;
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }
}