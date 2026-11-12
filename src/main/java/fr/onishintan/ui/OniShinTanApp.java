package fr.onishintan.ui;

import fr.onishintan.model.HeroClass;
import fr.onishintan.ui.app.SceneRouter;
import fr.onishintan.ui.app.Screen;
import fr.onishintan.ui.app.Sound;
import fr.onishintan.ui.match.BoardView;
import fr.onishintan.ui.match.MatchController;
import fr.onishintan.ui.screens.ClassSelectView;
import fr.onishintan.ui.screens.HomeView;
import fr.onishintan.ui.screens.InventoryView;

import java.util.function.Consumer;

import javafx.application.Application;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

public class OniShinTanApp extends Application {
    private MediaPlayer music;

    @Override
    public void start(Stage stage) {
        SceneRouter router = new SceneRouter(stage);
        music = Sound.music("music", 0.35);
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