package fr.onishintan.ui.screens;
import fr.onishintan.ui.cards.CardView;
import fr.onishintan.ui.cards.CardPreview;
import fr.onishintan.ui.cards.DeckListView;
import fr.onishintan.ui.fx.Fx;
import fr.onishintan.ui.app.SceneRouter;
import fr.onishintan.ui.app.Screen;
import fr.onishintan.ui.app.ScrollTheme;
import fr.onishintan.ui.app.Images;
import fr.onishintan.data.CardRepository;
import fr.onishintan.ai.AIController;
import fr.onishintan.data.AdventureDecks;
import fr.onishintan.adventure.Upgrade;
import fr.onishintan.adventure.AdventureMode;
import fr.onishintan.data.SaveManager;
import fr.onishintan.ui.OniShinTanApp;

import fr.onishintan.model.Card;
import fr.onishintan.model.HeroClass;
import fr.onishintan.model.Player;
import fr.onishintan.model.Keyword;
import fr.onishintan.model.MinionCard;
import fr.onishintan.model.Rarity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.EnumSet;
import java.util.Set;
import javafx.util.Duration;
import javafx.application.Platform;

import javafx.animation.ScaleTransition;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.beans.InvalidationListener;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.text.TextAlignment;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;

/** The Adventure run map on the 1600 x 900 grid, scaled to the window: three battles in order (Town, Ocean, Fuji),
 *  each with its banner over its panel, and the class deck in the frame on the right (GDD, Adventure). */
public class AdventureSelectView extends Pane {
    public static final double W = 1600, H = 900, K = W / 1672;   // the page image is 1672 x 941
    private static final String[] ADVENTURES = { "machi", "taifu", "fuji" };
    private static final double[] LEFT = { 8, 428, 822 }, RIGHT = { 418, 812, 1172 };   // each panel's edges in the image
    /** The decks of the run and its opponents, battle by battle (Pierre's data/AdventureDecks: boards and opponents too). */
    private static final AdventureDecks DECKS = new AdventureDecks(CardRepository.all());
    /** Each battle's music, in the same order as AdventureDecks.board(i): the town, the sea (the ship), Mount Fuji. */
    private static final String[] BOARD_MUSIC = { "town-map-music", "sea-map-music", "fuji-map-music" };
    /** The music of the story pages. */
    private static final String STORY_MUSIC = "battle-music";
    /** The deck list's size inside the frame. */
    private static final double LIST_W = 370, LIST_H = 485;
    /** How wide a bonus emblem shows on Choose Bonus, and in the row of bonuses picked. */
    private static final double BONUS_W = 240, PICKED_W = 40;
    /** Each class's reward for finishing its Adventure (GDD, Yokai and kami). */
    /** The sun goddess: the prize for finishing Shura no Michi with every class. */
    private static final String AMATERASU = "amaterasu-omikami";
    private static final Map<HeroClass, Card> KAMI = Map.of(
            HeroClass.NINJA, new MinionCard("tsukuyomi", "Tsukuyomi", "月読", "つくよみ", 8, Rarity.LEGENDARY, HeroClass.NINJA,
                    "Banzai: your other minions have Shinobi. Shinobi", "", 12, 10, Set.of(Keyword.SHINOBI, Keyword.BANZAI)),
            HeroClass.SAMURAI, new MinionCard("takemikazuchi", "Takemikazuchi", "建御雷", "たけみかづち", 8, Rarity.LEGENDARY, HeroClass.SAMURAI,
                    "Tatakae. Banzai: equip your hero with Futsu-no-mitama (10/2)", "", 10, 9, Set.of(Keyword.TATAKAE, Keyword.BANZAI)),
            HeroClass.KANNUSHI, new MinionCard("okuninushi", "Ōkuninushi", "大国主", "おおくにぬし", 8, Rarity.LEGENDARY, HeroClass.KANNUSHI,
                    "Banzai: heal all friendly characters for 10", "", 11, 12, Set.of(Keyword.BANZAI)),
            HeroClass.ONI, new MinionCard("izanami", "Izanami", "伊邪那美", "いざなみ", 9, Rarity.LEGENDARY, HeroClass.ONI,
                    "Banzai: return your 2 highest-cost dead minions to the board", "", 11, 11, Set.of(Keyword.BANZAI)));

    /** Shows the run map for this class after `won` victories. */
    /** Opens this class's run: where the saved run stopped if it is this class and mode, otherwise from the start. */
    static void resume(SceneRouter router, HeroClass c, AdventureMode mode) {
        Optional<SaveManager.SavedRun> saved = new SaveManager().loadRun().filter(r -> r.heroClass() == c && r.mode() == mode);
        if (saved.isPresent()) {
            open(router, c, mode, saved.get().won(), saved.get().picked(), saved.get().wounds());
        } else {
            open(router, c, mode, 0, List.of(), 0);
        }
    }

    /**
     * Saves where the run is (after each battle and each bonus, as the map opens again). Finished: it is marked done
     * for this class and mode, and there is nothing left to resume. Lost (back to 0): its saved run is forgotten.
     */
    private void remember() {
        SaveManager saves = new SaveManager();
        if (won >= AdventureDecks.BATTLES) {
            saves.markCompleted(heroClass, mode);
            saves.clearRun();
        } else if (won == 0) {
            saves.loadRun().filter(r -> r.heroClass() == heroClass && r.mode() == mode).ifPresent(r -> saves.clearRun());
        } else {
            saves.saveRun(new SaveManager.SavedRun(heroClass, mode, won, picked, wounds));
        }
    }

    /** True once every class has finished Shura no Michi and Amaterasu is not claimed yet. */
    private boolean sunEarned(SaveManager saves) {
        if (mode != AdventureMode.HARDCORE || saves.isUnlocked(AMATERASU)) {
            return false;
        }
        for (HeroClass c : KAMI.keySet()) {
            if (!saves.isCompleted(c, AdventureMode.HARDCORE)) {
                return false;
            }
        }
        return true;
    }

    static void open(SceneRouter router, HeroClass c, AdventureMode mode, int won, List<String> picked, int wounds) {
        router.register(Screen.ADVENTURE_SELECT, () -> new AdventureSelectView(router, c, mode, won, picked, wounds));
        router.goTo(Screen.ADVENTURE_SELECT);
    }

    private final Pane grid = new Pane();
    /** The deck in Ki sections, with cream titles on the dark frame; hovering a card opens it big on the whole grid. */
    private final DeckListView list = new DeckListView(LIST_W, "#f3ede1", new CardPreview(grid), card -> {});
    /** The list scrolls inside the frame, between the title and the bonus. */
    private final ScrollPane deckScroll = deckScroll();
    private final int won;
    private final HeroClass heroClass;
    private final List<String> picked;
    /** Normal, or Shura no Michi (hardcore). */
    private final AdventureMode mode;
    /** The HP the hero has lost so far in this run (kept only in Shura no Michi). */
    private final int wounds;
    /** True on the way back from a won battle, so the map opens on that battle's after-story. */
    private static boolean justWon;

    public AdventureSelectView(SceneRouter router, HeroClass heroClass, AdventureMode mode, int won, List<String> picked, int wounds) {
        this.picked = picked;
        this.heroClass = heroClass;
        this.won = won;
        this.mode = mode;
        this.wounds = wounds;
        remember();
        Scale scale = new Scale();
        grid.getTransforms().add(scale);
        InvalidationListener fit = o -> {
            scale.setX(getWidth() / W);    // stretched to fill the window: no strip on a screen that isn't 16:9
            scale.setY(getHeight() / H);
        };
        widthProperty().addListener(fit);
        heightProperty().addListener(fit);
        ImageView page = new ImageView(Images.get("/images/adventure-page.png"));
        page.setFitWidth(W);
        page.setPreserveRatio(true);
        Label title = text("札  " + heroClass.name() + " DECK", 30);
        grid.getChildren().addAll(page, title, deckScroll, HomeView.back(() -> router.goTo(Screen.CLASS_SELECT)));
        // The bonuses picked in this run: small emblems under the deck title (hover one to read it).
        HBox bonuses = new HBox(6);
        for (String label : picked) {
            Upgrade upgrade = Upgrade.fromLabel(label);
            if (upgrade != null) {
                ImageView small = emblem(upgrade, PICKED_W);
                Tooltip tip = new Tooltip(upgrade.getLabel() + "\n" + upgrade.getDescription());
                tip.setShowDelay(Duration.millis(100));
                Tooltip.install(small, tip);
                bonuses.getChildren().add(small);
            }
        }
        bonuses.relocate(1185, 180);
        grid.getChildren().add(bonuses);
        if (mode.keepsWounds()) {   // Shura no Michi: the hero's HP for the next battle, e.g. 命 60 / 100
            Player you = DECKS.hero("You", heroClass);
            you.getHero().carryWounds(wounds);
            Label life = text("♥  " + you.getHero().getHealth() + " / " + you.getHero().getMaxHealth(), 34);
            life.setStyle(life.getStyle() + " -fx-text-fill: #e0303a; -fx-alignment: center;");
            life.setPrefWidth(300);
            life.relocate(W / 2 - 150, 20);   // top middle of the map
            grid.getChildren().add(life);
        }
        if (bonusPending()) {   // one choice after win 1, one after win 2
            Label bonus = text("恩恵  CHOOSE BONUS", 26);
            bonus.relocate(1230, 740);
            HomeView.clickable(bonus, false, () -> chooseBonus(router));
            // It glows until picked: gold, slowly fading in and out.
            bonus.setStyle(bonus.getStyle() + " -fx-text-fill: #ffd84a;");
            FadeTransition glow = new FadeTransition(Duration.millis(700), bonus);
            glow.setFromValue(1);
            glow.setToValue(0.45);
            glow.setAutoReverse(true);
            glow.setCycleCount(Animation.INDEFINITE);
            glow.play();
            grid.getChildren().add(bonus);
        }
        title.relocate(1185, 130);
        deckScroll.relocate(1185, 230);   // under the deck title and the bonuses picked
        deckList();
        for (int i = 0; i < ADVENTURES.length; i++) panel(router, i);
        if (won == 3) reward(router, KAMI.get(heroClass));
        if (justWon) {   // back from a win: what happened after it, over the map (and over the reward)
            justWon = false;
            Platform.runLater(() -> router.playMusic(STORY_MUSIC));   // later: the map's own music starts just after this screen is built
            grid.getChildren().add(new StoryPanel(StoryLines.TITLES[won - 1], StoryLines.after(heroClass, won - 1), List.of(StoryLines.picture(heroClass, won - 1, 3)),
                    () -> router.playScreenMusic(Screen.ADVENTURE_SELECT)));   // Continue: back to the map's music
        }
        getChildren().add(grid);
        setStyle("-fx-background-color: black;");
    }

    /** The Choose bonus pop-up: the upgrades not taken yet; picking one keeps it for the rest of the run. */
    private void chooseBonus(SceneRouter router) {
        // The bonuses not taken yet, side by side: emblem, name and effect; clicking one keeps it for the rest of the run.
        HBox choices = new HBox(50);
        choices.setAlignment(Pos.CENTER);
        for (Upgrade upgrade : Upgrade.values()) {
            if (picked.contains(upgrade.getLabel())) {
                continue;   // already taken in this run
            }
            Label effect = text(upgrade.getDescription(), 18);
            effect.setWrapText(true);
            effect.setMaxWidth(BONUS_W);
            effect.setTextAlignment(TextAlignment.CENTER);
            VBox choice = new VBox(12, emblem(upgrade, BONUS_W), text(upgrade.getLabel(), 28), effect);
            choice.setAlignment(Pos.TOP_CENTER);
            HomeView.clickable(choice, false, () -> {
                List<String> now = new ArrayList<>(picked);
                now.add(upgrade.getLabel());
                open(router, heroClass, mode, won, now, wounds);
            });
            choices.getChildren().add(choice);
        }
        VBox box = new VBox(40, text("恩恵を選べ   CHOOSE A BONUS", 36), choices);
        box.setAlignment(Pos.CENTER);
        grid.getChildren().add(new StackPane(new Rectangle(W, H, Color.rgb(0, 0, 0, 0.8)), box));
    }

    /**
     * Finishing the run: the prize grows into view (GDD, Reward) and Claim adds it to the collection for good.
     * Once every class has finished Shura no Michi, Amaterasu comes as a second prize; then Claim goes back Home.
     */
    private void reward(SceneRouter router, Card prize) {
        CardView card = new CardView(prize, 300);
        ScaleTransition grow = new ScaleTransition(Duration.millis(700), card);
        grow.setFromX(0);
        grow.setFromY(0);
        grow.setToX(1);
        grow.setToY(1);
        grow.play();
        Label claim = text("受領  CLAIM", 30);
        HomeView.clickable(claim, false, () -> {
            SaveManager saves = new SaveManager();
            saves.unlock(prize.getId());   // the prize joins your collection for good
            if (!prize.getId().equals(AMATERASU) && sunEarned(saves)) {
                reward(router, CardRepository.byId(AMATERASU));   // every Shura no Michi finished: Amaterasu as well
                return;
            }
            router.goTo(Screen.HOME);
        });
        VBox box = new VBox(24, text("報酬  REWARD", 40), card, claim);
        box.setAlignment(Pos.CENTER);
        grid.getChildren().add(new StackPane(new Rectangle(W, H, Color.rgb(0, 0, 0, 0.8)), box));
    }

    /** Battle i: its banner over its panel. Won = red 完了 stamp, next = click to fight, later = shaded. */
    private void panel(SceneRouter router, int i) {
        double x0 = LEFT[i] * K, x1 = RIGHT[i] * K;
        Group banner = Fx.waving(Images.get("/images/banners/adventure-" + ADVENTURES[i] + ".png"), 250, 60);
        banner.relocate((x0 + x1) / 2 - 125, 70);
        if (i > won) banner.setEffect(new ColorAdjust(0, -0.6, -0.5, 0));
        if (i == won) HomeView.clickable(banner, true, () -> {
            if (bonusPending()) {
                chooseBonus(router);   // no next battle before the bonus is picked
                return;
            }
            // You against the AI, which gets harder battle by battle (Town EASY, Ocean NORMAL, Mount Fuji HARD).
            // The battle's story first, like a scene; Continue starts the fight.
            router.playMusic(STORY_MUSIC);   // the story is told over the battle theme
            grid.getChildren().add(new StoryPanel(StoryLines.TITLES[i], StoryLines.before(heroClass, i), List.of(StoryLines.picture(heroClass, i, 1), StoryLines.picture(heroClass, i, 2)), () -> {
                router.register(Screen.ADVENTURE_BATTLE, () -> {
                    Player you = DECKS.hero("You", heroClass);   // made here, so its HP can be read when the battle ends
                    if (mode.keepsWounds()) {
                        you.getHero().carryWounds(wounds);   // Shura no Michi: the HP lost before stays lost
                    }
                    return OniShinTanApp.adventure(AdventureDecks.board(i), you, DECKS.enemy(heroClass, i),
                            mode.difficulty(i), upgrades(),
                            v -> {
                                justWon = v;   // a win: its after-story shows on the map
                                open(router, heroClass, mode, v ? won + 1 : 0, v ? picked : List.of(), woundsAfter(v, you));
                            });
                });
                router.setMusic(Screen.ADVENTURE_BATTLE, BOARD_MUSIC[i]);   // the town, the sea or Mount Fuji theme
                router.goTo(Screen.ADVENTURE_BATTLE);
            }));
        });
        grid.getChildren().add(banner);
        if (i >= won) return;
        Label stamp = text("完了", 90);
        stamp.setStyle(stamp.getStyle() + " -fx-text-fill: #c41e1e;");
        stamp.setRotate(-12);
        stamp.relocate((x0 + x1) / 2 - 90, 380);
        grid.getChildren().add(stamp);
    }

    /** Fills the frame with the class deck in Ki sections, 4 cards per row; hovering a card shows it big. */
    private void deckList() {
        list.show(deck());
    }

    /** A transparent, vertical-only scroll area around the deck list, inside the frame. */
    private ScrollPane deckScroll() {
        ScrollPane scroll = new ScrollPane(list);
        scroll.setPrefSize(LIST_W + 14, LIST_H);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        ScrollTheme.apply(scroll);
        return scroll;
    }

    /** The 40-card deck this class is given for the run (AdventureDecks). */
    private List<Card> deck() {
        return DECKS.playerDeck(heroClass);
    }

    /** True while a bonus is owed (one after the 1st win, one after the 2nd): the next battle waits until it is picked. */
    private boolean bonusPending() {
        return picked.size() < Math.min(won, mode.getBonuses());   // 2 bonuses in a normal run, 1 in Shura no Michi
    }

    /** The HP the hero lost in the battle that just ended: carried to the next one in Shura no Michi, 0 otherwise or after a loss. */
    private int woundsAfter(boolean victory, Player you) {
        if (!victory || !mode.keepsWounds()) {
            return 0;
        }
        return you.getHero().getMaxHealth() - you.getHero().getHealth();
    }

    /** The bonuses picked so far in this run, as the engine's Upgrades (the screen keeps their labels, e.g. "命 Inochi"). */
    private Set<Upgrade> upgrades() {
        Set<Upgrade> chosen = EnumSet.noneOf(Upgrade.class);
        for (String label : picked) {
            Upgrade upgrade = Upgrade.fromLabel(label);
            if (upgrade != null) {
                chosen.add(upgrade);
            }
        }
        return chosen;
    }

    /** A bonus's round emblem (images/adventure/<name>.png), `size` wide. */
    private static ImageView emblem(Upgrade upgrade, double size) {
        return new ImageView(Images.get("/images/adventure/" + upgrade.name().toLowerCase() + ".png", size, size));
    }

    /** Cream text in the card font with a soft shadow, readable on the frame's dark inside. */
    private static Label text(String s, double size) {
        Label l = new Label(s);
        l.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: " + size + "; -fx-font-weight: bold; -fx-text-fill: #f3ede1;");
        l.setEffect(new DropShadow(6, Color.BLACK));
        return l;
    }
}