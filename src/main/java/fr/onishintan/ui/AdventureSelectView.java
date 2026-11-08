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
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;

/** Adventure Select on the 1600 x 900 grid, scaled to the window. Each adventure (Machi, Taifu, Fuji) has its banner
 *  waving over its panel; choosing one outlines its panel in gold and shows its decklist in the frame on the right. */
public class AdventureSelectView extends Pane {
    public static final double W = 1600, H = 900, K = W / 1672;   // the page image is 1672 x 941
    private static final String[] ADVENTURES = { "machi", "taifu", "fuji" };
    private static final double[] LEFT = { 8, 428, 822 }, RIGHT = { 418, 812, 1172 };   // each panel's edges in the image

    private final Pane grid = new Pane();
    private final GridPane list = new GridPane();
    private Rectangle chosen;
    private final HeroClass heroClass;

    public AdventureSelectView(SceneRouter router, HeroClass heroClass) {
        this.heroClass = heroClass;
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
        Label begin = text("出発  BEGIN", 28);
        begin.relocate(1265, 735);
        HomeView.clickable(begin, false, () -> router.goTo(Screen.ADVENTURE_MAP));
        grid.getChildren().addAll(page, title, list, begin);
        for (int i = 0; i < ADVENTURES.length; i++) panel(ADVENTURES[i], LEFT[i] * K, RIGHT[i] * K);
        getChildren().add(grid);
        setStyle("-fx-background-color: black;");
    }

    /** One adventure: its banner waving over its panel (click it to choose), and a gold outline while it's chosen. */
    private void panel(String name, double x0, double x1) {
        Rectangle r = new Rectangle(x0, 10 * K, x1 - x0, 921 * K);
        r.setFill(Color.TRANSPARENT);
        r.setStroke(Color.GOLD);
        r.setStrokeWidth(4);
        r.setEffect(new DropShadow(20, Color.GOLD));
        r.setOpacity(0);
        r.setMouseTransparent(true);   // only the banner chooses; the outline just shows which one is chosen
        Group banner = Fx.waving(new Image(getClass().getResourceAsStream("/images/banners/adventure-" + name + ".png")), 250, 60);
        banner.relocate((x0 + x1) / 2 - 125, 70);
        HomeView.clickable(banner, true, () -> choose(r, name));
        grid.getChildren().addAll(r, banner);
        if (chosen == null) choose(r, name);   // Machi is shown first
    }

    /** Outlines the chosen panel (and only it) and fills the frame with its decklist: Ki, name, copies, by Ki cost. */
    private void choose(Rectangle r, String name) {
        if (chosen != null) chosen.setOpacity(0);
        chosen = r;
        r.setOpacity(1);
        list.getChildren().clear();
        Map<Card, Long> counts = deck(name).stream().sorted(Comparator.comparingInt(Card::getKiCost))
                .collect(Collectors.groupingBy(c -> c, LinkedHashMap::new, Collectors.counting()));
        int row = 0;
        for (Map.Entry<Card, Long> e : counts.entrySet())
            list.addRow(row++, text(String.valueOf(e.getKey().getKiCost()), 20), text(e.getKey().getNameRomaji(), 20), text("×" + e.getValue(), 20));
    }

    /** The deck this class gets for an adventure. Placeholder: the class's mock cards plus the neutral ones, cycled to 30. */
    private List<Card> deck(String adventure) {
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