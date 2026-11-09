package fr.onishintan.ui;

import fr.onishintan.model.Card;
import fr.onishintan.model.HeroClass;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javafx.beans.InvalidationListener;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;

/** The Adventure run map on the 1600 x 900 grid, scaled to the window: three battles in order (Town, Ocean, Fuji),
 *  each with its banner over its panel, and the class deck in the frame on the right (GDD, Adventure). */
public class AdventureSelectView extends Pane {
    public static final double W = 1600, H = 900, K = W / 1672;   // the page image is 1672 x 941
    private static final String[] ADVENTURES = { "machi", "taifu", "fuji" };
    private static final double[] LEFT = { 8, 428, 822 }, RIGHT = { 418, 812, 1172 };   // each panel's edges in the image
    private static final String[] BOARDS = { "town", "ship", "fuji" };
    /** Your opponent in battles 1 to 3, per class (GDD, Adventure). */
    private static final Map<HeroClass, HeroClass[]> FOES = Map.of(
            HeroClass.SAMURAI, new HeroClass[] { HeroClass.KANNUSHI, HeroClass.ONI, HeroClass.NINJA },
            HeroClass.NINJA, new HeroClass[] { HeroClass.ONI, HeroClass.KANNUSHI, HeroClass.SAMURAI },
            HeroClass.KANNUSHI, new HeroClass[] { HeroClass.SAMURAI, HeroClass.NINJA, HeroClass.ONI },
            HeroClass.ONI, new HeroClass[] { HeroClass.NINJA, HeroClass.SAMURAI, HeroClass.KANNUSHI });

    /** Shows the run map for this class after `won` victories. */
    static void open(SceneRouter router, HeroClass c, int won) {
        router.register(Screen.ADVENTURE_SELECT, () -> new AdventureSelectView(router, c, won));
        router.goTo(Screen.ADVENTURE_SELECT);
    }

    private final Pane grid = new Pane();
    private final GridPane list = new GridPane();
    private final int won;
    private final HeroClass heroClass;

    public AdventureSelectView(SceneRouter router, HeroClass heroClass, int won) {
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
        title.relocate(1185, 130);
        list.setHgap(16);
        list.setVgap(6);
        list.relocate(1185, 185);
        grid.getChildren().addAll(page, title, list);
        deckList();
        for (int i = 0; i < ADVENTURES.length; i++) panel(router, i);
        getChildren().add(grid);
        setStyle("-fx-background-color: black;");
    }

    /** Battle i: its banner over its panel. Won = red 完了 stamp, next = click to fight, later = shaded. */
    private void panel(SceneRouter router, int i) {
        double x0 = LEFT[i] * K, x1 = RIGHT[i] * K;
        Group banner = Fx.waving(new Image(getClass().getResourceAsStream("/images/banners/adventure-" + ADVENTURES[i] + ".png")), 250, 60);
        banner.relocate((x0 + x1) / 2 - 125, 70);
        if (i > won) banner.setEffect(new ColorAdjust(0, -0.6, -0.5, 0));
        if (i == won) HomeView.clickable(banner, true, () -> {
            router.register(Screen.ADVENTURE_BATTLE, () -> OniShinTanApp.match(BOARDS[i], heroClass, FOES.get(heroClass)[i],
                    v -> open(router, heroClass, v ? won + 1 : 0)));   // a defeat restarts the run
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

    /** Fills the frame with the class deck: Ki, name, copies, by Ki cost. */
    private void deckList() {
        Map<Card, Long> counts = deck().stream().sorted(Comparator.comparingInt(Card::getKiCost))
                .collect(Collectors.groupingBy(c -> c, LinkedHashMap::new, Collectors.counting()));
        int row = 0;
        for (Map.Entry<Card, Long> e : counts.entrySet())
            list.addRow(row++, text(String.valueOf(e.getKey().getKiCost()), 20), text(e.getKey().getNameRomaji(), 20), text("×" + e.getValue(), 20));
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