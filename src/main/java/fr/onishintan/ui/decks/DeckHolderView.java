package fr.onishintan.ui.decks;

import fr.onishintan.model.Deck;
import fr.onishintan.model.HeroClass;
import fr.onishintan.ui.screens.HomeView;

import java.util.EnumMap;
import java.util.Map;

import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * One saved deck on the deck list: its class's deck holder (the painted card box), with the deck's name and n / 50 under it.
 * Clicking the box runs onOpen. The static helpers cut the holder image into its lid and its box for DeckOpening.
 */
class DeckHolderView extends VBox {
    /** Width of a holder on the list, in grid pixels. */
    static final double WIDTH = 220;
    /** The holder images are 1024 x 1536. Rows 0 to LID_ROWS are the lid's flap, cut off so it can fold open. */
    private static final double IMAGE_W = 1024, IMAGE_H = 1536, LID_ROWS = 250;
    /** The holder images, loaded once per class. */
    private static final Map<HeroClass, Image> IMAGES = new EnumMap<>(HeroClass.class);

    private final Pane picture = new Pane();
    private Runnable onOpen = () -> {};

    DeckHolderView(SavedDeck deck) {
        super(8);
        setAlignment(Pos.TOP_CENTER);
        setPrefWidth(WIDTH);

        // The box: the body, with the lid on top of it.
        HeroClass heroClass = deck.getHeroClass();
        ImageView body = body(heroClass, WIDTH);
        body.setLayoutY(lidHeight(WIDTH));   // the body starts where the lid ends
        ImageView lid = lid(heroClass, WIDTH);
        picture.getChildren().addAll(body, lid);
        HomeView.clickable(picture, false, () -> onOpen.run());

        // Under it: the deck's name, and its card count (green once the deck can be played).
        Label name = DecksView.text(deck.getName(), 20, DecksView.INK);
        name.setMaxWidth(WIDTH);
        String countColour = DecksView.INK;
        if (deck.isLegal()) {
            countColour = "#2e7d32";
        }
        Label count = DecksView.text(deck.getCards().size() + " / " + Deck.SIZE + " cards", 16, countColour);
        getChildren().addAll(picture, name, count);
    }

    /** What happens when the box is clicked. */
    void setOnOpen(Runnable onOpen) {
        this.onOpen = onOpen;
    }

    /** The box itself, so the opening animation can start exactly where it is. */
    Pane picture() {
        return picture;
    }

    /** The lid's flap: the top LID_ROWS rows of the class's holder image, `width` wide. */
    static ImageView lid(HeroClass heroClass, double width) {
        return piece(heroClass, width, new Rectangle2D(0, 0, IMAGE_W, LID_ROWS));
    }

    /** The box under the lid: the rest of the image, `width` wide. */
    static ImageView body(HeroClass heroClass, double width) {
        return piece(heroClass, width, new Rectangle2D(0, LID_ROWS, IMAGE_W, IMAGE_H - LID_ROWS));
    }

    /** How tall the lid is when the holder is `width` wide. */
    static double lidHeight(double width) {
        return LID_ROWS * width / IMAGE_W;
    }

    /** How tall the whole holder is when it is `width` wide. */
    static double height(double width) {
        return IMAGE_H * width / IMAGE_W;
    }

    /** One part of the holder image (a viewport shows only that rectangle of the picture), scaled to `width`. */
    private static ImageView piece(HeroClass heroClass, double width, Rectangle2D part) {
        ImageView view = new ImageView(image(heroClass));
        view.setViewport(part);
        view.setFitWidth(width);
        view.setPreserveRatio(true);
        return view;
    }

    /** The class's holder image, e.g. /images/deck-builder/Samurai-deck-holder.png, loaded the first time it is needed. */
    private static Image image(HeroClass heroClass) {
        if (!IMAGES.containsKey(heroClass)) {
            String path = "/images/deck-builder/" + heroClass.getDisplayName() + "-deck-holder.png";
            IMAGES.put(heroClass, new Image(DeckHolderView.class.getResourceAsStream(path)));
        }
        return IMAGES.get(heroClass);
    }
}