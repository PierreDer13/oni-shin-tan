package fr.onishintan.ui.app;

import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;

/**
 * The tatami floor (images/deck-builder/inventory-tatami.png) as a background that repeats downwards:
 * set on a scrolling area's content, it scrolls with the cards and never runs out.
 */
public final class TatamiFloor {
    private static final String PATH = "/images/deck-builder/inventory-tatami.png";

    private TatamiFloor() {
        // Only a static helper: nobody creates a TatamiFloor.
    }

    /** One tatami picture per screen (1600 x 900), from the top-left corner, repeated downwards. */
    public static Background background() {
        BackgroundImage tile = new BackgroundImage(Images.get(PATH, ScreenGrid.W, ScreenGrid.H),
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT);
        return new Background(tile);
    }
}