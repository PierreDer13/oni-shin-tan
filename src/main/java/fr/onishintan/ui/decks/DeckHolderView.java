package fr.onishintan.ui.decks;

import fr.onishintan.model.Deck;
import fr.onishintan.model.HeroClass;
import fr.onishintan.ui.app.Sound;
import fr.onishintan.ui.screens.HomeView;

import java.util.HashMap;
import java.util.Map;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * One saved deck on a shelf: its class's deck holder seen from the side (its spine), like a book.
 * Hovering shows a small tag with the deck's name and card count; clicking runs onOpen.
 * The static helpers give the holder's three pictures (side, front, open) for DeckOpening.
 */
class DeckHolderView extends Pane {
    /** Every holder picture (side, front, open) is a 512 x 768 image with the box in the same place. */
    static final double IMAGE_W = 512, IMAGE_H = 768;
    /**
     * Where the spine is inside each class's side image (x, y, width, height in image pixels), measured on the images.
     * The four spines are not drawn exactly the same size, so each is cut out on its own, then all are shown
     * with the same shape (SPINE_RATIO): the decks on a shelf all look the same size.
     */
    private static final Map<HeroClass, Rectangle2D> SPINE_AREA = Map.of(
            HeroClass.SAMURAI, new Rectangle2D(191, 5, 132, 718),
            HeroClass.NINJA, new Rectangle2D(193, 11, 126, 706),
            HeroClass.KANNUSHI, new Rectangle2D(183, 3, 148, 734),
            HeroClass.ONI, new Rectangle2D(187, 37, 136, 678));
    /** A spine's width / height: the same shape for every class. */
    static final double SPINE_RATIO = 38.0 / 184;
    /** The card stands are 512 x 768 pictures too. Only columns STAND_X to STAND_X + STAND_W, rows 0 to STAND_H, are drawn. */
    private static final double STAND_X = 65, STAND_W = 380, STAND_H = 760;
    /** Measured on the stands: the middle of the opening, the cushion the spine stands on, and the top of the front rail. */
    private static final double STAND_MIDDLE = 256, FLOOR_Y = 617.5, RAIL_Y = 620;
    /** The spine in the stand: SPINE_IN_STAND wide (the opening is about 220), as tall as SPINE_RATIO makes it. */
    private static final double SPINE_IN_STAND = 200, SPINE_IN_STAND_H = SPINE_IN_STAND / SPINE_RATIO;
    /** How far the spine sticks out above the stand, and the height of the whole (spine top to stand feet), in stand pixels. */
    private static final double ABOVE = SPINE_IN_STAND_H - FLOOR_Y, TOTAL_H = ABOVE + STAND_H;
    /** On hover a spine slides out of its row towards you: straight down, off the front of the shelf. */
    static final double OUT_DX = 0, OUT_DY = 22;
    /** How much it grows while it slides out (around its centre): the 3D "coming towards you" feel. */
    static final double OUT_SCALE = 1.06;
    /** How long that slide takes (slow, so it reads as being pulled out). */
    static final Duration SLIDE_TIME = Duration.millis(300);
    /** The shadow a spine casts while it sticks out of its row. */
    static final DropShadow OUT_SHADOW = new DropShadow(14, 0, 6, Color.rgb(0, 0, 0, 0.65));
    /** The pictures, loaded the first time each one is needed. */
    private static final Map<String, Image> IMAGES = new HashMap<>();

    private Runnable onOpen = () -> {};
    /** The hover slide in progress, so a new one can stop it. */
    private Timeline sliding;
    /** The spine, its hover tag, and where the spine is inside this view (grid pixels). */
    private final ImageView spine;
    private final Label tag;
    private final double spineX, spineW, spineH;

    /**
     * A deck standing in its class's card stand, `height` tall in all (from the top of the spine to the stand's feet).
     * Back to front: the stand, the spine (standing on the stand's cushion), the stand's front rail drawn again
     * so the spine sits inside the stand, then the hover tag.
     */
    DeckHolderView(SavedDeck deck, double height) {
        double k = height / TOTAL_H;   // stand image pixels -> grid pixels

        ImageView stand = standPart(deck.getHeroClass(), 0, STAND_H, k);
        stand.setLayoutY(ABOVE * k);

        spineW = SPINE_IN_STAND * k;
        spineH = SPINE_IN_STAND_H * k;
        spineX = (STAND_MIDDLE - STAND_X - SPINE_IN_STAND / 2) * k;
        spine = side(deck.getHeroClass(), spineW, spineH);
        spine.setLayoutX(spineX);

        ImageView rail = standPart(deck.getHeroClass(), RAIL_Y, STAND_H, k);
        rail.setLayoutY((ABOVE + RAIL_Y) * k);

        tag = tag(deck, spineX, spineW);
        tag.setVisible(false);
        getChildren().addAll(stand, spine, rail, tag);

        // Hover: the spine slides slowly out of its stand towards you (down, growing a little), in front of the
        // stand's rail and with a shadow behind it. The tag appears above the neighbouring stands. Leaving puts it back.
        spine.setOnMouseEntered(event -> {
            slide(OUT_DX, OUT_DY, OUT_SCALE);
            spine.setViewOrder(-1);   // in front of the rail
            spine.setEffect(OUT_SHADOW);
            tag.setVisible(true);
            setViewOrder(-1);
        });
        spine.setOnMouseExited(event -> putBack());
        // Click: the usual click sound, then open the deck.
        spine.setOnMousePressed(event -> {
            Sound.play(HomeView.CLICK);
            onOpen.run();
        });
    }

    /** What happens when the spine is clicked. */
    void setOnOpen(Runnable onOpen) {
        this.onOpen = onOpen;
    }

    /**
     * Slides the spine to (dx, dy) from its place in the stand and scales it to `scale` (around its centre),
     * smoothly, in SLIDE_TIME. Growing while it slides out makes it look like it comes towards you.
     */
    private void slide(double dx, double dy, double scale) {
        if (sliding != null) {
            sliding.stop();
        }
        sliding = new Timeline(new KeyFrame(SLIDE_TIME,
                new KeyValue(spine.translateXProperty(), dx, Interpolator.EASE_BOTH),
                new KeyValue(spine.translateYProperty(), dy, Interpolator.EASE_BOTH),
                new KeyValue(spine.scaleXProperty(), scale, Interpolator.EASE_BOTH),
                new KeyValue(spine.scaleYProperty(), scale, Interpolator.EASE_BOTH)));
        sliding.play();
    }

    /**
     * The mouse left: the tag hides at once and the spine slides back into its stand.
     * It stays in front of the rail (with its shadow) until the slide is over; only then does it go back behind the rail,
     * so the rail never covers it on the way in.
     */
    private void putBack() {
        tag.setVisible(false);
        slide(0, 0, 1);
        sliding.setOnFinished(event -> backInStand());
    }

    /** End of the slide back: the spine sits behind the rail again, without its shadow, and the holder goes back among its neighbours. */
    private void backInStand() {
        spine.setViewOrder(0);
        spine.setEffect(null);
        setViewOrder(0);
    }

    /** While the deck is open, its spine is away: only the empty stand stays on the shelf. */
    void hideSpine() {
        spine.setVisible(false);
        tag.setVisible(false);
    }

    /** The deck is back: its spine stands in the stand again, as if never touched. */
    void showSpine() {
        if (sliding != null) {
            sliding.stop();
        }
        spine.setTranslateX(0);
        spine.setTranslateY(0);
        spine.setScaleX(1);
        spine.setScaleY(1);
        spine.setViewOrder(0);
        spine.setEffect(null);
        setViewOrder(0);
        spine.setVisible(true);
    }

    /** Where the spine is inside this view (its top is this view's top), and its size: DeckOpening starts from it. */
    double spineX() {
        return spineX;
    }

    double spineWidth() {
        return spineW;
    }

    double spineHeight() {
        return spineH;
    }

    /** How wide a deck in its stand is when the whole is `height` tall. */
    static double width(double height) {
        return STAND_W * height / TOTAL_H;
    }

    /** How tall the empty stand is (without the spine sticking out) when the whole is `height` tall. */
    static double standHeight(double height) {
        return STAND_H * height / TOTAL_H;
    }

    /** Rows fromRow to toRow of the class's stand picture (only its drawn columns), at k grid pixels per image pixel. */
    private static ImageView standPart(HeroClass heroClass, double fromRow, double toRow, double k) {
        ImageView view = new ImageView(standImage(heroClass));
        view.setViewport(new Rectangle2D(STAND_X, fromRow, STAND_W, toRow - fromRow));
        view.setFitWidth(STAND_W * k);
        view.setFitHeight((toRow - fromRow) * k);
        return view;
    }

    /** The hover tag above the spine: the deck's name, then n / 50 (with a check mark once the deck can be played). */
    private static Label tag(SavedDeck deck, double spineX, double spineW) {
        String words = deck.getName() + "\n" + deck.getCards().size() + " / " + Deck.SIZE + " cards";
        if (deck.isLegal()) {
            words = words + "  ✓";
        }
        Label tag = DecksView.text(words, 14, DecksView.PAPER);
        tag.setStyle(tag.getStyle() + " -fx-background-color: rgba(20,12,8,0.9); -fx-background-radius: 6;"
                + " -fx-padding: 4 8; -fx-text-alignment: center;");
        tag.setMouseTransparent(true);
        // Centred above the spine, whatever the tag's width turns out to be.
        tag.layoutXProperty().bind(tag.widthProperty().multiply(-0.5).add(spineX + spineW / 2));
        tag.setLayoutY(-52);
        return tag;
    }

    /** The spine: only the spine's area of the class's side picture, stretched to exactly width x height. */
    static ImageView side(HeroClass heroClass, double width, double height) {
        ImageView view = new ImageView(image(heroClass, "-side"));
        view.setViewport(spineArea(heroClass));
        view.setPreserveRatio(false);   // the exact same shape for every class
        view.setFitWidth(width);
        view.setFitHeight(height);
        return view;
    }

    /** Where the spine is inside this class's side picture (image pixels). */
    static Rectangle2D spineArea(HeroClass heroClass) {
        return SPINE_AREA.get(heroClass);
    }

    /** The closed holder seen from the front: the whole picture, `width` wide. */
    static ImageView front(HeroClass heroClass, double width) {
        return whole(heroClass, "", width);
    }

    /** The open holder (lid up), drawn in the same frame as front(). */
    static ImageView open(HeroClass heroClass, double width) {
        return whole(heroClass, "-open", width);
    }

    private static ImageView whole(HeroClass heroClass, String suffix, double width) {
        ImageView view = new ImageView(image(heroClass, suffix));
        view.setFitWidth(width);
        view.setPreserveRatio(true);
        return view;
    }

    /** e.g. /images/deck-builder/Samurai-card-stand.png, loaded only once. */
    private static Image standImage(HeroClass heroClass) {
        String path = "/images/deck-builder/" + heroClass.getDisplayName() + "-card-stand.png";
        if (!IMAGES.containsKey(path)) {
            IMAGES.put(path, new Image(DeckHolderView.class.getResourceAsStream(path)));
        }
        return IMAGES.get(path);
    }

    /** e.g. /images/deck-builder/Samurai-deck-holder-side.png; each picture is loaded only once. */
    private static Image image(HeroClass heroClass, String suffix) {
        String path = "/images/deck-builder/" + heroClass.getDisplayName() + "-deck-holder" + suffix + ".png";
        if (!IMAGES.containsKey(path)) {
            IMAGES.put(path, new Image(DeckHolderView.class.getResourceAsStream(path)));
        }
        return IMAGES.get(path);
    }
}