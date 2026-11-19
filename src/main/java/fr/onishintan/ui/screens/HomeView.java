package fr.onishintan.ui.screens;

import fr.onishintan.ui.fx.Anim;
import fr.onishintan.ui.fx.Fx;
import fr.onishintan.model.HeroClass;
import fr.onishintan.ui.app.SceneRouter;
import fr.onishintan.ui.app.Profile;
import fr.onishintan.ui.decks.DecksView;
import fr.onishintan.ui.decks.DeckPickView;
import fr.onishintan.ui.app.Screen;
import fr.onishintan.ui.app.Sound;
import fr.onishintan.ui.app.Images;
import fr.onishintan.ui.app.Rei;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.beans.InvalidationListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;

/** The Home screen on a 1600 x 900 grid, scaled to the window. Layers, back to front:
 *  background, clouds, pines, sakura, waves, then banners, scroll and buttons. */
public class HomeView extends Pane {
    public static final double W = 1600, H = 900;
    public static final int MAX_REI = 6000;
    /** The BACK sign every screen uses to go back, and its height on screen. */
    private static final String BACK_SIGN = "/images/frames/back-button.png";
    private static final double BACK_H = 64;

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
        profile(175, 20, Profile.name(), Profile.stats().rei());
        button("adventure", 480, 610, 300, Screen.CLASS_SELECT);
        clickable(add("button-pvp", 820, 610, 300), false, () -> grid.getChildren().add(new DeckPickView(router)));   // laid over the home screen
        // Bottom bar: Deck Builder shows every deck, Inventory and Profile go to their screen, Settings opens over the home screen.
        clickable(add("button-deck-builder", 390, 790, 190), false, () -> DecksView.open(router, null));
        button("inventory", 600, 790, 190, Screen.INVENTORY);
        button("profile", 810, 790, 190, Screen.PROFILE);
        clickable(add("button-settings", 1020, 790, 190), false, () -> grid.getChildren().add(new SettingsPanel(null)));   // laid over the home screen
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
        clickable(g, true, () -> grid.getChildren().add(new HeroPicker(HeroClass.valueOf(heroClass.toUpperCase()))));   // pick this class's hero
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

    /** The 戻る BACK sign (images/frames/back-button.png) in a screen's top-left corner. */
    public static ImageView back(Runnable action) {
        ImageView sign = new ImageView(Images.get(BACK_SIGN));
        sign.setFitHeight(BACK_H);
        sign.setPreserveRatio(true);
        sign.relocate(24, 20);
        clickable(sign, false, action);
        return sign;
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

    /** The profile scroll: the player's name, the Rei rank (霊, the points out of 6000) and its stone, drawn on the paper
     *  so the writing waves with it like the banners. Hover explains the rank. */
    private void profile(double x, double y, String name, int rei) {
        int points = Math.max(0, Math.min(MAX_REI, rei));
        Rei.Stone held = Rei.stoneOf(points);
        ImageView stone = new ImageView(Images.get("/images/rei-stones/" + held.id() + ".png"));
        stone.setFitWidth(50);
        stone.setPreserveRatio(true);
        VBox text = new VBox(6,
                label(name, 26),
                label("霊", 40),
                label(points + " Rei", 24),
                label("/ " + MAX_REI, 14),
                stone,
                label(held.name() + " " + held.kanji(), 18));
        text.setAlignment(Pos.TOP_CENTER);
        VBox.setMargin(stone, new Insets(30, 0, 0, 0));   // the stone sits lower, apart from the Rei
        text.setPrefWidth(130);
        text.relocate(20, 90);
        ImageView paper = new ImageView(Images.get("/images/home/scroll.png"));
        paper.setFitWidth(170);
        paper.setPreserveRatio(true);
        Pane page = new Pane(paper, text);   // the paper with its writing, turned into one picture
        page.setStyle("-fx-background-color: transparent;");   // no default light background behind the paper
        new Scene(page, null);   // a scene of its own, so the labels get their look and size before the snapshot
        SnapshotParameters sharp = new SnapshotParameters();
        sharp.setFill(Color.TRANSPARENT);
        sharp.setTransform(new Scale(2, 2));   // drawn twice as big, so it stays sharp in a big window
        Group scroll = Fx.waving(page.snapshot(sharp, null), 170, 90);   // rod 90: 45 at twice the size
        scroll.relocate(x, y);
        Tooltip.install(scroll, new Tooltip("Rei (霊): your rank, from 0 to " + MAX_REI + " points. Your stone: " + held.name() + " " + held.kanji()));
        grid.getChildren().add(scroll);
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