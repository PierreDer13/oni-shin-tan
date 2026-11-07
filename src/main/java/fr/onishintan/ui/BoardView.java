package fr.onishintan.ui;

import fr.onishintan.model.Hero;
import fr.onishintan.model.Minion;
import fr.onishintan.model.Player;
import java.util.List;
import javafx.beans.InvalidationListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Scale;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;

/** The match table on a 1600 x 900 grid (zones from the Izakaya mock-up), scaled to fit the window. */
public class BoardView extends Pane {
    public static final double W = 1600, H = 900;
    public static final double SLOT_X = 352, SLOT_STEP = 130.5, ENEMY_Y = 257, YOUR_Y = 443, MINION_W = 114;

    private final Player enemy, you;
    private final String enemyPortrait, yourPortrait;
    private final Pane table = new Pane();
    private final ImageView bg;

    public BoardView(String board, Player enemy, String enemyPortrait, Player you, String yourPortrait) {
        this.enemy = enemy;
        this.you = you;
        this.enemyPortrait = enemyPortrait;
        this.yourPortrait = yourPortrait;
        bg = image("/images/boards/" + board + ".png");
        bg.setFitWidth(W);
        bg.setFitHeight(H);
        Scale scale = new Scale();
        table.getTransforms().add(scale);
        InvalidationListener fit = o -> {
            double s = Math.min(getWidth() / W, getHeight() / H);
            scale.setX(s);
            scale.setY(s);
        };
        widthProperty().addListener(fit);
        heightProperty().addListener(fit);
        getChildren().add(table);
        render();
    }

    /** Redraws heroes and minions from the players' current state. */
    public void render() {
        table.getChildren().setAll(bg);
        hero(enemy.getHero(), enemyPortrait, 107);
        hero(you.getHero(), yourPortrait, 637);
        row(enemy, ENEMY_Y);
        row(you, YOUR_Y);
    }

    /** One side's minions in slots 1 to 7, left to right. */
    private void row(Player p, double y) {
        List<Minion> minions = p.getSide().getMinions();
        for (int i = 0; i < minions.size(); i++) place(new MinionView(minions.get(i), MINION_W), SLOT_X + 2 + i * SLOT_STEP, y);
    }

    /** The hero's scroll, centred on the screen in its row, with HP in the scroll's bottom-right corner. */
    private void hero(Hero h, String portrait, double zoneY) {
        double hgt = 150, wid = hgt * 1.6;
        ImageView img = image("/images/heroes/" + portrait + ".png");
        img.setFitHeight(hgt);
        img.setPreserveRatio(true);
        Label hp = new Label(String.valueOf(h.getHealth()));
        hp.setStyle("-fx-background-color: #7d1a1e; -fx-text-fill: white; -fx-font-size: 18; -fx-font-weight: bold; -fx-padding: 2 10; -fx-background-radius: 12;");
        StackPane box = new StackPane(img, hp);
        StackPane.setAlignment(hp, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(hp, new Insets(0, wid * 0.05, hgt * 0.06, 0));
        place(box, W / 2 - wid / 2, zoneY + (119 - hgt) / 2);
    }

    private void place(Node n, double x, double y) {
        n.relocate(x, y);
        table.getChildren().add(n);
    }

    private ImageView image(String path) { return new ImageView(new Image(getClass().getResourceAsStream(path))); }
}