package fr.onishintan.ui.fx;

import fr.onishintan.ui.app.Images;

import java.util.ArrayList;
import java.util.List;

import javafx.animation.AnimationTimer;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;

/**
 * A hero's death, about 3 seconds: the killing blow shakes it red, then flames (hud/death-flame-*.png) rise over it
 * from the bottom; above them the portrait chars, behind them it crumbles into embers that cool into grey ash.
 * Then the fire dies down and `then` runs (the Victory / Defeat seal).
 */
public final class HeroBurn extends AnimationTimer {
    /** The shake, the burn and the whole effect, in ms; how long each flame frame shows. */
    private static final double SHAKE_MS = 400, BURN_MS = 2000, END_MS = 3200, FRAME_MS = 110;
    /** One ember per STEP x STEP pixels of the portrait; the charred band above the fire line. */
    private static final int STEP = 3;
    private static final double SCORCH_H = 70;
    /** Room around the hero for the embers to fly into. */
    private static final double SIDE = 100, ABOVE = 300;
    private static final Color EMBER = Color.rgb(255, 140, 30), ASH = Color.rgb(90, 85, 80);
    private static final Image[] FLAMES = {
            Images.get("/images/hud/death-flame-1.png"),
            Images.get("/images/hud/death-flame-2.png"),
            Images.get("/images/hud/death-flame-3.png") };

    /** One piece of the portrait: where it is, how it flies once the fire reaches it, its colour. */
    private static final class Ember {
        double x, y, vx, vy, born = -1, life, seed;
        Color colour;
    }

    private final Pane layer;
    private final Runnable then;
    private final double w, h;
    private final Pane piece = new Pane();          // the burning copy of the hero
    private final Rectangle shown = new Rectangle();   // the part of it the fire has not reached yet
    private final Rectangle scorch;
    private final Rectangle flash;
    private final Canvas embers;
    private final ImageView flame = new ImageView(), mirror = new ImageView();
    private final List<Ember> pieces = new ArrayList<>();
    private long start;

    /** Burns `hero` (a node on the board) into dust, then runs `then`. */
    public static void play(Node hero, Runnable then) {
        new HeroBurn(hero, then).start();
    }

    private HeroBurn(Node hero, Runnable then) {
        this.layer = (Pane) hero.getParent();
        this.then = then;
        Bounds box = hero.getBoundsInParent();
        w = box.getWidth();
        h = box.getHeight();
        SnapshotParameters clear = new SnapshotParameters();
        clear.setFill(Color.TRANSPARENT);
        WritableImage picture = hero.snapshot(clear, null);
        hero.setOpacity(0);   // its burning copy takes its place

        scorch = new Rectangle(w, SCORCH_H, new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(20, 10, 5, 0)), new Stop(0.7, Color.rgb(20, 10, 5, 0.85)),
                new Stop(0.92, Color.rgb(255, 120, 20, 0.95)), new Stop(1, Color.rgb(255, 220, 120))));
        flash = new Rectangle(w, h, Color.rgb(200, 20, 20));
        flash.setOpacity(0);
        shown.setWidth(w);
        shown.setHeight(h);
        piece.getChildren().addAll(new ImageView(picture), scorch, flash);
        piece.setClip(shown);
        piece.relocate(box.getMinX(), box.getMinY());

        embers = new Canvas(w + 2 * SIDE, h + ABOVE);
        embers.relocate(box.getMinX() - SIDE, box.getMinY() - ABOVE);
        PixelReader pixels = picture.getPixelReader();
        for (int py = 0; py < picture.getHeight(); py += STEP) {
            for (int px = 0; px < picture.getWidth(); px += STEP) {
                Color c = pixels.getColor(px, py);
                if (c.getOpacity() > 0.15) {
                    Ember e = new Ember();
                    e.x = px;
                    e.y = py;
                    e.colour = c;
                    e.life = 900 + Math.random() * 900;
                    e.seed = Math.random() * 6.28;
                    pieces.add(e);
                }
            }
        }

        flame.setFitWidth(w * 1.15);
        flame.setPreserveRatio(true);
        mirror.setFitWidth(w * 1.05);
        mirror.setPreserveRatio(true);
        mirror.setScaleX(-1);   // a mirrored second flame, so the fire looks fuller
        for (Node n : List.of(piece, embers, mirror, flame)) {
            n.setMouseTransparent(true);
        }
        layer.getChildren().addAll(piece, embers, mirror, flame);
    }

    /** One frame: t is the time since the start, the fire line goes from the bottom of the hero (h) to just above it. */
    @Override
    public void handle(long now) {
        if (start == 0) {
            start = now;
        }
        double t = (now - start) / 1e6;
        double line = h - (h + 40) * smooth((t - SHAKE_MS) / BURN_MS);
        blow(t);
        shown.setHeight(Math.max(0, line));
        scorch.setY(line - SCORCH_H);
        flames(t, line);
        embers(t, line);
        if (t >= END_MS) {
            stop();
            layer.getChildren().removeAll(piece, embers, mirror, flame);
            then.run();
        }
    }

    /** The killing blow: a shake that dies out and a red flash. */
    private void blow(double t) {
        piece.setTranslateX(0);
        flash.setOpacity(0);
        if (t < SHAKE_MS) {
            piece.setTranslateX(Math.sin(t / 18) * 6 * (1 - t / SHAKE_MS));
            flash.setOpacity(0.45 * Math.sin(t / SHAKE_MS * Math.PI));
        }
    }

    /** The flame frames flicker at the fire line: they grow in, burn, then die down at the end. */
    private void flames(double t, double line) {
        double strength = Math.min(1, Math.max(0, (t - SHAKE_MS) / 300));
        if (t > SHAKE_MS + BURN_MS) {
            strength = Math.max(0, 1 - (t - SHAKE_MS - BURN_MS) / (END_MS - SHAKE_MS - BURN_MS));
        }
        int frame = (int) (t / FRAME_MS);
        flame.setImage(FLAMES[frame % FLAMES.length]);
        mirror.setImage(FLAMES[(frame + 1) % FLAMES.length]);
        flame.setOpacity(strength);
        mirror.setOpacity(strength * 0.75);
        double fh = flame.getFitWidth() * flame.getImage().getHeight() / flame.getImage().getWidth();
        double top = piece.getLayoutY() + line - fh * 0.72;
        flame.relocate(piece.getLayoutX() + (w - flame.getFitWidth()) / 2 + Math.sin(t / 90) * 6, top);
        mirror.relocate(piece.getLayoutX() + (w - mirror.getFitWidth()) / 2, top + 10);
    }

    /** Every piece behind the fire line flies up as an ember, cools into grey ash and fades. */
    private void embers(double t, double line) {
        GraphicsContext g = embers.getGraphicsContext2D();
        g.clearRect(0, 0, embers.getWidth(), embers.getHeight());
        for (Ember e : pieces) {
            if (e.born < 0) {
                if (t < SHAKE_MS || e.y < line) {
                    continue;   // the fire has not reached it yet
                }
                e.born = t;
                e.vx = (Math.random() - 0.5) * 0.6;
                e.vy = -(0.4 + Math.random() * 1.2);
            }
            double age = (t - e.born) / e.life;
            if (age >= 1) {
                continue;   // gone
            }
            e.x += e.vx + Math.sin(t / 300 + e.seed) * 0.5;
            e.y += e.vy;
            e.vy -= 0.01;
            Color c = ASH;
            if (age < 0.25) {
                c = e.colour.interpolate(EMBER, age / 0.25);   // glowing first
            } else {
                c = EMBER.interpolate(ASH, (age - 0.25) / 0.75);   // then cooling into ash
            }
            g.setFill(Color.color(c.getRed(), c.getGreen(), c.getBlue(), 1 - age));
            g.fillRect(SIDE + e.x, ABOVE + e.y, STEP, STEP);
        }
    }

    /** 0 to 1 with a soft start and end (0 before, 1 after). */
    private static double smooth(double v) {
        double k = Math.max(0, Math.min(1, v));
        return k * k * (3 - 2 * k);
    }
}
