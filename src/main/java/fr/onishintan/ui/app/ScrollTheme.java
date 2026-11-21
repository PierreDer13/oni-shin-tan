package fr.onishintan.ui.app;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;

/**
 * Themes a ScrollPane's vertical bar from Java, from three slices of the paper strip (scroll-ui/): the top ornament
 * on the upper end button, the plain paper repeated along the track, the bottom ornament on the lower end button;
 * the rolled scroll with its tassels (scroll-handle.png) is the handle. Nothing of the default grey or white bar shows.
 */
public final class ScrollTheme {
    /** Width of the themed bar; the slices are 64 px wide, the ornaments 72 (top) and 78 (bottom) px tall. */
    private static final double BAR_W = 36, TOP_H = BAR_W * 72 / 64, BOTTOM_H = BAR_W * 78 / 64;
    /** The dark wood of the shelves' side boards, behind the strip where the paper is narrower than the ornaments. */
    private static final String WOOD = "#24140b";
    private static final String CLEAR = "-fx-background-color: transparent; -fx-padding: 0;";
    private static final String TRACK = "-fx-background-color: transparent; -fx-background-image: url('/images/scroll-ui/scroll-middle.png');"
            + " -fx-background-size: 100% auto; -fx-background-repeat: repeat-y;";
    private static final String HANDLE = "-fx-background-color: transparent; -fx-background-image: url('/images/scroll-ui/scroll-handle.png');"
            + " -fx-background-size: contain; -fx-background-repeat: no-repeat;";
    private static final String TOP = end("scroll-top", TOP_H), BOTTOM = end("scroll-bottom", BOTTOM_H);

    private ScrollTheme() {
        // Only a static helper: nobody creates a ScrollTheme.
    }

    /** Call once on a new ScrollPane: its vertical bar is themed as soon as JavaFX has built it. */
    public static void apply(ScrollPane scroll) {
        scroll.skinProperty().addListener((property, oldSkin, newSkin) -> {
            for (Node node : scroll.lookupAll(".scroll-bar")) {
                if (node instanceof ScrollBar bar && bar.getOrientation() == Orientation.VERTICAL) {
                    themeWhenBuilt(bar);
                }
            }
        });
    }

    /** The bar's own parts (track, handle, end buttons) exist only once its skin is made: style them then. */
    private static void themeWhenBuilt(ScrollBar bar) {
        bar.setPrefWidth(BAR_W);
        bar.setStyle("-fx-background-color: " + WOOD + "; -fx-padding: 0;");   // dark wood behind the strip: no window white beside it
        if (bar.getSkin() != null) {
            theme(bar);
            return;
        }
        bar.skinProperty().addListener((property, oldSkin, newSkin) -> theme(bar));
    }

    /** Pictures on the ends, the track and the handle; the default backgrounds and arrows are cleared. */
    private static void theme(ScrollBar bar) {
        style(bar, ".track-background", CLEAR);
        style(bar, ".track", TRACK);
        // The handle's picture follows the scroll: at the very top when scrolled to the top, at the very bottom at the end.
        bar.valueProperty().addListener((property, oldValue, newValue) -> placeHandle(bar));
        bar.maxProperty().addListener((property, oldValue, newValue) -> placeHandle(bar));
        placeHandle(bar);
        style(bar, ".decrement-button", TOP);
        style(bar, ".increment-button", BOTTOM);
        style(bar, ".decrement-arrow", CLEAR);
        style(bar, ".increment-arrow", CLEAR);
    }

    /**
     * The handle (JavaFX's "thumb") is often taller than its picture, so a centred picture never reached the ends of the bar.
     * Now the picture sits at the same height in the handle as the scroll is in the list: 0% (top) at the top, 100% (bottom) at the end.
     */
    private static void placeHandle(ScrollBar bar) {
        double range = bar.getMax() - bar.getMin();
        double percent = 0;
        if (range > 0) {
            percent = (bar.getValue() - bar.getMin()) / range * 100;
        }
        style(bar, ".thumb", HANDLE + " -fx-background-position: center " + percent + "%;");
    }

    /** Gives every part of the bar matching `part` this style (nothing happens if JavaFX has no such part). */
    private static void style(ScrollBar bar, String part, String style) {
        for (Node node : bar.lookupAll(part)) {
            node.setStyle(style);
        }
    }

    /** The style of an end button: the ornament picture filling it, `height` tall (its arrow is gone). */
    private static String end(String picture, double height) {
        return "-fx-background-color: transparent; -fx-background-image: url('/images/scroll-ui/" + picture + ".png');"
                + " -fx-background-size: 100% 100%; -fx-padding: " + height / 2 + " 0 " + height / 2 + " 0;";
    }
}
