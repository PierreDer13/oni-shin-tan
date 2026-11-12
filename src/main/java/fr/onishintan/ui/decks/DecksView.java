package fr.onishintan.ui.decks;

import fr.onishintan.model.HeroClass;
import fr.onishintan.ui.app.SceneRouter;
import fr.onishintan.ui.app.Screen;
import fr.onishintan.ui.app.ScreenGrid;
import fr.onishintan.ui.screens.HomeView;

import java.util.List;
import java.util.Map;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Deck Builder, screen 1: the decks stand on wooden shelves as their holders' spines, like books.
 * Click one to open it (DeckOpening), and its cards spread out (DeckReveal). A dashed slot makes a new deck.
 * Opened from the Home bottom bar (every deck) or from a class banner (only that class's decks).
 */
public class DecksView extends Pane {
    public static final double W = ScreenGrid.W, H = ScreenGrid.H;
    /** Where a spine's bottom edge stands on each of the background's three shelves (grid y), top to bottom. */
    private static final double[] SHELF_TOPS = { 312, 588, 828 };
    /** How tall a deck in its stand is on each shelf (spine top to stand feet): as tall as that shelf has room for. */
    private static final double[] HEIGHTS = { 250, 205, 170 };
    /** Spines stand from LEFT_X to RIGHT_X (inside the shelves' side boards), GAP apart, starting at the left like books. */
    private static final double LEFT_X = 120, RIGHT_X = 1490, GAP = 10;

    /** Each class's main ink colour (GDD, card layout), used for class buttons and card stripes. */
    static final Map<HeroClass, String> CLASS_COLOUR = Map.of(
            HeroClass.SAMURAI, "#7d1a1e",
            HeroClass.NINJA, "#1c2b4f",
            HeroClass.KANNUSHI, "#8a76b4",
            HeroClass.ONI, "#141110",
            HeroClass.NEUTRAL, "#2e5b3a");

    static final String PAPER = "#f3ede1";
    static final String INK = "#1f1b17";
    private static final String FONT = "-fx-font-family: 'Yu Mincho'; -fx-font-weight: bold;";

    private final SceneRouter router;
    /** The class whose decks are shown, or null to show every deck. */
    private final HeroClass onlyClass;
    /** Everything is placed on this 1600 x 900 grid, which is scaled to fit the window. */
    private final Pane grid = new Pane();
    /** The spines standing on the shelves; rebuilt after a deck is deleted. */
    private Pane list;

    /** Shows the deck list: onlyClass = a class to show only its decks, or null for every deck. */
    public static void open(SceneRouter router, HeroClass onlyClass) {
        router.register(Screen.DECK_BUILDER, () -> new DecksView(router, onlyClass));
        router.goTo(Screen.DECK_BUILDER);
    }

    private DecksView(SceneRouter router, HeroClass onlyClass) {
        this.router = router;
        this.onlyClass = onlyClass;
        ScreenGrid.fitToWindow(this, grid);

        // The wooden shelves the decks stand on, stretched to the 1600 x 900 grid (the image is 1672 x 941, the same shape).
        ImageView shelves = new ImageView(new Image(getClass().getResourceAsStream("/images/deck-builder/shelves.png")));
        shelves.setFitWidth(W);
        shelves.setFitHeight(H);
        grid.getChildren().add(shelves);

        grid.getChildren().add(HomeView.back(() -> router.goTo(Screen.HOME)));

        list = spineList();
        grid.getChildren().add(list);
    }

    /**
     * The decks stand on the three shelves in their card stands, in creation order, left to right, then the "+ New deck" slot.
     * Each shelf has its own height (HEIGHTS); when a shelf is full, the next deck goes on the shelf below.
     * Anything that doesn't fit on the three shelves is not shown.
     */
    private Pane spineList() {
        List<SavedDeck> decks;
        if (onlyClass == null) {
            decks = DeckStore.all();
        } else {
            decks = DeckStore.ofClass(onlyClass);
        }

        Pane shelves = new Pane();
        int shelf = 0;
        double x = LEFT_X;
        // One place per deck, plus one more for the "+ New deck" slot.
        for (int i = 0; i <= decks.size(); i++) {
            double height = HEIGHTS[shelf];
            double width = DeckHolderView.width(height);

            // No room left on this shelf: go on to the next one (stop after the last shelf).
            if (x + width > RIGHT_X) {
                shelf++;
                if (shelf == SHELF_TOPS.length) {
                    break;
                }
                x = LEFT_X;
                height = HEIGHTS[shelf];
                width = DeckHolderView.width(height);
            }

            if (i < decks.size()) {
                SavedDeck deck = decks.get(i);
                DeckHolderView holder = new DeckHolderView(deck, height);
                holder.setOnOpen(() -> openDeck(deck, holder));
                holder.relocate(x, SHELF_TOPS[shelf] - height);   // its stand's feet on the shelf
                shelves.getChildren().add(holder);
            } else {
                double slotHeight = DeckHolderView.standHeight(height);   // as tall as an empty stand
                StackPane slot = newDeckSlot(width, slotHeight);
                slot.relocate(x, SHELF_TOPS[shelf] - slotHeight);
                shelves.getChildren().add(slot);
            }
            x += width + GAP;
        }
        return shelves;
    }

    /** Rebuilds the spines from DeckStore (after a deck was deleted). */
    private void refreshList() {
        grid.getChildren().remove(list);
        list = spineList();
        grid.getChildren().add(list);
    }

    /**
     * Click on a spine: the real one hides and the opening plays on top of the screen, then the cards are shown.
     * The shelves ignore clicks meanwhile, so only one deck opens at a time.
     */
    private void openDeck(SavedDeck deck, DeckHolderView holder) {
        // The stands' Pane sits at (0, 0) on the grid, so a stand's layout position is its position on the grid.
        double spineX = holder.getLayoutX() + holder.spineX();
        double spineY = holder.getLayoutY();   // the spine's top is the top of the whole
        DeckOpening opening = new DeckOpening(grid, deck.getHeroClass(), spineX, spineY,
                holder.spineWidth(), holder.spineHeight());
        holder.hideSpine();   // the empty stand stays on the shelf
        list.setMouseTransparent(true);
        opening.open(() -> showCards(deck, holder, opening));
    }

    /** The opened deck's cards, with Edit, Delete and Close. Close sends the deck back onto its shelf, in reverse. */
    private void showCards(SavedDeck deck, DeckHolderView holder, DeckOpening opening) {
        DeckReveal reveal = new DeckReveal(deck,
                () -> openEditor(deck),
                () -> {
                    opening.remove();
                    backToList(holder);
                    confirmDelete(deck);
                },
                () -> opening.close(() -> backToList(holder)));
        grid.getChildren().add(reveal);
    }

    /** The spine is back on its shelf and the shelves take clicks again. */
    private void backToList(DeckHolderView holder) {
        holder.showSpine();
        list.setMouseTransparent(false);
    }

    /** The "+ New deck" slot: a dashed spine-sized outline. Makes a deck of the shown class, or asks which class. */
    private StackPane newDeckSlot(double width, double height) {
        StackPane slot = new StackPane(text("+", 22, INK));
        slot.setPrefSize(width, height);
        slot.setStyle("-fx-border-color: " + INK + "; -fx-border-width: 2; -fx-border-style: dashed; -fx-border-radius: 6;");
        HomeView.clickable(slot, false, () -> {
            if (onlyClass == null) {
                askClass();
            } else {
                createDeck(onlyClass);
            }
        });
        return slot;
    }

    /** Makes an empty deck of this class (named "Samurai deck 2" etc.), stores it, and opens it in the editor. */
    private void createDeck(HeroClass heroClass) {
        int number = DeckStore.ofClass(heroClass).size() + 1;
        SavedDeck deck = new SavedDeck(heroClass.getDisplayName() + " deck " + number, heroClass);
        DeckStore.add(deck);
        openEditor(deck);
    }

    /** Opens a deck in the editor; Save or Cancel there comes back to this same list. */
    private void openEditor(SavedDeck deck) {
        DeckEditorView.open(router, deck, onlyClass);
    }

    /** A dark veil with the four classes to pick from; Cancel closes it. */
    private void askClass() {
        VBox box = new VBox(24);
        box.setAlignment(Pos.CENTER);
        box.getChildren().add(text("Which class is this deck for?", 32, PAPER));

        HBox choices = new HBox(20);
        choices.setAlignment(Pos.CENTER);
        HeroClass[] classes = { HeroClass.SAMURAI, HeroClass.NINJA, HeroClass.KANNUSHI, HeroClass.ONI };
        for (HeroClass heroClass : classes) {
            Label choice = button(heroClass.getKanji() + "  " + heroClass.getDisplayName(), () -> createDeck(heroClass));
            choice.setStyle(choice.getStyle() + " -fx-background-color: " + CLASS_COLOUR.get(heroClass) + ";");
            choices.getChildren().add(choice);
        }
        box.getChildren().add(choices);

        StackPane veil = veil(box);
        box.getChildren().add(button("Cancel", () -> grid.getChildren().remove(veil)));
        grid.getChildren().add(veil);
    }

    /** Asks before deleting: Yes deletes the deck and redraws the shelves, No just closes the question. */
    private void confirmDelete(SavedDeck deck) {
        VBox box = new VBox(24);
        box.setAlignment(Pos.CENTER);
        box.getChildren().add(text("Delete \"" + deck.getName() + "\"?", 32, PAPER));

        HBox answers = new HBox(20);
        answers.setAlignment(Pos.CENTER);
        box.getChildren().add(answers);

        StackPane veil = veil(box);
        answers.getChildren().add(button("Yes, delete", () -> {
            DeckStore.delete(deck);
            grid.getChildren().remove(veil);
            refreshList();
        }));
        answers.getChildren().add(button("No", () -> grid.getChildren().remove(veil)));
        grid.getChildren().add(veil);
    }

    /** A full-screen dark veil with `content` in its centre; it blocks clicks on everything under it. */
    private StackPane veil(Node content) {
        Rectangle dark = new Rectangle(W, H, Color.rgb(0, 0, 0, 0.8));
        StackPane veil = new StackPane(dark, content);
        veil.setPrefSize(W, H);
        return veil;
    }

    /** A bold Yu Mincho label of this size and colour. */
    static Label text(String words, int size, String colour) {
        Label label = new Label(words);
        label.setStyle(FONT + " -fx-font-size: " + size + "; -fx-text-fill: " + colour + ";");
        return label;
    }

    /** A dark rounded button with light text, with the usual hover grow and click sound. */
    static Label button(String words, Runnable action) {
        Label button = text(words, 18, PAPER);
        button.setStyle(button.getStyle() + " -fx-background-color: rgba(20,12,8,0.85); -fx-background-radius: 8; -fx-padding: 6 14;");
        HomeView.clickable(button, false, action);
        return button;
    }
}