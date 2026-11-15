package fr.onishintan.ui.decks;

import fr.onishintan.model.Deck;
import fr.onishintan.model.Hero;
import fr.onishintan.model.HeroClass;
import fr.onishintan.model.Player;
import fr.onishintan.ui.MockCards;
import fr.onishintan.ui.OniShinTanApp;
import fr.onishintan.ui.app.Images;
import fr.onishintan.ui.app.SceneRouter;
import fr.onishintan.ui.app.Screen;
import fr.onishintan.ui.app.ScreenGrid;
import fr.onishintan.ui.screens.BoardPicker;
import fr.onishintan.ui.screens.HomeView;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * PvP, before the match: Player 1, then Player 2, chooses the deck they play with.
 * Every legal saved deck is offered, plus a starter deck of each class (MockCards), so a match can start before any deck is built.
 * Once both have chosen, they pick the battlefield (BoardPicker) and the match starts there.
 */
public class DeckPickView extends Pane {
    private static final double W = ScreenGrid.W, H = ScreenGrid.H;
    private static final double HOLDER_W = 170;
    private static final HeroClass[] CLASSES = { HeroClass.SAMURAI, HeroClass.NINJA, HeroClass.KANNUSHI, HeroClass.ONI };

    private final SceneRouter router;
    /** Everything is placed on this 1600 x 900 grid, which is scaled to fit the window. */
    private final Pane grid = new Pane();
    /** Player 1's deck, or null while Player 1 is still choosing. */
    private SavedDeck firstPick;
    /** Player 2's deck, or null while Player 2 is still choosing. */
    private SavedDeck secondPick;

    public DeckPickView(SceneRouter router) {
        this.router = router;
        ScreenGrid.fitToWindow(this, grid);
        showChoice("Player 1");
    }

    /** Draws the screen for this player: the page with its title, then one deck holder per deck to choose from. */
    private void showChoice(String playerName) {
        page(playerName + ", choose your deck");

        FlowPane choices = new FlowPane(40, 40);
        choices.setAlignment(Pos.TOP_CENTER);
        choices.setPrefWrapLength(W - 200);
        choices.setPrefWidth(W - 200);
        choices.relocate(100, 170);
        for (SavedDeck deck : decksToOffer()) {
            choices.getChildren().add(choice(deck));
        }
        grid.getChildren().add(choices);
    }

    /** After both decks: the page asks for the battlefield, with one picture per board (BoardPicker). */
    private void showBoards() {
        page("Choose the battlefield");

        GridPane boards = BoardPicker.grid(this::startMatch);
        boards.setPrefWidth(W);
        boards.relocate(0, 150);
        grid.getChildren().add(boards);
    }

    /** Clears the screen and draws its frame: the shelves under a dark veil, the title at the top, and RETURN. */
    private void page(String title) {
        grid.getChildren().clear();

        ImageView shelves = new ImageView(Images.get("/images/deck-builder/shelves.png", W, H));
        Rectangle veil = new Rectangle(W, H, Color.rgb(0, 0, 0, 0.55));

        Label titleLabel = DecksView.text(title, 40, DecksView.PAPER);
        HBox titleRow = new HBox(titleLabel);
        titleRow.setAlignment(Pos.CENTER);
        titleRow.setPrefWidth(W);
        titleRow.relocate(0, 60);

        Label back = HomeView.back(() -> router.goTo(Screen.HOME));
        grid.getChildren().addAll(shelves, veil, titleRow, back);
    }

    /** The legal saved decks, then one starter deck per class. */
    private List<SavedDeck> decksToOffer() {
        List<SavedDeck> decks = new ArrayList<>();
        for (SavedDeck deck : DeckStore.all()) {
            if (deck.isLegal()) {
                decks.add(deck);
            }
        }
        for (HeroClass heroClass : CLASSES) {
            SavedDeck starter = new SavedDeck("Starter " + heroClass.getDisplayName(), heroClass);
            starter.setCards(MockCards.deckCards(heroClass));
            decks.add(starter);
        }
        return decks;
    }

    /** One deck to choose: its holder seen from the front, its name, then its class and size. Clicking it chooses it. */
    private VBox choice(SavedDeck deck) {
        ImageView holder = DeckHolderView.front(deck.getHeroClass(), HOLDER_W);
        Label name = DecksView.text(deck.getName(), 20, DecksView.PAPER);
        String size = deck.getHeroClass().getDisplayName() + " · " + deck.getCards().size() + " cards";
        Label details = DecksView.text(size, 15, "#d9cfbd");

        VBox choice = new VBox(6, holder, name, details);
        choice.setAlignment(Pos.TOP_CENTER);
        choice.setPrefWidth(HOLDER_W + 20);
        HomeView.clickable(choice, false, () -> choose(deck));
        return choice;
    }

    /** Player 1's choice is kept and Player 2 chooses; after Player 2's choice comes the battlefield. */
    private void choose(SavedDeck deck) {
        if (firstPick == null) {
            firstPick = deck;
            showChoice("Player 2");
            return;
        }
        secondPick = deck;
        showBoards();
    }

    /** Starts the match on this board with both chosen decks; at the end, back to Home. */
    private void startMatch(String board) {
        SavedDeck firstDeck = firstPick;
        SavedDeck secondDeck = secondPick;
        router.register(Screen.MATCH, () -> OniShinTanApp.match(board, player("Player 1", firstDeck), player("Player 2", secondDeck),
                won -> router.goTo(Screen.HOME)));
        router.goTo(Screen.MATCH);
    }

    /** A player for the match, with the deck's class as hero and a copy of its cards. */
    private static Player player(String name, SavedDeck deck) {
        HeroClass heroClass = deck.getHeroClass();
        return new Player(name, new Hero(heroClass), new Deck(deck.getCards()));
    }
}
