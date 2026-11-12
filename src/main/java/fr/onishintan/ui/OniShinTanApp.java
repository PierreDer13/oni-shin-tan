package fr.onishintan.ui;

import fr.onishintan.model.HeroClass;
import fr.onishintan.ui.app.SceneRouter;
import fr.onishintan.ui.app.Screen;
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
        router.register(Screen.MATCH, () -> match("izakaya", HeroClass.SAMURAI, HeroClass.ONI, won -> router.goTo(Screen.HOME)));
        router.goTo(Screen.HOME);
        stage.setTitle("Oni Shin Tan");
        stage.show();
    }

    /** A hotseat match: you play first, at the bottom; at the end, onEnd gets whether you won. */
    public static BoardView match(String boardName, HeroClass yours, HeroClass theirs, Consumer<Boolean> onEnd) {
        MatchController match = new MatchController(boardName, yours, theirs, onEnd);
        return match.getBoard();
    }

    public static void main(String[] args) { launch(args); }
}