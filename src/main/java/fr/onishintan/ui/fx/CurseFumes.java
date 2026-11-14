package fr.onishintan.ui.fx;

import java.util.ArrayList;
import java.util.List;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Group;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * Dark fumes rising from a cursed card, over and over, in card coordinates (530 x 742).
 * Each puff starts low on the card, drifts up past its top edge, grows and fades, then starts again.
 * The animations stop by themselves when the card leaves the screen.
 */
public class CurseFumes extends Group {
    /** The card's size, in card coordinates. */
    private static final double CARD_W = 530, CARD_H = 742;
    private static final int PUFFS = 14;
    private static final Color FUME = Color.rgb(25, 8, 30);

    /** Every puff's animation, so they can all be stopped. */
    private final List<Timeline> loops = new ArrayList<>();

    public CurseFumes() {
        for (int i = 0; i < PUFFS; i++) {
            getChildren().add(puff());
        }
        setEffect(new GaussianBlur(25));   // one blur for all the puffs: soft smoke, cheap to draw
        setMouseTransparent(true);         // clicks and drags still reach the card

        // The hand is redrawn after every action: when this card leaves the screen, its puffs stop.
        sceneProperty().addListener((property, oldScene, newScene) -> {
            if (newScene == null) {
                stop();
            }
        });
    }

    /** One puff of smoke, with its own place, size, speed and start time, rising forever. */
    private Circle puff() {
        double x = Math.random() * CARD_W;
        double startY = CARD_H * (0.4 + Math.random() * 0.6);   // somewhere in the lower part of the card
        double radius = 35 + Math.random() * 35;
        double rise = 250 + Math.random() * 200;
        Duration life = Duration.seconds(2.5 + Math.random() * 1.5);

        Circle puff = new Circle(x, startY, radius, FUME);
        puff.setOpacity(0);

        // Small and invisible at the start, darkest a third of the way up, big and gone at the top.
        KeyFrame start = new KeyFrame(Duration.ZERO,
                new KeyValue(puff.translateYProperty(), 0),
                new KeyValue(puff.scaleXProperty(), 0.6),
                new KeyValue(puff.scaleYProperty(), 0.6),
                new KeyValue(puff.opacityProperty(), 0));
        KeyFrame darkest = new KeyFrame(life.multiply(0.3),
                new KeyValue(puff.opacityProperty(), 0.7));
        KeyFrame end = new KeyFrame(life,
                new KeyValue(puff.translateYProperty(), -rise),
                new KeyValue(puff.scaleXProperty(), 1.8),
                new KeyValue(puff.scaleYProperty(), 1.8),
                new KeyValue(puff.opacityProperty(), 0));

        Timeline loop = new Timeline(start, darkest, end);
        loop.setCycleCount(Animation.INDEFINITE);
        loop.setDelay(Duration.seconds(Math.random() * 3));   // the puffs don't all start together
        loop.play();
        loops.add(loop);
        return puff;
    }

    /** Stops every puff. */
    private void stop() {
        for (Timeline loop : loops) {
            loop.stop();
        }
    }
}
