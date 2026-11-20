package fr.onishintan.ui.screens;
import fr.onishintan.ui.app.Images;
import fr.onishintan.ui.fx.Fx;
import fr.onishintan.ui.app.SceneRouter;
import fr.onishintan.ui.app.Screen;

import java.util.Map;
import java.util.EnumMap;
import fr.onishintan.data.SaveManager;
import java.util.List;

import fr.onishintan.model.HeroClass;
import fr.onishintan.adventure.AdventureMode;

import javafx.geometry.Pos;
import javafx.beans.InvalidationListener;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.effect.BlurType;
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
    private static AdventureMode mode = AdventureMode.NORMAL;   // kept while the game runs: back on this page, the switch is as you left it
    /** The red frame shown around the screen while Shura no Michi is on. */
    private final Rectangle hardcoreBorder = new Rectangle(W, H);
    /** Each class's stamps: 完了 when it is finished in normal, 修羅 when it is finished in Shura no Michi. */
    private final Map<HeroClass, Label> normalStamps = new EnumMap<>(HeroClass.class);
    private final Map<HeroClass, Label> hardcoreStamps = new EnumMap<>(HeroClass.class);
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
            double x = (212 + i * 416) * 1600 / 1672;   // the banner's centre
            normalStamps.put(c, stamp(grid, "完了", "#ff1e1e", x, 340));
            hardcoreStamps.put(c, stamp(grid, "修羅", "#d40000", x, 440));
        }
        showStamps();
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
        showStamps();
    }

    /** How wide a stamp's box is, centred on its banner (the banners are 250 wide). */
    private static final double STAMP_W = 250;

    /**
     * One stamp in the middle of a banner (x = its centre), tilted like a seal, hidden until showStamps() shows it.
     * Bright red with a cream outline, so it stands out over the dark and red banners.
     */
    private static Label stamp(Pane grid, String word, String colour, double x, double y) {
        Label stamp = new Label(word);
        stamp.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: 70; -fx-font-weight: bold; -fx-text-fill: " + colour + ";");
        DropShadow outline = new DropShadow(BlurType.GAUSSIAN, Color.web("#f3ede1"), 5, 0.9, 0, 0);   // a thin cream edge
        outline.setInput(new DropShadow(14, Color.BLACK));
        stamp.setEffect(outline);
        stamp.setRotate(-12);
        stamp.setMouseTransparent(true);
        stamp.setVisible(false);
        stamp.setPrefWidth(STAMP_W);
        stamp.setAlignment(Pos.CENTER);
        stamp.relocate(x - STAMP_W / 2, y);   // centred on the banner
        grid.getChildren().add(stamp);
        return stamp;
    }

    /** Each class's stamps (SaveManager): 完了 for a normal clear, 修羅 for a Shura no Michi clear, whatever the switch shows. */
    private void showStamps() {
        SaveManager saves = new SaveManager();
        for (HeroClass c : normalStamps.keySet()) {
            normalStamps.get(c).setVisible(saves.isCompleted(c, AdventureMode.NORMAL));
            hardcoreStamps.get(c).setVisible(saves.isCompleted(c, AdventureMode.HARDCORE));
        }
    }
}
