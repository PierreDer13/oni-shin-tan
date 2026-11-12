package fr.onishintan.ui.decks;

import fr.onishintan.ui.app.ScreenGrid;
import fr.onishintan.ui.cards.CardView;
import fr.onishintan.ui.fx.Anim;

import javafx.animation.PauseTransition;
import javafx.geometry.Bounds;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * The deck editor's hover effect, like in a match: the hovered card grows a little with a shadow,
 * and after a short wait a big, readable copy opens beside it. Leaving the card puts everything back.
 * The big copy is drawn on `layer` (the editor's 1600 x 900 grid), so the collection's scroll area never cuts it.
 */
class CardPreview {
    /** Width of the big copy (the match's preview is 280). */
    private static final double BIG_W = 300;
    /** Gap between the hovered card and its big copy, and from the screen's edges. */
    private static final double MARGIN = 16;
    /** How much the hovered card grows. */
    private static final double HOVER_SCALE = 1.08;
    /** How long the mouse must stay on a card before the big copy opens. */
    private static final Duration WAIT = Duration.millis(400);
    private static final DropShadow HOVER_SHADOW = new DropShadow(16, 0, 6, Color.rgb(0, 0, 0, 0.5));

    private final Pane layer;
    private final PauseTransition wait = new PauseTransition(WAIT);
    /** The big copy on screen, or null when there is none. */
    private CardView big;

    CardPreview(Pane layer) {
        this.layer = layer;
    }

    /** Gives `view` the hover effect: it grows on enter, its big copy opens after WAIT, everything goes back on exit. */
    void attach(CardView view) {
        view.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
            grow(view);
            wait.setOnFinished(done -> showBig(view));
            wait.playFromStart();
        });
        view.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            shrink(view);
            hideBig();
        });
    }

    /** The hovered card: a little bigger, with a shadow, drawn above its neighbours. */
    private void grow(CardView view) {
        view.setViewOrder(-1);
        view.setEffect(HOVER_SHADOW);
        Anim.to(view, Anim.ease(view.scaleXProperty(), HOVER_SCALE), Anim.ease(view.scaleYProperty(), HOVER_SCALE));
    }

    /** Back to its normal size, without the shadow. */
    private void shrink(CardView view) {
        view.setViewOrder(0);
        view.setEffect(null);
        Anim.to(view, Anim.ease(view.scaleXProperty(), 1.0), Anim.ease(view.scaleYProperty(), 1.0));
    }

    /** Opens the big copy right of the card, or left of it when there is no room on the right; always inside the screen. */
    private void showBig(CardView view) {
        hideBig();
        Bounds card = layer.sceneToLocal(view.localToScene(view.getBoundsInLocal()));
        double bigH = BIG_W * CardView.H / CardView.W;

        double x = card.getMaxX() + MARGIN;
        if (x + BIG_W > ScreenGrid.W - MARGIN) {
            x = card.getMinX() - MARGIN - BIG_W;
        }
        double y = card.getCenterY() - bigH / 2;
        y = Math.max(MARGIN, Math.min(ScreenGrid.H - MARGIN - bigH, y));

        big = new CardView(view.getCard(), BIG_W);
        big.setMouseTransparent(true);
        big.setViewOrder(-10);
        big.relocate(x, y);
        layer.getChildren().add(big);
    }

    /** Closes the big copy, and cancels one still waiting to open. */
    private void hideBig() {
        wait.stop();
        if (big != null) {
            layer.getChildren().remove(big);
            big = null;
        }
    }
}
