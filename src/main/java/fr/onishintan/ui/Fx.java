package fr.onishintan.ui;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.util.Duration;

/** Combat effects on the board (GDD, "Match: cards on the board"). Positions are in the layer's own space. */
final class Fx {
    private Fx() {}

    /** The attacker grows and hovers, lunges most of the way to the target, then glides back; hit runs on impact. */
    static void attack(Node a, Node t, Runnable hit) {
        double dx = t.getBoundsInParent().getCenterX() - a.getBoundsInParent().getCenterX();
        double dy = t.getBoundsInParent().getCenterY() - a.getBoundsInParent().getCenterY();
        a.setViewOrder(-1);
        Timeline tl = new Timeline(
                new KeyFrame(Duration.millis(180), Anim.ease(a.scaleXProperty(), 1.15), Anim.ease(a.scaleYProperty(), 1.15),
                        Anim.ease(a.translateXProperty(), 0.0), Anim.ease(a.translateYProperty(), -15.0)),
                new KeyFrame(Duration.millis(300), e -> hit.run(),
                        new KeyValue(a.translateXProperty(), dx * 0.7, Interpolator.EASE_IN),
                        new KeyValue(a.translateYProperty(), dy * 0.7 - 15, Interpolator.EASE_IN)),
                new KeyFrame(Duration.millis(650), new KeyValue(a.translateXProperty(), dx * 0.7), new KeyValue(a.translateYProperty(), dy * 0.7 - 15)),
                new KeyFrame(Duration.millis(950), Anim.ease(a.translateXProperty(), 0.0), Anim.ease(a.translateYProperty(), 0.0),
                        Anim.ease(a.scaleXProperty(), 1.0), Anim.ease(a.scaleYProperty(), 1.0)));
        tl.setOnFinished(e -> a.setViewOrder(0));
        tl.play();
    }

    /** A quick white slash across the target, drawn from its top-right to its bottom-left corner, then fading. */
    static void slash(Pane layer, Node t) {
        Bounds b = t.getBoundsInParent();
        Line l = new Line(b.getMaxX(), b.getMinY(), b.getMaxX(), b.getMinY());
        l.setStroke(Color.WHITE);
        l.setStrokeWidth(5);
        l.setEffect(new DropShadow(12, Color.CRIMSON));
        l.setMouseTransparent(true);
        layer.getChildren().add(l);
        Timeline tl = new Timeline(
                new KeyFrame(Duration.millis(110), new KeyValue(l.endXProperty(), b.getMinX()), new KeyValue(l.endYProperty(), b.getMaxY())),
                new KeyFrame(Duration.millis(400), new KeyValue(l.opacityProperty(), 0)));
        tl.setOnFinished(e -> layer.getChildren().remove(l));
        tl.play();
    }

    /** A big red "-N" that pops up over a character, then floats up and fades out over 1 second. */
    static void damageNumber(Pane layer, Node over, int amount) {
        if (amount <= 0) return;
        Label l = new Label("-" + amount);
        l.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: 72; -fx-font-weight: bold; -fx-text-fill: #e0201a; -fx-alignment: center;");
        l.setEffect(new DropShadow(6, Color.BLACK));
        l.setMouseTransparent(true);
        l.setViewOrder(-3);
        l.setPrefWidth(200);
        Bounds b = over.getBoundsInParent();
        l.relocate(b.getCenterX() - 100, b.getCenterY() - 50);
        l.setScaleX(0.4);
        l.setScaleY(0.4);
        layer.getChildren().add(l);
        Timeline tl = new Timeline(
                new KeyFrame(Duration.millis(150), new KeyValue(l.scaleXProperty(), 1.25), new KeyValue(l.scaleYProperty(), 1.25)),
                new KeyFrame(Duration.millis(250), new KeyValue(l.scaleXProperty(), 1.0), new KeyValue(l.scaleYProperty(), 1.0)),
                new KeyFrame(Duration.millis(450), new KeyValue(l.translateYProperty(), 0), new KeyValue(l.opacityProperty(), 1)),
                new KeyFrame(Duration.seconds(1.2), new KeyValue(l.translateYProperty(), -50, Interpolator.EASE_OUT),
                        new KeyValue(l.opacityProperty(), 0, Interpolator.EASE_IN)));
        tl.setOnFinished(e -> layer.getChildren().remove(l));
        tl.play();
    }

    /** The card is cut in half diagonally: both halves slide apart, tilt and fade, then the card is gone. */
    static void deathCut(Pane layer, Node card) {
        Bounds b = card.getBoundsInParent();
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        Image img = card.snapshot(sp, null);
        card.setVisible(false);
        double w = img.getWidth(), h = img.getHeight();
        for (int side = 0; side < 2; side++) {
            ImageView half = new ImageView(img);
            half.setClip(side == 0 ? new Polygon(0, 0, w, 0, 0, h) : new Polygon(w, 0, w, h, 0, h));
            half.relocate(b.getMinX(), b.getMinY());
            half.setMouseTransparent(true);
            layer.getChildren().add(half);
            double dir = side == 0 ? -1 : 1;
            Timeline tl = new Timeline(new KeyFrame(Duration.millis(600),
                    new KeyValue(half.translateXProperty(), 25 * dir), new KeyValue(half.translateYProperty(), 25 * dir),
                    new KeyValue(half.rotateProperty(), 8 * dir), new KeyValue(half.opacityProperty(), 0)));
            tl.setOnFinished(e -> layer.getChildren().remove(half));
            tl.play();
        }
    }
}