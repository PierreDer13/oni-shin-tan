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
import javafx.scene.image.Image;
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

    /** Gap between two of Raijin's cuts. */
    private static final double SLICE_GAP = 90;
    /** How long a shuriken or Jikininki's beam takes to reach its target (the damage waits for it: MatchController.POWER_PAUSE). */
    private static final Duration FLIGHT = Duration.millis(400);
    /** Ninja's Shuriken: the star, how many are thrown, their width, and the time between two throws. */
    private static final Image STAR = new Image(SignatureFx.class.getResourceAsStream("/images/fx/shuriken-star.png"));
    private static final int STARS = 5;
    private static final double STAR_W = 30, STAR_GAP = 25;
    /** Jikininki's demon hand: its width, its clenched picture, and how many drops of life it squeezes out. */
    private static final double HAND_W = 180;
    private static final Image CLOSED_HAND = new Image(SignatureFx.class.getResourceAsStream("/images/fx/jikininki-closed.png"));
    private static final int DROPS = 8;

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

        puff(layer, midX, midY);   // above the two minions
        return 950;
    }

    /** One duellist: lunges by (dx, dy), holds there under the smoke, then goes back to its slot. */
    private static void clash(Node minion, double dx, double dy) {
        new Timeline(
                new KeyFrame(Duration.millis(250), new KeyValue(minion.translateXProperty(), dx), new KeyValue(minion.translateYProperty(), dy)),
                new KeyFrame(Duration.millis(650), new KeyValue(minion.translateXProperty(), dx), new KeyValue(minion.translateYProperty(), dy)),
                new KeyFrame(Duration.millis(900), new KeyValue(minion.translateXProperty(), 0), new KeyValue(minion.translateYProperty(), 0))).play();
    }


    /** A white smoke bursts above (x, y) and drifts up (Kettō's clash, Fūma Kotarō revealing himself). */
    static void puff(Pane layer, double x, double y) {
        ImageView smoke = BoardView.image("/images/fx/black-smoke.png");
        smoke.setEffect(new ColorAdjust(0, 0, 1, 0));   // brightened to white
        smoke.setFitWidth(240);
        smoke.setPreserveRatio(true);
        smoke.setMouseTransparent(true);
        smoke.setOpacity(0);
        smoke.relocate(x - 120, y - 140);
        layer.getChildren().add(smoke);
        Timeline rise = new Timeline(
                new KeyFrame(Duration.millis(250), new KeyValue(smoke.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(400), new KeyValue(smoke.opacityProperty(), 1)),
                new KeyFrame(Duration.millis(900), new KeyValue(smoke.opacityProperty(), 0), new KeyValue(smoke.translateYProperty(), -40)));
        rise.setOnFinished(e -> layer.getChildren().remove(smoke));
        rise.play();
    }

    /** Inari Ōkami: a blue foxfire flares on the minion as it calls a kitsune. */
    static void foxfire(Pane layer, Node minion) {
        Bounds box = minion.getBoundsInParent();
        Circle fire = new Circle(box.getCenterX(), box.getCenterY(), 30, Color.web("#7fd4ff", 0.7));
        fire.setEffect(new DropShadow(30, Color.web("#3aa0ff")));
        fire.setMouseTransparent(true);
        layer.getChildren().add(fire);
        ScaleTransition grow = new ScaleTransition(Duration.millis(600), fire);
        grow.setToX(3);
        grow.setToY(3);
        FadeTransition fade = new FadeTransition(Duration.millis(600), fire);
        fade.setToValue(0);
        ParallelTransition all = new ParallelTransition(grow, fade);
        all.setOnFinished(e -> layer.getChildren().remove(fire));
        all.play();
    }

    /** Ninja's Shuriken: a volley of small stars (fx/shuriken-star.png) spins from `from` (your hero) into `to`, sparks where each hits. */
    static void shuriken(Pane layer, Node from, Node to) {
        Bounds start = from.getBoundsInParent();
        Bounds end = to.getBoundsInParent();
        for (int i = 0; i < STARS; i++) {
            ImageView star = new ImageView(STAR);
            star.setFitWidth(STAR_W);
            star.setPreserveRatio(true);
            star.setMouseTransparent(true);
            star.relocate(start.getCenterX() - STAR_W / 2, start.getCenterY() - STAR_W / 2);
            TranslateTransition fly = new TranslateTransition(FLIGHT, star);
            fly.setToX(end.getCenterX() - start.getCenterX() + (Math.random() - 0.5) * 50);   // a little spread on the target
            fly.setToY(end.getCenterY() - start.getCenterY() + (Math.random() - 0.5) * 50);
            RotateTransition spin = new RotateTransition(FLIGHT, star);
            spin.setByAngle(1080);
            ParallelTransition thrown = new ParallelTransition(fly, spin);
            thrown.setOnFinished(e -> {
                layer.getChildren().remove(star);
                Fx.sparks(layer, to);
            });
            PauseTransition wait = new PauseTransition(Duration.millis(i * STAR_GAP));   // thrown one after the other
            wait.setOnFinished(e -> {
                layer.getChildren().add(star);
                thrown.play();
            });
            wait.play();
        }
    }

    /**
     * Oni's Jikininki: a demon hand rises over `from` (the target) and clenches, squeezing its life out;
     * drops of it fly into `to` (your hero). The damage shows at the clench (MatchController.POWER_PAUSE).
     */
    static void siphon(Pane layer, Node from, Node to) {
        Bounds start = from.getBoundsInParent();
        Bounds end = to.getBoundsInParent();
        ImageView hand = BoardView.image("/images/fx/jikininki-open.png");
        hand.setFitWidth(HAND_W);
        hand.setPreserveRatio(true);
        hand.setMouseTransparent(true);
        hand.relocate(start.getCenterX() - HAND_W / 2, start.getCenterY() - HAND_W * 0.5);
        hand.setOpacity(0);
        hand.setTranslateY(40);   // rises into place
        layer.getChildren().add(hand);
        Timeline grab = new Timeline(
                new KeyFrame(Duration.millis(250), new KeyValue(hand.opacityProperty(), 1), new KeyValue(hand.translateYProperty(), 0)),
                new KeyFrame(Duration.millis(400), e -> hand.setImage(CLOSED_HAND)),   // the clench
                new KeyFrame(Duration.millis(700), new KeyValue(hand.opacityProperty(), 1)),
                new KeyFrame(Duration.millis(1000), new KeyValue(hand.opacityProperty(), 0)));
        grab.setOnFinished(e -> layer.getChildren().remove(hand));
        grab.play();
        for (int i = 0; i < DROPS; i++) {
            drop(layer, start, end, 400 + i * 50);   // the drops leave one after the other from the clench
        }
    }

    /** One drop of stolen life: a red bead that flies from the target into your hero, after `delay` ms. */
    private static void drop(Pane layer, Bounds from, Bounds to, double delay) {
        double x = from.getCenterX() + (Math.random() - 0.5) * 60;
        double y = from.getCenterY() + (Math.random() - 0.5) * 40;
        Circle bead = new Circle(x, y, 6, Color.web("#c41e1e"));
        bead.setEffect(new DropShadow(10, Color.web("#ff4a4a")));
        bead.setMouseTransparent(true);
        bead.setOpacity(0);
        layer.getChildren().add(bead);
        Timeline fly = new Timeline(
                new KeyFrame(Duration.millis(delay), new KeyValue(bead.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(delay + 80), new KeyValue(bead.opacityProperty(), 1)),
                new KeyFrame(Duration.millis(delay + 500),
                        new KeyValue(bead.translateXProperty(), to.getCenterX() - x, Interpolator.EASE_IN),
                        new KeyValue(bead.translateYProperty(), to.getCenterY() - y, Interpolator.EASE_IN),
                        new KeyValue(bead.opacityProperty(), 0.9)),
                new KeyFrame(Duration.millis(delay + 560), new KeyValue(bead.opacityProperty(), 0)));
        fly.setOnFinished(e -> layer.getChildren().remove(bead));
        fly.play();
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