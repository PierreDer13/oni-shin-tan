package fr.onishintan.ui;

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

/** Class Select on the 1600 x 900 grid, scaled to the window: the four class banners wave side by side;
 *  clicking one opens the Adventure page for that class (its decklist, later its match sequence). */
public class ClassSelectView extends Pane {
    public static final double W = 1600, H = 900;
    private static final HeroClass[] CLASSES = { HeroClass.SAMURAI, HeroClass.NINJA, HeroClass.KANNUSHI, HeroClass.ONI };

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
        ImageView bg = new ImageView(new Image(getClass().getResourceAsStream("/images/home/background.png")));
        bg.setFitWidth(W);
        bg.setFitHeight(H);
        Label title = new Label("流派を選べ   CHOOSE YOUR CLASS");
        title.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: 40; -fx-font-weight: bold; -fx-text-fill: #1f1b17; -fx-alignment: center;");
        title.setEffect(new DropShadow(12, Color.WHITE));
        title.setPrefWidth(W);
        title.relocate(0, 30);
        grid.getChildren().addAll(bg, title);
        for (int i = 0; i < CLASSES.length; i++) {
            HeroClass c = CLASSES[i];
            Group banner = Fx.waving(new Image(getClass().getResourceAsStream("/images/banners/class-" + c.name().toLowerCase() + ".png")), 250, 60);
            banner.relocate(165 + i * 340, 110);
            HomeView.clickable(banner, true, () -> {
                router.register(Screen.ADVENTURE_SELECT, () -> new AdventureSelectView(router, c));
                router.goTo(Screen.ADVENTURE_SELECT);
            });
            grid.getChildren().add(banner);
        }
        getChildren().add(grid);
        setStyle("-fx-background-color: black;");
    }
}
