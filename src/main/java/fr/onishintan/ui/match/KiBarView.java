package fr.onishintan.ui.match;

import fr.onishintan.ui.app.Images;
import fr.onishintan.model.Player;

import java.util.ArrayList;
import java.util.List;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

/** Your Ki as 12 flames stacked from the bottom (Hearthstone style), on the board's 1600 x 900 grid.
 *  Lit = available (each flickers on its base), dim ember = spent this turn, faint = locked.
 *  A flame spent since the last redraw shrinks into its base, fades and lets out a little smoke. */
public class KiBarView extends Pane {
    public static final double X = 84, BOTTOM = 701, SIZE = 40, STEP = 38;
    private static final Image FLAME = Images.get("/images/hud/ki-flame.png");
    private static final double FH = SIZE * FLAME.getHeight() / FLAME.getWidth();   // a flame's height on screen

    private final List<ImageView> lit = new ArrayList<>();
    private Timeline pulse = new Timeline();

    /** before = the Ki this player had at the last redraw: the flames between before and now are put out. */
    public KiBarView(Player p, int before) {
        for (int i = 0; i < Player.KI_CAP; i++) {
            double y = BOTTOM - (i + 1) * STEP;
            if (i < p.getKi()) lit.add(burning(y));
            else flame(y, i < p.getMaxKi() ? 0.45 : 0.12).setEffect(new ColorAdjust(0, -0.8, -0.4, 0));
            if (i >= p.getKi() && i < before) putOut(y);
        }
        text("気 KI", 22, 205);
        text(p.getKi() + " / " + Player.KI_CAP, 20, 712);
        setMouseTransparent(true);
        sceneProperty().addListener((o, was, now) -> { if (now == null) pulse.stop(); });
    }

    /** Pulses the top lit flames that a hovered card or power would cost (0 = none). */
    public void preview(int cost) {
        pulse.stop();
        lit.forEach(f -> f.setOpacity(1));
        int n = Math.min(cost, lit.size());
        if (n == 0) return;
        pulse = new Timeline(new KeyFrame(Duration.millis(700),
                lit.subList(lit.size() - n, lit.size()).stream().map(f -> new KeyValue(f.opacityProperty(), 0.35)).toArray(KeyValue[]::new)));
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();
    }

    /** One flame image at height y, with the given opacity. */
    private ImageView flame(double y, double opacity) {
        ImageView f = new ImageView(FLAME);
        f.setFitWidth(SIZE);
        f.setPreserveRatio(true);
        f.setOpacity(opacity);
        f.relocate(X, y);
        getChildren().add(f);
        return f;
    }

    /** A lit flame that flickers on its base forever: stretches up, narrows and leans, at its own random pace. */
    private ImageView burning(double y) {
        ImageView f = flame(y, 1);
        f.setEffect(new DropShadow(10, Color.HOTPINK));
        Scale s = new Scale(1, 1, SIZE / 2, FH);   // pivot on the flame's base, so it never lifts off
        Rotate r = new Rotate(0, SIZE / 2, FH);
        f.getTransforms().addAll(s, r);
        Timeline t = new Timeline(new KeyFrame(Duration.millis(250 + Math.random() * 250),
                new KeyValue(s.yProperty(), 1.12, Interpolator.EASE_BOTH),
                new KeyValue(s.xProperty(), 0.93, Interpolator.EASE_BOTH),
                new KeyValue(r.angleProperty(), Math.random() * 8 - 4, Interpolator.EASE_BOTH)));
        t.setAutoReverse(true);
        t.setCycleCount(Animation.INDEFINITE);
        t.playFrom(Duration.millis(Math.random() * 500));
        f.sceneProperty().addListener((o, was, now) -> { if (now == null) t.stop(); });
        return f;
    }

    /** A flame spent a moment ago: it shrinks down into its base and fades, and a little grey smoke rises. */
    private void putOut(double y) {
        ImageView f = flame(y, 1);
        Scale s = new Scale(1, 1, SIZE / 2, FH);
        f.getTransforms().add(s);
        new Timeline(new KeyFrame(Duration.millis(400), new KeyValue(s.yProperty(), 0, Interpolator.EASE_IN),
                new KeyValue(s.xProperty(), 0.5), new KeyValue(f.opacityProperty(), 0))).play();
        for (int k = 0; k < 3; k++) {
            Circle c = new Circle(X + SIZE / 2 + Math.random() * 10 - 5, y + FH * 0.7, 2 + Math.random() * 2, Color.rgb(120, 110, 115, 0.7));
            c.setEffect(new GaussianBlur(2));
            getChildren().add(c);
            new Timeline(new KeyFrame(Duration.millis(700 + Math.random() * 300),
                    new KeyValue(c.translateYProperty(), -30 - Math.random() * 20),
                    new KeyValue(c.translateXProperty(), Math.random() * 12 - 6),
                    new KeyValue(c.opacityProperty(), 0))).play();
        }
    }

    /** Cream text with a soft pink glow, centred over the Ki zone, at height y. */
    private void text(String s, double size, double y) {
        Label l = new Label(s);
        l.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: " + size + "; -fx-font-weight: bold;"
                + " -fx-text-fill: #ffe6f2; -fx-alignment: center; -fx-text-alignment: center;");
        l.setPrefWidth(113);
        l.setEffect(new DropShadow(10, Color.HOTPINK));
        l.relocate(48, y);
        getChildren().add(l);
    }
}