package fr.onishintan.ui.match;

import fr.onishintan.model.BoardSide;

import java.util.List;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * The moves of single cards, played before their effects so you see what is coming:
 * Iaidō's blade draw, Nue's shockwave, Enma's judgement, Sakasama's flip, Kamikakushi's vanishing.
 * Each one that the effects wait for returns how long it plays.
 */
final class SignatureFx {
    private SignatureFx() { }

    /** Iaidō: one long blade flash, drawn from the middle out, between the two rows of minions. */
    static double iaido(Pane layer) {
        Rectangle blade = new Rectangle(BoardView.SLOT_X - 40, 430, BoardSide.MAX * BoardView.SLOT_STEP + 80, 6);
        blade.setFill(Color.WHITE);
        blade.setEffect(new DropShadow(20, Color.web("#bfe6ff")));
        blade.setRotate(-6);
        blade.setScaleX(0);
        blade.setMouseTransparent(true);
        layer.getChildren().add(blade);
        ScaleTransition draw = new ScaleTransition(Duration.millis(150), blade);
        draw.setToX(1);
        FadeTransition fade = new FadeTransition(Duration.millis(300), blade);
        fade.setToValue(0);
        SequentialTransition all = new SequentialTransition(draw, fade);
        all.setOnFinished(e -> layer.getChildren().remove(blade));
        all.play();
        return 450;
    }

    /** Nue: a purple ring bursts out from (x, y) across the whole board. */
    static double shockwave(Pane layer, double x, double y) {
        Circle ring = new Circle(x, y, 40, Color.TRANSPARENT);
        ring.setStroke(Color.web("#9b4dff"));
        ring.setStrokeWidth(8);
        ring.setEffect(new DropShadow(25, Color.web("#9b4dff")));
        ring.setMouseTransparent(true);
        layer.getChildren().add(ring);
        Timeline burst = new Timeline(new KeyFrame(Duration.millis(500),
                new KeyValue(ring.radiusProperty(), 700),
                new KeyValue(ring.opacityProperty(), 0)));
        burst.setOnFinished(e -> layer.getChildren().remove(ring));
        burst.play();
        return 500;
    }

    /** Enma: the whole board flashes blood red, then clears. */
    static double judgement(Pane layer) {
        Rectangle veil = new Rectangle(BoardView.W, BoardView.H, Color.web("#8b0000"));
        veil.setOpacity(0);
        veil.setMouseTransparent(true);
        layer.getChildren().add(veil);
        Timeline flash = new Timeline(
                new KeyFrame(Duration.millis(200), new KeyValue(veil.opacityProperty(), 0.45)),
                new KeyFrame(Duration.millis(700), new KeyValue(veil.opacityProperty(), 0)));
        flash.setOnFinished(e -> layer.getChildren().remove(veil));
        flash.play();
        return 700;
    }

    /** Sakasama: every enemy minion turns upside down and back as its ATK and HP swap. */
    static double upsideDown(List<Node> minions) {
        for (Node minion : minions) {
            ScaleTransition turn = new ScaleTransition(Duration.millis(300), minion);
            turn.setToY(-1);
            turn.setAutoReverse(true);
            turn.setCycleCount(2);
            turn.play();
        }
        return 600;
    }

    /** Kamikakushi: the minion is spirited away, shrinking and spinning into nothing. */
    static void vanish(Node minion) {
        Duration time = Duration.millis(450);
        ScaleTransition shrink = new ScaleTransition(time, minion);
        shrink.setToX(0);
        shrink.setToY(0);
        RotateTransition spin = new RotateTransition(time, minion);
        spin.setByAngle(180);
        FadeTransition fade = new FadeTransition(time, minion);
        fade.setToValue(0);
        new ParallelTransition(shrink, spin, fade).play();
    }
}