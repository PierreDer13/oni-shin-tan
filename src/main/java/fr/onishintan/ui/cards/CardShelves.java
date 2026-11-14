package fr.onishintan.ui.cards;

import fr.onishintan.model.Card;
import fr.onishintan.ui.app.Images;
import fr.onishintan.ui.app.ScreenGrid;

import java.util.List;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * A wall of wooden shelves (images/deck-builder/card-shelf.png, one shelf repeated downwards)
 * with the cards standing on it, PER_SHELF cards per shelf. As wide as the screen, as tall as the cards need.
 */
public class CardShelves extends VBox {
    private static final String SHELF = "/images/deck-builder/card-shelf.png";
    /** Height of one shelf picture (it is 1600 wide, like the screen). */
    private static final double SHELF_H = 444;
    /** Height in the shelf picture where the stands rest: the top of the plank. */
    private static final double PLANK_TOP = 411;
    private static final int PER_SHELF = 5;
    private static final double STAND_W = 280;
    private static final double GAP = 20;

    /** The shelves with these cards on them, in the order given. */
    public CardShelves(List<Card> cards) {
        setPrefWidth(ScreenGrid.W);
        setMinHeight(ScreenGrid.H);   // the shelves fill the page even with few cards
        setBackground(shelfBackground());
        for (int first = 0; first < cards.size(); first += PER_SHELF) {
            int end = Math.min(first + PER_SHELF, cards.size());
            getChildren().add(shelf(cards.subList(first, end)));
        }
    }

    /** One shelf: its cards side by side, centred, standing on the plank. */
    private HBox shelf(List<Card> cards) {
        HBox shelf = new HBox(GAP);
        shelf.setAlignment(Pos.BOTTOM_CENTER);
        shelf.setPrefHeight(SHELF_H);
        shelf.setMinHeight(SHELF_H);
        shelf.setMaxHeight(SHELF_H);
        // The stands end on the top of the plank, not at the bottom of the shelf picture.
        shelf.setPadding(new Insets(0, 0, SHELF_H - PLANK_TOP, 0));
        for (Card card : cards) {
            shelf.getChildren().add(new StandingCard(card, STAND_W));
        }
        return shelf;
    }

    /** The shelf picture at its own size, from the top-left corner, repeated downwards. */
    private static Background shelfBackground() {
        BackgroundImage tile = new BackgroundImage(Images.get(SHELF),
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT);
        return new Background(tile);
    }
}