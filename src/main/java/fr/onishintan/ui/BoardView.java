package fr.onishintan.ui;

import fr.onishintan.model.Hero;
import fr.onishintan.model.HeroClass;
import fr.onishintan.model.Card;
import fr.onishintan.model.Combatant;
import fr.onishintan.model.Minion;
import fr.onishintan.model.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.InvalidationListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.util.Duration;
import javafx.scene.layout.Region;

/** The match table on a 1600 x 900 grid (zones from the Izakaya mock-up), scaled to fit the window. */
public class BoardView extends Pane {
    public static final double W = 1600, H = 900;
    public static final double SLOT_X = 352, SLOT_STEP = 130.5, ENEMY_Y = 257, YOUR_Y = 443, MINION_W = 114;
    public static final double KI_X = 89, KI_TOP = 285, KI_W = 30, KI_H = 420, KI_STEP = KI_H / Player.KI_CAP;

    private final Player enemy, you;
    private final String enemyPortrait, yourPortrait;
    private final Pane table = new Pane();
    private final ImageView bg;
    private final Rectangle kiPreview = new Rectangle(KI_X, 0, KI_W, 0);
    private final FadeTransition kiPulse = new FadeTransition(Duration.millis(700), kiPreview);
    private Combatant selected;
    private BiConsumer<Combatant, Combatant> onAttack = (a, t) -> {};

    private static final String HUD = "-fx-background-color: rgba(20,12,8,0.75); -fx-text-fill: #f3ede1; -fx-font-weight: bold;"
        + " -fx-background-radius: 10; -fx-alignment: center; -fx-text-alignment: center;";

    private final Pane live = new Pane();
    private final Button endTurn = hud(new Button("END TURN"), 1381, 392, 190, 84, 26);
    private final Label yourClock = hud(new Label(), 1381, 500, 190, 71, 30);
    private final Label enemyClock = hud(new Label(), 1381, 301, 190, 71, 30);
    private BiConsumer<Card, Integer> onPlay = (c, slot) -> {};

    /** Hero powers until the engine has them: the GDD names, and a placeholder Ki cost (still to be balanced). */
    private static final Map<HeroClass, String[]> POWER = Map.of(
            HeroClass.NINJA, new String[] { "Shuriken", "手裏剣", "Deal 6 damage to any character" },
            HeroClass.SAMURAI, new String[] { "Katana", "刀", "Equip a weapon: more Ki, stronger blade" },
            HeroClass.KANNUSHI, new String[] { "Miko", "巫女", "Summon a 3/3 Miko" },
            HeroClass.ONI, new String[] { "Jikininki", "食人鬼", "Drain 4 HP from a character to heal your hero" });
    public static final int POWER_COST = 2;

    private boolean powerUsed;
    private Runnable onPower = () -> {};

    public BoardView(String board, Player enemy, String enemyPortrait, Player you, String yourPortrait) {
        this.enemy = enemy;
        this.you = you;
        this.enemyPortrait = enemyPortrait;
        this.yourPortrait = yourPortrait;
        bg = image("/images/boards/" + board + ".png");
        bg.setFitWidth(W);
        bg.setFitHeight(H);
        bg.setOnMouseClicked(e -> { selected = null; render(); });
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
        table.getChildren().addAll(bg, live, endTurn, yourClock, enemyClock);
        getChildren().add(table);
        setStyle("-fx-background-color: black;");
        kiPulse.setFromValue(0.9);
        kiPulse.setToValue(0.35);
        kiPulse.setAutoReverse(true);
        kiPulse.setCycleCount(Animation.INDEFINITE);
        render();
    }

    /** Redraws everything that changes during a match: heroes, minions, Ki and decks. */
    public void render() {
        live.getChildren().clear();
        hero(enemy.getHero(), enemyPortrait, 107, false);
        hero(you.getHero(), yourPortrait, 637, true);
        power(enemy, 112, false);
        power(you, 639, true);
        row(enemy, ENEMY_Y);
        row(you, YOUR_Y);
        kiBar(you);
        live.getChildren().add(hud(new Label("OPPONENT KI\n" + enemy.getKi() + " / " + Player.KI_CAP), 1452, 14, 132, 67, 16));
        deck(enemy, 182, 239);
        deck(you, 1227, 632);
        enemyHand(enemy);
        live.getChildren().add(new HandView(you, (card, slot) -> { onPlay.accept(card, slot); impact(card.getKiCost()); }, this::previewCost));
    }

    /** Both chess clocks: the active one is big and bright, the other small and grey (GDD). */
    public void showClocks(String yours, String theirs, boolean yourTurn) {
        yourClock.setText(yours);
        enemyClock.setText(theirs);
        clock(yourClock, yourTurn);
        clock(enemyClock, !yourTurn);
    }

    /** What happens when you drop a card on your row (the match will call the engine here). */
    public void setOnPlay(BiConsumer<Card, Integer> onPlay) {
        this.onPlay = onPlay;
        render();
    }

    /** What happens when you use your hero power (the match will call the engine here). */
    public void setOnPower(Runnable onPower) { this.onPower = onPower; }

    /** Start of your turn: the hero power can be used again. */
    public void resetPower() {
        powerUsed = false;
        render();
    }

    /** What happens when you pick an attacker and then a target (the match will call the engine here). */
    public void setOnAttack(BiConsumer<Combatant, Combatant> onAttack) { this.onAttack = onAttack; }

    /** Dims, with a slow pulse, the part of your available Ki that a hovered card or power would cost (0 = none). */
    public void previewCost(int cost) {
        int c = Math.min(cost, you.getKi());
        kiPulse.stop();
        kiPreview.setOpacity(0);
        if (c == 0) return;
        kiPreview.setY(KI_TOP + KI_H - you.getKi() * KI_STEP);
        kiPreview.setHeight(c * KI_STEP);
        kiPulse.playFromStart();
    }

    public Button getEndTurn() { return endTurn; }

    private static void clock(Label l, boolean active) {
        l.setOpacity(active ? 1 : 0.5);
        l.setScaleX(active ? 1 : 0.8);
        l.setScaleY(active ? 1 : 0.8);
    }

    /** Your Ki: a glowing rounded capsule filling from the bottom, a faint mark per Ki, and pink spirit motes
     *  rising beside the filled part. Bright = available, pale = spent this turn, empty = locked. */
    private void kiBar(Player p) {
        double x = KI_X, top = KI_TOP, w = KI_W, h = KI_H, step = KI_STEP, kiH = p.getKi() * step;
        Rectangle track = capsule(x, top, w, h, Color.rgb(20, 10, 15, 0.35));
        track.setStroke(Color.HOTPINK.deriveColor(0, 1, 1, 0.5));
        track.setStrokeWidth(1.5);
        track.setEffect(new DropShadow(6, Color.HOTPINK.deriveColor(0, 1, 1, 0.4)));
        Rectangle unlocked = capsule(x, top + h - p.getMaxKi() * step, w, p.getMaxKi() * step, Color.HOTPINK.deriveColor(0, 1, 1, 0.22));
        Rectangle ki = capsule(x, top + h - kiH, w, kiH, new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#ff4fa3")), new Stop(0.5, Color.web("#ffb3d6")), new Stop(1, Color.web("#ff4fa3"))));
        DropShadow halo = new DropShadow(10, Color.HOTPINK);
        halo.setInput(new Glow(0.2));
        ki.setEffect(halo);
        live.getChildren().addAll(track, unlocked, ki);
        kiPulse.stop();
        kiPreview.setFill(Color.rgb(45, 15, 30, 0.6));
        kiPreview.setEffect(null);
        kiPreview.setArcWidth(0);
        kiPreview.setArcHeight(0);
        kiPreview.setClip(capsule(x, top + h - kiH, w, kiH, Color.BLACK));   // only inside the glowing Ki: rounded top, flat cut below
        kiPreview.setMouseTransparent(true);
        kiPreview.setOpacity(0);
        live.getChildren().add(kiPreview);
        motes(top + h, kiH, p.getKi());
        place(spiritText("気 KI", 22), 48, 240);
        place(spiritText(p.getKi() + " / " + Player.KI_CAP, 20), 48, 715);
    }

    /** A rounded capsule: both ends fully round. */
    private static Rectangle capsule(double x, double y, double w, double h, Paint fill) {
        Rectangle r = new Rectangle(x, y, w, h);
        r.setArcWidth(w);
        r.setArcHeight(w);
        r.setFill(fill);
        r.setMouseTransparent(true);
        return r;
    }

    /** Pink spirit motes rising and fading on both sides of the filled part of the Ki bar. */
    private void motes(double bottom, double height, int count) {
        for (int i = 0; i < count; i++) {
            Circle c = new Circle(1.5 + Math.random() * 2, Color.web("#ff8ac4"));
            c.setEffect(new DropShadow(6, Color.HOTPINK));
            c.setMouseTransparent(true);
            double side = Math.random() < 0.5 ? KI_X - 6 - Math.random() * 12 : KI_X + KI_W + 6 + Math.random() * 12;
            c.setCenterX(side);
            c.setCenterY(bottom - Math.random() * height);
            TranslateTransition rise = new TranslateTransition(Duration.seconds(2 + Math.random() * 2), c);
            rise.setFromY(0);
            rise.setToY(-40 - Math.random() * 40);
            rise.setFromX(0);
            rise.setToX(Math.random() * 10 - 5);
            FadeTransition fade = new FadeTransition(rise.getDuration(), c);
            fade.setFromValue(0.9);
            fade.setToValue(0);
            ParallelTransition t = new ParallelTransition(rise, fade);
            t.setCycleCount(Animation.INDEFINITE);
            t.playFrom(Duration.seconds(Math.random() * 3));
            c.sceneProperty().addListener((o, was, now) -> { if (now == null) t.stop(); });
            live.getChildren().add(c);
        }
    }

    /** Cream text with a soft pink glow, centred over the Ki bar's zone. */
    private static Label spiritText(String s, double size) {
        Label l = new Label(s);
        l.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: " + size + "; -fx-font-weight: bold;"
                + " -fx-text-fill: #ffe6f2; -fx-alignment: center; -fx-text-alignment: center;");
        l.setPrefWidth(113);
        l.setEffect(new DropShadow(10, Color.HOTPINK));
        return l;
    }

    /** A deck as a stack of card backs (one layer per 5 cards) with the count on the top card; it shrinks as you draw. */
    private void deck(Player p, double x, double y) {
        int n = p.getDeck().size(), layers = (n + 4) / 5;
        double cardH = MINION_W * CardView.H / CardView.W, bx = x + (115 - MINION_W) / 2, by = y + (127 - cardH) / 2;
        ImageView top = null;
        for (int i = 0; i < layers; i++) place(top = cardBack(MINION_W), bx - i * 1.5, by - i * 1.5);
        if (top != null) Tooltip.install(top, new Tooltip(n + " cards left"));
        double tx = bx - (layers - 1) * 1.5, ty = by - (layers - 1) * 1.5;
        place(hud(new Label(String.valueOf(n)), 0, 0, 44, 30, 18), tx + MINION_W / 2 - 22, ty + cardH - 40);
    }

    /** A card back w wide, with the cards' rounded corners. */
    private ImageView cardBack(double w) {
        ImageView back = image("/images/frames/card-back.png");
        back.setFitWidth(w);
        back.setPreserveRatio(true);
        Rectangle corners = new Rectangle(w, w * CardView.H / CardView.W);
        corners.setArcWidth(30 * w / CardView.W);
        corners.setArcHeight(30 * w / CardView.W);
        back.setClip(corners);
        return back;
    }

    /** The opponent's hand: card backs upside down in an arc at the top; hovering shows how many (GDD). */
    private void enemyHand(Player p) {
        int n = p.getHand().size();
        double mid = (n - 1) / 2.0;
        for (int i = 0; i < n; i++) {
            double d = i - mid;
            ImageView back = cardBack(90);
            back.setRotate(180 - d * 4);
            Tooltip.install(back, new Tooltip(n + " cards in hand"));
            place(back, W / 2 - 45 + d * 45, -70 - d * d * 2);
        }
    }

    /** Styles a label or button as a HUD box of the given size and font, at (x, y) on the grid. */
    private static <T extends Labeled> T hud(T node, double x, double y, double w, double h, int font) {
        node.setStyle(HUD + " -fx-font-size: " + font + ";");
        node.setPrefSize(w, h);
        node.relocate(x, y);
        return node;
    }

    /** One side's minions in slots 1 to 7, left to right, each clickable for attacks. */
    private void row(Player p, double y) {
        List<Minion> minions = p.getSide().getMinions();
        for (int i = 0; i < minions.size(); i++) {
            double x = SLOT_X + 2 + i * SLOT_STEP;
            groundShadow(x, y);
            MinionView v = new MinionView(minions.get(i), MINION_W);
            clickable(v, minions.get(i), p == you);
            place(v, x, y);
        }
    }

    /** The hero's scroll, centred on the screen in its row, with HP in the scroll's bottom-right corner. */
    private void hero(Hero h, String portrait, double zoneY, boolean mine) {
        double hgt = 150, wid = hgt * 1.6;
        ImageView img = image("/images/heroes/" + portrait + ".png");
        img.setFitHeight(hgt);
        img.setPreserveRatio(true);
        boolean hurt = h.getHealth() < h.getMaxHealth();
        ImageView frame = image("/images/hud/hp-frame.png");
        frame.setFitWidth(54);
        frame.setPreserveRatio(true);
        Label number = new Label(String.valueOf(h.getHealth()));
        number.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: 17; -fx-font-weight: bold; -fx-text-fill: "
                + (hurt ? "#c41e1e" : "#1f1b17") + ";");
        StackPane hp = new StackPane(frame, number);
        hp.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane box = new StackPane(img, hp);
        StackPane.setAlignment(hp, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(hp, new Insets(0, wid * 0.08, hgt * 0.06, 0));
        clickable(box, h, mine);
        place(box, W / 2 - wid / 2, zoneY + (119 - hgt) / 2);
    }

    private void place(Node n, double x, double y) {
        n.relocate(x, y);
        live.getChildren().add(n);
    }

    private ImageView image(String path) { return new ImageView(new Image(getClass().getResourceAsStream(path))); }

    /** Attack selection on one character: yours can be picked as attacker (shadow on hover, gold when picked);
     *  once one is picked, the enemies it may hit glow red and attack on click. */
    private void clickable(Node n, Combatant c, boolean mine) {
        if (mine && c.canAttack()) {
            n.setEffect(c == selected ? new DropShadow(30, Color.GOLD) : null);
            n.setOnMouseEntered(e -> { if (c != selected) n.setEffect(new DropShadow(20, Color.BLACK)); });
            n.setOnMouseExited(e -> { if (c != selected) n.setEffect(null); });
            n.setOnMouseClicked(e -> { selected = c == selected ? null : c; render(); });
        } else if (!mine && selected != null && canTarget(c)) {
            n.setEffect(new DropShadow(30, Color.CRIMSON));
            n.setOnMouseClicked(e -> {
                Combatant attacker = selected;
                selected = null;
                onAttack.accept(attacker, c);
                render();
            });
        }
    }

    /** Stand-in for the engine's validTargets: Shirodu minions must be hit first, hidden Shinobi can't be targeted. */
    private boolean canTarget(Combatant t) {
        List<Minion> walls = enemy.getSide().shirodus();
        if (t instanceof Minion m) return !m.isHidden() && (walls.isEmpty() || walls.contains(m));
        return walls.isEmpty();
    }

    /** A soft dark patch under a card so it looks like it lies on the table. */
    private void groundShadow(double x, double y) {
        Rectangle r = new Rectangle(x - 4, y - 1, MINION_W + 8, MINION_W * CardView.H / CardView.W + 9);
        r.setArcWidth(16);
        r.setArcHeight(16);
        r.setFill(Color.rgb(0, 0, 0, 0.45));
        r.setEffect(new GaussianBlur(10));
        r.setMouseTransparent(true);
        live.getChildren().add(r);
    }

    /** A gentle heartbeat: grows 6 % and back, forever; stops once the node leaves the screen. */
    private static void throb(Node n) {
        ScaleTransition t = new ScaleTransition(Duration.millis(450), n);
        t.setToX(1.06);
        t.setToY(1.06);
        t.setAutoReverse(true);
        t.setCycleCount(Animation.INDEFINITE);
        t.play();
        n.sceneProperty().addListener((o, was, now) -> { if (now == null) t.stop(); });
    } 

    /** Screen shake when a card lands: 6+ Ki shakes; 10+ Ki shakes longer and harder, and every minion on the board wobbles. */
    private void impact(int ki) {
        if (ki < 6) return;
        double strength = ki >= 10 ? 14 : 6;
        int jolts = ki >= 10 ? 15 : 10;
        List<MinionView> cards = ki < 10 ? List.of()
                : live.getChildren().stream().filter(MinionView.class::isInstance).map(MinionView.class::cast).toList();
        Timeline t = new Timeline();
        for (int i = 0; i <= jolts; i++) {
            double fade = 1 - (double) i / jolts;
            List<KeyValue> moves = new ArrayList<>(List.of(
                    new KeyValue(table.translateXProperty(), (Math.random() * 2 - 1) * strength * fade),
                    new KeyValue(table.translateYProperty(), (Math.random() * 2 - 1) * strength * fade)));
            for (MinionView c : cards) moves.add(new KeyValue(c.rotateProperty(), (Math.random() * 2 - 1) * 8 * fade));
            t.getKeyFrames().add(new KeyFrame(Duration.millis(i * 35), moves.toArray(KeyValue[]::new)));
        }
        t.play();
    }

        /** A hero power in its mock-up zone: Ki cost in the circle, name below. Yours: hover shadow, press to use
     *  (pays the Ki), then it stays dimmed for the turn. The opponent's is only shown. */
    private void power(Player p, double zoneY, boolean mine) {
        double size = 120, k = size / 160;
        ImageView frame = image("/images/hud/hero-power.png");
        frame.setFitWidth(size);
        frame.setPreserveRatio(true);
        Label cost = new Label(String.valueOf(POWER_COST));
        cost.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: " + 26 * k + "; -fx-font-weight: bold; -fx-text-fill: #1f1b17;");
        cost.setTranslateY(-size * 0.28);
        String[] info = POWER.get(p.getHero().getHeroClass());   // English name, Japanese name, effect
        VBox text = new VBox(1, powerText(info[0], 15 * k), powerText(info[1], 12 * k), powerText(info[2], 9.5 * k));
        text.setAlignment(Pos.CENTER);
        text.setMaxWidth(size * 0.7);
        text.setTranslateY(size * 0.12);
        StackPane box = new StackPane(frame, cost, text);
        boolean used = mine && powerUsed;
        if (used) box.setEffect(new ColorAdjust(0, -0.3, -0.5, 0));
        if (mine && !used && you.canAfford(POWER_COST)) {
            box.setOnMouseEntered(e -> {
                previewCost(POWER_COST);
                box.setEffect(new DropShadow(12, Color.BLACK));
                Anim.to(box, Anim.ease(box.scaleXProperty(), 1.04), Anim.ease(box.scaleYProperty(), 1.04));
            });
            box.setOnMouseExited(e -> {
                previewCost(0);
                box.setEffect(null);
                Anim.to(box, Anim.ease(box.scaleXProperty(), 1.0), Anim.ease(box.scaleYProperty(), 1.0));
            });
            box.setOnMousePressed(e -> {
                powerUsed = true;
                onPower.run();
                render();
            });
        }
        place(box, 948 + (158 - size) / 2, zoneY + (113 - size) / 2);
    }

    /** Dark ink text for the hero power panel, centred and wrapping. */
    private static Label powerText(String s, double size) {
        Label l = new Label(s);
        l.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: " + size + "; -fx-font-weight: bold; -fx-text-fill: #1f1b17; -fx-text-alignment: center;");
        l.setWrapText(true);
        return l;
    }
}