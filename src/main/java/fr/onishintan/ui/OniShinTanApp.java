package fr.onishintan.ui;

import fr.onishintan.data.SaveManager;
import fr.onishintan.ai.AIController;
import fr.onishintan.adventure.Upgrade;
import fr.onishintan.model.HeroClass;
import fr.onishintan.model.Player;
import fr.onishintan.ui.app.SceneRouter;
import fr.onishintan.ui.app.Profile;
import fr.onishintan.ui.app.Screen;
import fr.onishintan.ui.match.BoardView;
import fr.onishintan.ui.match.MatchController;
import fr.onishintan.ui.lan.LanLink;
import fr.onishintan.ui.decks.LanView;
import fr.onishintan.ui.screens.adventure.OrochiView;
import fr.onishintan.ui.screens.adventure.ClassSelectView;
import fr.onishintan.ui.screens.HomeView;
import fr.onishintan.ui.screens.InventoryView;
import fr.onishintan.ui.screens.ProfileView;

import java.util.function.Consumer;
import java.util.Set;

import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class OniShinTanApp extends Application {

    @Override
    public void start(Stage stage) {
        SceneRouter router = new SceneRouter(stage);
        // Music per screen; Home, the Deck Builder and the Inventory keep the main theme ("music").
        router.setMusic(Screen.CLASS_SELECT, "adventure-music");
        router.setMusic(Screen.ADVENTURE_SELECT, "adventure-music");
        router.setMusic(Screen.OROCHI, OrochiView.STORM);   // the storm, on repeat, on his page and in his fight
        router.setMusic(Screen.MATCH, "battle-music");
        router.register(Screen.HOME, () -> new HomeView(router));
        router.register(Screen.CLASS_SELECT, () -> new ClassSelectView(router));
        router.register(Screen.INVENTORY, () -> new InventoryView(router));
        router.register(Screen.PROFILE, () -> new ProfileView(router));
        router.register(Screen.PVP, () -> new LanView(router));
        router.goTo(Screen.HOME);
        stage.setTitle("Oni Shin Tan");
        stage.getIcons().add(new Image(OniShinTanApp.class.getResourceAsStream("/images/logos/game-icon.png")));   // window and taskbar icon
        SaveManager.Window window = new SaveManager().loadWindow();   // the size and fullscreen chosen last time in Settings
        stage.setWidth(window.width());
        stage.setHeight(window.height());
        stage.setFullScreen(window.fullscreen());
        stage.show();
        stage.centerOnScreen();
    }

    /** A hotseat match: you play first, at the bottom; at the end, onEnd gets whether you won. */
    public static BoardView match(String boardName, HeroClass yours, HeroClass theirs, Consumer<Boolean> onEnd) {
        MatchController match = new MatchController(boardName, yours, theirs, onEnd);
        return match.getBoard();
    }

    /** A hotseat match between two players with their own decks (PvP): at the end, onEnd gets whether the first player won. */
    public static BoardView match(String boardName, Player first, Player second, Consumer<Boolean> onEnd) {
        HeroClass played = first.getHero().getHeroClass();   // the profile is the first player's: you, at the bottom
        MatchController match = new MatchController(boardName, first, second, won -> {
            Profile.record(played, won, true);   // PvP: counts the game and moves your Rei
            onEnd.accept(won);
        });
        return match.getBoard();
    }

    /** A LAN match: `you` is one of the two players, the other game plays the other; at the end, onEnd gets whether you won. */
    public static BoardView lan(String boardName, Player first, Player second, Player you, LanLink link, long seed, Consumer<Boolean> onEnd) {
        HeroClass played = you.getHero().getHeroClass();
        MatchController match = new MatchController(boardName, first, second, you, link, seed, won -> {
            Profile.record(played, won, true);   // PvP: counts the game and moves your Rei
            onEnd.accept(won);
        });
        return match.getBoard();
    }

    /** An Adventure battle: you play first at the bottom against the AI (no Oaths, no clocks, no pass veil), with the run's bonuses; onEnd gets whether you won. */
    public static BoardView adventure(String boardName, Player you, Player enemy, AIController.Difficulty difficulty,
                                      Set<Upgrade> upgrades, Consumer<Boolean> onEnd) {
        HeroClass played = you.getHero().getHeroClass();
        MatchController match = new MatchController(boardName, you, enemy, difficulty, won -> {
            Profile.record(played, won, false);   // Adventure: counts the game, no Rei
            onEnd.accept(won);
        });
        match.setUpgrades(upgrades);
        return match.getBoard();
    }

    public static void main(String[] args) { launch(args); }
}