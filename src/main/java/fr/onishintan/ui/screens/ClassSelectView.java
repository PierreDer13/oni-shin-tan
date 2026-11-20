package fr.onishintan.ui.screens;
import fr.onishintan.ui.app.Images;
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
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.scene.input.MouseEvent;

/** Class Select on the 1600 x 900 grid, scaled to the window: the four class banners wave side by side;
 *  clicking one opens the Adventure page for that class (its decklist, later its match sequence). */
public class ClassSelectView extends Pane {
    public static final double W = 1600, H = 900;
    private static final HeroClass[] CLASSES = { HeroClass.SAMURAI, HeroClass.NINJA, HeroClass.KANNUSHI, HeroClass.ONI };
    /** The run the banners start: normal, or Shura no Michi when the switch is on. */
    private AdventureMode mode = AdventureMode.NORMAL;
    /** The red frame shown around the screen while Shura no Michi is on. */
    private final Rectangle hardcoreBorder = new Rectangle(W, H);
    /** Hover text for each class's Adventure: its story, its three foes in order, and the kami it wins (StoryLines, AdventureDecks). */
    private static final Map<HeroClass, String> ABOUT = Map.of(
            HeroClass.SAMURAI, "侍 Samurai: a masterless rōnin hunts the traitor who burned his lord's castle. "
                    + "Face the Kannushi, the Oni, then the Ninja on Mount Fuji. Reward: Takemikazuchi, god of thunder.",
            HeroClass.NINJA, "忍者 Ninja: steal back the Scroll of the Hidden Moon before the new moon rises. "
                    + "Face the Oni, the Kannushi, then the shōgun's Samurai. Reward: Tsukuyomi, god of the moon.",
            HeroClass.KANNUSHI, "神主 Kannushi: the shrine bell has cracked and the kami have fallen silent. "
                    + "Face the Samurai, the Ninja, then the Oni on Mount Fuji. Reward: Ōkuninushi, kami of the land.",
            HeroClass.ONI, "鬼 Oni: break the paper charms that have bound your kin for three hundred years. "
                    + "Face the Ninja, the Samurai, then the Kannushi on Mount Fuji. Reward: Izanami, queen of the underworld.");

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
        ImageView bg = new ImageView(Images.get("/images/class-select.png"));
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
        hardcoreBorder.setFill(new RadialGradient(0, 0, 0.5, 0.5, 0.75, true, CycleMethod.NO_CYCLE,   // a dull red vignette, for Shura no Michi
                new Stop(0.45, Color.rgb(90, 12, 12, 0)),        // the middle stays clear
                new Stop(1, Color.rgb(90, 12, 12, 0.65))));      // the edges darken into a muted blood red
        hardcoreBorder.setMouseTransparent(true);
        grid.getChildren().add(hardcoreBorder);
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
        Label hardcore = new Label("Hardcore mode: health is not restored between fights.");
        hardcore.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #f3ede1; -fx-alignment: center;");
        hardcore.setEffect(new DropShadow(6, Color.BLACK));
        hardcore.setPrefWidth(500);
        hardcore.relocate(W / 2 - 250, 868);   // under the switch
        hardcore.visibleProperty().bind(hardcoreBorder.visibleProperty());   // only shown while Shura no Michi is on, like the red vignette
        grid.getChildren().add(hardcore);
        Label orochi = new Label(">");   // turns the page to Yamata no Orochi
        orochi.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: 28; -fx-font-weight: bold; -fx-text-fill: #f3ede1;"
                + " -fx-background-color: rgba(20, 12, 8, 0.85); -fx-background-radius: 12; -fx-padding: 0 10 2 10;"
                + " -fx-border-color: #c9a227; -fx-border-width: 2; -fx-border-radius: 12;");
        orochi.relocate(W - 46, H / 2 - 22);
        orochi.setViewOrder(-1);   // drawn over the class banners, which are added after it
        HomeView.clickable(orochi, false, () -> OrochiView.open(router));
        grid.getChildren().add(orochi);
        for (int i = 0; i < CLASSES.length; i++) {
            HeroClass c = CLASSES[i];
            Group banner = Fx.waving(Images.get("/images/banners/class-" + c.name().toLowerCase() + ".png"), 250, 60);
            banner.relocate((212 + i * 416) * 1600 / 1672 - 125, 110);
            HomeView.clickable(banner, true, () -> AdventureSelectView.resume(router, c, mode));
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
        hardcoreBorder.setVisible(mode == AdventureMode.HARDCORE);   // the red frame only in Shura no Michi
    }
}
