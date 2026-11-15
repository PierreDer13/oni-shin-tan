package fr.onishintan.ui.cards;

import fr.onishintan.model.Card;
import fr.onishintan.ui.app.Images;
import fr.onishintan.ui.app.ScreenGrid;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * A wall of wooden shelves (images/deck-builder/card-shelf.png, one shelf repeated downwards) with cards standing on it.
 * The shelf picture, the stands and the gaps are all scaled to the wall's width.
 * Cards come in groups (one per class in the deck editor): each group starts on a new shelf, its title in the shelf's top-left corner.
 */
public class CardShelves extends VBox {
    private static final String SHELF = "/images/deck-builder/card-shelf.png";
    /** The shelf picture's size. */
    private static final double PICTURE_W = 1600, PICTURE_H = 444;
    /** Height in the shelf picture where the stands rest: the top of the plank. */
    private static final double PLANK_TOP = 411;
    /** Width between the shelf's side boards, and the gap between two stands, in the picture. */
    private static final double INNER_W = 1480, GAP = 20;

    /** Picture pixels -> wall pixels. */
    private final double scale;
    private final int perShelf;
    private final double standWidth;
    /** Where the cards start on each shelf (and where its title tag sits). */
    private final double areaLeft;

    /** The Inventory: every card on screen-wide shelves, 5 per shelf, filling at least the page. */
    public CardShelves(List<Card> cards) {
        this(ScreenGrid.W, 5);
        addGroup(null, cards);
        setMinHeight(ScreenGrid.H);
    }

    /** An empty wall `width` wide, its cards spread over the whole shelf, perShelf per shelf; fill it with addGroup(). */
    public CardShelves(double width, int perShelf) {
        this(width, perShelf, (width - width * INNER_W / PICTURE_W) / 2, width * INNER_W / PICTURE_W, 0);
    }

    /**
     * An empty wall `width` wide whose cards stand only in part of each shelf: from areaLeft, areaWidth wide, perShelf per shelf
     * (the deck editor keeps the right of the page for the deck list). The first shelf starts topInset down, under a top bar.
     */
    public CardShelves(double width, int perShelf, double areaLeft, double areaWidth, double topInset) {
        this.scale = width / PICTURE_W;
        this.perShelf = perShelf;
        this.areaLeft = areaLeft;
        double gaps = (perShelf - 1) * GAP * scale;
        this.standWidth = (areaWidth - gaps) / perShelf;
        setPrefWidth(width);
        setPadding(new Insets(topInset, 0, 0, 0));
        setBackground(shelfBackground(width, PICTURE_H * scale, topInset));
    }

    /**
     * Puts these cards on new shelves, perShelf per shelf; the first of those shelves shows `title` (null: no title).
     * @return the standing cards, in order, so the caller can make them clickable
     */
    public List<StandingCard> addGroup(String title, List<Card> cards) {
        List<StandingCard> standing = new ArrayList<>();
        for (int first = 0; first < cards.size(); first += perShelf) {
            int end = Math.min(first + perShelf, cards.size());
            HBox shelf = shelf(cards.subList(first, end), standing);
            if (first == 0 && title != null) {
                getChildren().add(withTitle(shelf, title));
            } else {
                getChildren().add(shelf);
            }
        }
        return standing;
    }

    /** Puts the cards on the shelves one class at a time, in the order given: each class starts a new shelf with its name. */
    public List<StandingCard> addByClass(List<Card> cards) {
        List<StandingCard> standing = new ArrayList<>();
        int start = 0;
        for (int i = 1; i <= cards.size(); i++) {
            boolean lastCard = i == cards.size();
            if (lastCard || cards.get(i).getHeroClass() != cards.get(start).getHeroClass()) {
                List<Card> group = cards.subList(start, i);
                String title = group.get(0).getHeroClass().getDisplayName();
                standing.addAll(addGroup(title, group));
                start = i;
            }
        }
        return standing;
    }

    /** One shelf: its cards side by side from areaLeft, standing on the plank. Each standing card is also added to `standing`. */
    private HBox shelf(List<Card> cards, List<StandingCard> standing) {
        double shelfHeight = PICTURE_H * scale;
        HBox shelf = new HBox(GAP * scale);
        shelf.setAlignment(Pos.BOTTOM_LEFT);   // from areaLeft, like books
        shelf.setPrefHeight(shelfHeight);
        shelf.setMinHeight(shelfHeight);
        shelf.setMaxHeight(shelfHeight);
        // The stands end on the top of the plank, not at the bottom of the shelf picture.
        shelf.setPadding(new Insets(0, 0, (PICTURE_H - PLANK_TOP) * scale, areaLeft));
        for (Card card : cards) {
            StandingCard view = new StandingCard(card, standWidth);
            standing.add(view);
            shelf.getChildren().add(view);
        }
        return shelf;
    }

    /** The shelf with its group's title on a small dark tag in its top-left corner. */
    private StackPane withTitle(HBox shelf, String title) {
        Label tag = new Label(title);
        tag.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-weight: bold; -fx-font-size: 20; -fx-text-fill: #f3ede1;"
                + " -fx-background-color: rgba(20,12,8,0.85); -fx-background-radius: 6; -fx-padding: 2 12;");
        tag.setMouseTransparent(true);
        StackPane withTitle = new StackPane(shelf, tag);
        StackPane.setAlignment(tag, Pos.TOP_LEFT);
        StackPane.setMargin(tag, new Insets(8, 0, 0, areaLeft));   // above the first card of the shelf
        return withTitle;
    }

    /** The shelf picture at width x height, repeated downwards, its first shelf starting topInset down the wall. */
    private static Background shelfBackground(double width, double height, double topInset) {
        BackgroundPosition position = new BackgroundPosition(Side.LEFT, 0, false, Side.TOP, topInset, false);
        BackgroundImage tile = new BackgroundImage(Images.get(SHELF, width, height),
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.REPEAT,
                position, BackgroundSize.DEFAULT);
        return new Background(tile);
    }
}
