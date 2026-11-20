package fr.onishintan.ui.screens;
import fr.onishintan.ui.fx.Fx;
import fr.onishintan.ui.app.SceneRouter;
import fr.onishintan.ui.app.Screen;

import java.util.Map;
import java.util.List;

import fr.onishintan.model.HeroClass;
import fr.onishintan.adventure.AdventureMode;

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
    /** The run the banners start: normal, or Shura no Michi when the switch is on. */
    private AdventureMode mode = AdventureMode.NORMAL;
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
            scale.setX(getWidth() / W);    // stretched to fill the window: no strip on a screen that isn't 16:9
            scale.setY(getHeight() / H);
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
        about.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: 17; -fx-font-weight: bold; -fx-text-fill: #f3ede1; -fx-alignment: center;"
                + " -fx-background-color: rgba(20, 12, 8, 0.8); -fx-background-radius: 8; -fx-padding: 4 12 4 12;");   // cream on a dark strip, readable on the art
        about.setEffect(new DropShadow(8, Color.BLACK));
        about.setPrefWidth(1000);
        about.relocate((W - 1000) / 2, 82);   // a strip in the middle, not the whole width
        about.visibleProperty().bind(about.textProperty().isNotEmpty());   // the dark strip only while something is hovered
        ImageView back = HomeView.back(() -> router.goTo(Screen.HOME));
        grid.getChildren().addAll(bg, title, about, back);
        // The Shura no Michi switch: a click turns the hardcore run on or off; hovering it tells its rules.
        Label shura = new Label();
        shura.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: 22; -fx-font-weight: bold; -fx-alignment: center;"
                + " -fx-background-color: rgba(20, 12, 8, 0.85); -fx-background-radius: 12;"
                + " -fx-border-color: #c9a227; -fx-border-width: 2; -fx-border-radius: 12; -fx-padding: 6 16 6 16;");   // a dark plaque, gold edge
        shura.setEffect(new DropShadow(12, Color.BLACK));
        shura.setPrefWidth(380);
        shura.relocate(W / 2 - 190, 820);
        showMode(shura);
        HomeView.clickable(shura, false, () -> {
            if (mode == AdventureMode.NORMAL) {
                mode = AdventureMode.HARDCORE;
            } else {
                mode = AdventureMode.NORMAL;
            }
            showMode(shura);
        });
        shura.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> about.setText("Hard AI from the first battle, one bonus only, and your wounds stay from battle to battle."));
        shura.addEventHandler(MouseEvent.MOUSE_EXITED, e -> about.setText(""));
        grid.getChildren().add(shura);
        for (int i = 0; i < CLASSES.length; i++) {
            HeroClass c = CLASSES[i];
            Group banner = Fx.waving(new Image(getClass().getResourceAsStream("/images/banners/class-" + c.name().toLowerCase() + ".png")), 250, 60);
            banner.relocate((212 + i * 416) * 1600 / 1672 - 125, 110);
            HomeView.clickable(banner, true, () -> AdventureSelectView.open(router, c, mode, 0, List.of(), 0));
            banner.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> about.setText(ABOUT.get(c)));
            banner.addEventHandler(MouseEvent.MOUSE_EXITED, e -> about.setText(""));
            grid.getChildren().add(banner);
        }
        getChildren().add(grid);
        setStyle("-fx-background-color: black;");
    }

    /** The switch's text: Shura no Michi on (red) or off (ink). */
    private void showMode(Label shura) {
        if (mode == AdventureMode.HARDCORE) {
            shura.setText("修羅の道  Shura no Michi: ON");
            shura.setTextFill(Color.web("#ff4a4a"));   // bright red: on
        } else {
            shura.setText("修羅の道  Shura no Michi: OFF");
            shura.setTextFill(Color.web("#f3ede1"));   // cream on the dark plaque
        }
    }
}
