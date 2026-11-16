package fr.onishintan.ui.decks;

import fr.onishintan.ui.app.Images;
import fr.onishintan.ui.app.Sound;
import fr.onishintan.ui.screens.HomeView;

import javafx.animation.TranslateTransition;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * The Ki filter as a soroban (images/deck-builder/soroban-*.png): one rod per cost, 1 to 6 and 7+.
 * Clicking a rod slides its four beads up to the beam (that Ki is picked); clicking again slides them back down.
 * Several costs can be picked; with none picked every cost shows.
 */
public class SorobanFilter extends Pane {
    private static final String FRAME = "/images/deck-builder/soroban-frame.png";
    private static final String BEAD = "/images/deck-builder/soroban-bead.png";
    private static final String BEADS = "/images/deck-builder/soroban-beads.png";
    /** The frame picture's height for its width (2158 x 712 in the original drawing). */
    private static final double FRAME_RATIO = 712.0 / 2158;
    /** Each rod's centre as a part of the frame's width, left to right: Ki 1 to 6, then 7+. */
    private static final double[] RODS = { 300.0 / 2158, 563.0 / 2158, 823.0 / 2158, 1081.0 / 2158, 1341.0 / 2158, 1603.0 / 2158, 1863.0 / 2158 };
    private static final int[] KI = { 1, 2, 3, 4, 5, 6, CardFilter.KI_OR_MORE };
    /** Distance between two rods, as a part of the frame's width (the click area of each rod). */
    private static final double ROD_STEP = 260.0 / 2158;
    /** Top of the upper opening, top and bottom of the lower opening, as parts of the frame's height. */
    private static final double UPPER_TOP = 97.0 / 712, LOWER_TOP = 242.0 / 712, LOWER_BOTTOM = 542.0 / 712;
    /** Bead width as a part of the frame's width. */
    private static final double BEAD_W = 110.0 / 2158;
    private static final Duration SLIDE = Duration.millis(180);
    /** The green glow on the beads of a picked Ki. */
    private static final DropShadow PICKED_GLOW = new DropShadow(BlurType.GAUSSIAN, Color.web("#4cff7a"), 14, 0.45, 0, 0);

    private final CardFilter filter;
    private final Runnable onChange;

    /** A soroban `width` wide that writes the picked costs into `filter`, then calls onChange. */
    public SorobanFilter(CardFilter filter, Runnable onChange, double width) {
        this.filter = filter;
        this.onChange = onChange;
        double height = width * FRAME_RATIO;
        ImageView frame = new ImageView(Images.get(FRAME, width, height));
        frame.setEffect(new DropShadow(18, 6, 8, Color.rgb(0, 0, 0, 0.6)));   // the soroban casts a shadow on the plank
        getChildren().add(frame);

        Image bead = Images.get(BEAD);
        Image beads = Images.get(BEADS);
        double beadW = width * BEAD_W;
        double beadH = beadW * bead.getHeight() / bead.getWidth();
        double beadsH = beadW * beads.getHeight() / beads.getWidth();
        double upperTop = height * UPPER_TOP;
        double lowerTop = height * LOWER_TOP;
        double lowerBottom = height * LOWER_BOTTOM;
        double rodStep = width * ROD_STEP;

        for (int i = 0; i < RODS.length; i++) {
            double centre = width * RODS[i];

            // The single bead rests against the top bar, the four beads on the bottom bar.
            ImageView top = bead(bead, beadW, beadH);
            top.relocate(centre - beadW / 2, upperTop);
            ImageView group = bead(beads, beadW, beadsH);
            double restY = lowerBottom - beadsH;
            group.relocate(centre - beadW / 2, restY);
            double lift = restY - lowerTop;   // how far the four beads slide up to reach the beam

            // A see-through strip over the whole rod catches the click.
            Rectangle rod = new Rectangle(rodStep, height, Color.TRANSPARENT);
            rod.relocate(centre - rodStep / 2, 0);
            int ki = KI[i];
            rod.setOnMousePressed(event -> toggle(ki, top, group, lift));

            getChildren().addAll(top, group, rod);
        }
        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(width, height);
    }

    /** Picks or unpicks this Ki: the four beads slide up and the rod's beads glow green (picked), or slide back down; a wooden click, then the cards are filtered again. */
    private void toggle(int ki, ImageView top, ImageView group, double lift) {
        boolean picked = filter.toggleKi(ki);
        TranslateTransition slide = new TranslateTransition(SLIDE, group);
        if (picked) {
            slide.setToY(-lift);
            top.setEffect(PICKED_GLOW);
            group.setEffect(PICKED_GLOW);
        } else {
            slide.setToY(0);
            top.setEffect(null);
            group.setEffect(null);
        }
        slide.play();
        Sound.play(HomeView.CLICK);
        onChange.run();
    }

    /** A bead picture at this size; clicks go through it to the rod. */
    private static ImageView bead(Image picture, double width, double height) {
        ImageView view = new ImageView(picture);
        view.setFitWidth(width);
        view.setFitHeight(height);
        view.setMouseTransparent(true);
        return view;
    }
}
