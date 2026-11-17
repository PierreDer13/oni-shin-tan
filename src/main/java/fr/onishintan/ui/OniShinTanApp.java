package fr.onishintan.ui;

import fr.onishintan.ai.AIController;
import fr.onishintan.model.HeroClass;
import fr.onishintan.model.Player;
import fr.onishintan.ui.app.SceneRouter;
import fr.onishintan.ui.app.Screen;
import fr.onishintan.ui.decks.DeckPickView;
import fr.onishintan.ui.match.BoardView;
import fr.onishintan.ui.match.MatchController;
import fr.onishintan.ui.screens.ClassSelectView;
import fr.onishintan.ui.screens.HomeView;
import fr.onishintan.ui.screens.InventoryView;

import java.util.function.Consumer;

import javafx.application.Application;
import javafx.stage.Stage;

public class OniShinTanApp extends Application {

    @Override
    public void start(Stage stage) {
        SceneRouter router = new SceneRouter(stage);
        // Music per screen; Home, the Deck Builder and the Inventory keep the main theme ("music").
        router.setMusic(Screen.CLASS_SELECT, "adventure-music");
        router.setMusic(Screen.ADVENTURE_SELECT, "adventure-music");
        router.setMusic(Screen.MATCH, "battle-music");
        router.register(Screen.HOME, () -> new HomeView(router));
        router.register(Screen.CLASS_SELECT, () -> new ClassSelectView(router));
        router.register(Screen.INVENTORY, () -> new InventoryView(router));
        router.register(Screen.DECK_SELECT, () -> new DeckPickView(router));   // PvP: both players choose a deck, then the match starts
        router.goTo(Screen.HOME);
        stage.setTitle("Oni Shin Tan");
        stage.show();
    }

    /** A hotseat match: you play first, at the bottom; at the end, onEnd gets whether you won. */
    public static BoardView match(String boardName, HeroClass yours, HeroClass theirs, Consumer<Boolean> onEnd) {
        MatchController match = new MatchController(boardName, yours, theirs, onEnd);
        return match.getBoard();
    }

    /** A hotseat match between two players with their own decks (PvP): at the end, onEnd gets whether the first player won. */
    public static BoardView match(String boardName, Player first, Player second, Consumer<Boolean> onEnd) {
        MatchController match = new MatchController(boardName, first, second, onEnd);
        return match.getBoard();
    }

    /** An Adventure battle: you play first at the bottom against the AI (no Oaths, no clocks, no pass veil); onEnd gets whether you won. */
    public static BoardView adventure(String boardName, Player you, Player enemy, AIController.Difficulty difficulty, Consumer<Boolean> onEnd) {
        MatchController match = new MatchController(boardName, you, enemy, difficulty, onEnd);
        return match.getBoard();
    }

    public static void main(String[] args) { launch(args); }
}