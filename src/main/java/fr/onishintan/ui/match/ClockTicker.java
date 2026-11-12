package fr.onishintan.ui.match;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

/**
 * The beat of the chess clocks: every TICK it runs onTick (MatchController checks the engine's clock and redraws the times).
 * Also turns a time in ms into the "m:ss" shown on the table.
 */
class ClockTicker {
    /** How often the clocks are checked and redrawn (Pierre: about 200 ms). */
    static final Duration TICK = Duration.millis(200);

    private final Timeline timeline;

    ClockTicker(Runnable onTick) {
        timeline = new Timeline(new KeyFrame(TICK, event -> onTick.run()));
        timeline.setCycleCount(Animation.INDEFINITE);
    }

    /** Starts (or restarts) the ticking. */
    void start() {
        timeline.play();
    }

    /** Stops the ticking: during a timeout animation, and once the match is over. */
    void stop() {
        timeline.stop();
    }

    /** A time in ms as m:ss, rounded up so 0:00 only shows once the time is really out (7:00, 0:09). */
    static String format(long ms) {
        long seconds = (ms + 999) / 1000;
        long minutes = seconds / 60;
        long rest = seconds % 60;
        return minutes + ":" + String.format("%02d", rest);
    }
}