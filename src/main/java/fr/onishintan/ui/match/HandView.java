package fr.onishintan.ui.match;
import fr.onishintan.ui.fx.Anim;
import fr.onishintan.ui.cards.CardView;

import fr.onishintan.model.BoardSide;
import fr.onishintan.model.Card;
import fr.onishintan.model.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.input.MouseEvent;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.scene.image.ImageView;
import javafx.scene.Node;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/** Your hand fanned at the bottom of the board (grid coordinates). Hover raises and spreads it,
 *  hovering a card shows it big, dragging a playable card onto your row plays it. */
public class HandView extends Group {
    private static final Rectangle2D DECK = new Rectangle2D(BoardView.DECK_X, BoardView.DECK_Y, 115, 127);
    public static final double CARD_W = 120, GAP = 70, RAISED_GAP = 105;

    private final Player player;
    private final Consumer<Card> onTrade;
    private final BiConsumer<Card, Integer> onPlay;
    private final List<CardView> cards = new ArrayList<>();
    private final List<Node> glows = new ArrayList<>();
    private final IntConsumer onHover;
    private CardView hovered;
    private final PauseTransition delay = new PauseTransition(Duration.millis(500));
    private CardView big;

    public HandView(Player player, BiConsumer<Card, Integer> onPlay, Consumer<Card> onTrade, IntConsumer onHover) {
        this.player = player;
        this.onPlay = onPlay;
        this.onHover = onHover;
        this.onTrade = onTrade;
        for (Card c : player.getHand()) {
            CardView v = new CardView(c, CARD_W);
            // Every card can be dragged: onto the deck to trade it (free, even when you can't afford it),
            // or onto the board to play it (the engine refuses it if you can't pay). The pink glow shows what you can afford.
            boolean affordable = player.canAfford(c.getKiCost());
            v.setPlayable(affordable);
            drag(v);
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
        setOnMouseExited(e -> {showBig(null);hovered = null; fan(false, true); onHover.accept(0); });
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
            showBig(null);
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
            else if (DECK.contains(p)) onTrade.accept(v.getCard());
            else fan(true, true);
        });
    }

    /** While dragging: a soft pink glow on every free slot of your row, and pink spirit motes over your deck (drop there to trade). */
    private void showSlots(boolean on) {
        getChildren().removeAll(glows);
        glows.clear();
        if (!on) return;
        for (int i = 0; i < BoardSide.MAX; i++)
            if (free(i)) glows.add(glow(BoardView.SLOT_X + i * BoardView.SLOT_STEP, BoardView.YOUR_Y, 118, 160));
        for (int i = 0; i < 20; i++) glows.add(mote(DECK.getMinX() + Math.random() * DECK.getWidth(), DECK.getMaxY() - Math.random() * DECK.getHeight()));
        getChildren().addAll(0, glows);
    }

    private static Rectangle glow(double x, double y, double w, double h) {
        Rectangle r = new Rectangle(x, y, w, h);
        r.setArcWidth(12);
        r.setArcHeight(12);
        r.setFill(Color.HOTPINK.deriveColor(0, 1, 1, 0.12));
        r.setStroke(Color.HOTPINK.deriveColor(0, 1, 1, 0.5));
        r.setEffect(new DropShadow(15, Color.HOTPINK));
        r.setMouseTransparent(true);
        return r;
    }

    /** A pink spirit mote rising and fading from (x, y), over and over until it is removed. */
    private static Circle mote(double x, double y) {
        Circle c = new Circle(x, y, 1.5 + Math.random() * 2.5, Color.web("#ff8ac4"));
        c.setEffect(new DropShadow(8, Color.HOTPINK));
        c.setMouseTransparent(true);
        TranslateTransition rise = new TranslateTransition(Duration.seconds(1.2 + Math.random()), c);
        rise.setFromY(0);
        rise.setToY(-50);
        FadeTransition fade = new FadeTransition(rise.getDuration(), c);
        fade.setFromValue(1);
        fade.setToValue(0);
        ParallelTransition t = new ParallelTransition(rise, fade);
        t.setCycleCount(Animation.INDEFINITE);
        t.playFrom(Duration.seconds(Math.random() * 2));
        c.sceneProperty().addListener((o, was, now) -> { if (now == null) t.stop(); });
        return c;
    }

    /** Whether slot i of your row is empty. The board has 7 fixed slots: a minion stays where it was dropped. */
    private boolean free(int i) {
        return player.getSide().isFree(i);
    }

    /** The hovered card is chosen by the mouse's x only: the whole column above and below a card counts as that card,
     *  so a lifted card never "escapes" the mouse and flickers. */
    private void pick(MouseEvent e) {
        if (cards.isEmpty()) return;
        double x = sceneToLocal(e.getSceneX(), e.getSceneY()).getX();
        int i = (int) Math.round((x - BoardView.W / 2) / RAISED_GAP + (cards.size() - 1) / 2.0);
        CardView v = cards.get(Math.max(0, Math.min(cards.size() - 1, i)));
        if (v == hovered) return;
        hovered = v;
        showBig(null);
        delay.setOnFinished(f -> showBig(v));
        delay.playFromStart();
        fan(true, true);
        onHover.accept(v.getCard().getKiCost());
    }

    /** The big readable copy of a hovered card, just above it; null hides it (and cancels the wait). */
    private void showBig(CardView v) {
        getChildren().remove(big);
        big = null;
        if (v == null) { delay.stop(); return; }
        big = new CardView(v.getCard(), 280);
        big.setMouseTransparent(true);
        big.relocate(v.getLayoutX() + CARD_W / 2 - 140, 200);
        getChildren().add(big);
    }

        /** Opening hand: every card in the hand flies in from the deck, one after the other. */
    public void deal() {
        int firstCard = 0;
        dealFrom(firstCard, Duration.ZERO);
    }

    /** Start of turn: the last `count` cards of the hand (the ones just drawn) fly in from the deck, one after the other. */
    public void dealLastCards(int count) {
        int firstNewCard = cards.size() - count;
        dealFrom(firstNewCard, Duration.ZERO);
    }

    /** Same as dealLastCard(), but the card waits on the deck first (a trade waits for the shuffle). */
    public void dealLastCardAfter(Duration wait) {
        int lastCard = cards.size() - 1;
        dealFrom(lastCard, wait);
    }

    /**
     * Makes the cards fly in from the deck, from index firstIndex to the end of the hand, after waiting `wait`.
     * Each card leaves the deck face down, flips halfway, and lands face up in its place in the fan.
     */
    private void dealFrom(int firstIndex, Duration wait) {
        // While cards are flying, the hand ignores the mouse, so it doesn't fan out in the middle of the deal.
        setMouseTransparent(true);

        for (int i = firstIndex; i < cards.size(); i++) {
            CardView card = cards.get(i);

            // 1. Where the card must land: its place in the fan.
            double endX = card.getLayoutX();
            double endY = card.getLayoutY();

            // 2. The halfway point between the deck and that place: this is where the card flips.
            double midX = (BoardView.DECK_X + endX) / 2;
            double midY = (BoardView.DECK_Y + endY) / 2;

            // 3. A card back on the deck: it is what you see during the first half of the flight.
            ImageView back = BoardView.image("/images/frames/card-back.png");
            back.setFitWidth(CARD_W);
            back.setPreserveRatio(true);
            back.relocate(BoardView.DECK_X, BoardView.DECK_Y);
            getChildren().add(back);

            // 4. The real card starts on the deck too, squashed to zero width so you can't see it.
            card.relocate(BoardView.DECK_X, BoardView.DECK_Y);
            card.setScaleX(0);

            // 5. First half (0 to 200 ms): the back flies to the halfway point and narrows to nothing (seen edge-on).
            //    The card travels with it, still invisible.
            KeyFrame halfway = new KeyFrame(Duration.millis(200),
                    new KeyValue(back.layoutXProperty(), midX),
                    new KeyValue(back.layoutYProperty(), midY),
                    new KeyValue(back.scaleXProperty(), 0),
                    new KeyValue(card.layoutXProperty(), midX),
                    new KeyValue(card.layoutYProperty(), midY),
                    new KeyValue(card.scaleXProperty(), 0));

            // 6. Second half (200 to 400 ms): the face widens back to full size and lands in its place.
            KeyFrame landed = new KeyFrame(Duration.millis(400),
                    new KeyValue(card.layoutXProperty(), endX),
                    new KeyValue(card.layoutYProperty(), endY),
                    new KeyValue(card.scaleXProperty(), 1));

            Timeline flight = new Timeline(halfway, landed);

            // 7. Each card leaves 300 ms after the one before it.
            int positionInDeal = i - firstIndex;
            flight.setDelay(wait.add(Duration.millis(positionInDeal * 300)));

            // 8. Once the card has landed, remove its back. After the last card, the hand listens to the mouse again.
            boolean isLastCard = (i == cards.size() - 1);
            flight.setOnFinished(event -> {
                getChildren().remove(back);
                if (isLastCard) {
                    setMouseTransparent(false);
                }
            });

            flight.play();
        }
    }
}