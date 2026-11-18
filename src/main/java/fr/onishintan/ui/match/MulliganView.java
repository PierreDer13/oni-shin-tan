package fr.onishintan.ui.match;

import fr.onishintan.model.Card;
import fr.onishintan.model.Player;
import fr.onishintan.ui.app.Images;
import fr.onishintan.ui.cards.CardView;
import fr.onishintan.ui.screens.HomeView;
import javafx.animation.Animation;
import javafx.animation.PauseTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.function.Function;

/**
 * Mulligan (GDD): the opening hand shown big over the table. Click a card to send it back (it flips face down),
 * click it again to keep it; CONFIRM swaps them through `exchange` (engine.mulligan), the new cards flip in, then onDone runs.
 */
final class MulliganView extends StackPane {
    private static final double CARD_W = 210;
    private static final double CARD_H = CARD_W * 742 / 530;   // card art ratio
    private static final Duration FLIP = Duration.millis(120);
    private static final Duration FLY = Duration.millis(350);

    /** The cards picked to go back, in click order. */
    private final List<Card> chosen = new ArrayList<>();
    /** Each opening card's slot, so its new card can flip in there. */
    private final Map<Card, StackPane> slots = new HashMap<>();
    /** The deck the cards go back into, where it sits on the table. */
    private final ImageView deck = new ImageView(Images.get("/images/frames/card-back.png", CARD_W, CARD_H));

    MulliganView(Player player, Function<List<Card>, List<Card>> exchange, Runnable onDone) {
        Label title = text("交換  MULLIGAN", 44);
        Label hint = text(player.getName() + ": click the cards to send back, then confirm", 22);

        HBox cards = new HBox(24);
        cards.setAlignment(Pos.CENTER);
        for (Card card : player.getHand()) cards.getChildren().add(slot(card));

        Label confirm = text("決定  CONFIRM", 28);
        confirm.setStyle(confirm.getStyle() + " -fx-background-color: rgba(20,12,8,0.85); -fx-background-radius: 10;"
                + " -fx-padding: 8 22; -fx-border-color: #c9a227; -fx-border-radius: 10;");
        HomeView.clickable(confirm, false, () -> {
            cards.setMouseTransparent(true);   // one exchange only
            confirm.setMouseTransparent(true);
            List<Card> fresh = exchange.apply(List.copyOf(chosen));   // the engine shuffles them into the deck and draws new ones
            if (fresh.isEmpty()) {
                close(onDone);
                return;
            }
            hint.setText("Shuffled back into the deck: your new cards");
            swapThroughDeck(fresh, onDone);
        });

        VBox box = new VBox(30, title, hint, cards, confirm);
        box.setAlignment(Pos.CENTER);
        getChildren().addAll(new Rectangle(BoardView.W, BoardView.H, Color.rgb(0, 0, 0, 0.78)), box);
        deck.setFitWidth(115);
        deck.setPreserveRatio(true);
        deck.setManaged(false);
        deck.relocate(BoardView.DECK_X, BoardView.DECK_Y);   // where the deck sits on the table
        getChildren().add(deck);
        setPrefSize(BoardView.W, BoardView.H);
    }

    /** One opening card: face up = kept, face down = sent back. Hovering lifts it a little. */
    private StackPane slot(Card card) {
        CardView face = new CardView(card, CARD_W);
        ImageView back = new ImageView(Images.get("/images/frames/card-back.png", CARD_W, CARD_H));
        back.setFitWidth(CARD_W);
        back.setFitHeight(CARD_H);
        back.setVisible(false);
        StackPane slot = new StackPane(face, back);
        slots.put(card, slot);
        slot.setMaxSize(CARD_W, CARD_H);
        slot.setOnMouseEntered(e -> slot.setTranslateY(-14));
        slot.setOnMouseExited(e -> slot.setTranslateY(0));
        slot.setOnMouseClicked(e -> {
            boolean sendBack = !chosen.contains(card);
            if (sendBack) chosen.add(card); else chosen.remove(card);
            flip(slot, face, back, sendBack);
        });
        return slot;
    }

    /** The card turns over: it narrows to nothing, shows its other side, then widens again. */
    private static void flip(Node slot, Node face, Node back, boolean showBack) {
        ScaleTransition close = new ScaleTransition(FLIP, slot);
        close.setToX(0);
        close.setOnFinished(e -> {
            face.setVisible(!showBack);
            back.setVisible(showBack);
            ScaleTransition open = new ScaleTransition(FLIP, slot);
            open.setToX(1);
            open.play();
        });
        close.play();
    }

    /**
     * The sent-back cards fly into the deck, the deck shakes as it is shuffled, then each new card flies out to the empty place
     * and turns face up. After a moment to look at them, the view closes and onDone runs.
     */
    private void swapThroughDeck(List<Card> fresh, Runnable onDone) {
        ParallelTransition out = new ParallelTransition();
        ParallelTransition in = new ParallelTransition();
        for (int i = 0; i < fresh.size(); i++) {
            StackPane slot = slots.get(chosen.get(i));
            slot.setTranslateY(0);   // drop the hover lift
            slot.getChildren().set(0, new CardView(fresh.get(i), CARD_W));   // the new card, hidden under the back
            Bounds place = slot.getBoundsInParent();
            Bounds pile = deck.getBoundsInLocal();
            Point2D deckAt = slot.getParent().sceneToLocal(deck.localToScene(pile.getCenterX(), pile.getCenterY()));
            double dx = deckAt.getX() - place.getCenterX();
            double dy = deckAt.getY() - place.getCenterY();
            out.getChildren().add(fly(slot, 0, 0, dx, dy, 1, 0.5, i));
            in.getChildren().add(fly(slot, dx, dy, 0, 0, 0.5, 1, i));
        }
        RotateTransition shuffle = new RotateTransition(Duration.millis(70), deck);
        shuffle.setFromAngle(-8);
        shuffle.setToAngle(8);
        shuffle.setAutoReverse(true);
        shuffle.setCycleCount(6);
        SequentialTransition all = new SequentialTransition(out, shuffle, in);
        all.setOnFinished(e -> {
            for (int i = 0; i < fresh.size(); i++) {
                StackPane slot = slots.get(chosen.get(i));
                flip(slot, slot.getChildren().get(0), slot.getChildren().get(1), false);
            }
            PauseTransition look = new PauseTransition(Duration.millis(1200));   // time to see the new cards
            look.setOnFinished(f -> close(onDone));
            look.play();
        });
        all.play();
    }

    /** One card's flight from (fromX, fromY) to (toX, toY), growing or shrinking; the `order`-th card leaves 120 ms after the one before. */
    private static Animation fly(Node card, double fromX, double fromY, double toX, double toY, double fromSize, double toSize, int order) {
        TranslateTransition slide = new TranslateTransition(FLY, card);
        slide.setFromX(fromX);
        slide.setFromY(fromY);
        slide.setToX(toX);
        slide.setToY(toY);
        ScaleTransition size = new ScaleTransition(FLY, card);
        size.setFromX(fromSize);
        size.setFromY(fromSize);
        size.setToX(toSize);
        size.setToY(toSize);
        ParallelTransition both = new ParallelTransition(slide, size);
        both.setDelay(Duration.millis(order * 120));
        return both;
    }

    /** Takes the view off the table, then the match goes on. */
    private void close(Runnable onDone) {
        if (getParent() instanceof Pane parent) parent.getChildren().remove(this);
        onDone.run();
    }

    private static Label text(String words, double size) {
        Label label = new Label(words);
        label.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: " + size + "; -fx-font-weight: bold; -fx-text-fill: #f3ede1;");
        return label;
    }
}