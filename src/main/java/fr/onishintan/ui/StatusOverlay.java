package fr.onishintan.ui;

import fr.onishintan.model.Combatant;
import fr.onishintan.model.Keyword;
import fr.onishintan.model.Minion;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

/** Keyword effects drawn over board cards: Shinobi ink clouds, and the Torii gate (drops in once, is cut when it breaks). */
final class StatusOverlay {
    private final Pane live;
    private final Runnable shake;
    private final Set<Minion> toriiShown = new HashSet<>();
    private final Map<Minion, ImageView> toriiNodes = new HashMap<>();
    private final Set<List<Object>> shown = new HashSet<>();          // (image, minion) pairs already dropped in
    private final Map<List<Object>, ImageView> gates = new HashMap<>();

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

    /** Shinobi: two ink clouds drift slowly in opposite directions over a hidden card, on top of its darkening. */
    void shinobi(double x, double y) {
        for (int k = 0; k < 2; k++) {
            ImageView c = BoardView.image("/images/fx/shinobi-cloud-" + (1 + (int) (Math.random() * 5)) + ".png");
            c.setFitWidth(100);
            c.setPreserveRatio(true);
            c.setOpacity(0.9);
            add(c, x + (k == 0 ? -10 : 20), y + (k == 0 ? 20 : 95));
            double dir = k == 0 ? 1 : -1;
            TranslateTransition t = new TranslateTransition(Duration.seconds(3 + Math.random() * 2), c);
            t.setFromX(-10 * dir);
            t.setToX(10 * dir);
            t.setAutoReverse(true);
            t.setCycleCount(Animation.INDEFINITE);
            t.setInterpolator(Interpolator.EASE_BOTH);
            t.playFrom(Duration.seconds(Math.random() * 3));
            c.sceneProperty().addListener((o, was, now) -> { if (now == null) t.stop(); });
        }
    }

    /** A gate over a card (torii across the middle, shirodu wall at the top): drops in and shakes the first time, then stays still. */
    void gate(String image, Minion m, double x, double y) {
        boolean torii = image.equals("torii");
        ImageView g = BoardView.image("/images/fx/" + image + ".png");
        g.setFitWidth(torii ? 190 : 150);
        g.setPreserveRatio(true);
        g.setOpacity(0.8);
        add(g, x + BoardView.MINION_W / 2 - g.getFitWidth() / 2, y + (torii ? 15 : -30));
        List<Object> key = List.of(image, m);
        gates.put(key, g);
        if (shown.add(key)) Fx.drop(g, shake);
    }

    /** Whether a minion still has what this gate shows. */
    private static boolean keeps(String image, Minion m) {
        return !m.isDead() && (image.equals("torii") ? m.hasTorii() : m.hasKeyword(Keyword.SHIRODU));
    }

    /** After a hit: if a character has just lost its Torii (it blocked the hit), its gate is cut in half and vanishes. */
    void broken(Combatant c) {
        if (!(c instanceof Minion m)) return;
        for (String image : List.of("torii", "shirodu"))
            if (!keeps(image, m) && shown.remove(List.of(image, m))) Fx.deathCut(live, gates.get(List.of(image, m)));
    }

    /** Adds an effect image at (x, y) on the board; effects never catch the mouse. */
    private void add(Node n, double x, double y) {
        n.setMouseTransparent(true);
        n.relocate(x, y);
        live.getChildren().add(n);
    }
}
