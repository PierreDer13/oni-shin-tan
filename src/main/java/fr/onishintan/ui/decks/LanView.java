package fr.onishintan.ui.decks;

import fr.onishintan.data.CardRepository;
import fr.onishintan.model.Card;
import fr.onishintan.model.Deck;
import fr.onishintan.model.Hero;
import fr.onishintan.model.HeroClass;
import fr.onishintan.model.Player;
import fr.onishintan.ui.OniShinTanApp;
import fr.onishintan.ui.app.Profile;
import fr.onishintan.ui.app.SceneRouter;
import fr.onishintan.ui.app.Screen;
import fr.onishintan.ui.app.ScreenGrid;
import fr.onishintan.ui.lan.LanLink;
import fr.onishintan.ui.screens.BoardPicker;
import fr.onishintan.ui.screens.HomeView;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * LAN PvP, laid over the home screen: you choose your deck, then host a game (and the battlefield) or join one by its address.
 * The joiner sends his deck (HELLO); the host answers with the seed, who plays first, the board and his own deck (START);
 * both games then build the same match from the same cards and the same seed.
 */
public class LanView extends Pane {
    private static final double W = ScreenGrid.W, H = ScreenGrid.H;

    private final SceneRouter router;
    private final Pane grid = new Pane();
    private SavedDeck deck;
    private String board;
    /** While hosting: the socket waiting for a player (closed on RETURN). */
    private ServerSocket hosting;
    /** Once connected: the link to the other game. */
    private LanLink link;

    public LanView(SceneRouter router) {
        this.router = router;
        getChildren().add(grid);
        showDecks();
    }

    /** Step 1: your deck. */
    private void showDecks() {
        page("LAN: choose your deck");
        FlowPane choices = new FlowPane(40, 40);
        choices.setAlignment(Pos.TOP_CENTER);
        choices.setPrefWrapLength(W - 200);
        choices.relocate(100, 170);
        for (SavedDeck saved : DeckPickView.decksToOffer()) {
            choices.getChildren().add(DeckPickView.choice(saved, () -> {
                deck = saved;
                showRole();
            }));
        }
        grid.getChildren().add(choices);
    }

    /** Step 2: host a game, or join one. */
    private void showRole() {
        page("LAN: host or join");
        HBox buttons = new HBox(60, DecksView.button("Host a game", this::showBoards), DecksView.button("Join a game", this::showJoin));
        buttons.setAlignment(Pos.CENTER);
        buttons.setPrefWidth(W);
        buttons.relocate(0, 400);
        grid.getChildren().add(buttons);
    }

    /** Host, step 3: the battlefield, then wait for a player. */
    private void showBoards() {
        page("Choose the battlefield");
        GridPane boards = BoardPicker.grid(chosen -> {
            board = chosen;
            host();
        });
        boards.setPrefWidth(W);
        boards.relocate(0, 150);
        grid.getChildren().add(boards);
    }

    /** Host, step 4: listens for one player and shows the address to give him. */
    private void host() {
        String addresses = String.join("  or  ", LanLink.localAddresses());
        message("Waiting for a player…\n\nYour address:  " + addresses);
        hosting = LanLink.host(joined -> {
            link = joined;
            link.setOnClosed(() -> message("The other player left."));
            link.setOnMessage(this::hostHears);
        }, this::message);
    }

    /** Host: the joiner's deck arrives; the host picks the seed and who plays first, answers with START, and the match begins. */
    private void hostHears(String line) {
        if (!line.startsWith("HELLO ")) {
            return;
        }
        String[] words = line.split(" ", 4);   // HELLO class ids name (the name may hold spaces: it comes last)
        Random coin = new Random();
        long seed = coin.nextLong();
        boolean hostFirst = coin.nextBoolean();
        link.send("START " + seed + " " + hostFirst + " " + board + " " + yourDeck());
        start(seed, hostFirst, words[1], words[2], words[3]);
    }

    /** Join, step 3: type the host's address. */
    private void showJoin() {
        page("Join a game");
        TextField address = new TextField();
        address.setPromptText("192.168.1.12");
        address.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: 24; -fx-alignment: center;");
        address.setMaxWidth(360);
        Label join = DecksView.button("Join", () -> {
            message("Joining " + address.getText().trim() + "…");
            LanLink.join(address.getText().trim(), this::joined, this::message);
        });
        VBox box = new VBox(24, DecksView.text("The host's address:", 24, DecksView.PAPER), address, join);
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(W);
        box.relocate(0, 340);
        grid.getChildren().add(box);
    }

    /** Joined: sends your deck and waits for the host's START. */
    private void joined(LanLink joinedLink) {
        link = joinedLink;
        link.setOnClosed(() -> message("The host left."));
        link.setOnMessage(line -> {
            if (!line.startsWith("START ")) {
                return;
            }
            String[] words = line.split(" ", 7);   // START seed hostFirst board class ids name
            board = words[3];
            start(Long.parseLong(words[1]), !Boolean.parseBoolean(words[2]), words[4], words[5], words[6]);
        });
        link.send("HELLO " + yourDeck());
    }

    /** Both games: the same two players from the same cards, the same seed; the match listens to the link from now on. */
    private void start(long seed, boolean youFirst, String theirClass, String theirCards, String theirName) {
        link.setOnMessage(null);   // lines wait until the match listens
        Player you = player(Profile.name(), deck.getHeroClass().name(), ids(deck));
        Player them = player(theirName, theirClass, theirCards);
        Player first = them;
        Player second = you;
        if (youFirst) {
            first = you;
            second = them;
        }
        Player firstPlayer = first;
        Player secondPlayer = second;
        LanLink match = link;
        link = null;   // the match owns it now: RETURN no longer closes it
        router.register(Screen.MATCH, () -> OniShinTanApp.lan(board, firstPlayer, secondPlayer, you, match, seed,
                won -> router.goTo(Screen.HOME)));
        router.goTo(Screen.MATCH);
    }

    /** Your deck as the other game reads it: class, card ids, name. */
    private String yourDeck() {
        return deck.getHeroClass().name() + " " + ids(deck) + " " + Profile.name();
    }

    /** The deck's card ids, in order, joined by commas. */
    private static String ids(SavedDeck saved) {
        List<String> ids = new ArrayList<>();
        for (Card card : saved.getCards()) {
            ids.add(card.getId());
        }
        return String.join(",", ids);
    }

    /** A player built from card ids, the same way on both computers (so both engines hold the very same cards). */
    private static Player player(String name, String heroClass, String cardIds) {
        List<Card> cards = new ArrayList<>();
        for (String id : cardIds.split(",")) {
            cards.add(CardRepository.byId(id));
        }
        return new Player(name, new Hero(HeroClass.valueOf(heroClass)), new Deck(cards));
    }

    /** A page with only a message in the middle (waiting, an error). */
    private void message(String text) {
        page("LAN");
        Label label = DecksView.text(text, 26, DecksView.PAPER);
        label.setStyle(label.getStyle() + " -fx-text-alignment: center;");
        HBox row = new HBox(label);
        row.setAlignment(Pos.CENTER);
        row.setPrefWidth(W);
        row.relocate(0, 380);
        grid.getChildren().add(row);
    }

    /** Clears the panel and draws its frame: a dark veil over the home screen, the title, and RETURN. */
    private void page(String title) {
        grid.getChildren().clear();
        Rectangle veil = new Rectangle(W, H, Color.rgb(0, 0, 0, 0.8));
        HBox titleRow = new HBox(DecksView.text(title, 40, DecksView.PAPER));
        titleRow.setAlignment(Pos.CENTER);
        titleRow.setPrefWidth(W);
        titleRow.relocate(0, 60);
        grid.getChildren().addAll(veil, titleRow, HomeView.back(this::close));
    }

    /** RETURN: stops hosting or the link, and takes the panel off the home screen. */
    private void close() {
        try {
            if (hosting != null) {
                hosting.close();
            }
        } catch (IOException e) {
            // already closed
        }
        if (link != null) {
            link.close();
        }
        ((Pane) getParent()).getChildren().remove(this);
    }
}
