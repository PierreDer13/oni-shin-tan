package fr.onishintan.ui.fx;

import fr.onishintan.ui.app.Sound;
import javafx.scene.media.AudioClip;
import javafx.animation.AnimationTimer;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.util.Duration;
import javafx.scene.Group;

/** Combat effects on the board (GDD, "Match: cards on the board"). Positions are in the layer's own space. */
public final class Fx {
    /** The sound of any heal (sounds/heal.wav). */
    private static final AudioClip HEAL = Sound.clip("heal");
    private Fx() {}

    /** The attacker grows and hovers, lunges most of the way to the target, then glides back; hit runs on impact. */
    public static void attack(Node a, Node t, Runnable hit) {
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
    public static void slash(Pane layer, Node t) {
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
    public static void damageNumber(Pane layer, Node over, int amount) {
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

    /**
     * Heal: soft green and gold motes rise from the character, it glows green for a moment,
     * and a big green "+N" pops up and floats away (the mirror of damageNumber).
     */
    public static void heal(Pane layer, Node over, int amount) {
        if (amount <= 0) {
            return;
        }
        Sound.play(HEAL);   // every heal, with the green number
        Bounds b = over.getBoundsInParent();
        healMotes(layer, b);
        healGlow(over);
        healNumber(layer, b, "+" + amount);
    }

    /** A buff (Shōgun, a Shrine's aura, Ōishi): the heal's glow and motes, with "+ATK/+HP" as the number. */
    public static void buff(Pane layer, Node over, int attack, int health) {
        Sound.play(HEAL);
        Bounds b = over.getBoundsInParent();
        healMotes(layer, b);
        healGlow(over);
        healNumber(layer, b, "+" + attack + "/+" + health);
    }

    /** 18 small glowing motes, spread over the bottom half of the character, that drift up while fading. */
    private static void healMotes(Pane layer, Bounds b) {
        for (int i = 0; i < 18; i++) {
            double x = b.getMinX() + Math.random() * b.getWidth();
            double y = b.getCenterY() + Math.random() * b.getHeight() / 2;
            Color colour = Color.web("#7dff9a");
            if (Math.random() < 0.35) {
                colour = Color.web("#fff3a8");   // a few golden ones
            }
            Circle mote = new Circle(x, y, 2 + Math.random() * 2.5, colour);
            mote.setEffect(new DropShadow(10, Color.web("#3ccf6e")));
            mote.setMouseTransparent(true);
            mote.setViewOrder(-3);
            mote.setOpacity(0);
            layer.getChildren().add(mote);

            double rise = 60 + Math.random() * 70;
            double drift = (Math.random() - 0.5) * 20;
            Timeline tl = new Timeline(
                    new KeyFrame(Duration.millis(150), new KeyValue(mote.opacityProperty(), 1)),
                    new KeyFrame(Duration.millis(1100),
                            new KeyValue(mote.translateYProperty(), -rise, Interpolator.EASE_OUT),
                            new KeyValue(mote.translateXProperty(), drift),
                            new KeyValue(mote.opacityProperty(), 0, Interpolator.EASE_IN)));
            tl.setDelay(Duration.millis(Math.random() * 300));   // they don't all start at once
            tl.setOnFinished(e -> layer.getChildren().remove(mote));
            tl.play();
        }
    }

    /** The character glows green, then gets back the effect it had before (its selection border, if any). */
    private static void healGlow(Node over) {
        Effect before = over.getEffect();
        DropShadow glow = new DropShadow(0, Color.web("#3ccf6e"));
        glow.setSpread(0.35);
        glow.setInput(before);   // the border stays under the glow
        over.setEffect(glow);
        Timeline tl = new Timeline(
                new KeyFrame(Duration.millis(250), new KeyValue(glow.radiusProperty(), 40)),
                new KeyFrame(Duration.millis(900), new KeyValue(glow.radiusProperty(), 0)));
        tl.setOnFinished(e -> {
            if (over.getEffect() == glow) {
                over.setEffect(before);   // only if nothing else (a hover) changed it meanwhile
            }
        });
        tl.play();
    }

    /** A big green "+N" that pops up over the character, then floats up and fades out. */
    private static void healNumber(Pane layer, Bounds b, String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: 72; -fx-font-weight: bold; -fx-text-fill: #3ccf6e; -fx-alignment: center;");
        l.setEffect(new DropShadow(6, Color.BLACK));
        l.setMouseTransparent(true);
        l.setViewOrder(-3);
        l.setPrefWidth(300);   // room for "+4/+4"
        l.relocate(b.getCenterX() - 150, b.getCenterY() - 50);
        l.setScaleX(0.4);
        l.setScaleY(0.4);
        layer.getChildren().add(l);
        Timeline tl = new Timeline(
                new KeyFrame(Duration.millis(150), new KeyValue(l.scaleXProperty(), 1.25), new KeyValue(l.scaleYProperty(), 1.25)),
                new KeyFrame(Duration.millis(250), new KeyValue(l.scaleXProperty(), 1.0), new KeyValue(l.scaleYProperty(), 1.0)),
                new KeyFrame(Duration.millis(450), new KeyValue(l.translateYProperty(), 0), new KeyValue(l.opacityProperty(), 1)),
                new KeyFrame(Duration.seconds(1.4), new KeyValue(l.translateYProperty(), -70, Interpolator.EASE_OUT),
                        new KeyValue(l.opacityProperty(), 0, Interpolator.EASE_IN)));
        tl.setOnFinished(e -> layer.getChildren().remove(l));
        tl.play();
    }

    /** The card is cut in half diagonally: both halves slide apart, tilt and fade, then the card is gone. */
    public static void deathCut(Pane layer, Node card) {
        Bounds b = layer.sceneToLocal(card.localToScene(card.getBoundsInLocal()));
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

    /** A burst of hot sparks from the middle of a node: glowing dots fly out in every direction, drop a little and fade. */
    public static void sparks(Pane layer, Node over) {
        Bounds b = layer.sceneToLocal(over.localToScene(over.getBoundsInLocal()));
        for (int i = 0; i < 28; i++) {
            Circle s = new Circle(b.getCenterX(), b.getCenterY(), 1.5 + Math.random() * 2.5, Color.web(Math.random() < 0.5 ? "#ffd84a" : "#ff8a1f"));
            s.setEffect(new DropShadow(8, Color.ORANGE));
            s.setMouseTransparent(true);
            s.setViewOrder(-3);
            layer.getChildren().add(s);
            double angle = Math.random() * 2 * Math.PI, dist = 40 + Math.random() * 70;
            Timeline tl = new Timeline(new KeyFrame(Duration.millis(400 + Math.random() * 300),
                    new KeyValue(s.translateXProperty(), Math.cos(angle) * dist, Interpolator.EASE_OUT),
                    new KeyValue(s.translateYProperty(), Math.sin(angle) * dist + 30, Interpolator.EASE_IN),
                    new KeyValue(s.opacityProperty(), 0)));
            tl.setOnFinished(e -> layer.getChildren().remove(s));
            tl.play();
        }
    }

    /** A Torii gate drops slowly from the sky onto a card; on landing, onLand runs (a small screen shake)
     *  and the gate rocks a few times, then settles at its own opacity. */
    public static void drop(Node g, Runnable onLand) {
        double end = g.getOpacity();
        g.setTranslateY(-700);
        g.setOpacity(0);
        g.setScaleX(1.15);
        g.setScaleY(1.15);
        new Timeline(
                new KeyFrame(Duration.millis(650), e -> onLand.run(), new KeyValue(g.translateYProperty(), 0, Interpolator.EASE_IN),
                        new KeyValue(g.opacityProperty(), end), new KeyValue(g.scaleXProperty(), 1), new KeyValue(g.scaleYProperty(), 1)),
                new KeyFrame(Duration.millis(710), new KeyValue(g.rotateProperty(), -5)),
                new KeyFrame(Duration.millis(770), new KeyValue(g.rotateProperty(), 4)),
                new KeyFrame(Duration.millis(830), new KeyValue(g.rotateProperty(), -2)),
                new KeyFrame(Duration.millis(890), new KeyValue(g.rotateProperty(), 0))).play();
    }

    /** A hanging banner, w wide: the top `rod` px of the image (the rod) stay still, the cloth below is cut in 10 px
     *  strips that sway side to side, more towards the bottom, like a light breeze. Stops when removed from the screen. */
    public static Group waving(Image img, double w, double rod) {
        double k = w / img.getWidth(), step = 10, phase = Math.random() * 10;
        Group g = new Group();
        for (double sy = 0; sy < img.getHeight(); sy += sy == 0 ? rod : step) {
            ImageView s = new ImageView(img);
            s.setViewport(new Rectangle2D(0, sy, img.getWidth(), Math.min((sy == 0 ? rod : step) + 1, img.getHeight() - sy)));
            s.setFitWidth(w);
            s.setPreserveRatio(true);
            s.setY(sy * k);
            g.getChildren().add(s);
        }
        AnimationTimer wave = new AnimationTimer() {
            @Override public void handle(long now) {
                for (Node s : g.getChildren()) {
                    double d = Math.max(0, (((ImageView) s).getY() / k - rod) / (img.getHeight() - rod));   // 0 under the rod, 1 at the bottom
                    s.setTranslateX(5 * d * Math.sin(now / 5e8 + phase - d * 4));
                }
            }
        };
        wave.start();
        g.sceneProperty().addListener((o, was, now) -> { if (now == null) wave.stop(); });
        return g;
    }
}