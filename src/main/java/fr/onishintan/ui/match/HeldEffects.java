package fr.onishintan.ui.match;

import javafx.animation.PauseTransition;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Banners and stamps held back from the end of a turn until the pass veil is clicked,
 * so the veil doesn't hide them; then they play one after the other.
 */
class HeldEffects {

    /** Time between two held effects, so they don't play on top of each other. */
    private static final Duration HELD_GAP = Duration.millis(1900);

    /** True from the end of a turn until the pass veil is clicked. */
    private boolean holding;
    /** The banners and stamps waiting for the veil to lift, in order. */
    private final List<Runnable> waiting = new ArrayList<>();

    /** From now until the next pass veil is clicked, banners and stamps wait. */
    void hold() {
        holding = true;
    }

    /** Runs the effect now, or keeps it for when the pass veil lifts. */
    void runOrHold(Runnable effect) {
        if (holding) {
            waiting.add(effect);
            return;
        }
        effect.run();
    }

    /** The veil has lifted: the waiting effects play one after the other. */
    void release() {
        holding = false;
        for (int i = 0; i < waiting.size(); i++) {
            Runnable effect = waiting.get(i);
            PauseTransition wait = new PauseTransition(HELD_GAP.multiply(i));
            wait.setOnFinished(event -> effect.run());
            wait.play();
        }
        waiting.clear();
    }
}
