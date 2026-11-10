package fr.onishintan.ui.app;

import java.util.List;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.media.AudioClip;
import javafx.util.Duration;

/** Two shōji doors over the whole window: they slide shut, the screen is swapped while they're closed, then they slide open (GDD).
 *  Sounds: a slide as they close, a wooden tap when they meet, a slide as they open. */
public class ShojiTransition extends Pane {
    private static final Duration SLIDE = Duration.millis(350), HOLD = Duration.millis(400);

    private final ImageView left = door("left"), right = door("right");
    private final AudioClip closeSound = Sound.clip("door-close"), tapSound = Sound.clip("door-tap"), openSound = Sound.clip("door-open");

    public ShojiTransition() {
        for (ImageView d : List.of(left, right)) {
            d.fitWidthProperty().bind(widthProperty().divide(2));
            d.fitHeightProperty().bind(heightProperty());
        }
        right.layoutXProperty().bind(widthProperty().divide(2));
        getChildren().addAll(left, right);
        setVisible(false);
    }

    /** Closes the doors, runs swap while the screen is hidden, then opens them again. */
    public void play(Runnable swap) {
        double half = getWidth() / 2;
        left.setTranslateX(-half);
        right.setTranslateX(half);
        Sound.play(closeSound);
        setVisible(true);
        Duration shut = SLIDE, reopen = SLIDE.add(HOLD);
        new Timeline(
            new KeyFrame(shut, e -> { Sound.play(tapSound); swap.run(); }, slide(left, 0, Interpolator.EASE_IN), slide(right, 0, Interpolator.EASE_IN)),
            new KeyFrame(reopen, e -> Sound.play(openSound), slide(left, 0, Interpolator.LINEAR), slide(right, 0, Interpolator.LINEAR)),
            new KeyFrame(reopen.add(SLIDE), e -> setVisible(false), slide(left, -half, Interpolator.EASE_OUT), slide(right, half, Interpolator.EASE_OUT))
        ).play();
    }

    private static KeyValue slide(ImageView d, double x, Interpolator how) { return new KeyValue(d.translateXProperty(), x, how); }

    private ImageView door(String side) {
        return new ImageView(new Image(getClass().getResourceAsStream("/images/sliding-doors/edo_sliding_door_" + side + ".png")));
    }
}