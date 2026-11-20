package fr.onishintan.ui.cards;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

/**
 * The black line under a card's art, opened just wide enough for the card's English name.
 * The art files stay untouched: the opening is cut in a copy of the art, once per card, then reused.
 */
final class NameGap {
    /** The middle of the card (530 wide), where the name sits. */
    static final double CENTRE_X = 265;
    /** Where the line is looked for: between these rows, on its two outer parts (x 60 to 140 and 390 to 470). */
    private static final int TOP = 228, BOTTOM = 262;
    /** How far above and below its middle row the line is cleared, and where the paper that refills it is taken from. */
    private static final int HALF_THICKNESS = 3, PAPER_ABOVE = 8;

    /** A card's art with its opened line, and the row of that line (the name is centred on it). */
    record Opened(Image image, int lineY) {}

    private static final Map<String, Opened> DONE = new HashMap<>();

    private NameGap() {
        // Only static helpers: nobody creates a NameGap.
    }

    /** The art of card `id` with its line opened over `width` around CENTRE_X (cut the first time, then reused). */
    static Opened open(String id, Image art, double width) {
        return DONE.computeIfAbsent(id, key -> cut(art, width));
    }

    /** Copies the art, then refills the line over `width` with the paper just above it. */
    private static Opened cut(Image art, double width) {
        PixelReader in = art.getPixelReader();
        int line = lineY(in);
        WritableImage out = new WritableImage(in, (int) art.getWidth(), (int) art.getHeight());
        PixelWriter pen = out.getPixelWriter();
        int from = (int) (CENTRE_X - width / 2);
        int to = (int) (CENTRE_X + width / 2);
        for (int x = from; x < to; x++) {
            for (int y = line - HALF_THICKNESS; y <= line + HALF_THICKNESS; y++) {
                pen.setArgb(x, y, in.getArgb(x, y - PAPER_ABOVE));
            }
        }
        return new Opened(out, line);
    }

    /** The line's row: the darkest row of its two outer parts (it is not at the same height on every card). */
    private static int lineY(PixelReader in) {
        int best = TOP;
        double darkest = Double.MAX_VALUE;
        for (int y = TOP; y <= BOTTOM; y++) {
            double sum = 0;
            for (int x = 60; x < 140; x++) {
                sum += in.getColor(x, y).getBrightness() + in.getColor(x + 330, y).getBrightness();
            }
            if (sum < darkest) {
                darkest = sum;
                best = y;
            }
        }
        return best;
    }
}
