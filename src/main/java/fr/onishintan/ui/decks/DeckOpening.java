package fr.onishintan.ui.decks;

import fr.onishintan.ui.app.Images;
import fr.onishintan.model.HeroClass;
import fr.onishintan.ui.app.ScreenGrid;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * Opening a deck from the shelf, as one timeline: the spine slides out of its row, glides to the middle of the
 * screen growing, turns to show the holder's front, the lid snaps open, the whole deck stack rises out of the box,
 * glides to the centre and flips over. Then the cards spread out (DeckReveal).
 * close() plays the same timeline backwards, so the deck goes back exactly where it came from.
 */
final class DeckOpening {
    // When each step ends, counted from the click.
    private static final Duration PULLED_OUT = Duration.millis(600);    // slow pull out of the shelf
    private static final Duration AT_CENTRE = Duration.millis(1100);
    private static final Duration SIDE_GONE = Duration.millis(1250);
    private static final Duration FRONT_SHOWN = Duration.millis(1400);
    private static final Duration LID_OPEN = Duration.millis(1650);
    private static final Duration STACK_OUT = Duration.millis(2050);
    private static final Duration CLIP_OFF = Duration.millis(2060);
    private static final Duration STACK_CENTRE = Duration.millis(2450);
    private static final Duration FLIPPED = Duration.millis(2650);

    /** In the middle of the screen, a holder picture is drawn BOX_W wide with its top-left corner at (BOX_X, BOX_Y). */
    private static final double BOX_W = 300, BOX_X = ScreenGrid.W / 2 - BOX_W / 2, BOX_Y = 280;
    /** From image pixels to grid pixels, at that size. */
    private static final double K = BOX_W / DeckHolderView.IMAGE_W;
    /** The row of the open holder's front edge in its image: the stack comes out from behind it. */
    private static final double RIM_ROW = 250;
    private static final double RIM_Y = BOX_Y + RIM_ROW * K;
    /** deck-stack.png is 1052 x 1494; the stack is drawn STACK_W wide inside the box. */
    private static final double STACK_W = 190, STACK_H = STACK_W * 1494 / 1052;
    /** How much the stack grows in the middle of the screen. */
    private static final double CENTRE_SCALE = 1.6;
    /** On click the spine slides further out of the shelf: this many times its hover distance, before gliding away. */
    private static final double PULL = 2.5;
    /** On click the spine also keeps growing, to this many times its shelf size, as it comes further towards you. */
    private static final double PULL_SCALE = 1.15;

    private final Pane layer;
    private final Group pieces = new Group();
    private final Timeline timeline;

    /** Prepares the opening on `layer` (the 1600 x 900 grid) for the spine at (spineX, spineY), spineW x spineH on its shelf. */
    DeckOpening(Pane layer, HeroClass heroClass, double spineX, double spineY, double spineW, double spineH) {
        this.layer = layer;

        // 1. The spine, exactly as it looks under the mouse: slid out by (OUT_DX, OUT_DY) and grown by OUT_SCALE
        //    around its centre, so its top-left corner moves by half of what it grew.
        double outW = spineW * DeckHolderView.OUT_SCALE;
        double outH = spineH * DeckHolderView.OUT_SCALE;
        double outX = spineX + DeckHolderView.OUT_DX - (outW - spineW) / 2;
        double outY = spineY + DeckHolderView.OUT_DY - (outH - spineH) / 2;
        // On click it comes further out the same way: PULL times as far, and PULL_SCALE times its shelf size.
        double pulledW = spineW * PULL_SCALE;
        double pulledH = spineH * PULL_SCALE;
        double pulledX = spineX + DeckHolderView.OUT_DX * PULL - (pulledW - spineW) / 2;
        double pulledY = spineY + DeckHolderView.OUT_DY * PULL - (pulledH - spineH) / 2;

        ImageView side = DeckHolderView.side(heroClass, outW, outH);
        side.relocate(outX, outY);
        side.setEffect(DeckHolderView.OUT_SHADOW);

        // 2. The closed front and the open box, in the middle of the screen, hidden for now.
        ImageView front = DeckHolderView.front(heroClass, BOX_W);
        front.relocate(BOX_X, BOX_Y);
        front.setScaleX(0);
        ImageView open = DeckHolderView.open(heroClass, BOX_W);
        open.relocate(BOX_X, BOX_Y);
        open.setOpacity(0);

        // 3. The stack, sitting inside the box. It is drawn in front of the box but cut off (clipped) below the box's
        //    front edge, so it seems to come out from behind the front. Once it is out, the clip is lifted.
        ImageView stack = stackImage();
        stack.relocate(ScreenGrid.W / 2 - STACK_W / 2, RIM_Y);
        Rectangle clip = new Rectangle(0, 0, ScreenGrid.W, RIM_Y);
        Group stackLayer = new Group(stack);
        stackLayer.setClip(clip);

        pieces.getChildren().addAll(side, front, open, stackLayer);
        pieces.setMouseTransparent(true);

        // Where things travel.
        // In the middle, the spine takes its real proportions, exactly where it sits inside the big front picture.
        Rectangle2D area = DeckHolderView.spineArea(heroClass);
        double sideCentreX = BOX_X + area.getMinX() * K;
        double sideCentreY = BOX_Y + area.getMinY() * K;
        double sideCentreW = area.getWidth() * K;
        double sideCentreH = area.getHeight() * K;
        double rise = -(STACK_H + 10);                              // until its bottom edge is above the box's front
        double toScreenCentre = ScreenGrid.H / 2 - (RIM_Y + STACK_H / 2);

        timeline = new Timeline(
                // At the click: everything as built above. close() ends here too.
                new KeyFrame(Duration.ZERO,
                        new KeyValue(side.layoutXProperty(), outX),
                        new KeyValue(side.layoutYProperty(), outY),
                        new KeyValue(side.fitWidthProperty(), outW),
                        new KeyValue(side.fitHeightProperty(), outH),
                        new KeyValue(side.scaleXProperty(), 1),
                        new KeyValue(front.scaleXProperty(), 0),
                        new KeyValue(front.opacityProperty(), 1),
                        new KeyValue(open.opacityProperty(), 0),
                        new KeyValue(stack.translateYProperty(), 0),
                        new KeyValue(stack.scaleXProperty(), 1),
                        new KeyValue(stack.scaleYProperty(), 1),
                        new KeyValue(clip.heightProperty(), RIM_Y)),
                // 1. Slides further out of the shelf, the same way (down and to the left).
                new KeyFrame(PULLED_OUT,
                        new KeyValue(side.layoutXProperty(), pulledX),
                        new KeyValue(side.layoutYProperty(), pulledY),
                        new KeyValue(side.fitWidthProperty(), pulledW),
                        new KeyValue(side.fitHeightProperty(), pulledH)),
                // 2. Glides to the middle of the screen, growing.
                new KeyFrame(AT_CENTRE,
                        new KeyValue(side.layoutXProperty(), sideCentreX, Interpolator.EASE_BOTH),
                        new KeyValue(side.layoutYProperty(), sideCentreY, Interpolator.EASE_BOTH),
                        new KeyValue(side.fitWidthProperty(), sideCentreW, Interpolator.EASE_BOTH),
                        new KeyValue(side.fitHeightProperty(), sideCentreH, Interpolator.EASE_BOTH),
                        new KeyValue(side.scaleXProperty(), 1)),
                // 3. It turns: the spine narrows to nothing...
                new KeyFrame(SIDE_GONE,
                        new KeyValue(side.scaleXProperty(), 0),
                        new KeyValue(front.scaleXProperty(), 0)),
                // ... and the closed front widens.
                new KeyFrame(FRONT_SHOWN,
                        new KeyValue(front.scaleXProperty(), 1),
                        new KeyValue(front.opacityProperty(), 1),
                        new KeyValue(open.opacityProperty(), 0)),
                // 4. The lid snaps open: the open picture replaces the closed one at once (DISCRETE = no fade).
                new KeyFrame(LID_OPEN,
                        new KeyValue(front.opacityProperty(), 0, Interpolator.DISCRETE),
                        new KeyValue(open.opacityProperty(), 1, Interpolator.DISCRETE),
                        new KeyValue(stack.translateYProperty(), 0),
                        new KeyValue(clip.heightProperty(), RIM_Y)),
                // 5. The whole stack rises out of the box.
                new KeyFrame(STACK_OUT,
                        new KeyValue(stack.translateYProperty(), rise),
                        new KeyValue(stack.scaleXProperty(), 1),
                        new KeyValue(stack.scaleYProperty(), 1),
                        new KeyValue(open.opacityProperty(), 1),
                        new KeyValue(clip.heightProperty(), RIM_Y)),
                // It is out of the box: nothing to hide any more.
                new KeyFrame(CLIP_OFF,
                        new KeyValue(clip.heightProperty(), ScreenGrid.H, Interpolator.DISCRETE)),
                // 6. It glides to the centre and grows, while the empty box fades away.
                new KeyFrame(STACK_CENTRE,
                        new KeyValue(stack.translateYProperty(), toScreenCentre, Interpolator.EASE_BOTH),
                        new KeyValue(stack.scaleXProperty(), CENTRE_SCALE),
                        new KeyValue(stack.scaleYProperty(), CENTRE_SCALE),
                        new KeyValue(open.opacityProperty(), 0)),
                // 7. It flips over (it narrows to nothing, seen edge-on); the cards then spread out from there.
                new KeyFrame(FLIPPED,
                        new KeyValue(stack.scaleXProperty(), 0)));
    }

    /** Plays the opening; onOpened runs once the stack has flipped. */
    void open(Runnable onOpened) {
        layer.getChildren().add(pieces);
        timeline.setRate(1);
        timeline.setOnFinished(event -> onOpened.run());
        timeline.playFromStart();
    }

    /** Plays everything backwards, back onto the shelf; then removes the pieces and runs onClosed. */
    void close(Runnable onClosed) {
        timeline.setOnFinished(event -> {
            layer.getChildren().remove(pieces);
            onClosed.run();
        });
        timeline.jumpTo(timeline.getTotalDuration());   // start from the end...
        timeline.setRate(-1);                           // ... and run towards the beginning
        timeline.play();
    }

    /** Removes the pieces at once, without the backwards animation (used for Delete). */
    void remove() {
        timeline.stop();
        layer.getChildren().remove(pieces);
    }

    /** The deck-stack picture (a pile of card backs), STACK_W wide. */
    private static ImageView stackImage() {
        Image image = Images.get("/images/deck-builder/deck-stack.png");
        ImageView stack = new ImageView(image);
        stack.setFitWidth(STACK_W);
        stack.setPreserveRatio(true);
        return stack;
    }
}