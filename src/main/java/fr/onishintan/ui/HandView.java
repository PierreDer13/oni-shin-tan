package fr.onishintan.ui;

import fr.onishintan.model.BoardSide;
import fr.onishintan.model.Card;
import fr.onishintan.model.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.input.MouseEvent;

/** Your hand fanned at the bottom of the board (grid coordinates). Hover raises and spreads it,
 *  hovering a card shows it big, dragging a playable card onto your row plays it. */
public class HandView extends Group {
    public static final double CARD_W = 120, GAP = 70, RAISED_GAP = 105;

    private final Player player;
    private final BiConsumer<Card, Integer> onPlay;
    private final List<CardView> cards = new ArrayList<>();
    private final List<Rectangle> glows = new ArrayList<>();
    private final IntConsumer onHover;
    private CardView hovered;

    public HandView(Player player, BiConsumer<Card, Integer> onPlay, IntConsumer onHover) {
        this.player = player;
        this.onPlay = onPlay;
        this.onHover = onHover;
        for (Card c : player.getHand()) {
            CardView v = new CardView(c, CARD_W);
            boolean playable = player.canAfford(c.getKiCost());
            v.setPlayable(playable);
            if (playable) drag(v);
            cards.add(v);
        }
        getChildren().addAll(cards);
        double half = (cards.size() - 1) / 2.0 * RAISED_GAP + CARD_W / 2 + 10;
        Rectangle zone = new Rectangle(BoardView.W / 2 - half, 700, half * 2, 200);
        zone.setFill(Color.TRANSPARENT);
        zone.setPickOnBounds(true);
        getChildren().add(0, zone);
        setOnMouseEntered(e -> fan(true, true));
        setOnMouseMoved(this::pick);
        setOnMouseExited(e -> { hovered = null; fan(false, true); onHover.accept(0); });
        fan(false, false);
    }

    /** Places the cards on an arc centred on the board; raised = lifted and spread out; animate = glide there.
     *  The hovered card rises above the others, straightens up and grows. */
    private void fan(boolean raised, boolean animate) {
        double mid = (cards.size() - 1) / 2.0, gap = raised ? RAISED_GAP : GAP;
        for (int i = 0; i < cards.size(); i++) {
            CardView v = cards.get(i);
            boolean up = v == hovered;
            double d = i - mid, x = BoardView.W / 2 - CARD_W / 2 + d * gap;
            double y = up ? 640 : (raised ? 720 : 790) + d * d * 3, r = up ? 0 : d * 4, s = up ? 1.5 : 1;
            v.setViewOrder(up ? -1 : 0);
            if (animate) Anim.to(v, Anim.ease(v.layoutXProperty(), x), Anim.ease(v.layoutYProperty(), y),
                    Anim.ease(v.rotateProperty(), r), Anim.ease(v.scaleXProperty(), s), Anim.ease(v.scaleYProperty(), s));
            else { v.relocate(x, y); v.setRotate(r); v.setScaleX(s); v.setScaleY(s); }
        }
    }

    /** The card follows the mouse; released over your row, onPlay gets the card and the slot under the mouse. */
    private void drag(CardView v) {
        v.setOnMousePressed(e -> {
            Anim.stop(v);
            hovered = null;
            v.setRotate(0);
            v.setScaleX(1);
            v.setScaleY(1);
            v.setViewOrder(-2);
            showSlots(true);
        });
        v.setOnMouseDragged(e -> {
            Point2D p = sceneToLocal(e.getSceneX(), e.getSceneY());
            v.relocate(p.getX() - CARD_W / 2, p.getY() - CARD_W * 0.7);
        });

        v.setOnMouseReleased(e -> {
            showSlots(false);
            Point2D p = sceneToLocal(e.getSceneX(), e.getSceneY());
            int slot = (int) ((p.getX() - BoardView.SLOT_X) / BoardView.SLOT_STEP);
            if (p.getY() > BoardView.YOUR_Y && p.getY() < BoardView.YOUR_Y + 160 && slot >= 0 && slot < BoardSide.MAX && free(slot))
                onPlay.accept(v.getCard(), slot);
            else fan(true, true);
        });
    }

    /** While dragging: a soft pink glow on every free slot of your row. */
    private void showSlots(boolean on) {
        getChildren().removeAll(glows);
        glows.clear();
        if (!on) return;
        for (int i = 0; i < BoardSide.MAX; i++) {
            if (!free(i)) continue;
            Rectangle r = new Rectangle(BoardView.SLOT_X + i * BoardView.SLOT_STEP, BoardView.YOUR_Y, 118, 160);
            r.setArcWidth(12);
            r.setArcHeight(12);
            r.setFill(Color.HOTPINK.deriveColor(0, 1, 1, 0.12));
            r.setStroke(Color.HOTPINK.deriveColor(0, 1, 1, 0.5));
            r.setEffect(new DropShadow(15, Color.HOTPINK));
            r.setMouseTransparent(true);
            glows.add(r);
        }
        getChildren().addAll(0, glows);
    }

    /** Whether slot i of your row is empty. */
    private boolean free(int i) { return i >= player.getSide().size(); }

    /** The hovered card is chosen by the mouse's x only: the whole column above and below a card counts as that card,
     *  so a lifted card never "escapes" the mouse and flickers. */
    private void pick(MouseEvent e) {
        if (cards.isEmpty()) return;
        double x = sceneToLocal(e.getSceneX(), e.getSceneY()).getX();
        int i = (int) Math.round((x - BoardView.W / 2) / RAISED_GAP + (cards.size() - 1) / 2.0);
        CardView v = cards.get(Math.max(0, Math.min(cards.size() - 1, i)));
        if (v == hovered) return;
        hovered = v;
        fan(true, true);
        onHover.accept(v.getCard().getKiCost());
    }
}