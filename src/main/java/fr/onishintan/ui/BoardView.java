package fr.onishintan.ui;

import fr.onishintan.model.Hero;
import fr.onishintan.model.Minion;
import fr.onishintan.model.Player;
import java.util.List;
import javafx.beans.InvalidationListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.scene.shape.Line;

/** The match table on a 1600 x 900 grid (zones from the Izakaya mock-up), scaled to fit the window. */
public class BoardView extends Pane {
    public static final double W = 1600, H = 900;
    public static final double SLOT_X = 352, SLOT_STEP = 130.5, ENEMY_Y = 257, YOUR_Y = 443, MINION_W = 114;

    private final Player enemy, you;
    private final String enemyPortrait, yourPortrait;
    private final Pane table = new Pane();
    private final ImageView bg;

    private static final String HUD = "-fx-background-color: rgba(20,12,8,0.75); -fx-text-fill: #f3ede1; -fx-font-weight: bold;"
        + " -fx-background-radius: 10; -fx-alignment: center; -fx-text-alignment: center;";

    private final Pane live = new Pane();
    private final Button endTurn = hud(new Button("END TURN"), 1381, 392, 190, 84, 26);
    private final Button yourPower = hud(new Button("HERO\nPOWER"), 948, 639, 158, 113, 18);
    private final Button enemyPower = hud(new Button("HERO\nPOWER"), 948, 112, 158, 113, 18);
    private final Label yourClock = hud(new Label(), 1381, 500, 190, 71, 30);
    private final Label enemyClock = hud(new Label(), 1381, 301, 190, 71, 30);

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
        endTurn.setEffect(new DropShadow(25, Color.GOLD));
        enemyPower.setMouseTransparent(true);
        table.getChildren().addAll(bg, live, endTurn, yourPower, enemyPower, yourClock, enemyClock);
        getChildren().add(table);
        render();
    }

    /** Redraws everything that changes during a match: heroes, minions, Ki and decks. */
    public void render() {
        live.getChildren().clear();
        hero(enemy.getHero(), enemyPortrait, 107);
        hero(you.getHero(), yourPortrait, 637);
        row(enemy, ENEMY_Y);
        row(you, YOUR_Y);
        kiBar(you);
        live.getChildren().add(hud(new Label("OPPONENT KI\n" + enemy.getKi() + " / " + Player.KI_CAP), 1452, 14, 132, 67, 16));
        deck(enemy, 182, 239);
        deck(you, 1227, 632);
    }

    /** Both chess clocks: the active one is big and bright, the other small and grey (GDD). */
    public void showClocks(String yours, String theirs, boolean yourTurn) {
        yourClock.setText(yours);
        enemyClock.setText(theirs);
        clock(yourClock, yourTurn);
        clock(enemyClock, !yourTurn);
    }

    public Button getEndTurn() { return endTurn; }
    public Button getYourPower() { return yourPower; }

    private static void clock(Label l, boolean active) {
        l.setOpacity(active ? 1 : 0.5);
        l.setScaleX(active ? 1 : 0.8);
        l.setScaleY(active ? 1 : 0.8);
    }

    /** Your Ki as a vertical bar filling from the bottom, with a marker every 1 Ki.
     *  Bright pink = available, pale pink = spent this turn, empty = not unlocked yet. */
    private void kiBar(Player p) {
        Label bar = hud(new Label("KI\n(MAX 12)"), 48, 220, 113, 546, 16);
        bar.setStyle(bar.getStyle() + " -fx-alignment: top-center; -fx-padding: 8 0 0 0;");
        double x = 89, top = 285, w = 30, h = 420, step = h / Player.KI_CAP;
        Rectangle track = new Rectangle(x, top, w, h);
        track.setFill(Color.TRANSPARENT);
        track.setStroke(Color.web("#f3ede1", 0.4));
        track.setStrokeWidth(2);
        Rectangle unlocked = new Rectangle(x, top + h - p.getMaxKi() * step, w, p.getMaxKi() * step);
        unlocked.setFill(Color.HOTPINK.deriveColor(0, 1, 1, 0.25));
        Rectangle ki = new Rectangle(x, top + h - p.getKi() * step, w, p.getKi() * step);
        ki.setFill(Color.HOTPINK);
        ki.setEffect(new DropShadow(15, Color.HOTPINK));
        live.getChildren().addAll(bar, track, unlocked, ki);
        for (int i = 1; i < Player.KI_CAP; i++) {
            Line mark = new Line(x - 5, top + i * step, x + w + 5, top + i * step);
            mark.setStroke(Color.web("#f3ede1", 0.6));
            mark.setStrokeWidth(1.5);
            live.getChildren().add(mark);
        }
        live.getChildren().add(hud(new Label(p.getKi() + " / " + Player.KI_CAP), 59, 713, 92, 38, 18));
    }

    /** A deck as a card back, centred in its zone; hovering shows how many cards are left (GDD). */
    private void deck(Player p, double x, double y) {
        ImageView back = image("/images/frames/card-back.png");
        back.setFitWidth(MINION_W);
        back.setPreserveRatio(true);
        Tooltip.install(back, new Tooltip(p.getDeck().size() + " cards left"));
        Rectangle corners = new Rectangle(MINION_W, MINION_W * CardView.H / CardView.W);
        corners.setArcWidth(30 * MINION_W / CardView.W);
        corners.setArcHeight(30 * MINION_W / CardView.W);
        back.setClip(corners);
        place(back, x + (115 - MINION_W) / 2, y + (127 - MINION_W * CardView.H / CardView.W) / 2);
    }

    /** Styles a label or button as a HUD box of the given size and font, at (x, y) on the grid. */
    private static <T extends Labeled> T hud(T node, double x, double y, double w, double h, int font) {
        node.setStyle(HUD + " -fx-font-size: " + font + ";");
        node.setPrefSize(w, h);
        node.relocate(x, y);
        return node;
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
        live.getChildren().add(n);
    }

    private ImageView image(String path) { return new ImageView(new Image(getClass().getResourceAsStream(path))); }
}