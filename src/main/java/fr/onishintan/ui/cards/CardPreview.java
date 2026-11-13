package fr.onishintan.ui.cards;

import fr.onishintan.model.Card;
import fr.onishintan.ui.app.ScreenGrid;
import fr.onishintan.ui.fx.Anim;

import javafx.animation.PauseTransition;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * The hover effect on cards outside a match (deck editor, deck lists): the hovered card or chip grows a little
 * with a shadow, and after a short wait a big, readable copy of the card opens beside it. Leaving puts everything back.
 * The big copy is drawn on `layer` (the screen's 1600 x 900 grid), so no scroll area ever cuts it.
 */
public class CardPreview {
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

    public CardPreview(Pane layer) {
        this.layer = layer;
    }

    /** Gives `node` (a card or a chip showing `card`) the hover effect: it grows, the big copy opens after WAIT, all goes back on exit. */
    public void attach(Node node, Card card) {
        node.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
            grow(node);
            wait.setOnFinished(done -> showBig(node, card));
            wait.playFromStart();
        });
        node.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            shrink(node);
            hideBig();
        });
    }

    /** The hovered node: a little bigger, with a shadow, drawn above its neighbours. */
    private void grow(Node node) {
        node.setViewOrder(-1);
        node.setEffect(HOVER_SHADOW);
        Anim.to(node, Anim.ease(node.scaleXProperty(), HOVER_SCALE), Anim.ease(node.scaleYProperty(), HOVER_SCALE));
    }

    /** Back to its normal size, without the shadow. */
    private void shrink(Node node) {
        node.setViewOrder(0);
        node.setEffect(null);
        Anim.to(node, Anim.ease(node.scaleXProperty(), 1.0), Anim.ease(node.scaleYProperty(), 1.0));
    }

    /** Opens the big copy right of the node, or left of it when there is no room on the right; always inside the screen. */
    private void showBig(Node node, Card card) {
        hideBig();
        Bounds hovered = layer.sceneToLocal(node.localToScene(node.getBoundsInLocal()));
        double bigH = BIG_W * CardView.H / CardView.W;

        double x = hovered.getMaxX() + MARGIN;
        if (x + BIG_W > ScreenGrid.W - MARGIN) {
            x = hovered.getMinX() - MARGIN - BIG_W;
        }
        double y = hovered.getCenterY() - bigH / 2;
        y = Math.max(MARGIN, Math.min(ScreenGrid.H - MARGIN - bigH, y));

        big = new CardView(card, BIG_W);
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


    /** Closes the big copy now: for a node about to disappear, like a deck list chip just clicked. */
    public void hide() {
        hideBig();
    }
}