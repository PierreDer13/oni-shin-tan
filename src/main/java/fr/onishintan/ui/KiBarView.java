package fr.onishintan.ui;

import fr.onishintan.model.Player;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/** Your Ki: a glowing rounded capsule filling from the bottom, and pink spirit motes rising beside the filled part.
 *  Bright = available, pale = spent this turn, empty = locked. Drawn on the board's 1600 x 900 grid. */
public class KiBarView extends Pane {
    public static final double X = 89, TOP = 285, W = 30, H = 420, STEP = H / Player.KI_CAP;

    private final Player p;
    private final Rectangle shade = new Rectangle(X, 0, W, 0);
    private final FadeTransition pulse = new FadeTransition(Duration.millis(700), shade);

    public KiBarView(Player p) {
        this.p = p;
        double kiH = p.getKi() * STEP;
        Rectangle track = capsule(X, TOP, W, H, Color.rgb(20, 10, 15, 0.35));
        track.setStroke(Color.HOTPINK.deriveColor(0, 1, 1, 0.5));
        track.setStrokeWidth(1.5);
        track.setEffect(new DropShadow(6, Color.HOTPINK.deriveColor(0, 1, 1, 0.4)));
        Rectangle unlocked = capsule(X, TOP + H - p.getMaxKi() * STEP, W, p.getMaxKi() * STEP, Color.HOTPINK.deriveColor(0, 1, 1, 0.22));
        Rectangle ki = capsule(X, TOP + H - kiH, W, kiH, new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#ff4fa3")), new Stop(0.5, Color.web("#ffb3d6")), new Stop(1, Color.web("#ff4fa3"))));
        DropShadow halo = new DropShadow(10, Color.HOTPINK);
        halo.setInput(new Glow(0.2));
        ki.setEffect(halo);
        shade.setFill(Color.rgb(45, 15, 30, 0.6));
        shade.setClip(capsule(X, TOP + H - kiH, W, kiH, Color.BLACK));   // only inside the glowing Ki: rounded top, flat cut below
        shade.setOpacity(0);
        pulse.setFromValue(0.9);
        pulse.setToValue(0.35);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        getChildren().addAll(track, unlocked, ki, shade);
        motes(TOP + H, kiH, p.getKi());
        text("気 KI", 22, 240);
        text(p.getKi() + " / " + Player.KI_CAP, 20, 715);
        setMouseTransparent(true);
        sceneProperty().addListener((o, was, now) -> { if (now == null) pulse.stop(); });
    }

    /** Dims, with a slow pulse, the part of your available Ki that a hovered card or power would cost (0 = none). */
    public void preview(int cost) {
        int c = Math.min(cost, p.getKi());
        pulse.stop();
        shade.setOpacity(0);
        if (c == 0) return;
        shade.setY(TOP + H - p.getKi() * STEP);
        shade.setHeight(c * STEP);
        pulse.playFromStart();
    }

    /** A rounded capsule: both ends fully round. */
    private static Rectangle capsule(double x, double y, double w, double h, Paint fill) {
        Rectangle r = new Rectangle(x, y, w, h);
        r.setArcWidth(w);
        r.setArcHeight(w);
        r.setFill(fill);
        return r;
    }

    /** Pink spirit motes rising and fading on both sides of the filled part of the bar. */
    private void motes(double bottom, double height, int count) {
        for (int i = 0; i < count; i++) {
            Circle c = new Circle(1.5 + Math.random() * 2, Color.web("#ff8ac4"));
            c.setEffect(new DropShadow(6, Color.HOTPINK));
            c.setCenterX(Math.random() < 0.5 ? X - 6 - Math.random() * 12 : X + W + 6 + Math.random() * 12);
            c.setCenterY(bottom - Math.random() * height);
            TranslateTransition rise = new TranslateTransition(Duration.seconds(2 + Math.random() * 2), c);
            rise.setFromY(0);
            rise.setToY(-40 - Math.random() * 40);
            rise.setFromX(0);
            rise.setToX(Math.random() * 10 - 5);
            FadeTransition fade = new FadeTransition(rise.getDuration(), c);
            fade.setFromValue(0.9);
            fade.setToValue(0);
            ParallelTransition t = new ParallelTransition(rise, fade);
            t.setCycleCount(Animation.INDEFINITE);
            t.playFrom(Duration.seconds(Math.random() * 3));
            c.sceneProperty().addListener((o, was, now) -> { if (now == null) t.stop(); });
            getChildren().add(c);
        }
    }

    /** Cream text with a soft pink glow, centred over the bar's zone, at height y. */
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
