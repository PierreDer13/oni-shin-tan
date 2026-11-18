package fr.onishintan.ui.match;

import fr.onishintan.model.BoardSide;
import fr.onishintan.ui.fx.Fx;

import java.util.List;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
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

    /** Gap between two of Raijin's cuts. */
    private static final double SLICE_GAP = 90;
    /** How long a shuriken or Jikininki's beam takes to reach its target (the damage waits for it: MatchController.POWER_PAUSE). */
    private static final Duration FLIGHT = Duration.millis(400);

    /** Raijin: a slash on each target in turn, one after another. */
    static double slices(Pane layer, List<Node> targets) {
        for (int i = 0; i < targets.size(); i++) {
            Node target = targets.get(i);
            PauseTransition wait = new PauseTransition(Duration.millis(i * SLICE_GAP));
            wait.setOnFinished(e -> Fx.slash(layer, target));
            wait.play();
        }
        return targets.size() * SLICE_GAP + 300;
    }

    /** Kettō: both minions lunge to meet in the middle, a white smoke bursts above them, then they go back to their slots. */
    static double duel(Pane layer, Node a, Node b) {
        Bounds boxA = a.getBoundsInParent();
        Bounds boxB = b.getBoundsInParent();
        double midX = (boxA.getCenterX() + boxB.getCenterX()) / 2;
        double midY = (boxA.getCenterY() + boxB.getCenterY()) / 2;
        clash(a, (midX - boxA.getCenterX()) * 0.7, (midY - boxA.getCenterY()) * 0.7);
        clash(b, (midX - boxB.getCenterX()) * 0.7, (midY - boxB.getCenterY()) * 0.7);

        ImageView smoke = BoardView.image("/images/fx/black-smoke.png");
        smoke.setEffect(new ColorAdjust(0, 0, 1, 0));   // brightened to white
        smoke.setFitWidth(240);
        smoke.setPreserveRatio(true);
        smoke.setMouseTransparent(true);
        smoke.setOpacity(0);
        smoke.relocate(midX - 120, midY - 140);   // above the two minions
        layer.getChildren().add(smoke);
        Timeline puff = new Timeline(
                new KeyFrame(Duration.millis(250), new KeyValue(smoke.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(400), new KeyValue(smoke.opacityProperty(), 1)),
                new KeyFrame(Duration.millis(900), new KeyValue(smoke.opacityProperty(), 0), new KeyValue(smoke.translateYProperty(), -40)));
        puff.setOnFinished(e -> layer.getChildren().remove(smoke));
        puff.play();
        return 950;
    }

    /** One duellist: lunges by (dx, dy), holds there under the smoke, then goes back to its slot. */
    private static void clash(Node minion, double dx, double dy) {
        new Timeline(
                new KeyFrame(Duration.millis(250), new KeyValue(minion.translateXProperty(), dx), new KeyValue(minion.translateYProperty(), dy)),
                new KeyFrame(Duration.millis(650), new KeyValue(minion.translateXProperty(), dx), new KeyValue(minion.translateYProperty(), dy)),
                new KeyFrame(Duration.millis(900), new KeyValue(minion.translateXProperty(), 0), new KeyValue(minion.translateYProperty(), 0))).play();
    }

    /** Ninja's Shuriken: a steel star spins from `from` (your hero) to `to`, sparks where it hits. */
    static void shuriken(Pane layer, Node from, Node to) {
        Bounds start = from.getBoundsInParent();
        Bounds end = to.getBoundsInParent();
        Polygon star = new Polygon(0, -14, 4, -4, 14, 0, 4, 4, 0, 14, -4, 4, -14, 0, -4, -4);
        star.setFill(Color.web("#c9ced6"));
        star.setStroke(Color.web("#3a3f47"));
        star.setMouseTransparent(true);
        star.setLayoutX(start.getCenterX());
        star.setLayoutY(start.getCenterY());
        layer.getChildren().add(star);
        TranslateTransition fly = new TranslateTransition(FLIGHT, star);
        fly.setToX(end.getCenterX() - start.getCenterX());
        fly.setToY(end.getCenterY() - start.getCenterY());
        RotateTransition spin = new RotateTransition(FLIGHT, star);
        spin.setByAngle(1080);
        ParallelTransition thrown = new ParallelTransition(fly, spin);
        thrown.setOnFinished(e -> {
            layer.getChildren().remove(star);
            Fx.sparks(layer, to);
        });
        thrown.play();
    }

    /** Oni's Jikininki: a black beam flows from `from` (the target) into `to` (your hero), draining its life, then fades. */
    static void siphon(Pane layer, Node from, Node to) {
        Bounds start = from.getBoundsInParent();
        Bounds end = to.getBoundsInParent();
        Line beam = new Line(start.getCenterX(), start.getCenterY(), end.getCenterX(), end.getCenterY());
        beam.setStroke(Color.web("#120a14"));
        beam.setStrokeWidth(10);
        beam.getStrokeDashArray().addAll(24.0, 12.0);
        beam.setEffect(new DropShadow(18, Color.web("#7a1fa2")));
        beam.setMouseTransparent(true);
        beam.setOpacity(0);
        layer.getChildren().add(beam);
        Timeline flow = new Timeline(   // the dashes run toward the hero: a lower dash offset moves them to the line's end
                new KeyFrame(Duration.millis(100), new KeyValue(beam.opacityProperty(), 1)),
                new KeyFrame(Duration.millis(700), new KeyValue(beam.strokeDashOffsetProperty(), -144), new KeyValue(beam.opacityProperty(), 1)),
                new KeyFrame(Duration.millis(900), new KeyValue(beam.opacityProperty(), 0)));
        flow.setOnFinished(e -> layer.getChildren().remove(beam));
        flow.play();
    }

    /** Seppuku: a katana drives down into the dying minion, then fades. */
    static void seppuku(Pane layer, Node minion) {
        Bounds box = minion.getBoundsInParent();
        ImageView blade = BoardView.image("/images/weapons/katana.png");   // 400 x 55, hilt left, tip right
        blade.setFitWidth(150);
        blade.setPreserveRatio(true);
        blade.setRotate(90);   // tip down
        blade.setMouseTransparent(true);
        blade.relocate(box.getCenterX() - 75, box.getCenterY() - 30);
        blade.setTranslateY(-90);
        layer.getChildren().add(blade);
        Timeline stab = new Timeline(
                new KeyFrame(Duration.millis(150), new KeyValue(blade.translateYProperty(), 0, Interpolator.EASE_IN)),
                new KeyFrame(Duration.millis(450), new KeyValue(blade.opacityProperty(), 1)),
                new KeyFrame(Duration.millis(750), new KeyValue(blade.opacityProperty(), 0)));
        stab.setOnFinished(e -> layer.getChildren().remove(blade));
        stab.play();
    }
}