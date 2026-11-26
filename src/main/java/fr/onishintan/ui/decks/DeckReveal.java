package fr.onishintan.ui.decks;

import fr.onishintan.model.Card;
import fr.onishintan.model.Deck;
import fr.onishintan.ui.app.ScreenGrid;
import fr.onishintan.ui.app.ScrollTheme;
import fr.onishintan.ui.cards.CardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * What you see once a deck is opened: the deck's cards over a dark veil, flying out of the middle of the screen
 * to their places (each different card once, with its copy count), plus Edit, Delete and Close.
 * Close first gathers the cards back into the middle, then the deck goes back onto its shelf.
 */
class DeckReveal extends Pane {
    private static final double CARD_W = 120, GAP = 14;
    private static final int PER_ROW = 10;
    private static final double AREA_W = ScreenGrid.W - 120, AREA_H = 740;
    /** How long the cards take to reach their places (and to come back on Close). */
    private static final Duration SPREAD = Duration.millis(350);

    /** The cards on show, and the point in the middle they all start from (and go back to on Close). */
    private final List<VBox> shownCards = new ArrayList<>();
    private double startX, startY;

    DeckReveal(SavedDeck deck, Runnable onEdit, Runnable onDelete, Runnable onClose) {
        setPrefSize(ScreenGrid.W, ScreenGrid.H);
        getChildren().add(new Rectangle(ScreenGrid.W, ScreenGrid.H, Color.rgb(0, 0, 0, 0.85)));

        // Top: the deck's name, class and card count.
        String words = deck.getName() + "     " + deck.getHeroClass().getKanji() + " " + deck.getHeroClass().getDisplayName()
                + "     " + deck.getCards().size() + " / " + Deck.SIZE;
        Label title = DecksView.text(words, 30, DecksView.PAPER);
        title.relocate(60, 30);
        getChildren().add(title);

        // Middle: the cards.
        getChildren().add(cardArea(deck));

        // Bottom: the buttons. Edit and Delete close this view at once; Close gathers the cards first.
        HBox buttons = new HBox(20,
                DecksView.button("Edit", () -> closeThen(onEdit)),
                DecksView.button("Delete", () -> closeThen(onDelete)),
                DecksView.button("Close", () -> gatherThen(onClose)));
        buttons.relocate(60, 840);
        getChildren().add(buttons);
    }

    /** The deck's different cards in rows of PER_ROW, cheapest first. They all start in the middle at once and spread out. */
    private ScrollPane cardArea(SavedDeck deck) {
        Pane cards = new Pane();
        ScrollPane scroll = new ScrollPane(cards);
        scroll.setPrefSize(AREA_W, AREA_H);
        scroll.relocate(60, 85);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        ScrollTheme.apply(scroll);

        Map<Card, Integer> copies = SavedDeck.copiesByKi(deck.getCards());
        if (copies.isEmpty()) {
            cards.getChildren().add(DecksView.text("This deck is empty. Press Edit to add cards.", 24, DecksView.PAPER));
            return scroll;
        }

        double cardH = CARD_W * CardView.H / CardView.W;
        double rowH = cardH + GAP + 24;   // + room for the copy count under each card
        // The middle of the screen, in the card area's own coordinates: every card starts there.
        startX = AREA_W / 2 - CARD_W / 2;
        startY = AREA_H / 2 - cardH / 2;

        int index = 0;
        for (Card card : copies.keySet()) {
            int column = index % PER_ROW;
            int row = index / PER_ROW;
            double x = 10 + column * (CARD_W + GAP);
            double y = 10 + row * rowH;

            VBox shown = cardWithCount(card, copies.get(card));
            shown.relocate(startX, startY);
            shown.setScaleX(0);   // edge-on, like the stack at the end of its flip
            cards.getChildren().add(shown);
            shownCards.add(shown);

            Timeline fly = new Timeline(new KeyFrame(SPREAD,
                    new KeyValue(shown.layoutXProperty(), x),
                    new KeyValue(shown.layoutYProperty(), y),
                    new KeyValue(shown.scaleXProperty(), 1)));
            fly.play();
            index++;
        }
        return scroll;
    }

    /** A card with "x2" or "x3" under it (nothing under a single copy). */
    private VBox cardWithCount(Card card, int count) {
        String countText = "";
        if (count > 1) {
            countText = "x" + count;
        }
        VBox box = new VBox(4, new CardView(card, CARD_W), DecksView.text(countText, 18, DecksView.PAPER));
        box.setAlignment(Pos.TOP_CENTER);
        return box;
    }

    /** Close: every card flies back to the middle and turns edge-on (the spread in reverse), then this view goes away. */
    private void gatherThen(Runnable action) {
        if (shownCards.isEmpty()) {
            closeThen(action);
            return;
        }
        Timeline gather = new Timeline();
        for (VBox card : shownCards) {
            gather.getKeyFrames().add(new KeyFrame(SPREAD,
                    new KeyValue(card.layoutXProperty(), startX),
                    new KeyValue(card.layoutYProperty(), startY),
                    new KeyValue(card.scaleXProperty(), 0)));
        }
        gather.setOnFinished(event -> closeThen(action));
        gather.play();
    }

    /** Removes this view from the screen, then does the button's job. */
    private void closeThen(Runnable action) {
        ((Pane) getParent()).getChildren().remove(this);
        action.run();
    }
}