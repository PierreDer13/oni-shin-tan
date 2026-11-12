package fr.onishintan.ui.screens;
import fr.onishintan.ui.cards.CardView;
import fr.onishintan.ui.cards.CardPreview;
import fr.onishintan.ui.cards.DeckListView;
import fr.onishintan.ui.fx.Fx;
import fr.onishintan.ui.app.SceneRouter;
import fr.onishintan.ui.app.Screen;
import fr.onishintan.ui.MockCards;
import fr.onishintan.ui.OniShinTanApp;

import fr.onishintan.model.Card;
import fr.onishintan.model.HeroClass;
import fr.onishintan.model.Keyword;
import fr.onishintan.model.MinionCard;
import fr.onishintan.model.Rarity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import javafx.util.Duration;

import javafx.animation.ScaleTransition;
import javafx.beans.InvalidationListener;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;

/** The Adventure run map on the 1600 x 900 grid, scaled to the window: three battles in order (Town, Ocean, Fuji),
 *  each with its banner over its panel, and the class deck in the frame on the right (GDD, Adventure). */
public class AdventureSelectView extends Pane {
    public static final double W = 1600, H = 900, K = W / 1672;   // the page image is 1672 x 941
    private static final String[] ADVENTURES = { "machi", "taifu", "fuji" };
    private static final double[] LEFT = { 8, 428, 822 }, RIGHT = { 418, 812, 1172 };   // each panel's edges in the image
    private static final String[] BOARDS = { "town", "ship", "fuji" };
    /** Each battle's music, in the same order as BOARDS: the town, the sea (the ship), Mount Fuji. */
    private static final String[] BOARD_MUSIC = { "town-map-music", "sea-map-music", "fuji-map-music" };
    /** The deck list's size inside the frame. */
    private static final double LIST_W = 370, LIST_H = 530;
    /** Your opponent in battles 1 to 3, per class (GDD, Adventure). */
    private static final Map<HeroClass, HeroClass[]> FOES = Map.of(
            HeroClass.SAMURAI, new HeroClass[] { HeroClass.KANNUSHI, HeroClass.ONI, HeroClass.NINJA },
            HeroClass.NINJA, new HeroClass[] { HeroClass.ONI, HeroClass.KANNUSHI, HeroClass.SAMURAI },
            HeroClass.KANNUSHI, new HeroClass[] { HeroClass.SAMURAI, HeroClass.NINJA, HeroClass.ONI },
            HeroClass.ONI, new HeroClass[] { HeroClass.NINJA, HeroClass.SAMURAI, HeroClass.KANNUSHI });
    /** The run's upgrades (GDD): name, effect. */
    private static final String[][] UPGRADES = { { "命 Inochi", "Hero max HP +30" },
            { "力 Chikara", "Minions and weapons +3 ATK, spells +3 damage" }, { "速さ Hayasa", "Your cards cost 1 Ki less (min 1)" } };
    /** Each class's reward for finishing its Adventure (GDD, Yokai and kami). */
    private static final Map<HeroClass, Card> KAMI = Map.of(
            HeroClass.NINJA, new MinionCard("tsukuyomi", "Tsukuyomi", "月読", "つくよみ", 8, Rarity.LEGENDARY, HeroClass.NINJA,
                    "Shinobi. Banzai: your other minions gain Shinobi", "", 12, 10, Set.of(Keyword.SHINOBI, Keyword.BANZAI)),
            HeroClass.SAMURAI, new MinionCard("takemikazuchi", "Takemikazuchi", "建御雷", "たけみかづち", 8, Rarity.LEGENDARY, HeroClass.SAMURAI,
                    "Tatakae. Banzai: equip your hero with Futsu-no-mitama (10/2)", "", 10, 9, Set.of(Keyword.TATAKAE, Keyword.BANZAI)),
            HeroClass.KANNUSHI, new MinionCard("okuninushi", "Ōkuninushi", "大国主", "おおくにぬし", 8, Rarity.LEGENDARY, HeroClass.KANNUSHI,
                    "Banzai: heal all friendly characters for 10", "", 11, 12, Set.of(Keyword.BANZAI)),
            HeroClass.ONI, new MinionCard("izanami", "Izanami", "伊邪那美", "いざなみ", 9, Rarity.LEGENDARY, HeroClass.ONI,
                    "Banzai: return your 2 highest-cost dead minions to the board", "", 11, 11, Set.of(Keyword.BANZAI)));

    /** Shows the run map for this class after `won` victories. */
    static void open(SceneRouter router, HeroClass c, int won, List<String> picked) {
        router.register(Screen.ADVENTURE_SELECT, () -> new AdventureSelectView(router, c, won, picked));
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

    public AdventureSelectView(SceneRouter router, HeroClass heroClass, int won, List<String> picked) {
        this.picked = picked;
        this.heroClass = heroClass;
        this.won = won;
        Scale scale = new Scale();
        grid.getTransforms().add(scale);
        InvalidationListener fit = o -> {
            double s = Math.min(getWidth() / W, getHeight() / H);
            scale.setX(s);
            scale.setY(s);
        };
        widthProperty().addListener(fit);
        heightProperty().addListener(fit);
        ImageView page = new ImageView(new Image(getClass().getResourceAsStream("/images/adventure-page.png")));
        page.setFitWidth(W);
        page.setPreserveRatio(true);
        Label title = text("札  " + heroClass.name() + " DECK", 30);
        grid.getChildren().addAll(page, title, deckScroll, HomeView.back(() -> router.goTo(Screen.CLASS_SELECT)));
        Label held = text(String.join("   ", picked), 20);
        held.relocate(1185, 790);
        grid.getChildren().add(held);
        if (picked.size() < Math.min(won, 2)) {   // one choice after win 1, one after win 2
            Label bonus = text("恩恵  CHOOSE BONUS", 26);
            bonus.relocate(1230, 740);
            HomeView.clickable(bonus, false, () -> chooseBonus(router));
            grid.getChildren().add(bonus);
        }
        title.relocate(1185, 130);
        deckScroll.relocate(1185, 185);
        deckList();
        for (int i = 0; i < ADVENTURES.length; i++) panel(router, i);
        if (won == 3) reward(router);
        getChildren().add(grid);
        setStyle("-fx-background-color: black;");
    }

    /** The Choose bonus pop-up: the upgrades not taken yet; picking one keeps it for the rest of the run. */
    private void chooseBonus(SceneRouter router) {
        VBox box = new VBox(24, text("恩恵を選べ   CHOOSE A BONUS", 36));
        box.setAlignment(Pos.CENTER);
        for (String[] u : UPGRADES) if (!picked.contains(u[0])) {
            Label b = text(u[0] + "   " + u[1], 26);
            HomeView.clickable(b, false, () -> {
                List<String> p = new ArrayList<>(picked);
                p.add(u[0]);
                open(router, heroClass, won, p);
            });
            box.getChildren().add(b);
        }
        grid.getChildren().add(new StackPane(new Rectangle(W, H, Color.rgb(0, 0, 0, 0.8)), box));
    }

    /** Finishing the run: the class's kami grows into view (GDD, Reward); Claim goes back Home. */
    private void reward(SceneRouter router) {
        CardView card = new CardView(KAMI.get(heroClass), 300);
        ScaleTransition grow = new ScaleTransition(Duration.millis(700), card);
        grow.setFromX(0);
        grow.setFromY(0);
        grow.setToX(1);
        grow.setToY(1);
        grow.play();
        Label claim = text("受領  CLAIM", 30);
        HomeView.clickable(claim, false, () -> router.goTo(Screen.HOME));
        VBox box = new VBox(24, text("報酬  REWARD", 40), card, claim);
        box.setAlignment(Pos.CENTER);
        grid.getChildren().add(new StackPane(new Rectangle(W, H, Color.rgb(0, 0, 0, 0.8)), box));
    }

    /** Battle i: its banner over its panel. Won = red 完了 stamp, next = click to fight, later = shaded. */
    private void panel(SceneRouter router, int i) {
        double x0 = LEFT[i] * K, x1 = RIGHT[i] * K;
        Group banner = Fx.waving(new Image(getClass().getResourceAsStream("/images/banners/adventure-" + ADVENTURES[i] + ".png")), 250, 60);
        banner.relocate((x0 + x1) / 2 - 125, 70);
        if (i > won) banner.setEffect(new ColorAdjust(0, -0.6, -0.5, 0));
        if (i == won) HomeView.clickable(banner, true, () -> {
            router.register(Screen.ADVENTURE_BATTLE, () -> OniShinTanApp.match(BOARDS[i], heroClass, FOES.get(heroClass)[i],
                    v -> open(router, heroClass, v ? won + 1 : 0, v ? picked : List.of())));
            router.setMusic(Screen.ADVENTURE_BATTLE, BOARD_MUSIC[i]);   // the town, the sea or Mount Fuji theme
            router.goTo(Screen.ADVENTURE_BATTLE);
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
        return scroll;
    }

    /** The deck this class gets for the run. Placeholder: the class's mock cards plus the neutral ones, cycled to 30. */
    private List<Card> deck() {
        List<Card> pool = MockCards.ALL.stream().filter(c -> c.getHeroClass() == heroClass || c.getHeroClass() == HeroClass.NEUTRAL).toList();
        return IntStream.range(0, 30).mapToObj(i -> pool.get(i % pool.size())).toList();
    }

    /** Cream text in the card font with a soft shadow, readable on the frame's dark inside. */
    private static Label text(String s, double size) {
        Label l = new Label(s);
        l.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: " + size + "; -fx-font-weight: bold; -fx-text-fill: #f3ede1;");
        l.setEffect(new DropShadow(6, Color.BLACK));
        return l;
    }
}