package fr.onishintan.ui.decks;

import fr.onishintan.data.CardRepository;
import fr.onishintan.model.Card;
import fr.onishintan.model.Deck;
import fr.onishintan.model.Hero;
import fr.onishintan.model.HeroClass;
import fr.onishintan.model.Player;
import fr.onishintan.ui.OniShinTanApp;
import fr.onishintan.ui.app.Images;
import fr.onishintan.ui.app.Profile;
import fr.onishintan.ui.app.SceneRouter;
import fr.onishintan.ui.app.Screen;
import fr.onishintan.ui.app.ScreenGrid;
import fr.onishintan.ui.lan.LanCode;
import fr.onishintan.ui.lan.LanLink;
import fr.onishintan.ui.screens.BoardPicker;
import fr.onishintan.ui.screens.HomeView;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * The PvP screen: host a game or join one on the local network (or play on the same screen).
 * The host picks best of 1 or best of 3, the battlefield and his decks, and gets a game code; the joiner types the code and picks
 * as many decks. In a best of 3 each deck is a different class and is played once: before each game, each player picks one he hasn't played.
 * Lines between the games: HELLO name (joiner), FORMAT bestOf board name (host), DECK class ids (joiner), START seed hostFirst class ids (host).
 */
public class LanView extends Pane {
    private static final double W = ScreenGrid.W, H = ScreenGrid.H;
    private static final DropShadow PICKED = new DropShadow(30, Color.GOLD);

    private final SceneRouter router;
    private final Pane grid = new Pane();
    private boolean host;
    /** 1 or 3 games. */
    private int bestOf;
    private String board;
    private String theirName = "";
    /** Your 1 or 3 decks, those already played, and the one for the next game (null until picked). */
    private final List<SavedDeck> decks = new ArrayList<>();
    private final List<SavedDeck> played = new ArrayList<>();
    private SavedDeck next;
    /** Host: the joiner's deck for the next game (class and card ids), null until it comes. */
    private String[] theirNext;
    private int yourWins;
    private int theirWins;
    /** Host: who plays first in the next game (the loser of the last one). */
    private boolean hostFirstNext;
    private ServerSocket hosting;
    private LanLink link;

    public LanView(SceneRouter router) {
        this.router = router;
        ScreenGrid.fitToWindow(this, grid);
        showStart();
    }

    /** Host a game, join one, or play on the same screen. */
    private void showStart() {
        page("PvP", () -> router.goTo(Screen.HOME));
        Label hotseat = DecksView.button("Same screen", () -> grid.getChildren().add(new DeckPickView(router)));
        buttons(DecksView.button("Host a game", this::showFormat), DecksView.button("Join a game", this::showJoin), hotseat);
    }

    /** Host: best of 1 or best of 3. */
    private void showFormat() {
        host = true;
        page("Host a game", this::showStart);
        buttons(DecksView.button("Best of 1", () -> pickFormat(1)), DecksView.button("Best of 3", () -> pickFormat(3)));
    }

    private void pickFormat(int games) {
        bestOf = games;
        page("Choose the battlefield", this::showFormat);
        GridPane boards = BoardPicker.grid(chosen -> {
            board = chosen;
            showDecks();
        });
        boards.setPrefWidth(W);
        boards.relocate(0, 150);
        grid.getChildren().add(boards);
    }

    /**
     * Your decks for the match: one for a best of 1 (it starts at once), three of different classes for a best of 3
     * (a deck of a class already picked is greyed out; click a picked deck again to put it back).
     */
    private void showDecks() {
        String title = "Choose your deck";
        if (bestOf == 3) {
            title = "Choose 3 decks of different classes (" + decks.size() + " / 3)";
        }
        page(title, this::leave);
        FlowPane choices = new FlowPane(40, 40);
        choices.setAlignment(Pos.TOP_CENTER);
        choices.setPrefWrapLength(W - 200);
        choices.relocate(100, 170);
        for (SavedDeck deck : DeckPickView.decksToOffer()) {
            boolean picked = decks.contains(deck);
            boolean classTaken = !picked && decks.stream().anyMatch(d -> d.getHeroClass() == deck.getHeroClass());
            Node choice = DeckPickView.choice(deck, () -> toggle(deck));
            if (picked) {
                choice.setEffect(PICKED);
            }
            if (classTaken) {
                choice.setOpacity(0.35);
                choice.setDisable(true);
            }
            choices.getChildren().add(choice);
        }
        grid.getChildren().add(choices);
    }

    /** A deck clicked: picked or put back; once enough are picked, the host starts hosting and the joiner starts the series. */
    private void toggle(SavedDeck deck) {
        if (!decks.remove(deck)) {
            decks.add(deck);
        }
        if (decks.size() < bestOf) {
            showDecks();
        } else if (host && link == null) {
            hostGame();
        } else {
            showNextDeck();
        }
    }

    /** Host: listens for the joiner and shows the game code to give him. */
    private void hostGame() {
        String address = LanLink.mainAddress();
        message("Your game code:\n\n" + LanCode.of(address) + "\n\nWaiting for a player…", this::leave);
        hosting = LanLink.host(joined -> {
            link = joined;
            listen();
        }, error -> message(error, this::leave));
    }

    /** Join: type the host's game code. */
    private void showJoin() {
        host = false;
        page("Join a game", this::showStart);
        TextField code = new TextField();
        code.setPromptText("K7QX-2MB");
        code.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: 30; -fx-font-weight: bold; -fx-alignment: center;");
        code.setMaxWidth(320);
        Label join = DecksView.button("Join", () -> join(code.getText()));
        code.setOnAction(e -> join(code.getText()));   // Enter joins too
        VBox box = new VBox(24, DecksView.text("The host's game code:", 26, DecksView.PAPER), code, join);
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(W);
        box.relocate(0, 320);
        grid.getChildren().add(box);
    }

    private void join(String typed) {
        String address = LanCode.address(typed);
        if (address == null) {
            message("\"" + typed.trim() + "\" is not a game code.", this::showJoin);
            return;
        }
        message("Joining…", this::leave);
        LanLink.join(address, joined -> {
            link = joined;
            listen();
            link.send("HELLO " + Profile.name());
        }, error -> message("No game found with this code.", this::showJoin));
    }

    /** From now on this screen hears the other game (between games too). */
    private void listen() {
        link.setOnClosed(() -> message(theirName + " left the match.", this::leave));
        link.setOnMessage(this::hear);
    }

    /** A line from the other game. */
    private void hear(String line) {
        String[] words = line.split(" ");
        switch (words[0]) {
            case "HELLO" -> {   // host: the joiner arrived; he learns the format and picks his decks
                theirName = line.substring(6);
                link.send("FORMAT " + bestOf + " " + board + " " + Profile.name());
                hostFirstNext = new Random().nextBoolean();   // the first game: a coin flip
                showNextDeck();
            }
            case "FORMAT" -> {   // joiner: best of 1 or 3, on this board
                bestOf = Integer.parseInt(words[1]);
                board = words[2];
                theirName = line.split(" ", 4)[3];
                showDecks();
            }
            case "DECK" -> {   // host: the joiner's deck for the next game
                theirNext = new String[] { words[1], words[2] };
                startIfReady();
            }
            case "START" -> launch(Long.parseLong(words[1]), !Boolean.parseBoolean(words[2]), words[3], words[4]);   // joiner
            default -> { }
        }
    }

    /** Before each game: the deck you play (in a best of 1 it is your only one). */
    private void showNextDeck() {
        List<SavedDeck> left = new ArrayList<>(decks);
        left.removeAll(played);
        if (left.size() == 1) {
            pickNext(left.get(0));
            return;
        }
        page("Game " + (played.size() + 1) + "  ·  " + score() + "  ·  choose your deck", this::leave);
        HBox choices = new HBox(40);
        choices.setAlignment(Pos.CENTER);
        choices.setPrefWidth(W);
        choices.relocate(0, 250);
        for (SavedDeck deck : left) {
            choices.getChildren().add(DeckPickView.choice(deck, () -> pickNext(deck)));
        }
        grid.getChildren().add(choices);
    }

    /** Your deck for the next game: the joiner sends it; the host starts once he has the joiner's too. */
    private void pickNext(SavedDeck deck) {
        next = deck;
        message("Game " + (played.size() + 1) + "  ·  " + score() + "\n\nWaiting for " + theirName + "…", this::leave);
        if (host) {
            startIfReady();
        } else {
            link.send("DECK " + deck.getHeroClass().name() + " " + ids(deck));
        }
    }

    /** Host: both decks known: the seed and who plays first go to the joiner with your deck, and the game starts on both sides. */
    private void startIfReady() {
        if (next == null || theirNext == null) {
            return;
        }
        long seed = new Random().nextLong();
        link.send("START " + seed + " " + hostFirstNext + " " + next.getHeroClass().name() + " " + ids(next));
        launch(seed, hostFirstNext, theirNext[0], theirNext[1]);
    }

    /** Both games: the same two players from the same cards and the same seed; the match listens to the link until it is over. */
    private void launch(long seed, boolean youFirst, String theirClass, String theirCards) {
        link.setOnMessage(null);   // lines wait until the match listens
        Player you = player(Profile.name(), next.getHeroClass().name(), ids(next));
        Player them = player(theirName, theirClass, theirCards);
        Player first = them;
        Player second = you;
        if (youFirst) {
            first = you;
            second = them;
        }
        Player firstPlayer = first;
        Player secondPlayer = second;
        router.register(Screen.MATCH, () -> OniShinTanApp.lan(board, firstPlayer, secondPlayer, you, link, seed, this::gameOver));
        router.goTo(Screen.MATCH);
    }

    /** A game is over: the score, then the next game or the end of the series, back on this screen. */
    private void gameOver(boolean won) {
        played.add(next);
        next = null;
        theirNext = null;
        if (won) {
            yourWins++;
        } else {
            theirWins++;
        }
        hostFirstNext = won != host;   // the loser plays first in the next game
        int needed = bestOf / 2 + 1;
        if (yourWins == needed || theirWins == needed) {
            String result = "You lose the match " + score();
            if (yourWins == needed) {
                result = "You win the match " + score();
            }
            message(result, this::leave);
        } else if (!link.isOpen()) {
            message(theirName + " left the match.", this::leave);
        } else {
            listen();
            showNextDeck();
        }
        router.register(Screen.PVP, () -> this);   // this same screen, with the series as it stands
        router.goTo(Screen.PVP);
    }

    /** The score from your side, like "2 – 1". */
    private String score() {
        return yourWins + " – " + theirWins;
    }

    /** Leaves the PvP screen: stops hosting, closes the link, and the next PvP starts fresh. */
    private void leave() {
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
        router.register(Screen.PVP, () -> new LanView(router));
        router.goTo(Screen.HOME);
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

    /** A row of buttons in the middle of the page. */
    private void buttons(Node... buttons) {
        HBox row = new HBox(60, buttons);
        row.setAlignment(Pos.CENTER);
        row.setPrefWidth(W);
        row.relocate(0, 400);
        grid.getChildren().add(row);
    }

    /** A page with only a message in the middle (the game code, waiting, the result, an error). */
    private void message(String text, Runnable back) {
        page("PvP", back);
        Label label = DecksView.text(text, 30, DecksView.PAPER);
        label.setStyle(label.getStyle() + " -fx-text-alignment: center;");
        HBox row = new HBox(label);
        row.setAlignment(Pos.CENTER);
        row.setPrefWidth(W);
        row.relocate(0, 300);
        grid.getChildren().add(row);
    }

    /** Clears the page and draws its frame: the Izakaya under a dark veil, the title, and the back sign (what `back` does). */
    private void page(String title, Runnable back) {
        grid.getChildren().clear();
        ImageView picture = new ImageView(Images.get("/images/boards/izakaya.png"));
        picture.setFitWidth(W);
        picture.setFitHeight(H);
        Rectangle veil = new Rectangle(W, H, Color.rgb(0, 0, 0, 0.75));
        HBox titleRow = new HBox(DecksView.text(title, 40, DecksView.PAPER));
        titleRow.setAlignment(Pos.CENTER);
        titleRow.setPrefWidth(W);
        titleRow.relocate(0, 60);
        grid.getChildren().addAll(picture, veil, titleRow, HomeView.back(back));
    }
}
