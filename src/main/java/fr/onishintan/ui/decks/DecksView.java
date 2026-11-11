package fr.onishintan.ui.decks;

import fr.onishintan.model.HeroClass;
import fr.onishintan.ui.app.SceneRouter;
import fr.onishintan.ui.app.Screen;
import fr.onishintan.ui.app.ScreenGrid;
import fr.onishintan.ui.screens.HomeView;

import java.util.List;
import java.util.Map;

import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Deck Builder, screen 1: each saved deck is shown as its class's deck holder. Click one to open it: the lid opens,
 * the stack comes out and flips, and its cards spread out. A "+ New deck" slot makes a new deck.
 * Opened from the Home bottom bar (every deck) or from a class banner (only that class's decks).
 */
public class DecksView extends Pane {
    public static final double W = ScreenGrid.W, H = ScreenGrid.H;
    private static final double GAP = 40;
    private static final int PER_ROW = 5;

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
    /** The scrolling area with the holders; rebuilt after a deck is deleted. */
    private ScrollPane list;

    /** Shows the deck list: onlyClass = a class to show only its decks, or null for every deck. */
    public static void open(SceneRouter router, HeroClass onlyClass) {
        router.register(Screen.DECK_BUILDER, () -> new DecksView(router, onlyClass));
        router.goTo(Screen.DECK_BUILDER);
    }

    private DecksView(SceneRouter router, HeroClass onlyClass) {
        this.router = router;
        this.onlyClass = onlyClass;
        ScreenGrid.fitToWindow(this, grid);

        // Plain washi-paper background, until the painted one exists.
        Rectangle paper = new Rectangle(W, H, Color.web(PAPER));
        grid.getChildren().add(paper);

        grid.getChildren().add(HomeView.back(() -> router.goTo(Screen.HOME)));
        grid.getChildren().add(title());

        list = holderList();
        grid.getChildren().add(list);
    }

    /** "札 DECKS" at the top, or e.g. "侍 SAMURAI DECKS" when only one class is shown. */
    private Label title() {
        String words = "札  DECKS";
        if (onlyClass != null) {
            words = onlyClass.getKanji() + "  " + onlyClass.getDisplayName().toUpperCase() + " DECKS";
        }
        Label title = text(words, 44, INK);
        title.setPrefWidth(W);
        title.setAlignment(Pos.CENTER);
        title.relocate(0, 50);
        title.setMouseTransparent(true);   // it spans the whole width: let clicks reach the RETURN button
        return title;
    }

    /** One holder per deck, then "+ New deck", in rows of PER_ROW that scroll when there are many. */
    private ScrollPane holderList() {
        FlowPane holders = new FlowPane(GAP, 30);
        holders.setPrefWrapLength(PER_ROW * DeckHolderView.WIDTH + (PER_ROW - 1) * GAP);
        holders.setPadding(new Insets(10));

        List<SavedDeck> decks;
        if (onlyClass == null) {
            decks = DeckStore.all();
        } else {
            decks = DeckStore.ofClass(onlyClass);
        }
        for (SavedDeck deck : decks) {
            DeckHolderView holder = new DeckHolderView(deck);
            holder.setOnOpen(() -> openDeck(deck, holder));
            holders.getChildren().add(holder);
        }
        holders.getChildren().add(newDeckSlot());

        ScrollPane scroll = new ScrollPane(holders);
        scroll.setPrefSize(PER_ROW * DeckHolderView.WIDTH + (PER_ROW - 1) * GAP + 40, 700);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scroll.relocate((W - scroll.getPrefWidth()) / 2, 150);
        return scroll;
    }

    /** Rebuilds the holders from DeckStore (after a deck was deleted). */
    private void refreshList() {
        grid.getChildren().remove(list);
        list = holderList();
        grid.getChildren().add(list);
    }

    /**
     * Click on a holder: the real one hides and its opening plays on top of the screen, then its cards are shown.
     * The list ignores clicks meanwhile, so only one deck opens at a time.
     */
    private void openDeck(SavedDeck deck, DeckHolderView holder) {
        Point2D corner = grid.sceneToLocal(holder.picture().localToScene(0, 0));   // where the holder is on the grid
        holder.setVisible(false);
        list.setMouseTransparent(true);
        DeckOpening.play(grid, deck.getHeroClass(), corner.getX(), corner.getY(), () -> showCards(deck, holder));
    }

    /** The opened deck's cards, with Edit, Delete and Close. */
    private void showCards(SavedDeck deck, DeckHolderView holder) {
        DeckReveal reveal = new DeckReveal(deck,
                () -> openEditor(deck),
                () -> {
                    backToList(holder);
                    confirmDelete(deck);
                },
                () -> backToList(holder));
        grid.getChildren().add(reveal);
    }

    /** The holder is back on the list and the list takes clicks again. */
    private void backToList(DeckHolderView holder) {
        holder.setVisible(true);
        list.setMouseTransparent(false);
    }

    /** The "+ New deck" slot, the size of a holder: makes a deck of the shown class, or asks which class when every deck is shown. */
    private StackPane newDeckSlot() {
        StackPane slot = new StackPane(text("+  New deck", 24, INK));
        slot.setPrefSize(DeckHolderView.WIDTH, DeckHolderView.height(DeckHolderView.WIDTH));
        slot.setStyle("-fx-border-color: " + INK + "; -fx-border-width: 3; -fx-border-style: dashed; -fx-border-radius: 10;");
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

    /** Asks before deleting: Yes deletes the deck and redraws the list, No just closes the question. */
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