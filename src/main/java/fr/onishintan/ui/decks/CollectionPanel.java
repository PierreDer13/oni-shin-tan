package fr.onishintan.ui.decks;

import fr.onishintan.model.Card;
import fr.onishintan.ui.cards.CardView;

import java.util.List;
import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;

/** Left side of the deck editor: the cards you may add, as a scrolling grid. Left click adds a copy, right click removes one. */
class CollectionPanel extends ScrollPane {
    private static final double CARD_W = 160, GAP = 16;
    private static final int PER_ROW = 6;

    CollectionPanel(List<Card> cards, Consumer<Card> onAdd, Consumer<Card> onRemove) {
        FlowPane cardGrid = new FlowPane(GAP, GAP);
        cardGrid.setPrefWrapLength(PER_ROW * CARD_W + (PER_ROW - 1) * GAP);
        cardGrid.setPadding(new Insets(10));

        for (Card card : cards) {
            CardView view = new CardView(card, CARD_W);
            view.setOnMousePressed(event -> {
                if (event.getButton() == MouseButton.PRIMARY) {
                    onAdd.accept(card);
                } else if (event.getButton() == MouseButton.SECONDARY) {
                    onRemove.accept(card);
                }
            });
            cardGrid.getChildren().add(view);
        }

        setContent(cardGrid);
        setPrefSize(1100, 750);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setStyle("-fx-background-color: transparent; -fx-background: transparent;");
    }
}