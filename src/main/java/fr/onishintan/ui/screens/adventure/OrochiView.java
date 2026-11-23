package fr.onishintan.ui.screens.adventure;

import fr.onishintan.ai.AIController;
import fr.onishintan.data.AdventureDecks;
import fr.onishintan.data.CardRepository;
import fr.onishintan.data.SaveManager;
import fr.onishintan.model.Deck;
import fr.onishintan.model.Hero;
import fr.onishintan.model.Player;
import fr.onishintan.ui.OniShinTanApp;
import fr.onishintan.ui.app.AudioSettings;
import fr.onishintan.ui.app.Images;
import fr.onishintan.ui.app.SceneRouter;
import fr.onishintan.ui.app.Screen;
import fr.onishintan.ui.app.Sound;
import fr.onishintan.ui.decks.DeckPickView;
import fr.onishintan.ui.decks.DeckStore;
import fr.onishintan.ui.decks.SavedDeck;
import fr.onishintan.ui.fx.Fx;

import java.util.List;
import java.util.Set;

import javafx.beans.InvalidationListener;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import fr.onishintan.ui.screens.HomeView;

/**
 * The page after the classes: Yamata no Orochi, the bonus boss. Locked until every Adventure is finished in both modes;
 * then his banner opens a choice of the player's decks, his story, and the fight (EXTREME AI, 200 HP, the dragon board).
 * No reward: the page counts how many times he was slain.
 */
public class OrochiView extends Pane {
    private static final double W = HomeView.W, H = HomeView.H;
    private static final AdventureDecks DECKS = new AdventureDecks(CardRepository.all());
    /** His story's pictures (images/story/orochi-N), one per part of each page. */
    private static final List<String> BEFORE_PICTURES = List.of("orochi-1", "orochi-2", "orochi-3", "orochi-4");
    private static final List<String> WIN_PICTURES = List.of("orochi-5", "orochi-6", "orochi-7");
    private static final List<String> LOSS_PICTURES = List.of("orochi-8");
    /** The storm that plays on his page, through his story and his fight. */
    public static final String STORM = "orochi-storm";
    /** How the last fight ended, so the page opens on its story: null when there was none. */
    private static Boolean lastFightWon;
    /** The storm under the battle music during his fight (the page's own music is the storm alone). */
    private static MediaPlayer storm;

    private final Pane grid = new Pane();
    private final SceneRouter router;

    /** Shows the Orochi page. */
    public static void open(SceneRouter router) {
        router.register(Screen.OROCHI, () -> new OrochiView(router));
        router.goTo(Screen.OROCHI);
    }

    public OrochiView(SceneRouter router) {
        this.router = router;
        Scale scale = new Scale();
        grid.getTransforms().add(scale);
        InvalidationListener fit = o -> {
            scale.setX(getWidth() / W);
            scale.setY(getHeight() / H);
        };
        widthProperty().addListener(fit);
        heightProperty().addListener(fit);

        ImageView backdrop = new ImageView(Images.get("/images/orochi-select.png"));
        backdrop.setFitWidth(W);
        backdrop.setFitHeight(H);
        Label title = text("八岐大蛇   YAMATA NO OROCHI", 40);
        title.setPrefWidth(W);
        title.setAlignment(Pos.CENTER);
        title.relocate(0, 40);
        Label back = text("←  流派  Classes", 24);   // back to the class page
        back.relocate(40, H / 2 - 20);
        HomeView.clickable(back, false, () -> router.goTo(Screen.CLASS_SELECT));
        grid.getChildren().addAll(backdrop, title, back);

        SaveManager saves = new SaveManager();
        Group banner = Fx.waving(Images.get("/images/banners/adventure-dragon.png"), 260, 60);
        banner.relocate(W / 2 - 130, 120);
        Label below = text(slainText(saves.loadOrochiKills()), 24);
        if (saves.orochiUnlocked()) {
            HomeView.clickable(banner, true, this::chooseDeck);
        } else {
            banner.setEffect(new ColorAdjust(0, -0.6, -0.5, 0));   // locked: dimmed
            below.setText("Finish every Adventure, in normal and in Shura no Michi, to face him.");
        }
        below.setPrefWidth(W);
        below.setAlignment(Pos.CENTER);
        below.relocate(0, 800);
        grid.getChildren().addAll(banner, below);

        getChildren().add(grid);
        setStyle("-fx-background-color: black;");
        afterFight();
    }

    /** "Never slain." or "Slain N times. He always returns." */
    private static String slainText(int kills) {
        if (kills == 0) {
            return "討伐  Never slain.";
        }
        return "討伐  Slain " + kills + " times. He always returns.";
    }

    /** Back from a fight: the victory or defeat page of his story, over this page. */
    private void afterFight() {
        if (lastFightWon == null) {
            return;
        }
        boolean won = lastFightWon;
        lastFightWon = null;
        if (won) {
            grid.getChildren().add(new StoryPanel("討伐  Slain", StoryLines.OROCHI_WIN, WIN_PICTURES, "orochi-win",
                    () -> router.playScreenMusic(Screen.OROCHI)));
        } else {
            grid.getChildren().add(new StoryPanel("敗北  Defeat", StoryLines.OROCHI_LOSS, LOSS_PICTURES, "orochi-loss", null));
        }
    }

    /** The decks the player can fight him with: every legal saved deck, one click each. */
    private void chooseDeck() {
        FlowPane decks = new FlowPane(40, 40);   // the deck holders, as in PvP
        decks.setAlignment(Pos.CENTER);
        decks.setPrefWrapLength(W - 200);
        for (SavedDeck deck : DeckStore.all()) {
            if (deck.isLegal()) {
                decks.getChildren().add(DeckPickView.choice(deck, () -> beforeFight(deck)));
            }
        }
        Label cancel = text("戻る  Back", 22);
        VBox box = new VBox(30, text("札を選べ   CHOOSE YOUR DECK", 36), decks, cancel);
        box.setAlignment(Pos.CENTER);
        StackPane veil = new StackPane(new Rectangle(W, H, Color.rgb(0, 0, 0, 0.85)), box);
        HomeView.clickable(cancel, false, () -> grid.getChildren().remove(veil));
        grid.getChildren().add(veil);
    }

    /** His story first, over the storm; Continue starts the fight with this deck. */
    private void beforeFight(SavedDeck deck) {
        grid.getChildren().add(new StoryPanel("八岐大蛇  Yamata no Orochi", StoryLines.OROCHI_BEFORE, BEFORE_PICTURES, "orochi-before", () -> fight(deck)));
    }

    /** The fight: your deck against him on the dragon board, EXTREME AI, no bonus; a win adds one to the count. */
    private void fight(SavedDeck deck) {
        Player you = new Player("You", new Hero(deck.getHeroClass()), new Deck(deck.getCards()));
        router.register(Screen.ADVENTURE_BATTLE, () -> OniShinTanApp.adventure("dragon", you, DECKS.orochi(),
                AIController.Difficulty.EXTREME, Set.of(), won -> {
                    if (won) {
                        new SaveManager().addOrochiKill();
                    }
                    calmStorm();
                    lastFightWon = won;
                    open(router);
                }));
        router.setMusic(Screen.ADVENTURE_BATTLE, "battle-music");   // the battle theme...
        storm = Sound.music(STORM, AudioSettings.MUSIC.get());     // ...with the storm still raging under it
        if (storm != null) {
            storm.volumeProperty().bind(AudioSettings.MUSIC);      // follows the Settings slider
        }
        router.goTo(Screen.ADVENTURE_BATTLE);
    }

    /** The fight is over: the storm under the battle music stops (the page's own storm takes over). */
    private static void calmStorm() {
        if (storm != null) {
            storm.stop();
            storm.dispose();
            storm = null;
        }
    }

    /** Cream text in the card font with a soft shadow, like the Adventure pages. */
    private static Label text(String s, double size) {
        Label l = new Label(s);
        l.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: " + size + "; -fx-font-weight: bold; -fx-text-fill: #f3ede1;");
        l.setEffect(new DropShadow(6, Color.BLACK));
        l.setWrapText(true);
        return l;
    }
}
