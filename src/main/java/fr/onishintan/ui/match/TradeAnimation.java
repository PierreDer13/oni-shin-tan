package fr.onishintan.ui.match;

import fr.onishintan.model.Card;
import fr.onishintan.ui.cards.CardView;

import java.util.function.Supplier;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

/**
 * The trade animation (GDD, Trade): the card you dropped on your deck sinks into it face down,
 * then the deck is shuffled. After LENGTH, BoardView deals the card you drew into your hand.
 */
final class TradeAnimation {
    /** How long sinking + shuffling take; the drawn card starts flying in after this. */
    static final Duration LENGTH = Duration.millis(900);

    private static final double SINK_MS = 300;     // the traded card going into the deck
    private static final double RIFFLE_MS = 200;   // one shuffle: halves apart, then together again
    private static final int RIFFLES = 3;
    private static final double SPREAD = 40;       // how far each half slides out, in grid pixels
    private static final double TILT = 8;          // how much each half turns, in degrees

    private TradeAnimation() {
        // Only static methods.
    }

    /**
     * Plays the animation on `layer` over your deck.
     * x, y = top-left corner of the deck's top card, width = card width, cardBack = makes one card-back image.
     */
    static void play(Pane layer, Card traded, double x, double y, double width, Supplier<ImageView> cardBack) {
        sink(layer, traded, x, y, width);
        shuffle(layer, x, y, cardBack);
    }

    /** The traded card starts just above the deck (where you let go of it), drops onto it and turns edge-on. */
    private static void sink(Pane layer, Card traded, double x, double y, double width) {
        CardView card = new CardView(traded, width);
        card.setMouseTransparent(true);
        card.relocate(x, y - 80);
        layer.getChildren().add(card);

        Timeline sinking = new Timeline(new KeyFrame(Duration.millis(SINK_MS),
                new KeyValue(card.layoutYProperty(), y),
                new KeyValue(card.scaleXProperty(), 0)));   // edge-on: from now on you only see the deck's back
        sinking.setOnFinished(event -> layer.getChildren().remove(card));
        sinking.play();
    }

    /** Two card backs on the deck (its left and right halves) slide apart and tilt, then come back together, RIFFLES times. */
    private static void shuffle(Pane layer, double x, double y, Supplier<ImageView> cardBack) {
        ImageView left = cardBack.get();
        ImageView right = cardBack.get();
        left.relocate(x, y);
        right.relocate(x, y);
        left.setMouseTransparent(true);
        right.setMouseTransparent(true);
        layer.getChildren().addAll(left, right);

        Timeline riffles = new Timeline();
        for (int i = 0; i < RIFFLES; i++) {
            double start = SINK_MS + i * RIFFLE_MS;
            // Start of a riffle: both halves together on the deck, straight.
            riffles.getKeyFrames().add(together(Duration.millis(start), left, right));
            // Halfway: the halves are apart and tilted away from each other.
            riffles.getKeyFrames().add(new KeyFrame(Duration.millis(start + RIFFLE_MS / 2),
                    new KeyValue(left.translateXProperty(), -SPREAD),
                    new KeyValue(right.translateXProperty(), SPREAD),
                    new KeyValue(left.rotateProperty(), -TILT),
                    new KeyValue(right.rotateProperty(), TILT)));
        }
        // The end: together again, then the two images are removed (the real deck is underneath).
        riffles.getKeyFrames().add(together(LENGTH, left, right));
        riffles.setOnFinished(event -> layer.getChildren().removeAll(left, right));
        riffles.play();
    }

    /** A key frame where both halves are back on the deck, straight. */
    private static KeyFrame together(Duration time, ImageView left, ImageView right) {
        return new KeyFrame(time,
                new KeyValue(left.translateXProperty(), 0),
                new KeyValue(right.translateXProperty(), 0),
                new KeyValue(left.rotateProperty(), 0),
                new KeyValue(right.rotateProperty(), 0));
    }
}