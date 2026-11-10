package fr.onishintan.ui.screens;
import fr.onishintan.ui.fx.Anim;
import fr.onishintan.ui.fx.Fx;
import fr.onishintan.ui.app.SceneRouter;
import fr.onishintan.ui.app.Screen;
import fr.onishintan.ui.app.Sound;

import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.beans.InvalidationListener;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Scale;
import javafx.scene.Node;
import javafx.util.Duration;
import javafx.scene.media.AudioClip;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.Group;

/** The Home screen on a 1600 x 900 grid, scaled to the window. Layers, back to front:
 *  background, clouds, pines, sakura, waves, then banners, scroll and buttons. */
public class HomeView extends Pane {
    public static final double W = 1600, H = 900;
    public static final int MAX_REI = 6000;
    private static final String PLAYER_NAME = "Kayne";   // placeholders until the player profile is saved
    private static final int PLAYER_REI = 2450;

    private final Pane grid = new Pane();
    private final SceneRouter router;

    public static final AudioClip CLICK = Sound.clip("button-click");

    public HomeView(SceneRouter router) {
        this.router = router;
        Scale scale = new Scale();
        grid.getTransforms().add(scale);
        InvalidationListener fit = o -> {
            double s = Math.min(getWidth() / W, getHeight() / H);
            scale.setX(s);
            scale.setY(s);
        };
        widthProperty().addListener(fit);
        heightProperty().addListener(fit);
        getChildren().add(grid);

        add("background", 0, 0, W);
        // 1. clouds, in the sky: slow, wide drift
        floatOnSpot(add("cloud-1", 1040, 150, 380), 30, 5, 12);
        floatOnSpot(add("cloud-3", 380, 120, 300), 25, 4, 10);
        floatOnSpot(add("cloud-5", 1250, 250, 300), 30, 5, 14);
        floatOnSpot(add("cloud-7", 560, 250, 260), 20, 4, 11);
        // 2. pine foliage on the left tree: 15 % bigger to hide the bare branches, a very light sway
        floatOnSpot(add("pine-1", -75, 6, 530), 1.5, 1, 4.5);
        floatOnSpot(add("pine-2", 120, 220, 440), 1.5, 1, 4);
        floatOnSpot(add("pine-3", -52, 382, 345), 1.5, 1, 5);
        // 3. sakura on the right hill: the tree stays, the leaves barely rustle
        add("sakura-tree", 1405, 580, 90);
        floatOnSpot(add("sakura-leaves", 1403, 577, 90), 1, 0.8, 3.5);
        // 4. waves: a calm swell, the front layer still moves the most
        floatOnSpot(add("wave-back", -120, 452, 1840), 6, 3, 6);
        floatOnSpot(add("wave-mid", -120, 542, 1840), 9, 4, 5);
        floatOnSpot(add("wave-front", -120, 602, 1840), 12, 5, 4.5);
                // 5. top layer: banners, scroll, buttons
        String[] classes = { "samurai", "ninja", "kannushi", "oni" };
        for (int i = 0; i < classes.length; i++) banner(classes[i], 960 + i * 160, 20, 150);
        add("scroll", 175, 20, 170);
        add("scroll", 175, 20, 170);
        profile(175, 20, PLAYER_NAME, PLAYER_REI);
        button("adventure", 480, 610, 300, Screen.CLASS_SELECT);
        button("pvp", 820, 610, 300, Screen.MATCH);
        String[] bar = { "deck-builder", "inventory", "profile", "settings" };
        Screen[] to = { Screen.DECK_BUILDER, Screen.INVENTORY, Screen.PROFILE, Screen.SETTINGS };
        for (int i = 0; i < bar.length; i++) button(bar[i], 390 + i * 210, 790, 190, to[i]);
    }

    /** Puts /images/home/<name>.png on the grid at (x, y), w wide (height follows the image), and returns it. */
    private ImageView add(String name, double x, double y, double w) {
        ImageView v = new ImageView(new Image(getClass().getResourceAsStream("/images/home/" + name + ".png")));
        v.setFitWidth(w);
        v.setPreserveRatio(true);
        v.relocate(x, y);
        grid.getChildren().add(v);
        return v;
    }

    /** A plaque that goes to its screen the moment it's pressed. */
    private void button(String name, double x, double y, double w, Screen to) {
        clickable(add("button-" + name, x, y, w), false, () -> router.goTo(to));
    }

    private void banner(String heroClass, double x, double y, double w) {
        Group g = Fx.waving(new Image(getClass().getResourceAsStream("/images/home/banner-" + heroClass + ".png")), w, 45);
        g.relocate(x, y);
        grid.getChildren().add(g);
        clickable(g, true, () -> router.goTo(Screen.DECK_BUILDER));
    }

    /** Hover: small dark shadow and 4 % bigger (hanging items grow down from their top). Press: click sound, then the action. */
    public static void clickable(Node v, boolean hanging, Runnable action) {
        double s = 1.04;
        v.setOnMouseEntered(e -> {
            double down = hanging ? (s - 1) * v.getBoundsInLocal().getHeight() / 2 : 0;
            v.setEffect(new DropShadow(12, Color.BLACK));
            Anim.to(v, Anim.ease(v.scaleXProperty(), s), Anim.ease(v.scaleYProperty(), s), Anim.ease(v.translateYProperty(), down));
        });
        v.setOnMouseExited(e -> {
            v.setEffect(null);
            Anim.to(v, Anim.ease(v.scaleXProperty(), 1.0), Anim.ease(v.scaleYProperty(), 1.0), Anim.ease(v.translateYProperty(), 0.0));
        });
        v.setOnMousePressed(e -> { Sound.play(CLICK); action.run(); });
    }

    /** The "← 戻る RETURN" box in a screen's top-left corner. */
    static Label back(Runnable action) {
        Label b = new Label("← 戻る  RETURN");
        b.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: #f3ede1;"
                + " -fx-background-color: rgba(20,12,8,0.75); -fx-background-radius: 10; -fx-padding: 8 16;");
        b.relocate(30, 30);
        clickable(b, false, action);
        return b;
    }

    /** Floats a node on the spot: drifts up to dx / dy each way and back, forever, one way every `seconds`.
     *  Each starts at a random point so nothing moves in sync; it stops when the screen is left. */
    private static void floatOnSpot(Node n, double dx, double dy, double seconds) {
        TranslateTransition t = new TranslateTransition(Duration.seconds(seconds), n);
        t.setFromX(-dx);
        t.setToX(dx);
        t.setFromY(-dy);
        t.setToY(dy);
        t.setAutoReverse(true);
        t.setCycleCount(Animation.INDEFINITE);
        t.setInterpolator(Interpolator.EASE_BOTH);
        t.playFrom(Duration.seconds(Math.random() * seconds));
        n.sceneProperty().addListener((o, was, now) -> { if (now == null) t.stop(); });
    }

    /** The player's name, then the Rei rank: 霊 as a title, the points labelled "Rei", out of 6000. Hover explains it. */
    private void profile(double x, double y, String name, int rei) {
        int points = Math.max(0, Math.min(MAX_REI, rei));
        VBox text = new VBox(6,
                label(name, 26),
                label("霊", 40),
                label(points + " Rei", 24),
                label("/ " + MAX_REI, 14));
        text.setAlignment(Pos.TOP_CENTER);
        text.setPrefWidth(130);
        text.relocate(x + 20, y + 90);
        Tooltip.install(text, new Tooltip("Rei (霊): your rank, from 0 to " + MAX_REI + " points"));
        grid.getChildren().add(text);
    }

    /** Dark ink text in the card font, centred and wrapping if too long. */
    private static Label label(String s, double size) {
        Label l = new Label(s);
        l.setFont(Font.font("Yu Mincho", FontWeight.BOLD, size));
        l.setTextFill(Color.web("#1f1b17"));
        l.setWrapText(true);
        l.setTextAlignment(TextAlignment.CENTER);
        return l;
    }
}