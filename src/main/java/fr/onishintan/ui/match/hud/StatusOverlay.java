package fr.onishintan.ui.match.hud;
import fr.onishintan.ui.fx.Fx;

import fr.onishintan.model.Combatant;
import fr.onishintan.model.Keyword;
import fr.onishintan.model.Minion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.ColorAdjust;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import fr.onishintan.ui.match.BoardView;

/** Keyword and status effects drawn over board cards: Shinobi clouds, Torii and Shirodu gates, Passive flag, Seppuku, Tatakae seal, Banzai shout, Sleep zzz. */
public final class StatusOverlay {
    private final Pane live;
    private final Runnable shake;
    private final Set<List<Object>> shown = new HashSet<>();          // (image, minion) pairs already dropped in
    private Set<List<Object>> before = new HashSet<>();   // (status, minion or hero) pairs shown at the last redraw
    private Set<List<Object>> now = new HashSet<>();      // and at this one
    private static final Duration DIM = Duration.millis(500);   // a Shinobi card darkening as it hides
    private final Map<List<Object>, Node> gates = new HashMap<>();
    private Group on;   // the card the effects being added belong to
    /** The Torii column: gap between two gates, and the most room the column may take below the first gate. */
    private static final double TORII_STEP = 30, TORII_ROOM = 90;
    /** How wide a minion's armour plate is drawn. */
    private static final double ARMOUR_W = 42;

    /** live: the board layer to draw on; shake: the small screen shake played when a gate lands. */
    public StatusOverlay(Pane live, Runnable shake) {
        this.live = live;
        this.shake = shake;
    }

    /** Call at every redraw: forgets gates that are gone and the old images. */
    public void clear() {
        shown.removeIf(key -> !keeps(key));
        before = now;
        now = new HashSet<>();
        gates.clear();
    }

    /** Notes that `who` shows `status` at this redraw; true if it did not at the last one (it just got it). */
    private boolean isNew(String status, Object who) {
        List<Object> key = List.of(status, who);
        now.add(key);
        return !before.contains(key);
    }


    /** The effects added next ride with this card: their group follows its position and layer, so they lunge with it when it attacks. */
    public void follow(Node card) {
        on = new Group();
        on.translateXProperty().bind(card.translateXProperty());
        on.translateYProperty().bind(card.translateYProperty());
        on.viewOrderProperty().bind(card.viewOrderProperty());
        live.getChildren().add(on);
    }

    /** Shinobi: the hidden card darkens and two ink clouds drift over it in opposite directions; the first time, both come in over 0.5 s. */
    public void shinobi(Node card, Minion m, double x, double y) {
        boolean fresh = isNew("shinobi", m);
        if (card.getEffect() == null) {   // a glow (it can attack, it is a target) wins over the darkening
            ColorAdjust dark = new ColorAdjust(0, -0.4, -0.45, 0);
            card.setEffect(dark);
            if (fresh) {
                new Timeline(
                        new KeyFrame(Duration.ZERO, new KeyValue(dark.saturationProperty(), 0), new KeyValue(dark.brightnessProperty(), 0)),
                        new KeyFrame(DIM, new KeyValue(dark.saturationProperty(), -0.4), new KeyValue(dark.brightnessProperty(), -0.45))).play();
            }
        }
        for (int k = 0; k < 2; k++) {
            ImageView c = BoardView.image("/images/fx/shinobi-cloud-" + (1 + (int) (Math.random() * 5)) + ".png");
            c.setFitWidth(100);
            c.setPreserveRatio(true);
            c.setOpacity(0.9);
            add(c, x + (k == 0 ? -10 : 20), y + (k == 0 ? 20 : 95));
            sway(c, k == 0 ? 1 : -1, 10);
            if (fresh) {
                FadeTransition in = new FadeTransition(DIM, c);
                in.setFromValue(0);
                in.setToValue(0.9);
                in.play();
            }
        }
    }

    /** Armour on a minion (Yoroi): the hero's armour plate with its number, just above the HP. It drops in the first time; later only the number changes. */
    public void armour(Minion m, double x, double y) {
        ImageView plate = BoardView.image("/images/hud/armour.png");
        plate.setFitWidth(ARMOUR_W);
        plate.setPreserveRatio(true);
        Label value = new Label(String.valueOf(m.getArmour()));
        value.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #1f1b17;");
        StackPane badge = new StackPane(plate, value);
        add(badge, x + BoardView.MINION_W - ARMOUR_W - 2, y + 104);
        drop(List.of("armour", m), badge);
    }

    /** Seppuku: a kneeling samurai, still, at the bottom middle of the card between its ATK and HP. */
    public void seppuku(double x, double y) {
        ImageView s = BoardView.image("/images/fx/seppuku.png");
        s.setFitWidth(26);
        s.setPreserveRatio(true);
        s.setOpacity(0.8);
        add(s, x + BoardView.MINION_W / 2 - 13, y + 128);
    }

    /** Banzai: the first time the minion is drawn (just played), a gold 万歳! bursts out of it, grows and rises away, with sparks. */
    public void banzai(Minion m, double x, double y) {
        List<Object> key = List.of("banzai", m);
        if (!shown.add(key)) {
            return;   // already shouted when it was played
        }
        Label shout = new Label("万歳!");
        shout.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: 34; -fx-font-weight: bold; -fx-text-fill: #ffd84a;");
        shout.setEffect(new DropShadow(14, Color.web("#ff8a1f")));
        add(shout, x + 4, y + 55);
        Fx.sparks(live, shout);

        // Grows from small to big while it rises, then fades away.
        ScaleTransition grow = new ScaleTransition(Duration.millis(1200), shout);
        grow.setFromX(0.4);
        grow.setFromY(0.4);
        grow.setToX(1.3);
        grow.setToY(1.3);
        TranslateTransition rise = new TranslateTransition(Duration.millis(1200), shout);
        rise.setByY(-70);
        FadeTransition fade = new FadeTransition(Duration.millis(500), shout);
        fade.setDelay(Duration.millis(700));
        fade.setToValue(0);
        new ParallelTransition(grow, rise, fade).play();
    }

    /** Asleep (Status.SLEEP): z, z, Z float up from the top-right corner of the card or hero at (x, y) and fade, over and over. */
    public void sleep(Object who, double x, double y) {
        if (isNew("sleep", who)) {   // just fell asleep: the dart hits first
            dart(x, y);
        }
        String[] letters = { "z", "z", "Z" };
        int[] sizes = { 16, 20, 26 };
        for (int i = 0; i < letters.length; i++) {
            Label z = new Label(letters[i]);
            z.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: " + sizes[i] + "; -fx-font-weight: bold; -fx-text-fill: #f3ede1;");
            z.setEffect(new DropShadow(6, Color.web("#1c2b4f")));
            add(z, x + BoardView.MINION_W - 34 + i * 9, y + 6);

            // Each letter rises and fades, then starts again; the three start one after the other.
            TranslateTransition up = new TranslateTransition(Duration.seconds(1.8), z);
            up.setFromY(0);
            up.setToY(-40);
            FadeTransition fade = new FadeTransition(Duration.seconds(1.8), z);
            fade.setFromValue(1);
            fade.setToValue(0);
            ParallelTransition loop = new ParallelTransition(up, fade);
            loop.setCycleCount(Animation.INDEFINITE);
            loop.setDelay(Duration.seconds(i * 0.6));
            loop.play();
            z.sceneProperty().addListener((o, was, now) -> { if (now == null) loop.stop(); });
        }
    }


    /** A sleep dart flies in from the top-right, sticks in the corner of the card at (x, y), then fades away. */
    private void dart(double x, double y) {
        ImageView d = BoardView.image("/images/fx/sleep-dart.png");   // the tip is at the bottom-left
        d.setFitWidth(90);
        d.setPreserveRatio(true);
        add(d, x + BoardView.MINION_W - 40, y - 40);
        TranslateTransition fly = new TranslateTransition(Duration.millis(250), d);
        fly.setFromX(120);
        fly.setFromY(-80);
        fly.setToX(0);
        fly.setToY(0);
        FadeTransition fade = new FadeTransition(Duration.millis(400), d);
        fade.setDelay(Duration.millis(800));
        fade.setToValue(0);
        fly.setOnFinished(e -> fade.play());
        fly.play();
    }

    /** Drifts a node `dist` px left and right forever, 3 to 5 s each way, from a random point; stops once it leaves the screen. */
    private static void sway(Node n, double dir, double dist) {
        TranslateTransition t = new TranslateTransition(Duration.seconds(3 + Math.random() * 2), n);
        t.setFromX(-dist * dir);
        t.setToX(dist * dir);
        t.setAutoReverse(true);
        t.setCycleCount(Animation.INDEFINITE);
        t.setInterpolator(Interpolator.EASE_BOTH);
        t.playFrom(Duration.seconds(Math.random() * 3));
        n.sceneProperty().addListener((o, was, now) -> { if (now == null) t.stop(); });
    }

    /** Passive: a war banner planted over the card's top-left corner; the cloth ripples from the pole to the tip. */
    public void flag(Minion m, double x, double y) {
        Image img = BoardView.image("/images/fx/flag.png").getImage();   // 400 px wide, cloth starts at x = 125
        Group g = new Group();
        for (int sx = 0; sx < 400; sx += sx == 0 ? 125 : 11) {          // the pole in one piece, then 11 px strips of cloth
            ImageView s = new ImageView(img);
            s.setViewport(new Rectangle2D(sx, 0, sx == 0 ? 125 : 11, img.getHeight()));
            s.setX(sx);
            g.getChildren().add(s);
        }
        g.getTransforms().add(new Scale(0.2, 0.2));  // 400 px -> 80 px on the board
        g.setOpacity(0.8);
        add(g, x - 10, y - 45);
        drop(List.of("flag", m), g);
        AnimationTimer wave = new AnimationTimer() {
            @Override public void handle(long now) {
                for (Node s : g.getChildren()) {
                    double d = Math.max(0, (((ImageView) s).getX() - 125) / 275);   // 0 at the pole, 1 at the tip
                    s.setTranslateY(20 * d * Math.sin(now / 3e8 - d * 5));
                }
            }
        };
        wave.start();
        g.sceneProperty().addListener((o, was, now) -> { if (now == null) wave.stop(); });
    }

    /** A gate over a card: the Torii gates in a column (one per stack), or the shirodu wall at the top. Each drops in and shakes the first time. */
    public void gate(String image, Minion m, double x, double y) {
        if (image.equals("torii")) {
            toriiColumn(m, x, y);
            return;
        }
        ImageView g = BoardView.image("/images/fx/" + image + ".png");
        g.setFitWidth(150);
        g.setPreserveRatio(true);
        g.setOpacity(0.8);
        add(g, x + BoardView.MINION_W / 2 - g.getFitWidth() / 2, y - 30);
        drop(List.of(image, m), g);
    }

    /**
     * One Torii gate per stack, in a column going down the card: the first across the middle, each next one TORII_STEP lower.
     * With many stacks they move closer together, so the column never goes further than TORII_ROOM below the first gate.
     */
    private void toriiColumn(Minion m, double x, double y) {
        int stacks = m.getToriiStacks();
        double step = TORII_STEP;
        if (stacks > 1) {
            step = Math.min(TORII_STEP, TORII_ROOM / (stacks - 1));
        }
        for (int stack = 0; stack < stacks; stack++) {
            ImageView g = BoardView.image("/images/fx/torii.png");
            g.setFitWidth(190);
            g.setPreserveRatio(true);
            g.setOpacity(0.8);
            add(g, x + BoardView.MINION_W / 2 - 95, y + 15 + stack * step);
            drop(List.of("torii", m, stack), g);
        }
    }

    /** Remembers an effect (its key: image, minion, and the stack number for a Torii); the first time it appears it drops from the sky with a small shake. */
    private void drop(List<Object> key, Node g) {
        gates.put(key, g);
        if (shown.add(key)) {
            Fx.drop(g, shake);
        }
    }

    /** Whether a minion still has what this gate shows. A Torii key carries its stack number: stack i stays while the minion has more than i stacks. */
    private static boolean keeps(List<Object> key) {
        String image = (String) key.get(0);
        Minion m = (Minion) key.get(1);
        if (m.isDead()) {
            return false;
        }
        if (image.equals("torii")) {
            int stack = (Integer) key.get(2);
            return m.getToriiStacks() > stack;
        }
        if (image.equals("shirodu")) {
            return m.hasKeyword(Keyword.SHIRODU);
        }
        if (image.equals("armour")) {
            return m.getArmour() > 0;   // cut once the armour is gone
        }
        if (image.equals("banzai")) {
            return true;   // remembered while the minion lives, so its shout plays only once
        }
        return m.hasKeyword(Keyword.PASSIVE);
    }

    /** After a hit: every gate of this character that it no longer has (a Torii stack that blocked the hit) is cut in half and vanishes. */
    public void broken(Combatant c) {
        if (!(c instanceof Minion m)) {
            return;
        }
        for (List<Object> key : new ArrayList<>(gates.keySet())) {
            boolean thisMinion = key.get(1) == m;
            if (thisMinion && !keeps(key) && shown.remove(key)) {
                Fx.deathCut(live, gates.get(key));
            }
        }
    }

    /** Adds an effect image at (x, y) on the board; effects never catch the mouse. */
    private void add(Node n, double x, double y) {
        n.setMouseTransparent(true);
        n.relocate(x, y);
        if (on != null) on.getChildren().add(n); else live.getChildren().add(n);
    }
}
