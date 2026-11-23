package fr.onishintan.ui.match.dialog;

import fr.onishintan.model.Card;
import fr.onishintan.ui.cards.CardView;
import fr.onishintan.ui.match.BoardView;
import fr.onishintan.ui.screens.HomeView;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/** Izanagi: the enemy's hand laid open over the table; clicking a card takes it. A card that can't be taken is dimmed. */
public final class StealPicker extends StackPane {
    /** How wide each card of the enemy hand is shown. */
    private static final double CARD_W = 150;

    /** cards = the enemy hand; canTake says which may be taken; onPick gets the chosen card. */
    public StealPicker(List<Card> cards, Predicate<Card> canTake, Consumer<Card> onPick) {
        setPrefSize(BoardView.W, BoardView.H);
        Rectangle veil = new Rectangle(BoardView.W, BoardView.H, Color.rgb(0, 0, 0, 0.8));

        Label title = new Label("伊邪那岐  Izanagi looks into the enemy's hand\nTake one card");
        title.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: 30; -fx-font-weight: bold; -fx-text-fill: #f3ede1; -fx-text-alignment: center;");

        FlowPane hand = new FlowPane(14, 14);
        hand.setAlignment(Pos.CENTER);
        hand.setPrefWrapLength(BoardView.W - 200);
        for (Card card : cards) {
            CardView view = new CardView(card, CARD_W);
            if (canTake.test(card)) {
                HomeView.clickable(view, false, () -> {
                    ((Pane) getParent()).getChildren().remove(this);
                    onPick.accept(card);
                });
            } else {
                view.setEffect(new ColorAdjust(0, -0.6, -0.5, 0));   // a held Curse: it can't be taken
            }
            hand.getChildren().add(view);
        }

        VBox box = new VBox(30, title, hand);
        box.setAlignment(Pos.CENTER);
        getChildren().addAll(veil, box);
    }
}
