package fr.onishintan.ui.decks;

import fr.onishintan.model.Deck;
import fr.onishintan.model.HeroClass;
import fr.onishintan.ui.app.SceneRouter;
import fr.onishintan.ui.app.Screen;
import fr.onishintan.ui.screens.HomeView;

import java.util.List;
import java.util.Map;

import javafx.beans.InvalidationListener;
import javafx.geometry.Insets;
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
import javafx.scene.transform.Scale;

/**
 * Deck Builder, screen 1: the player's decks as tiles, each with Edit and Delete, plus a "+ New deck" tile.
 * Opened from the Home bottom bar (every deck) or from a class banner (only that class's decks).
 */
public class DecksView extends Pane {
    public static final double W = 1600, H = 900;
    private static final double TILE_W = 300, TILE_H = 170, GAP = 40;

    /** Each class's main ink colour (GDD, card layout), used for tile borders and class buttons. */
    static final Map<HeroClass, String> CLASS_COLOUR = Map.of(
            HeroClass.SAMURAI, "#7d1a1e",
            HeroClass.NINJA, "#1c2b4f",
            HeroClass.KANNUSHI, "#8a76b4",
            HeroClass.ONI, "#141110",
            HeroClass.NEUTRAL, "#2e5b3a");

    private static final String PAPER = "#f3ede1";
    private static final String INK = "#1f1b17";
    private static final String FONT = "-fx-font-family: 'Yu Mincho'; -fx-font-weight: bold;";

    private final SceneRouter router;
    /** The class whose decks are shown, or null to show every deck. */
    private final HeroClass onlyClass;
    /** Everything is placed on this 1600 x 900 grid, which is scaled to fit the window. */
    private final Pane grid = new Pane();
    /** The scrolling area with the tiles; rebuilt after a deck is deleted. */
    private ScrollPane list;

    /** Shows the deck list: onlyClass = a class to show only its decks, or null for every deck. */
    public static void open(SceneRouter router, HeroClass onlyClass) {
        router.register(Screen.DECK_BUILDER, () -> new DecksView(router, onlyClass));
        router.goTo(Screen.DECK_BUILDER);
    }

    private DecksView(SceneRouter router, HeroClass onlyClass) {
        this.router = router;
        this.onlyClass = onlyClass;
        scaleGridToWindow();

        // Plain washi-paper background, until the painted one exists.
        Rectangle paper = new Rectangle(W, H, Color.web(PAPER));
        grid.getChildren().add(paper);

        grid.getChildren().add(HomeView.back(() -> router.goTo(Screen.HOME)));
        grid.getChildren().add(title());

        list = tileList();
        grid.getChildren().add(list);
    }

    /** Keeps the 1600 x 900 grid as big as the window allows, without stretching it. */
    private void scaleGridToWindow() {
        Scale scale = new Scale();
        grid.getTransforms().add(scale);
        InvalidationListener fit = change -> {
            double factor = Math.min(getWidth() / W, getHeight() / H);
            scale.setX(factor);
            scale.setY(factor);
        };
        widthProperty().addListener(fit);
        heightProperty().addListener(fit);
        getChildren().add(grid);
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

    /** One tile per deck, then "+ New deck", in rows of four that scroll when there are many. */
    private ScrollPane tileList() {
        FlowPane tiles = new FlowPane(GAP, 30);
        tiles.setPrefWrapLength(TILE_W * 4 + GAP * 3);
        tiles.setPadding(new Insets(10));

        List<SavedDeck> decks;
        if (onlyClass == null) {
            decks = DeckStore.all();
        } else {
            decks = DeckStore.ofClass(onlyClass);
        }
        for (SavedDeck deck : decks) {
            tiles.getChildren().add(deckTile(deck));
        }
        tiles.getChildren().add(newDeckTile());

        ScrollPane scroll = new ScrollPane(tiles);
        scroll.setPrefSize(TILE_W * 4 + GAP * 3 + 40, 680);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scroll.relocate((W - scroll.getPrefWidth()) / 2, 160);
        return scroll;
    }

    /** Rebuilds the tiles from DeckStore (after a deck was deleted). */
    private void refreshList() {
        grid.getChildren().remove(list);
        list = tileList();
        grid.getChildren().add(list);
    }

    /** One deck: its name, class, card count, whether it can be played, and its Edit and Delete buttons. */
    private VBox deckTile(SavedDeck deck) {
        HeroClass heroClass = deck.getHeroClass();
        Label name = text(deck.getName(), 26, INK);
        Label classLine = text(heroClass.getKanji() + "  " + heroClass.getDisplayName(), 18, CLASS_COLOUR.get(heroClass));
        Label count = text(deck.getCards().size() + " / " + Deck.SIZE + " cards", 18, INK);

        // A legal deck says so in green; otherwise its first problem is shown in red.
        Label status;
        if (deck.isLegal()) {
            status = text("✓ Ready to play", 16, "#2e7d32");
        } else {
            status = text("✗ " + deck.problems().get(0), 16, "#c41e1e");
        }
        status.setWrapText(true);
        status.setMaxWidth(TILE_W - 28);

        Label edit = button("Edit", () -> openEditor(deck));
        Label delete = button("Delete", () -> confirmDelete(deck));
        HBox buttons = new HBox(12, edit, delete);

        VBox tile = new VBox(6, name, classLine, count, status, buttons);
        tile.setPrefSize(TILE_W, TILE_H);
        tile.setPadding(new Insets(14));
        tile.setStyle("-fx-background-color: #fbf8f1; -fx-background-radius: 10; -fx-border-radius: 10;"
                + " -fx-border-width: 4; -fx-border-color: " + CLASS_COLOUR.get(heroClass) + ";");
        return tile;
    }

    /** The "+ New deck" tile: makes a deck of the shown class, or asks which class when every deck is shown. */
    private StackPane newDeckTile() {
        StackPane tile = new StackPane(text("+  New deck", 26, INK));
        tile.setPrefSize(TILE_W, TILE_H);
        tile.setStyle("-fx-border-color: " + INK + "; -fx-border-width: 3; -fx-border-style: dashed; -fx-border-radius: 10;");
        HomeView.clickable(tile, false, () -> {
            if (onlyClass == null) {
                askClass();
            } else {
                createDeck(onlyClass);
            }
        });
        return tile;
    }

    /** Makes an empty deck of this class (named "Samurai deck 2" etc.), stores it, and opens it in the editor. */
    private void createDeck(HeroClass heroClass) {
        int number = DeckStore.ofClass(heroClass).size() + 1;
        SavedDeck deck = new SavedDeck(heroClass.getDisplayName() + " deck " + number, heroClass);
        DeckStore.add(deck);
        openEditor(deck);
    }

    /** Opens a deck in the editor. The editor is step 3: for now this shows the router's placeholder screen. */
    private void openEditor(SavedDeck deck) {
        router.goTo(Screen.DECK_EDITOR);
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

    /** Asks before deleting: Yes deletes the deck and redraws the tiles, No just closes the question. */
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
    private static Label text(String words, int size, String colour) {
        Label label = new Label(words);
        label.setStyle(FONT + " -fx-font-size: " + size + "; -fx-text-fill: " + colour + ";");
        return label;
    }

    /** A dark rounded button with light text, with the usual hover grow and click sound. */
    private static Label button(String words, Runnable action) {
        Label button = text(words, 18, PAPER);
        button.setStyle(button.getStyle() + " -fx-background-color: rgba(20,12,8,0.85); -fx-background-radius: 8; -fx-padding: 6 14;");
        HomeView.clickable(button, false, action);
        return button;
    }
}