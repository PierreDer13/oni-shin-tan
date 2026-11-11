package fr.onishintan.ui.match;
import fr.onishintan.ui.cards.CardView;
import fr.onishintan.ui.cards.MinionView;
import fr.onishintan.ui.fx.Fx;
import fr.onishintan.ui.screens.HomeView;
import fr.onishintan.ui.app.Sound;

import fr.onishintan.model.Hero;
import fr.onishintan.model.BoardSide;
import fr.onishintan.model.HeroClass;
import fr.onishintan.model.Keyword;
import fr.onishintan.model.Card;
import fr.onishintan.model.Combatant;
import fr.onishintan.model.Minion;
import fr.onishintan.model.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.Predicate;

import javafx.util.Duration;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;

import javafx.beans.InvalidationListener;

import javafx.geometry.Insets;
import javafx.geometry.Pos;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.layout.Region;
import javafx.scene.input.MouseEvent;

import javafx.event.EventHandler;

/** The match table on a 1600 x 900 grid (zones from the Izakaya mock-up), scaled to fit the window. */
public class BoardView extends Pane {
    public static final double DECK_X = 1227, DECK_Y = 632;
    public static final double W = 1600, H = 900;
    /** The shadow every object on the table casts: down and to the right. */
    static final DropShadow SHADOW = new DropShadow(15, 8, 6, Color.rgb(0, 0, 0, 0.6));
    public static final double SLOT_X = 352, SLOT_STEP = 130.5, ENEMY_Y = 257, YOUR_Y = 443, MINION_W = 114;

    private Player enemy, you;
    private String enemyPortrait, yourPortrait;
    private final Pane table = new Pane();
    private final ImageView bg;
    private final boolean izakaya;
    /** Izakaya props cut out of the table: file, x, y. */
    private static final String[] PROPS = { "cup-1 130 46", "dice-1 306 105", "pot 1274 13", "dice-2 1271 681", "cup-2 1376 602" };
    private Combatant selected;
    private BiConsumer<Combatant, Combatant> onAttack = (a, t) -> {};
    private final Map<Combatant, Node> nodes = new HashMap<>();

    private static final String HUD = "-fx-background-color: rgba(20,12,8,0.75); -fx-text-fill: #f3ede1; -fx-font-weight: bold;"
        + " -fx-background-radius: 10; -fx-alignment: center; -fx-text-alignment: center;";

    private final Pane live = new Pane();
    private final StatusOverlay status = new StatusOverlay(live, () -> impact(0, true));
    private KiBarView ki;
    private HandView hand;
    private boolean dealt;   // the hand stays hidden until the opening deal
    /** Opening hand: deals the bottom player's cards in from their deck. */
    public void dealHand() {
        dealt = true;
        hand.setVisible(true);
        hand.deal();
    }
    /** Start of turn: the `count` cards the active player just drew fly in from their deck. */
    public void dealDrawnCards(int count) {
        hand.dealLastCards(count);
    }
    
    private Player kiOwner;   // who the Ki bar showed at the last redraw, and how much Ki they had
    private int kiBefore;
    private static final Image SWITCH_ON = new Image(BoardView.class.getResourceAsStream("/images/hud/end-turn.png")),
            SWITCH_OFF = new Image(BoardView.class.getResourceAsStream("/images/hud/end-turn-off.png"));
    private final ImageView endTurn = new ImageView(SWITCH_ON);
    private Runnable onEndTurn = () -> {};
    private final Label yourClock = hud(new Label(), 1381, 545, 190, 71, 30);
    private Predicate<Card> onTrade = card -> false;
    private final Label enemyClock = hud(new Label(), 1381, 255, 190, 71, 30);
    private BiConsumer<Card, Integer> onPlay = (c, slot) -> {};

    private boolean powerUsed;
    private Runnable onPower = () -> {};
    private Runnable onGameOver = () -> {};
    public void setOnGameOver(Runnable r) { onGameOver = r; }
    private Node preview;

    public BoardView(String board, Player enemy, String enemyPortrait, Player you, String yourPortrait) {
        this.enemy = enemy;
        this.you = you;
        this.enemyPortrait = enemyPortrait;
        this.yourPortrait = yourPortrait;
        bg = image("/images/boards/" + board + ".png");
        izakaya = board.equals("izakaya");
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
        endTurn.setFitHeight(200);
        endTurn.setPreserveRatio(true);
        endTurn.setEffect(SHADOW);
        endTurn.relocate(1415, 335);
        endTurn.setOnMousePressed(e -> {
            endTurn.setDisable(true);   // one press per turn
            Sound.play(HomeView.CLICK);
            endTurn.setImage(SWITCH_OFF);   // the paddle and the lights switch over
            PauseTransition wait = new PauseTransition(Duration.millis(200));
            wait.setOnFinished(f -> onEndTurn.run());
            wait.play();
        });
        table.getChildren().addAll(bg, live, endTurn, yourClock, enemyClock);
        getChildren().add(table);
        setStyle("-fx-background-color: black;");
        render();
    }

    /** Redraws everything that changes during a match: heroes, minions, Ki and decks. */
    public void render() {
        live.getChildren().clear();
        if (izakaya) props();
        nodes.clear();
        status.clear();
        hero(enemy.getHero(), enemyPortrait, 107, false);
        oath(enemy, 107);
        oath(you, 637);
        stackCounter();
        hero(you.getHero(), yourPortrait, 637, true);
        power(enemy, 112, false);
        power(you, 639, true);
        row(enemy, ENEMY_Y);
        row(you, YOUR_Y);
        place(ki = new KiBarView(you, you == kiOwner ? kiBefore : you.getKi()), 0, 0);
        kiOwner = you;
        kiBefore = you.getKi();
        deck(you, DECK_X, DECK_Y);
        deck(enemy, 182, 239);
        live.getChildren().add(hand = new HandView(you, (card, slot) -> { onPlay.accept(card, slot); impact(card.getKiCost(), false); }, this::trade, this::previewCost));
        hand.setVisible(dealt);
        enemyHand(enemy);
        if (enemy.getHero().isDead() || you.getHero().isDead()) gameOver(enemy.getHero().isDead());
    }

    /** Game over: a dark veil over the whole table blocks every click, with VICTORY or DEFEAT in the middle. */
    private void gameOver(boolean won) {
        Label text = new Label(won ? "勝利\nVICTORY" : "敗北\nDEFEAT");
        text.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: 96; -fx-font-weight: bold; -fx-text-alignment: center; -fx-text-fill: "
                + (won ? "#ffd84a" : "#c41e1e") + ";");
        text.setEffect(new DropShadow(30, Color.BLACK));
        StackPane end = new StackPane(new Rectangle(W, H, Color.rgb(0, 0, 0, 0.6)), text);
        end.setOnMousePressed(e -> onGameOver.run());
        table.getChildren().add(end);
    }

    /** Both chess clocks: the active one is big and bright, the other small and grey (GDD). */
    public void showClocks(String yours, String theirs, boolean yourTurn) {
        yourClock.setText(yours);
        enemyClock.setText(theirs);
        clock(yourClock, yourTurn);
        clock(enemyClock, !yourTurn);
    }

    /** What happens when you drop a card on your row (the match will call the engine here). */
    /** What happens when you drop a card from your hand on your deck: true if the engine accepted the trade. */
      public void setOnTrade(Predicate<Card> onTrade) { this.onTrade = onTrade; }

      /**
       * A hand card dropped on your deck. If the engine accepts the trade, the card sinks into the deck,
       * the deck shuffles, and the card you drew flies into your hand.
       */
      private void trade(Card card) {
          boolean accepted = onTrade.test(card);
          render();   // the hand now holds the drawn card instead of the traded one
          if (!accepted) {
              return;   // refused (already traded this turn, or not enough Ki): the card is simply back in the hand
          }
          double cardH = MINION_W * CardView.H / CardView.W;
          double deckX = DECK_X + (115 - MINION_W) / 2;   // the same corner deck() uses for the top card
          double deckY = DECK_Y + (127 - cardH) / 2;
          TradeAnimation.play(live, card, deckX, deckY, MINION_W, () -> cardBack(MINION_W));
          hand.dealLastCardAfter(TradeAnimation.LENGTH);
      }
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
    public void previewCost(int cost) { ki.preview(cost); }

    public void setOnEndTurn(Runnable r) { onEndTurn = r; }

    /** Start of a match: the class's Oath choice covers the table until one is sworn (GDD, Oaths). */
    public void chooseOath(HeroClass c, Consumer<String> onSworn) { table.getChildren().add(new OathView(c, onSworn)); }

    private final Map<Player, String> oaths = new HashMap<>();
    /** Records the Oath a player swore; it then shows next to their hero. */
    public void swear(Player p, String oath) { oaths.put(p, oath); render(); }

    /** The sworn Oath as a small stamped scroll right of the hero power; hover it to reread it (an enemy Ninja's stays hidden below 3 stacks). */
    private void oath(Player p, double zoneY) {
        if (!oaths.containsKey(p)) return;
        HeroClass c = p.getHero().getHeroClass();
        boolean hidden = p == enemy && c == HeroClass.NINJA && stacksOf(p) < 3;
        StackPane mini = OathView.mini(c, oaths.get(p), hidden);
        place(mini, 1120, zoneY - 15);
        bigOnHover(mini, () -> OathView.sworn(c, oaths.get(p)), 850, 80);
    }

    /** Your Oath stacks, bottom left: 9 marks, the 3 / 6 / 9 reward marks bigger with a gold ring; earned marks are red. */
    private void stackCounter() {
        if (!oaths.containsKey(you)) return;
        int n = stacksOf(you);
        for (int i = 1; i <= 9; i++) {
            Circle c = new Circle(i % 3 == 0 ? 11 : 7, i <= n ? Color.web("#c41e1e") : Color.rgb(0, 0, 0, 0.35));
            c.setStroke(i % 3 == 0 ? Color.GOLD : Color.web("#f3ede1"));
            c.setStrokeWidth(2);
            place(c, 40 + i * 30, 840);
        }
        place(hud(new Label("誓 " + n + " / 9"), 0, 0, 90, 30, 16), 340, 829);
    }

    /** The player's Oath stacks, read from the engine's Oath (0 before an Oath is sworn). */
    private int stacksOf(Player p) {
        if (p.getOath() == null) {
            return 0;
        }
        return p.getOath().getStacks();
    }

    /** Hotseat: the other player now sits at the bottom (their hand, Ki and hero power) and the board flips. */
    public void swapSides() {
        endTurn.setImage(SWITCH_ON);
        endTurn.setDisable(false);
        Player p = you;
        you = enemy;
        enemy = p;
        String s = yourPortrait;
        yourPortrait = enemyPortrait;
        enemyPortrait = s;
        selected = null;
        powerUsed = false;
        render();
    }

    /** Hotseat: a dark veil hides the table until the next player clicks, so nobody sees the other's hand (GDD). */
public void passTo(String name, Runnable onReady) {
        Label text = new Label("次の番\nPass to " + name + "\n\nclick when ready");
        text.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: 56; -fx-font-weight: bold; -fx-text-alignment: center; -fx-text-fill: #f3ede1;");
        StackPane veil = new StackPane(new Rectangle(W, H, Color.rgb(0, 0, 0, 0.95)), text);
        veil.setOnMousePressed(e -> { table.getChildren().remove(veil); onReady.run(); });
        table.getChildren().add(veil);
    }

    private static void clock(Label l, boolean active) {
        l.setOpacity(active ? 1 : 0.5);
        l.setScaleX(active ? 1 : 0.8);
        l.setScaleY(active ? 1 : 0.8);
    }

    /** A deck as a stack of card backs (one layer per 5 cards) with the count on the top card; it shrinks as you draw. */
    private void deck(Player p, double x, double y) {
        int n = p.getDeck().size(), layers = (n + 4) / 5;
        double cardH = MINION_W * CardView.H / CardView.W, bx = x + (115 - MINION_W) / 2, by = y + (127 - cardH) / 2;
        ImageView top = null;
        for (int i = 0; i < layers; i++) {
            top = cardBack(MINION_W);
            Node layer = i == 0 ? new Group(top) : top;   // the bottom card carries the stack's shadow; in a Group so its rounded clip doesn't cut the shadow off
            layer.setEffect(i == 0 ? SHADOW : null);
            place(layer, bx - i * 1.5, by - i * 1.5);
        }
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

    /** One side's 7 fixed slots, left to right. Each minion is drawn in the slot it was dropped on; empty slots stay empty. */
    private void row(Player p, double y) {
        for (int slot = 0; slot < BoardSide.MAX; slot++) {
            Minion minion = p.getSide().getAt(slot);
            if (minion == null) {
                continue;   // empty slot: nothing to draw
            }

            double x = SLOT_X + 2 + slot * SLOT_STEP;
            groundShadow(x, y);

            MinionView view = new MinionView(minion, MINION_W);
            clickable(view, minion, p == you);
            if (minion.isHidden() && view.getEffect() == null) {
                view.setEffect(new ColorAdjust(0, -0.4, -0.45, 0));   // Shinobi: darkened while hidden
            }

            // The big preview opens on the left for the right-hand slots, so it stays on screen.
            boolean rightHandSlot = slot >= 4;
            readable(view, minion, x, y, rightHandSlot);
            place(view, x, y);

            // Keyword overlays. follow() makes them move with the card when it attacks.
            status.follow(view);
            if (minion.hasKeyword(Keyword.PASSIVE)) {
                status.flag(minion, x, y);
            }
            if (minion.hasTorii()) {
                status.gate("torii", minion, x, y);
            }
            if (minion.hasKeyword(Keyword.SHIRODU)) {
                status.gate("shirodu", minion, x, y);
            }
            if (minion.isHidden()) {
                status.shinobi(x, y);
            }
            if (minion.hasKeyword(Keyword.SEPPUKU)) {
                status.seppuku(x, y);
            }
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
        if (h.getArmour() > 0) {
            ImageView plate = image("/images/hud/armour.png");
            plate.setFitWidth(60);
            plate.setPreserveRatio(true);
            Label value = new Label(String.valueOf(h.getArmour()));
            value.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #1f1b17;");
            StackPane armour = new StackPane(plate, value);
            armour.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            box.getChildren().add(armour);
            box.getProperties().put("armour", armour);
            StackPane.setAlignment(armour, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(armour, new Insets(0, wid * 0.08 - 3, hgt * 0.06 + 34, 0));
        }
        if (h.getWeapon() != null) {
            ImageView ring = image("/images/hud/atk-frame.png");
            ring.setFitWidth(50);
            ring.setPreserveRatio(true);
            Label atk = new Label(String.valueOf(h.getAttack()));
            atk.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #1f1b17;");
            atk.setTranslateY(-4);   // centre of the circle, above the waves
            StackPane badge = new StackPane(ring, atk);
            badge.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            box.getChildren().add(badge);
            StackPane.setAlignment(badge, Pos.BOTTOM_LEFT);
            StackPane.setMargin(badge, new Insets(0, 0, hgt * 0.06, wid * 0.08));
        }
        clickable(box, h, mine);
        place(box, W / 2 - wid / 2, zoneY + (119 - hgt) / 2);
        if (h.getHeroClass() == HeroClass.SAMURAI) weaponSlot(h, box, zoneY);
    }

    /** Samurai: the sword rack on the left of the hero, always there; the equipped weapon rests on its pegs. */
    private void weaponSlot(Hero h, Node box, double zoneY) {
        ImageView rack = image("/images/weapons/weapon-slot.png");
        rack.setFitWidth(180);
        rack.setPreserveRatio(true);
        StackPane slot = new StackPane(rack);
        if (h.getWeapon() != null) {
            ImageView w = image("/images/weapons/" + h.getWeapon().getSource().getId() + ".png");
            w.setFitWidth(150);
            w.setPreserveRatio(true);
            w.setTranslateY(4);   // down onto the pegs
            slot.getChildren().add(w);
            box.getProperties().put("weapon", w);
        }
        place(slot, W / 2 - 120 - 195, zoneY + (119 - 83) / 2);
    }

    private void place(Node n, double x, double y) {
        n.relocate(x, y);
        live.getChildren().add(n);
    }

    static ImageView image(String path) { return new ImageView(new Image(BoardView.class.getResourceAsStream(path))); }

    /** Attack selection on one character. Yours that can attack: yellow border (thicker on hover, gold glow when picked),
     *  click to pick or unpick. Once one is picked: legal enemies glow red and attack on click; anything else cancels. */
    private void clickable(Node n, Combatant c, boolean mine) {
        nodes.put(c, n);
        if (mine && c.canAttack()) {
            if (c == selected) floatReady(n);
            n.setEffect(c == selected ? new DropShadow(BlurType.GAUSSIAN, Color.GOLD, 28, 0.45, 0, 0) : border(8));
            n.setOnMouseEntered(e -> { if (c != selected) n.setEffect(border(14)); });
            n.setOnMouseExited(e -> { if (c != selected) n.setEffect(border(8)); });
            n.setOnMouseClicked(e -> { selected = c == selected ? null : c; render(); });
        } else if (!mine && selected != null && canTarget(c)) {
            n.setEffect(new DropShadow(30, Color.CRIMSON));
            n.setOnMouseClicked(e -> {
                Combatant attacker = selected;
                selected = null;
                fight(attacker, c);
            });
        } else if (selected != null) {
            n.setOnMouseClicked(e -> { selected = null; render(); });
        }
    }

    /** The yellow "ready to attack" border: a crisp outline whose thickness grows with the radius. */
    static DropShadow border(double radius) {
        return new DropShadow(BlurType.GAUSSIAN, Color.web("#ffd84a"), radius, 0.8, 0, 0);
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

    /** Screen shake when a card lands: 6+ Ki shakes; 10+ Ki shakes longer and harder, and every minion on the board wobbles. */
    private void impact(int ki, boolean attack) {
        if (ki < 6 && !attack) return;
        double strength = ki >= 10 ? 14 : ki >= 6 ? 6 : 3;
        int jolts = ki >= 10 ? 15 : ki >= 6 ? 10 : 6;
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

    /** A hero power in its mock-up zone; yours can be pressed once per turn (pays the Ki), the opponent's is only shown. */
    private void power(Player p, double zoneY, boolean mine) {
        boolean usable = mine && !powerUsed && you.canAfford(HeroPowerView.COST);
        HeroPowerView v = new HeroPowerView(p.getHero().getHeroClass(), mine && powerUsed, usable, this::previewCost,
                () -> { powerUsed = true; onPower.run(); render(); });
        bigOnHover(v, () -> {
            Node b = new HeroPowerView(p.getHero().getHeroClass(), mine && powerUsed, false, c -> {}, () -> {});
            b.setScaleX(2.5);
            b.setScaleY(2.5);
            return b;
        }, 900, 330);
        place(v, 948 + (158 - HeroPowerView.SIZE) / 2, zoneY + (113 - HeroPowerView.SIZE) / 2);
    }

    /** Plays an attack (GDD): lunge, slash, red damage on both, dead minions cut in half; the board redraws after. */
    private void fight(Combatant attacker, Combatant target) {
        Node a = nodes.get(attacker), t = nodes.get(target);
        if (a.getProperties().get("float") instanceof Animation f) { f.stop(); a.setTranslateY(0); }
        int aHp = life(attacker), tHp = life(target);
        Fx.attack(a, t, () -> {
            onAttack.accept(attacker, target);
            status.broken(target);
            status.broken(attacker);
            crack(target, t);
            crack(attacker, a);
            Fx.slash(live, t);
            impact(attacker instanceof Minion m ? m.getSource().getKiCost() : 0, true);
            Fx.damageNumber(live, t, tHp - life(target));
            Fx.damageNumber(live, a, aHp - life(attacker));
            if (target.isDead()) Fx.deathCut(live, t);
            if (attacker.isDead()) Fx.deathCut(live, a);
            PauseTransition wait = new PauseTransition(Duration.millis(attacker instanceof Hero ? 1300 : 1100));
            wait.setOnFinished(e -> render());
            wait.play();
        });
    }

    /** If a hero's armour or weapon just ran out, it breaks with the diagonal cut. */
    private void crack(Combatant c, Node n) {
        if (!(c instanceof Hero h)) return;
        if (h.getArmour() == 0 && n.getProperties().get("armour") instanceof Node plate) Fx.deathCut(live, plate);
        if (h.getWeapon() == null && n.getProperties().get("weapon") instanceof Node blade) {
            PauseTransition back = new PauseTransition(Duration.millis(650));   // once the hero has glided back
            back.setOnFinished(e -> { Fx.sparks(live, blade); Fx.deathCut(live, blade); impact(0, true); });
            back.play();
        }
    }

    /** What a hit can take away: HP, plus armour for a hero. */
    private static int life(Combatant c) { return c.getHealth() + (c instanceof Hero h ? h.getArmour() : 0); }

    /** Hovering a node for half a second shows a big copy at (x, y); leaving or clicking hides it. */
    private void bigOnHover(Node n, Supplier<Node> big, double x, double y) {
        PauseTransition delay = new PauseTransition(Duration.millis(500));
        delay.setOnFinished(e -> {
            preview = big.get();
            preview.setMouseTransparent(true);
            preview.setViewOrder(-4);
            preview.relocate(x, y);
            live.getChildren().add(preview);
        });
        EventHandler<MouseEvent> hide = e -> { delay.stop(); live.getChildren().remove(preview); };
        n.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> delay.playFromStart());
        n.addEventHandler(MouseEvent.MOUSE_EXITED, hide);
        n.addEventHandler(MouseEvent.MOUSE_PRESSED, hide);
    }

    /** A board card's big copy: on the left for the three right-hand slots, on the right otherwise. */
    private void readable(MinionView v, Minion m, double x, double y, boolean left) {
        double w = 280, h = w * CardView.H / CardView.W;
        bigOnHover(v, () -> new MinionView(m, w), left ? x - 20 - w : x + MINION_W + 20, Math.max(10, Math.min(H - h - 10, y + 80 - h / 2)));
    }

    /** A slow float for a character that can attack: rises 6 px and back, about every 3 seconds; stops when removed. */
    private static void floatReady(Node n) {
        TranslateTransition t = new TranslateTransition(Duration.seconds(1.4), n);
        t.setFromY(0);
        t.setToY(-6);
        t.setAutoReverse(true);
        t.setCycleCount(Animation.INDEFINITE);
        t.setInterpolator(Interpolator.EASE_BOTH);
        t.playFrom(Duration.seconds(Math.random() * 1.4));
        n.getProperties().put("float", t);
        n.sceneProperty().addListener((o, was, now) -> { if (now == null) t.stop(); });
    }

    /** Izakaya: the dice, sake flask and cups lie on the table; click one to make it jiggle. */
    private void props() {
        for (String p : PROPS) {
            String[] a = p.split(" ");
            ImageView v = image("/images/boards/izakaya/" + a[0] + ".png");
            Rotate r = new Rotate(0, v.getImage().getWidth() / 2, v.getImage().getHeight());   // tilts on its base
            v.getTransforms().add(r);
            v.setOnMouseClicked(e -> new Timeline(new KeyFrame(Duration.millis(80), new KeyValue(r.angleProperty(), -5)),
                    new KeyFrame(Duration.millis(170), new KeyValue(r.angleProperty(), 4)),
                    new KeyFrame(Duration.millis(250), new KeyValue(r.angleProperty(), -2)),
                    new KeyFrame(Duration.millis(320), new KeyValue(r.angleProperty(), 0))).play());
            place(v, Double.parseDouble(a[1]), Double.parseDouble(a[2]));
        }
    }
}