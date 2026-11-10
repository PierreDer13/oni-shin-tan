package fr.onishintan.ui.match;
import fr.onishintan.ui.fx.Fx;

import fr.onishintan.model.Combatant;
import fr.onishintan.model.Keyword;
import fr.onishintan.model.Minion;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.transform.Scale;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

/** Keyword effects drawn over board cards: Shinobi ink clouds, and the Torii gate (drops in once, is cut when it breaks). */
final class StatusOverlay {
    private final Pane live;
    private final Runnable shake;
    private final Set<List<Object>> shown = new HashSet<>();          // (image, minion) pairs already dropped in
    private final Map<List<Object>, Node> gates = new HashMap<>();
    private Group on;   // the card the effects being added belong to

    /** live: the board layer to draw on; shake: the small screen shake played when a gate lands. */
    StatusOverlay(Pane live, Runnable shake) {
        this.live = live;
        this.shake = shake;
    }

    /** Call at every redraw: forgets gates that are gone and the old images. */
    void clear() {
        shown.removeIf(k -> !keeps((String) k.get(0), (Minion) k.get(1)));
        gates.clear();
    }


    /** The effects added next ride with this card: their group follows its position and layer, so they lunge with it when it attacks. */
    void follow(Node card) {
        on = new Group();
        on.translateXProperty().bind(card.translateXProperty());
        on.translateYProperty().bind(card.translateYProperty());
        on.viewOrderProperty().bind(card.viewOrderProperty());
        live.getChildren().add(on);
    }

    /** Shinobi: two ink clouds drift slowly in opposite directions over a hidden card, on top of its darkening. */
    void shinobi(double x, double y) {
        for (int k = 0; k < 2; k++) {
            ImageView c = BoardView.image("/images/fx/shinobi-cloud-" + (1 + (int) (Math.random() * 5)) + ".png");
            c.setFitWidth(100);
            c.setPreserveRatio(true);
            c.setOpacity(0.9);
            add(c, x + (k == 0 ? -10 : 20), y + (k == 0 ? 20 : 95));
            sway(c, k == 0 ? 1 : -1, 10);
        }
    }

    /** Seppuku: a kneeling samurai, still, at the bottom middle of the card between its ATK and HP. */
    void seppuku(double x, double y) {
        ImageView s = BoardView.image("/images/fx/seppuku.png");
        s.setFitWidth(26);
        s.setPreserveRatio(true);
        s.setOpacity(0.8);
        add(s, x + BoardView.MINION_W / 2 - 13, y + 128);
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
    void flag(Minion m, double x, double y) {
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
        drop("flag", m, g);
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

    /** A gate over a card (torii across the middle, shirodu wall at the top): drops in and shakes the first time, then stays still. */
    void gate(String image, Minion m, double x, double y) {
        boolean torii = image.equals("torii");
        ImageView g = BoardView.image("/images/fx/" + image + ".png");
        g.setFitWidth(torii ? 190 : 150);
        g.setPreserveRatio(true);
        g.setOpacity(0.8);
        add(g, x + BoardView.MINION_W / 2 - g.getFitWidth() / 2, y + (torii ? 15 : -30));
        drop(image, m, g);
    }

    /** Remembers an effect on a minion; the first time it appears it drops from the sky with a small shake. */
    private void drop(String image, Minion m, Node g) {
        List<Object> key = List.of(image, m);
        gates.put(key, g);
        if (shown.add(key)) Fx.drop(g, shake);
    }

    /** Whether a minion still has what this gate shows. */
    private static boolean keeps(String image, Minion m) {
        return !m.isDead() && switch (image) {
            case "torii" -> m.hasTorii();
            case "shirodu" -> m.hasKeyword(Keyword.SHIRODU);
            default -> m.hasKeyword(Keyword.PASSIVE);
        };
    }

    /** After a hit: if a character has just lost its Torii (it blocked the hit), its gate is cut in half and vanishes. */
    void broken(Combatant c) {
        if (!(c instanceof Minion m)) return;
        for (String image : List.of("torii", "shirodu", "flag"))
            if (!keeps(image, m) && shown.remove(List.of(image, m))) Fx.deathCut(live, gates.get(List.of(image, m)));
    }

    /** Adds an effect image at (x, y) on the board; effects never catch the mouse. */
    private void add(Node n, double x, double y) {
        n.setMouseTransparent(true);
        n.relocate(x, y);
        if (on != null) on.getChildren().add(n); else live.getChildren().add(n);
    }
}
