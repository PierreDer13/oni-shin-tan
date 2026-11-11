package fr.onishintan.ui.decks;

import fr.onishintan.model.HeroClass;
import fr.onishintan.ui.app.ScreenGrid;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

/**
 * A deck holder opening: the lid folds back, the whole deck stack rises out of the box, glides to the middle
 * of the screen while growing, then flips over. Then onOpened runs (DecksView shows the cards).
 * It plays on its own copy of the holder, drawn on top of the screen, so it can leave the scrolling list.
 */
final class DeckOpening {
    // When each step ends, counted from the click.
    private static final Duration LID_OPEN = Duration.millis(250);
    private static final Duration STACK_OUT = Duration.millis(650);
    private static final Duration AT_CENTRE = Duration.millis(1050);
    private static final Duration FLIPPED = Duration.millis(1250);

    /** deck-stack.png is 1052 x 1494. */
    private static final double STACK_IMAGE_W = 1052, STACK_IMAGE_H = 1494;
    /** How much the stack grows in the middle of the screen. */
    private static final double CENTRE_SCALE = 1.6;

    private DeckOpening() {
        // Only static methods.
    }

    /** Plays on `layer` (the 1600 x 900 grid) for a holder of this class whose top-left corner is at (x, y). */
    static void play(Pane layer, HeroClass heroClass, double x, double y, Runnable onOpened) {
        double width = DeckHolderView.WIDTH;
        double lidH = DeckHolderView.lidHeight(width);

        // The pieces, back to front: the stack (hidden inside the box at first), the box, the lid.
        double stackW = width * 0.8;
        double stackH = stackW * STACK_IMAGE_H / STACK_IMAGE_W;
        double stackX = (width - stackW) / 2;
        double stackY = lidH + 10;
        ImageView stack = stackImage(stackW);
        stack.relocate(stackX, stackY);

        ImageView body = DeckHolderView.body(heroClass, width);
        body.setLayoutY(lidH);

        ImageView lid = DeckHolderView.lid(heroClass, width);
        Scale fold = new Scale(1, 1, 0, 0);   // squashes the lid upwards, pinned at its top edge: it looks like it tilts back
        lid.getTransforms().add(fold);

        Pane box = new Pane(stack, body, lid);
        box.relocate(x, y);
        box.setMouseTransparent(true);
        layer.getChildren().add(box);

        // How far the stack travels: first straight up out of the box, then to the centre of the screen.
        double rise = -(stackH + 20);
        double toCentreX = ScreenGrid.W / 2 - (x + stackX + stackW / 2);
        double toCentreY = ScreenGrid.H / 2 - (y + stackY + stackH / 2);

        Timeline opening = new Timeline(
                // 1. The lid folds back. The stack waits inside the box.
                new KeyFrame(LID_OPEN,
                        new KeyValue(fold.yProperty(), 0.15),
                        new KeyValue(stack.translateYProperty(), 0)),
                // 2. The whole stack rises out of the box at once.
                new KeyFrame(STACK_OUT,
                        new KeyValue(stack.translateYProperty(), rise),
                        new KeyValue(stack.translateXProperty(), 0),
                        new KeyValue(stack.scaleXProperty(), 1),
                        new KeyValue(stack.scaleYProperty(), 1),
                        new KeyValue(body.opacityProperty(), 1),
                        new KeyValue(lid.opacityProperty(), 1)),
                // 3. It glides to the middle of the screen and grows, while the empty box fades away.
                new KeyFrame(AT_CENTRE,
                        new KeyValue(stack.translateXProperty(), toCentreX),
                        new KeyValue(stack.translateYProperty(), toCentreY),
                        new KeyValue(stack.scaleXProperty(), CENTRE_SCALE),
                        new KeyValue(stack.scaleYProperty(), CENTRE_SCALE),
                        new KeyValue(body.opacityProperty(), 0),
                        new KeyValue(lid.opacityProperty(), 0)),
                // 4. It flips over: it narrows to nothing, as if seen edge-on. The cards then open up from there.
                new KeyFrame(FLIPPED,
                        new KeyValue(stack.scaleXProperty(), 0)));
        opening.setOnFinished(event -> {
            layer.getChildren().remove(box);
            onOpened.run();
        });
        opening.play();
    }

    /** The deck-stack picture (a pile of card backs), `width` wide. */
    private static ImageView stackImage(double width) {
        Image image = new Image(DeckOpening.class.getResourceAsStream("/images/deck-builder/deck-stack.png"));
        ImageView stack = new ImageView(image);
        stack.setFitWidth(width);
        stack.setPreserveRatio(true);
        return stack;
    }
}