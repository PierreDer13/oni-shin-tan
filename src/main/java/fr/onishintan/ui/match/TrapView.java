package fr.onishintan.ui.match;

import fr.onishintan.model.TrapCard;
import fr.onishintan.ui.cards.CardView;
import fr.onishintan.ui.fx.Fx;

import javafx.animation.KeyFrame;
import javafx.animation.Interpolator;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * Traps (GDD, Ninja traps): set face down next to their owner's hero. The owner sees each of their traps;
 * the enemy only sees that at least one is set. When a trap goes off, both players see it flip face up
 * in the middle of the table, then it is cut in half and disappears.
 */
final class TrapView {
    /** Width of a face-down trap next to the hero. */
    static final double BACK_W = 60;
    /** How far each of your traps sits from the previous one (they overlap a little). */
    static final double SPACING = 22;

    private static final double REVEAL_W = 260;              // the trap's size when it goes off
    private static final Duration HALF_FLIP = Duration.millis(200);
    private static final Duration READ_TIME = Duration.millis(1200);
    // Setting a trap, counted from the moment it is played.
    private static final Duration SET_SHOWN = Duration.millis(200);        // popped up in the middle
    private static final Duration SET_READ = Duration.millis(1000);        // stayed there to be read
    private static final Duration SET_FACE_GONE = Duration.millis(1200);   // first half of the flip
    private static final Duration SET_BACK_SHOWN = Duration.millis(1400);  // second half: the back faces you
    private static final Duration SET_LANDED = Duration.millis(2200);      // slowly glided to its place by the hero

    private TrapView() {
        // Only static methods.
    }

    /** One face-down trap: a small card back with a red TRAP label on it. */
    static StackPane back() {
        ImageView image = BoardView.image("/images/frames/card-back.png");
        image.setFitWidth(BACK_W);
        image.setPreserveRatio(true);

        Label seal = new Label("TRAP");
        seal.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #f3ede1;"
                + " -fx-background-color: #b3372a; -fx-background-radius: 20; -fx-padding: 2 7;");

        StackPane back = new StackPane(image, seal);
        back.setEffect(BoardView.SHADOW);
        return back;
    }

    /**
     * A trap going off: it appears face down in the middle of `layer`, flips face up, stays long enough to be read,
     * then is cut in half. `layer` must not be redrawn meanwhile (BoardView passes its table, not the live layer).
     */
    static void reveal(Pane layer, TrapCard trap) {
        double height = REVEAL_W * CardView.H / CardView.W;
        double x = BoardView.W / 2 - REVEAL_W / 2;
        double y = BoardView.H / 2 - height / 2;

        // The back and the face start at the same place; the face starts squashed to zero width (invisible).
        ImageView back = BoardView.image("/images/frames/card-back.png");
        back.setFitWidth(REVEAL_W);
        back.setPreserveRatio(true);
        back.relocate(x, y);
        CardView face = new CardView(trap, REVEAL_W);
        face.relocate(x, y);
        face.setScaleX(0);
        back.setMouseTransparent(true);
        face.setMouseTransparent(true);
        layer.getChildren().addAll(back, face);

        // The flip: the back narrows to nothing, then the face widens to full size.
        Timeline flip = new Timeline(
                new KeyFrame(HALF_FLIP,
                        new KeyValue(back.scaleXProperty(), 0),
                        new KeyValue(face.scaleXProperty(), 0)),
                new KeyFrame(HALF_FLIP.multiply(2),
                        new KeyValue(face.scaleXProperty(), 1)));
        flip.setOnFinished(event -> {
            layer.getChildren().remove(back);
            // Time to read it, then the diagonal cut (deathCut hides the card and animates its two halves).
            PauseTransition read = new PauseTransition(READ_TIME);
            read.setOnFinished(e -> {
                Fx.deathCut(layer, face);
                layer.getChildren().remove(face);
            });
            read.play();
        });
        flip.play();
    }

    /**
     * Setting a trap: the card pops up big in the middle of `layer`, stays to be read, flips to its back,
     * then slowly glides and shrinks to its place by the hero, top-left corner (toX, toY). onLanded runs when it is there.
     */
    static void set(Pane layer, TrapCard trap, double toX, double toY, Runnable onLanded) {
        double centreX = BoardView.W / 2;
        double centreY = BoardView.H / 2;
        double faceH = REVEAL_W * CardView.H / CardView.W;

        // The face, big in the middle, starting a little small and invisible so it can pop up.
        CardView face = new CardView(trap, REVEAL_W);
        face.relocate(centreX - REVEAL_W / 2, centreY - faceH / 2);
        face.setOpacity(0);
        face.setScaleX(0.7);
        face.setScaleY(0.7);

        // The back: the same small back that stays by the hero, already placed at its spot,
        // but moved to the middle (translate) and blown up to the face's size (scale), with zero width for the flip.
        StackPane back = back();
        back.relocate(toX, toY);
        double big = REVEAL_W / BACK_W;
        double fromX = centreX - (toX + BACK_W / 2);
        double fromY = centreY - (toY + backHeight() / 2);
        back.setTranslateX(fromX);
        back.setTranslateY(fromY);
        back.setScaleX(0);
        back.setScaleY(big);

        face.setMouseTransparent(true);
        back.setMouseTransparent(true);
        layer.getChildren().addAll(face, back);

        Timeline setting = new Timeline(
                // 1. The card pops up in the middle...
                new KeyFrame(SET_SHOWN,
                        new KeyValue(face.opacityProperty(), 1),
                        new KeyValue(face.scaleXProperty(), 1),
                        new KeyValue(face.scaleYProperty(), 1)),
                // 2. ... and stays there to be read.
                new KeyFrame(SET_READ,
                        new KeyValue(face.scaleXProperty(), 1)),
                // 3. The flip: the face narrows to nothing...
                new KeyFrame(SET_FACE_GONE,
                        new KeyValue(face.scaleXProperty(), 0),
                        new KeyValue(back.scaleXProperty(), 0)),
                // ... then the back widens to full size, still big in the middle.
                new KeyFrame(SET_BACK_SHOWN,
                        new KeyValue(back.scaleXProperty(), big),
                        new KeyValue(back.scaleYProperty(), big),
                        new KeyValue(back.translateXProperty(), fromX),
                        new KeyValue(back.translateYProperty(), fromY)),
                // 4. It slowly glides to its place by the hero and shrinks to its normal size.
                new KeyFrame(SET_LANDED,
                        new KeyValue(back.scaleXProperty(), 1, Interpolator.EASE_BOTH),
                        new KeyValue(back.scaleYProperty(), 1, Interpolator.EASE_BOTH),
                        new KeyValue(back.translateXProperty(), 0, Interpolator.EASE_BOTH),
                        new KeyValue(back.translateYProperty(), 0, Interpolator.EASE_BOTH)));
        setting.setOnFinished(event -> {
            layer.getChildren().removeAll(face, back);
            onLanded.run();
        });
        setting.play();
    }

    /** How tall a face-down trap is: the card-back image keeps its proportions at BACK_W wide. */
    private static double backHeight() {
        ImageView image = BoardView.image("/images/frames/card-back.png");
        return BACK_W * image.getImage().getHeight() / image.getImage().getWidth();
    }
}