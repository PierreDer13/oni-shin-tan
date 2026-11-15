package fr.onishintan.ui.decks;

import fr.onishintan.ui.app.Images;

import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;

/** The wooden plank (images/deck-builder/filter-banner.png) across the top of the Inventory and the deck editor, behind the filters. */
public final class FilterBanner {
    private static final String PLANK = "/images/deck-builder/filter-banner.png";

    private FilterBanner() {
        // Only a static helper: nobody creates a FilterBanner.
    }

    /** The plank stretched to width x height (place it at the top-left corner), with a shadow under it. Clicks go through it. */
    public static ImageView plank(double width, double height) {
        ImageView plank = new ImageView(Images.get(PLANK, width, height));
        plank.setMouseTransparent(true);
        plank.setEffect(new DropShadow(20, 0, 8, Color.rgb(0, 0, 0, 0.6)));
        return plank;
    }
}
