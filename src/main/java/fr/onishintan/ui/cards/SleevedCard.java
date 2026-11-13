package fr.onishintan.ui.cards;

import fr.onishintan.model.Card;
import fr.onishintan.ui.app.Images;

import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

/**
 * A card inside its plastic sleeve (images/deck-builder/card-sleeve.png): the card a little smaller,
 * centred, with the sleeve's glossy edges drawn over it. Used in the deck editor's collection and the Inventory.
 */
public class SleevedCard extends Pane {
    private static final String SLEEVE = "/images/deck-builder/card-sleeve.png";
    /** The sleeve picture's size (width x height). */
    private static final double SLEEVE_W = 1061, SLEEVE_H = 1482;
    /** The card's width as a part of the sleeve's width. */
    private static final double CARD_SCALE = 0.92;
    /** The gap above the card as a part of the sleeve's height (room for the sleeve's top lip). */
    private static final double CARD_TOP = 0.045;

    private final Card card;

    /** The card in its sleeve, the sleeve `width` wide (it is a little taller than the card). */
    public SleevedCard(Card card, double width) {
        this.card = card;
        double height = width * SLEEVE_H / SLEEVE_W;

        // The card, a little smaller than the sleeve, centred left to right, just under the sleeve's top lip.
        double cardWidth = width * CARD_SCALE;
        CardView view = new CardView(card, cardWidth);
        view.relocate((width - cardWidth) / 2, height * CARD_TOP);

        // The sleeve over it: glossy edges, clear in the middle. It lets the mouse through to the card.
        ImageView sleeve = new ImageView(Images.get(SLEEVE, width, height));
        sleeve.setMouseTransparent(true);

        getChildren().addAll(view, sleeve);
        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(width, height);
    }

    /** The card inside the sleeve. */
    public Card getCard() {
        return card;
    }
}