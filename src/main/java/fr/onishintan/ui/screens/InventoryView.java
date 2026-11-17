package fr.onishintan.ui.screens;

import fr.onishintan.model.Card;
import fr.onishintan.ui.MockCards;
import fr.onishintan.ui.app.SceneRouter;
import fr.onishintan.ui.app.Screen;
import fr.onishintan.ui.app.ScreenGrid;
import fr.onishintan.ui.app.ScrollTheme;
import fr.onishintan.ui.cards.CardShelves;
import fr.onishintan.ui.cards.CardPreview;
import fr.onishintan.ui.cards.StandingCard;
import fr.onishintan.ui.decks.CardFilter;
import fr.onishintan.ui.decks.ClassFilterBar;
import fr.onishintan.ui.decks.FilterBar;
import fr.onishintan.ui.decks.FilterBanner;
import fr.onishintan.ui.decks.SorobanFilter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

/**
 * Inventory: every card in the game standing on wooden shelves you scroll with the mouse wheel, each class on its own shelves.
 * Sorted by class, then Ki, then name. The top bar filters them (search, Ki, type like the deck editor, plus class).
 * The BACK sign goes back to the home screen.
 */
public class InventoryView extends Pane {
    private static final String PAPER = "#f3ede1";
    private static final String FONT = "-fx-font-family: 'Yu Mincho'; -fx-font-weight: bold;";
    /** Height of the top bar: the first shelf starts under it. */
    private static final double TOP_BAR_H = 150;
    /** 5 cards per shelf, across the whole width between the side boards. */
    private static final int PER_SHELF = 5;
    private static final double AREA_LEFT = 60, AREA_W = 1480;
    /** Pixels moved per mouse-wheel notch (JavaFX's default of about 40 is too slow for big cards). */
    private static final double SCROLL_SPEED = 3;
    /** How long the mouse stays on a card before its big copy opens, and how far the hovered card rises. */
    private static final Duration PREVIEW_WAIT = Duration.millis(750);
    private static final double LIFT = 14;

    private final Pane grid = new Pane();
    private final List<Card> allCards = sortedCards();
    /** What the filter bars have picked: the shelves show only the cards that pass it. */
    private final CardFilter filter = new CardFilter();
    private final ScrollPane scroll = new ScrollPane();
    private final Label count = button("");
    /** The hover effect: the card grows and rises, and after PREVIEW_WAIT a big copy opens beside it (drawn on the whole grid). */
    private final CardPreview preview = new CardPreview(grid, PREVIEW_WAIT);

    public InventoryView(SceneRouter router) {
        ScreenGrid.fitToWindow(this, grid);

        // The cards stand on shelves that fill the whole page and scroll with them.
        scroll.setPrefSize(ScreenGrid.W, ScreenGrid.H);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scroll.addEventFilter(ScrollEvent.SCROLL, this::scrollFaster);
        ScrollTheme.apply(scroll);
        grid.getChildren().add(scroll);

        // Top bar: the BACK sign and the card count on the left, the two filter planks beside them.
        ImageView back = HomeView.back(() -> router.goTo(Screen.HOME));
        count.relocate(40, 96);
        FilterBar filters = new FilterBar(filter, this::showCards);
        filters.relocate(240, 26);
        ClassFilterBar classes = new ClassFilterBar(filter, this::showCards);
        classes.relocate(240, 86);
        SorobanFilter soroban = new SorobanFilter(filter, this::showCards, 420);
        soroban.relocate(1150, 5);
        grid.getChildren().addAll(FilterBanner.plank(ScreenGrid.W, TOP_BAR_H), back, count, filters, classes, soroban);

        showCards();
    }

    /**
     * Every card, sorted by class, then Ki, then name.
     * Placeholder: the test cards from MockCards, until the player's own collection is saved.
     */
    private static List<Card> sortedCards() {
        List<Card> cards = new ArrayList<>(MockCards.ALL);
        Comparator<Card> byClass = Comparator.comparing(Card::getHeroClass);
        Comparator<Card> byKi = Comparator.comparingInt(Card::getKiCost);
        Comparator<Card> byName = Comparator.comparing(Card::getNameRomaji);
        cards.sort(byClass.thenComparing(byKi).thenComparing(byName));
        return cards;
    }

    /** Puts the cards that pass the filters on fresh shelves, one class at a time, scrolls to the top and updates the count. */
    private void showCards() {
        List<Card> shown = new ArrayList<>();
        for (Card card : allCards) {
            if (filter.matches(card)) {
                shown.add(card);
            }
        }
        CardShelves shelves = new CardShelves(ScreenGrid.W, PER_SHELF, AREA_LEFT, AREA_W, TOP_BAR_H);
        for (StandingCard view : shelves.addByClass(shown)) {
            preview.attach(view.getSleeve(), view.getCard(), LIFT);   // only the card in its sleeve grows and rises
        }
        shelves.setMinHeight(ScreenGrid.H);   // the shelves fill the page even with few cards
        scroll.setContent(shelves);
        scroll.setVvalue(0);
        count.setText(shown.size() + " cards");
    }

    /** Moves the shelves by SCROLL_SPEED times the wheel's distance, instead of JavaFX's slow default. */
    private void scrollFaster(ScrollEvent event) {
        double contentHeight = scroll.getContent().getBoundsInLocal().getHeight();
        double hiddenHeight = contentHeight - scroll.getViewportBounds().getHeight();
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
