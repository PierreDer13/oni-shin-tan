package fr.onishintan.ui.match;

import fr.onishintan.model.Card;
import fr.onishintan.model.MinionCard;
import fr.onishintan.ui.app.Sound;
import fr.onishintan.ui.cards.CardView;
import fr.onishintan.ui.cards.MinionView;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

import static fr.onishintan.ui.match.BoardView.DROP_MS;
import static fr.onishintan.ui.match.BoardView.FLOAT_MS;
import static fr.onishintan.ui.match.BoardView.MINION_W;
import static fr.onishintan.ui.match.BoardView.YOUR_Y;

/** The board's table effects: arrival smoke and petals, ground shadows, the float and drop of a played minion, screen shake. */
class TableFx {

    /** The arrival smoke's size: wide and tall enough to hide a minion while it appears. */
    private static final double SMOKE_W = 320, SMOKE_H = 220;
    /** The heavy thud of a big minion landing (sounds/heavy-minion.wav). */
    private static final AudioClip HEAVY_THUD = Sound.clip("heavy-minion");
    private static final int HEAVY_KI = 6;

    private final Pane table;
    private final Pane live;

    TableFx(Pane table, Pane live) {
        this.table = table;
        this.live = live;
    }

    /** Smoke rises over the slot at x and fades: black, or white (the same image brightened). On the table: the redraw clears `live`. */
    void smoke(double x, boolean white) {
        ImageView smoke = BoardView.image("/images/fx/black-smoke.png");   // 600 x 198
        smoke.setFitWidth(SMOKE_W);
        smoke.setFitHeight(SMOKE_H);   // stretched taller than the picture, to hide the whole minion
        smoke.setMouseTransparent(true);
        if (white) {
            smoke.setEffect(new ColorAdjust(0, 0, 1, 0));
        }
        smoke.relocate(x + MINION_W / 2 - SMOKE_W / 2, YOUR_Y - 20);
        smoke.setOpacity(0);
        smoke.setId("arrival-smoke");   // render() keeps it in front, so the minion appears from behind it
        table.getChildren().add(smoke);
        Timeline puff = new Timeline(
                new KeyFrame(Duration.millis(400), new KeyValue(smoke.opacityProperty(), 1)),   // thick first, hiding the slot
                new KeyFrame(Duration.millis(1200), new KeyValue(smoke.opacityProperty(), 1), new KeyValue(smoke.translateYProperty(), -20)),
                new KeyFrame(Duration.millis(2600), new KeyValue(smoke.opacityProperty(), 0), new KeyValue(smoke.translateYProperty(), -70)));   // then it clears slowly
        puff.setOnFinished(e -> table.getChildren().remove(smoke));
        puff.play();
    }

    /** The Miko arrives: sakura petals drift down over the slot at x, spinning and swaying, then fade. */
    void petals(double x) {
        for (int i = 0; i < 16; i++) {
            Ellipse petal = new Ellipse(6, 3.5);
            petal.setFill(Color.web("#f6c1d0"));
            petal.setMouseTransparent(true);
            petal.relocate(x + Math.random() * MINION_W, YOUR_Y - 40 - Math.random() * 60);
            table.getChildren().add(petal);
            Duration time = Duration.millis(2600 + Math.random() * 1200);   // a slow fall
            TranslateTransition fall = new TranslateTransition(time, petal);
            fall.setByX(-30 + Math.random() * 60);
            fall.setByY(180);
            RotateTransition spin = new RotateTransition(time, petal);
            spin.setByAngle(360);
            FadeTransition fade = new FadeTransition(time, petal);
            fade.setFromValue(1);
            fade.setToValue(0);
            fade.setOnFinished(e -> table.getChildren().remove(petal));
            fall.play();
            spin.play();
            fade.play();
        }
    }

    /** A soft dark patch under a card so it looks like it lies on the table. */
    void groundShadow(double x, double y) {
        Rectangle r = new Rectangle(x - 4, y - 1, MINION_W + 8, MINION_W * CardView.H / CardView.W + 9);
        r.setArcWidth(16);
        r.setArcHeight(16);
        r.setFill(Color.rgb(0, 0, 0, 0.45));
        r.setEffect(new GaussianBlur(10));
        r.setMouseTransparent(true);
        live.getChildren().add(r);
    }

    /** A minion of HEAVY_KI or more lands with a heavy thud (sounds/heavy-minion.wav), yours or the AI's. */
    static void heavyLanding(Card card) {
        if (card instanceof MinionCard && card.getKiCost() >= HEAVY_KI) {
            Sound.play(HEAVY_THUD);
        }
    }

    /** A minion just played: it floats above its slot, a little bigger, then drops onto it. */
    static void floatAndDrop(Node minion) {
        minion.setTranslateY(-40);   // starts above its slot
        minion.setScaleX(1.15);
        minion.setScaleY(1.15);
        // It floats: drifts up a little, still big.
        KeyFrame floating = new KeyFrame(Duration.millis(FLOAT_MS),
                new KeyValue(minion.translateYProperty(), -50),
                new KeyValue(minion.scaleXProperty(), 1.15),
                new KeyValue(minion.scaleYProperty(), 1.15));
        // It drops: down onto its slot, back to its size, faster and faster.
        KeyFrame landed = new KeyFrame(Duration.millis(FLOAT_MS + DROP_MS),
                new KeyValue(minion.translateYProperty(), 0, Interpolator.EASE_IN),
                new KeyValue(minion.scaleXProperty(), 1, Interpolator.EASE_IN),
                new KeyValue(minion.scaleYProperty(), 1, Interpolator.EASE_IN));
        new Timeline(floating, landed).play();
    }

    /** Runs `then` when a played card hits the table: after the float and the drop for a minion, at once otherwise. */
    static void afterLanding(Card card, Runnable then) {
        double wait = 0;
        if (card instanceof MinionCard) {
            wait = FLOAT_MS + DROP_MS;
        }
        PauseTransition landing = new PauseTransition(Duration.millis(wait));
        landing.setOnFinished(event -> then.run());
        landing.play();
    }

    /** Screen shake when a card lands: 6+ Ki shakes; 10+ Ki shakes longer and harder, and every minion on the board wobbles. */
    void impact(int ki, boolean attack) {
        if (ki < 6 && !attack) return;
        double strength = ki >= 10 ? 14 : ki >= 6 ? 6 : 3;
        int jolts = ki >= 10 ? 15 : ki >= 6 ? 10 : 6;
        List<MinionView> cards = ki < 10 ? List.of()
                : live.getChildren().stream().filter(MinionView.class::isInstance).map(MinionView.class::cast).toList();
        Timeline t = new Timeline();
        for (int i = 0; i <= jolts; i++) {
            double fade = 1 - (double) i / jolts;
            List<KeyValue> moves = new ArrayList<>(List.of(
                    new KeyValue(table.translateXProperty(), (Math.random() * 2 - 1) * strength * fade),
                    new KeyValue(table.translateYProperty(), (Math.random() * 2 - 1) * strength * fade)));
            for (MinionView c : cards) moves.add(new KeyValue(c.rotateProperty(), (Math.random() * 2 - 1) * 8 * fade));
            t.getKeyFrames().add(new KeyFrame(Duration.millis(i * 35), moves.toArray(KeyValue[]::new)));
        }
        t.play();
    }
}
