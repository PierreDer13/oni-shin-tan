package fr.onishintan.ui.screens;

import fr.onishintan.model.Card;
import fr.onishintan.ui.MockCards;
import fr.onishintan.ui.app.Images;
import fr.onishintan.ui.app.SceneRouter;
import fr.onishintan.ui.app.Screen;
import fr.onishintan.ui.app.ScreenGrid;
import fr.onishintan.ui.cards.SleevedCard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.image.ImageView;

/**
 * Inventory: every card in the game as a grid you scroll with the mouse wheel.
 * Sorted by class, then Ki, then name. Home goes back to the home screen.
 */
public class InventoryView extends Pane {
    private static final String PAPER = "#f3ede1";
    private static final String TATAMI = "/images/deck-builder/inventory-tatami.png";
    private static final String INK = "#1f1b17";
    private static final String FONT = "-fx-font-family: 'Yu Mincho'; -fx-font-weight: bold;";

    private static final double CARD_W = 190;
    private static final double GAP = 24;
    private static final int PER_ROW = 7;
    /** Pixels moved per mouse-wheel notch (JavaFX's default of about 40 is too slow for big cards). */
    private static final double SCROLL_SPEED = 3;

    private final Pane grid = new Pane();

    public InventoryView(SceneRouter router) {
        ScreenGrid.fitToWindow(this, grid);

        // Tatami floor over the whole page (the picture has the grid's shape, 1672 x 941).
        ImageView tatami = new ImageView(Images.get(TATAMI, ScreenGrid.W, ScreenGrid.H));
        grid.getChildren().add(tatami);

        List<Card> cards = allCards();

        // Top bar: Home on the left, the card count next to it.
        Label home = button("Home");
        HomeView.clickable(home, false, () -> router.goTo(Screen.HOME));
        home.relocate(30, 36);
        Label count = text(cards.size() + " cards", 26, INK);
        count.relocate(150, 34);
        grid.getChildren().addAll(home, count);

        // The cards, in a scrolling area under the top bar.
        ScrollPane scroll = cardArea(cards);
        scroll.relocate(30, 110);
        grid.getChildren().add(scroll);
    }

    /**
     * Every card to show, sorted by class, then Ki, then name.
     * Placeholder: the test cards from MockCards, until the player's own collection is saved.
     */
    private List<Card> allCards() {
        List<Card> cards = new ArrayList<>(MockCards.ALL);
        Comparator<Card> byClass = Comparator.comparing(Card::getHeroClass);
        Comparator<Card> byKi = Comparator.comparingInt(Card::getKiCost);
        Comparator<Card> byName = Comparator.comparing(Card::getNameRomaji);
        cards.sort(byClass.thenComparing(byKi).thenComparing(byName));
        return cards;
    }

    /** The cards as a centred grid inside a vertical-only ScrollPane. */
    private ScrollPane cardArea(List<Card> cards) {
        FlowPane cardGrid = new FlowPane(GAP, GAP);
        cardGrid.setPrefWrapLength(PER_ROW * CARD_W + (PER_ROW - 1) * GAP);
        cardGrid.setAlignment(Pos.TOP_CENTER);
        cardGrid.setPadding(new Insets(10));
        for (Card card : cards) {
            cardGrid.getChildren().add(new SleevedCard(card, CARD_W));
        }

        ScrollPane scroll = new ScrollPane(cardGrid);
        scroll.setPrefSize(ScreenGrid.W - 60, ScreenGrid.H - 130);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scroll.addEventFilter(ScrollEvent.SCROLL, event -> scrollFaster(scroll, cardGrid, event));
        return scroll;
    }

    /** Moves the grid by SCROLL_SPEED times the wheel's distance, instead of JavaFX's slow default. */
    private void scrollFaster(ScrollPane scroll, FlowPane cardGrid, ScrollEvent event) {
        double hiddenHeight = cardGrid.getHeight() - scroll.getViewportBounds().getHeight();
        if (hiddenHeight <= 0) {
            return;   // everything fits: nothing to scroll
        }
        double move = event.getDeltaY() * SCROLL_SPEED / hiddenHeight;
        double newValue = scroll.getVvalue() - move;
        newValue = Math.max(0, Math.min(1, newValue));
        scroll.setVvalue(newValue);
        event.consume();
    }

    /** A bold Yu Mincho label of this size and colour. */
    private static Label text(String words, int size, String colour) {
        Label label = new Label(words);
        label.setStyle(FONT + " -fx-font-size: " + size + "; -fx-text-fill: " + colour + ";");
        return label;
    }

    /** A dark rounded button with light text (same look as the Deck Builder's buttons). */
    private static Label button(String words) {
        Label button = text(words, 18, PAPER);
        button.setStyle(button.getStyle() + " -fx-background-color: rgba(20,12,8,0.85); -fx-background-radius: 8; -fx-padding: 6 14;");
        return button;
    }
}
