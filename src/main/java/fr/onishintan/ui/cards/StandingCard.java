package fr.onishintan.ui.cards;

import fr.onishintan.model.Card;
import fr.onishintan.ui.app.Images;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

/**
 * A card in its sleeve, standing in a wooden card stand (images/deck-builder/card-stand.png).
 * The card stands between the posts, on the base's front step; the step is drawn over the card's bottom edge.
 * The pane's bottom is where the stand's feet end, so it can be set straight on a shelf.
 */
public class StandingCard extends Pane {
    private static final String STAND = "/images/deck-builder/card-stand.png";
    /** The stand picture's size (width x height). */
    private static final double PICTURE_W = 1190, PICTURE_H = 1322;
    /** Left and right edges of the gap between the two posts, as parts of the picture's width. */
    private static final double SLOT_LEFT = 236 / PICTURE_W, SLOT_RIGHT = 952 / PICTURE_W;
    /** The sleeve's width as a part of the gap between the posts (1 = touching both posts). */
    private static final double SLEEVE_IN_SLOT = 1.03;
    /** Where the bottom of the sleeve goes, as a part of the picture's height: on the stand's front step. */
    private static final double SLEEVE_BOTTOM = 0.71;
    /** Top of the stand's front step, as a part of the picture's height: drawn again over the card. */
    private static final double FRONT_STEP = 932 / PICTURE_H;
    /** Where the stand's feet end, as a part of the picture's height (below is empty). */
    private static final double FEET = 1260 / PICTURE_H;

    private final Card card;
    /** The card in its sleeve, standing between the posts. */
    private final SleevedCard sleeved;

    /** The card standing in a stand `width` wide. */
    public StandingCard(Card card, double width) {
        this.card = card;
        double height = width * PICTURE_H / PICTURE_W;

        // The sleeved card, as wide as the gap between the posts, so it touches both.
        double slotLeft = width * SLOT_LEFT;
        double slotWidth = width * (SLOT_RIGHT - SLOT_LEFT);
        double sleeveWidth = slotWidth * SLEEVE_IN_SLOT;
        sleeved = new SleevedCard(card, sleeveWidth);
        double sleeveX = slotLeft + (slotWidth - sleeveWidth) / 2;
        double sleeveY = height * SLEEVE_BOTTOM - sleeved.getPrefHeight();
        sleeved.relocate(sleeveX, sleeveY);

        // The whole stand behind the card, and its front step drawn again in front of it, so the card stands on the step.
        Image picture = Images.get(STAND, width, height);
        ImageView stand = new ImageView(picture);
        stand.setMouseTransparent(true);

        double frontTop = height * FRONT_STEP;
        ImageView front = new ImageView(picture);
        front.setViewport(new Rectangle2D(0, frontTop, width, height - frontTop));   // only the picture below the step's top
        front.relocate(0, frontTop);
        front.setMouseTransparent(true);

        getChildren().addAll(stand, sleeved, front);

        // As tall as the stand's feet, so the pane's bottom is where the stand touches the shelf.
        double feetHeight = height * FEET;
        setPrefSize(width, feetHeight);
        setMinSize(width, feetHeight);
        setMaxSize(width, feetHeight);
    }

    /** The card in the stand. */
    public Card getCard() {
        return card;
    }

    /** The card in its sleeve, without the stand: the part that grows when hovered. */
    public SleevedCard getSleeve() {
        return sleeved;
    }
}