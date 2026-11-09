package fr.onishintan.ui;

import java.util.Map;
import java.util.List;

import fr.onishintan.model.HeroClass;

import javafx.beans.InvalidationListener;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.scene.input.MouseEvent;

/** Class Select on the 1600 x 900 grid, scaled to the window: the four class banners wave side by side;
 *  clicking one opens the Adventure page for that class (its decklist, later its match sequence). */
public class ClassSelectView extends Pane {
    public static final double W = 1600, H = 900;
    private static final HeroClass[] CLASSES = { HeroClass.SAMURAI, HeroClass.NINJA, HeroClass.KANNUSHI, HeroClass.ONI };
    /** Hover text for each class (GDD, Hero classes): its pace, then its passive and hero power. */
    private static final Map<HeroClass, String> ABOUT = Map.of(
            HeroClass.SAMURAI, "侍 Samurai: minion-heavy tempo and board control. Starts with armour; Katana equips a blade.",
            HeroClass.NINJA, "忍者 Ninja: swift play, minions and spells balanced. Survives one lethal hit; Shuriken deals 6.",
            HeroClass.KANNUSHI, "神主 Kannushi: spells, small minions and buffs. Heals every turn; summons a 3/3 Miko.",
            HeroClass.ONI, "鬼 Oni: slow, large minions. Curses the enemy deck with Korori; Jikininki drains 4 HP.");

    public ClassSelectView(SceneRouter router) {
        Pane grid = new Pane();
        Scale scale = new Scale();
        grid.getTransforms().add(scale);
        InvalidationListener fit = o -> {
            double s = Math.min(getWidth() / W, getHeight() / H);
            scale.setX(s);
            scale.setY(s);
        };
        widthProperty().addListener(fit);
        heightProperty().addListener(fit);
        ImageView bg = new ImageView(new Image(getClass().getResourceAsStream("/images/class-select.png")));
        bg.setFitWidth(W);
        Label title = new Label("流派を選べ   CHOOSE YOUR CLASS");
        title.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: 40; -fx-font-weight: bold; -fx-text-fill: #1f1b17; -fx-alignment: center;");
        title.setEffect(new DropShadow(12, Color.WHITE));
        title.setPrefWidth(W);
        title.relocate(0, 30);
        Label about = new Label();   // the hovered class's description, empty otherwise
        about.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #1f1b17; -fx-alignment: center;");
        about.setEffect(new DropShadow(10, Color.WHITE));
        about.setPrefWidth(W);
        about.relocate(0, 82);
        Label back = HomeView.back(() -> router.goTo(Screen.HOME));
        grid.getChildren().addAll(bg, title, about, back);
        for (int i = 0; i < CLASSES.length; i++) {
            HeroClass c = CLASSES[i];
            Group banner = Fx.waving(new Image(getClass().getResourceAsStream("/images/banners/class-" + c.name().toLowerCase() + ".png")), 250, 60);
            banner.relocate((212 + i * 416) * 1600 / 1672 - 125, 110);
            HomeView.clickable(banner, true, () -> AdventureSelectView.open(router, c, 0, List.of()));
            banner.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> about.setText(ABOUT.get(c)));
            banner.addEventHandler(MouseEvent.MOUSE_EXITED, e -> about.setText(""));
            grid.getChildren().add(banner);
        }
        getChildren().add(grid);
        setStyle("-fx-background-color: black;");
    }
}
