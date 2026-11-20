package fr.onishintan.ui.match;
import fr.onishintan.ui.cards.CardView;
import fr.onishintan.ui.cards.MinionView;
import fr.onishintan.ui.cards.ShrineView;
import fr.onishintan.ui.fx.EventBanner;
import fr.onishintan.ui.fx.Fx;
import fr.onishintan.ui.fx.HeroBurn;
import fr.onishintan.ui.screens.HomeView;
import fr.onishintan.ui.screens.SettingsPanel;
import fr.onishintan.ui.app.Images;
import fr.onishintan.ui.app.Sound;
import javafx.scene.media.AudioClip;
import javafx.application.Platform;
import fr.onishintan.model.Hero;
import fr.onishintan.model.BoardSide;
import fr.onishintan.model.BoardEntity;
import fr.onishintan.model.HeroClass;
import fr.onishintan.model.Keyword;
import fr.onishintan.model.Card;
import fr.onishintan.model.Combatant;
import fr.onishintan.model.Minion;
import fr.onishintan.model.MinionCard;
import fr.onishintan.model.Oath;
import fr.onishintan.model.Shrine;
import fr.onishintan.model.ShrineCard;
import fr.onishintan.model.Player;
import fr.onishintan.model.TrapCard;
import fr.onishintan.engine.GameEngine;
import fr.onishintan.engine.GameEvent;
import fr.onishintan.model.KatanaTier;
import fr.onishintan.model.Status;

import java.util.HashSet;
import java.util.Set;
import javafx.scene.effect.Effect;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.Predicate;

import javafx.util.Duration;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;

import javafx.beans.InvalidationListener;

import javafx.geometry.Insets;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
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
    private BiFunction<Combatant, Combatant, List<GameEvent>> onAttack = (a, t) -> List.of();
    private final Map<Combatant, Node> nodes = new HashMap<>();
    /** Every character's HP at the last redraw: a higher HP at the next one plays the heal effect. */
    private Map<Combatant, Integer> healthSeen = new HashMap<>();

    private static final String HUD = "-fx-background-color: rgba(20,12,8,0.75); -fx-text-fill: #f3ede1; -fx-font-weight: bold;"
        + " -fx-background-radius: 10; -fx-alignment: center; -fx-text-alignment: center;";

    private final Pane live = new Pane();
    private final StatusOverlay status = new StatusOverlay(live, () -> impact(0, true));
    private KiBarView ki;
    private HandView hand;
    private boolean dealt;   // the hand stays hidden until the opening deal
    private boolean rising;   // the card just played brings dead minions back (black smoke on them)
    private boolean yoi;      // Yōi was just played: the costs in hand sparkle after the redraw
    /** The thud of a big minion landing, and the cost from which a minion is big. */
    private static final AudioClip HEAVY_THUD = Sound.clip("heavy-minion");
    private static final int HEAVY_KI = 6;
    /** The sound of any hit (sounds/hit.wav). */
    private static final AudioClip HIT = Sound.clip("hit");
    /** The minions Shike has silenced: drawn grey for the rest of the match. */
    private final Set<Minion> silenced = new HashSet<>();
    /** A silenced minion's look: colours drained, a little darker, over its other effect (its yellow outline). */
    private static ColorAdjust grey(Effect over) {
        ColorAdjust drained = new ColorAdjust(0, -1, -0.15, 0);
        drained.setInput(over);
        return drained;
    }
    private String signature = "";   // the card just played, for its own move before its effects (SignatureFx)
    private boolean burnt;           // the dead hero has already burned (only once, then the seal straight away)
    private int signatureSlot;       // and its slot (Nue's shockwave starts there)
    private final Map<Combatant, Integer> duelHp = new HashMap<>();   // Kettō: each duellist's HP just before the duel
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
    /** Where the opponent's Ki shows: just right of its hero power. */
    private static final double ENEMY_KI_X = 1116, ENEMY_KI_Y = 150;
    /** How many Oath stacks each player's counter showed last time, so a new one pops in once. */
    private final Map<Player, Integer> stacksShown = new HashMap<>();
    private int kiBefore;
    private static final Image SWITCH_ON = Images.get("/images/hud/end-turn.png"),
            SWITCH_OFF = Images.get("/images/hud/end-turn-off.png");
    private final ImageView endTurn = new ImageView(SWITCH_ON);
    private Runnable onEndTurn = () -> {};
    private Runnable onReswear = () -> {};
    private final Label yourClock = hud(new Label(), 1441, 558, 130, 44, 22);
    private Predicate<Card> onTrade = card -> false;
    private final Label enemyClock = hud(new Label(), 1441, 268, 130, 44, 22);
    private CardPlayer player;
    /** A card dropped on the board that waits for its target(s): its slot, its legal targets and, for Kettō, the first minion picked. */
    private Card held;
    private int heldSlot;
    private List<Combatant> heldTargets = List.of();
    private Combatant firstPick;
    /** The arrow from what aims (an attacker, a held card, a hero power) to the mouse. */
    private final TargetArrow arrow = new TargetArrow();
    /** The red rings on a legal target under the mouse while something aims, and how wide they are drawn. */
    private final ImageView targetMark = image("/images/fx/target.png");
    private static final double MARK_W = 90;

    private Predicate<Combatant> onPower = target -> false;
    private Predicate<KatanaTier> onKatana = tier -> false;
    /** True after pressing a power that needs a target (Shuriken, Jikininki): the next click on a character aims it. */
    private boolean aiming;
    private Runnable onGameOver = () -> {};
    public void setOnGameOver(Runnable r) { onGameOver = r; }
    private Runnable onSurrender = () -> {};
    /** What happens when the player at the bottom confirms they surrender. */
    public void setOnSurrender(Runnable r) { onSurrender = r; }
    private Node preview;
    /** Where traps sit: left of each hero (grid coordinates). */
    private static final double TRAPS_X = 320, YOUR_TRAPS_Y = 660, ENEMY_TRAPS_Y = 120;
    /** The game-over seals (512 x 768) and how tall they are drawn. */
    private static final String VICTORY = "/images/hud/victory.png", DEFEAT = "/images/hud/defeat.png";
    private static final double END_H = 560;
    /** A trap still flying to its place (TrapView.set): traps() doesn't draw it until it lands. */
    private TrapCard arriving;

    public BoardView(String board, Player enemy, String enemyPortrait, Player you, String yourPortrait) {
        this.enemy = enemy;
        this.you = you;
        this.enemyPortrait = enemyPortrait;
        this.yourPortrait = yourPortrait;
        bg = image("/images/boards/" + board + ".png");
        izakaya = board.equals("izakaya");
        bg.setFitWidth(W);
        bg.setFitHeight(H);
        bg.setOnMouseClicked(e -> { selected = null; aiming = false; held = null; render(); });
        Scale scale = new Scale();
        table.getTransforms().add(scale);
        InvalidationListener fit = o -> {
            scale.setX(getWidth() / W);    // stretched to fill the window: no strip on a screen that isn't 16:9
            scale.setY(getHeight() / H);
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
        live.setPickOnBounds(false);   // a click on empty table goes through to the board (cancels an aim)
        // Settings, top left: volumes, window size and Surrender (which asks first).
        Label settings = darkButton("設定  SETTINGS", () -> table.getChildren().add(new SettingsPanel(this::askSurrender)));
        settings.relocate(20, 20);
        table.getChildren().add(settings);
        targetMark.setFitWidth(MARK_W);
        targetMark.setPreserveRatio(true);
        targetMark.setMouseTransparent(true);   // clicks still reach the target under it
        targetMark.setVisible(false);
        table.getChildren().add(targetMark);    // under the arrow
        // The targeting arrow, above the board: from what aims to the mouse.
        table.getChildren().add(arrow);
        table.addEventFilter(MouseEvent.MOUSE_MOVED, e -> pointArrow(e.getX(), e.getY()));
        arrow.visibleProperty().addListener((property, wasShown, shown) -> {
            if (shown) {
                table.setCursor(Cursor.NONE);   // while aiming, the arrowhead is the cursor
            } else {
                table.setCursor(null);          // back to the game's brush cursor
            }
        });
        getChildren().add(table);
        setStyle("-fx-background-color: black;");
        render();
    }

    /** Redraws everything that changes during a match: heroes, minions, Ki and decks. */
    public void render() {
        live.getChildren().clear();
        arrow.setVisible(false);   // shown again as soon as the mouse moves, if something still aims
        targetMark.setVisible(false);
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
        Label enemyKi = new Label("気 " + enemy.getKi() + " / " + enemy.getMaxKi());   // the opponent's Ki, next to its hero power
        enemyKi.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: #f3ede1;");
        enemyKi.setEffect(new DropShadow(6, Color.BLACK));
        place(enemyKi, ENEMY_KI_X, ENEMY_KI_Y);
        row(enemy, ENEMY_Y);
        row(you, YOUR_Y);
        place(ki = new KiBarView(you, you == kiOwner ? kiBefore : you.getKi()), 0, 0);
        kiOwner = you;
        kiBefore = you.getKi();
        deck(you, DECK_X, DECK_Y);
        traps(enemy, TRAPS_X, ENEMY_TRAPS_Y);
        traps(you, TRAPS_X, YOUR_TRAPS_Y);
        deck(enemy, 182, 239);
        drawHand();
        enemyHand(enemy);
        healEffects();
        table.lookupAll("#arrival-smoke").forEach(Node::toFront);   // a minion just drawn stays behind its smoke
        if (enemy.getHero().isDead() || you.getHero().isDead()) endGame(enemy.getHero().isDead());
    }

    /**
     * After a redraw: every character whose HP went up since the last redraw shows the heal effect
     * (heals, lifesteal, and buffs that raise HP). A character seen for the first time (just summoned) doesn't.
     */
    private void healEffects() {
        Map<Combatant, Integer> healthNow = new HashMap<>();
        for (Map.Entry<Combatant, Node> entry : nodes.entrySet()) {
            Combatant character = entry.getKey();
            int health = character.getHealth();
            healthNow.put(character, health);
            Integer healthBefore = healthSeen.get(character);
            if (healthBefore != null && health > healthBefore) {
                Fx.heal(live, entry.getValue(), health - healthBefore);
            }
        }
        healthSeen = healthNow;
    }


    /**
     * The traps a player has set, left of their hero. Yours: one back per trap, readable on hover.
     * The enemy's: a single back as soon as they have at least one, never how many (GDD).
     */
    private void traps(Player p, double x, double y) {
        if (p.trapCount() == 0) {
            return;
        }
        if (p != you) {
            place(TrapView.back(), x, y);
            return;
        }
        for (int i = 0; i < p.getTraps().size(); i++) {
            TrapCard trap = p.getTraps().get(i);
            if (trap == arriving) {
                continue;   // still flying to its place: TrapView.set draws it until it lands
            }
            Node back = TrapView.back();
            place(back, x + i * TrapView.SPACING, y);
            bigOnHover(back, () -> new CardView(trap, 280), 300, 300);
        }
    }

    /** A trap went off: both players see it flip face up in the middle of the table, then it is cut. */
    public void revealTrap(TrapCard trap) {
        TrapView.reveal(table, trap);   // the table, not `live`: a redraw in the middle would erase it
    }

    /** The card just played, big on the right of the table: its width and top-left corner. */
    private static final double PLAYED_W = 220, BIG_X = 1350, BIG_Y = 20;
    /** Where the AI's card shows big: on the left of the table. */
    private static final double ENEMY_BIG_X = 30, ENEMY_BIG_Y = 184, ENEMY_BIG_W = 380;   // left, centred: (900 - the card's 532 px) / 2
    /** How long each AI card shows on the left, and when the spot is free for the next one (System.currentTimeMillis). */
    private static final long ENEMY_CARD_MS = 2000;
    private long enemyCardFree;

    /** The card just played, big at `x` on the table (right for yours, left for the AI's), to read while its effects play (the redraw takes it away). */
    private Node bigCard(Card card, double x) {
        CardView big = new CardView(card, PLAYED_W);
        big.relocate(x, BIG_Y);
        big.setMouseTransparent(true);
        return big;
    }

    /**
     * Adventure, right after the AI has played: its card shows big on the left, and a minion appears in its slot at once,
     * so its Banzai, smoke and damage play on it (like yours in showPlayed). Its Traps stay hidden.
     */
    public void showAiPlayed(Card card) {
        heavyLanding(card);
        if (card instanceof TrapCard) {
            return;
        }
        long now = System.currentTimeMillis();
        long start = Math.max(now, enemyCardFree);   // after the AI's previous card, if it is still showing
        enemyCardFree = start + ENEMY_CARD_MS;
        CardView big = new CardView(card, ENEMY_BIG_W);   // much bigger than yours, easy to read
        big.relocate(ENEMY_BIG_X, ENEMY_BIG_Y);
        big.setMouseTransparent(true);
        big.setVisible(false);
        table.getChildren().add(big);   // on the table: a redraw does not cut the queue short
        new Timeline(
                new KeyFrame(Duration.millis(start - now), e -> big.setVisible(true)),
                new KeyFrame(Duration.millis(start - now + ENEMY_CARD_MS), e -> table.getChildren().remove(big))).play();
        for (int slot = 0; slot < BoardSide.MAX; slot++) {
            Minion m = enemy.getSide().getAt(slot);
            if (m != null && m.getSource() == card && !nodes.containsKey(m)) {
                Node played = new MinionView(m, MINION_W);
                played.relocate(SLOT_X + 2 + slot * SLOT_STEP, ENEMY_Y);
                nodes.put(m, played);   // its effects show on it, and it is not redrawn as a newcomer
                live.getChildren().add(played);
            }
        }
    }

    /**
     * The card just played appears at once, before its effects: a minion in its slot (as a board minion), a Shrine in its slot,
     * a spell big in the middle; the hand is drawn again without it. The redraw after the effects replaces all of this.
     */
    public void showPlayed(Card card, int slot) {
        rising = card.getId().equals("kuchiyose") || card.getId().equals("izanami");
        yoi = card.getId().equals("yoi");
        signature = card.getId();
        signatureSlot = slot;
        drawHand();
        Node played;
        if (card instanceof MinionCard && you.getSide().getAt(slot) != null) {
            played = new MinionView(you.getSide().getAt(slot), MINION_W);
            played.relocate(SLOT_X + 2 + slot * SLOT_STEP, YOUR_Y);
            nodes.put(you.getSide().getAt(slot), played);   // its effects show on this card, and it gets no smoke
        } else if (card instanceof MinionCard || card instanceof ShrineCard) {
            played = new CardView(card, MINION_W);   // already gone (a trap killed it): its card shows in the slot
            played.relocate(SLOT_X + 2 + slot * SLOT_STEP, YOUR_Y);
        } else {
            played = bigCard(card, BIG_X);   // a spell shows big on the right
        }
        played.setMouseTransparent(true);
        // Placed: it comes down onto the table, from big to its size.
        ScaleTransition land = new ScaleTransition(Duration.millis(180), played);
        land.setFromX(1.4);
        land.setFromY(1.4);
        land.setToX(1);
        land.setToY(1);
        land.setInterpolator(Interpolator.EASE_IN);
        land.play();
        live.getChildren().add(played);
        if (card instanceof MinionCard) {
            Minion minion = you.getSide().getAt(slot);
            if (minion != null && minion.hasKeyword(Keyword.BANZAI)) {
                status.follow(played);
                status.banzai(minion, SLOT_X + 2 + slot * SLOT_STEP, YOUR_Y);   // 万歳! bursts out before the effect (shown once)
            }
        }
    }

    /** Your hand, drawn again from your cards (after a play, the card is no longer in it). */
    private void drawHand() {
        live.getChildren().remove(hand);
        hand = new HandView(you, this::drop, this::trade, this::previewCost, this::costOf, this::bonusOf, this::canPlay);
        hand.setVisible(dealt);
        live.getChildren().add(hand);
    }

    /**
     * Shows what an engine action did, then redraws: waits `pauseMs` (the card just played stays where it was dropped),
     * plays the effects one step after the other on the cards still on screen, holds `showMs`, then redraws and runs `then`.
     * With nothing to show, the hold is skipped.
     */
    public void replay(List<GameEvent> events, double pauseMs, double showMs, Runnable then) {
        double hold = showMs;
        if (events.isEmpty()) {
            hold = 0;
        }
        double holdMs = hold;
        PauseTransition pause = new PauseTransition(Duration.millis(pauseMs));
        pause.setOnFinished(e -> afterSignature(events, () -> showFrom(events, 0, () -> {   // the card's move, the effects step by step, the hold
            arrivals();   // the minions that just came: smoke or petals
            PauseTransition wait = new PauseTransition(Duration.millis(holdMs));
            wait.setOnFinished(f -> {
                render();
                if (yoi) {
                    hand.sparkleCosts();   // the hand is new now, with its lowered costs
                    yoi = false;
                }
                then.run();
            });
            wait.play();
        })));
        pause.play();
    }


    /** The card just played makes its own move first (Iaidō's draw, Nue's shockwave, Sakasama's flip…), then `then` runs. */
    private void afterSignature(List<GameEvent> events, Runnable then) {
        double ms = 0;
        switch (signature) {
            case "iaido" -> ms = SignatureFx.iaido(live);
            case "nue" -> ms = SignatureFx.shockwave(live, SLOT_X + 2 + signatureSlot * SLOT_STEP + MINION_W / 2, YOUR_Y + 80);
            case "enma" -> ms = SignatureFx.judgement(live);
            case "sakasama" -> {
                List<Node> enemies = new ArrayList<>();
                for (Minion m : enemy.getSide().getMinions()) {
                    enemies.add(nodes.get(m));
                }
                ms = SignatureFx.upsideDown(enemies);
            }
            case "chiburi", "katana-kaji" -> {
                if (nodes.get(you.getHero()).getProperties().get("weapon") instanceof Node blade) {
                    Fx.sparks(live, blade);
                    ms = 400;
                }
            }
            case "raijin" -> ms = SignatureFx.slices(live, raijinCuts(events));
            case "ketto" -> ms = duel();
            default -> { }   // no move of its own
        }
        signature = "";
        PauseTransition wait = new PauseTransition(Duration.millis(ms));
        wait.setOnFinished(e -> then.run());
        wait.play();
    }

    /** Raijin: one cut per point of damage on each character it hit, in a random order (12 in all). */
    private List<Node> raijinCuts(List<GameEvent> events) {
        List<Node> cuts = new ArrayList<>();
        for (GameEvent e : events) {
            Node target = nodes.get(e.target());
            boolean raijinHit = e.type() == GameEvent.Type.DAMAGE && e.source() instanceof Minion m && m.getSource().getId().equals("raijin");
            if (raijinHit && target != null) {
                for (int i = 0; i < e.amount(); i++) {
                    cuts.add(target);
                }
            }
        }
        Collections.shuffle(cuts);
        return cuts;
    }

    /** Kettō: the two minions clash under white smoke and go back; then the winner shows the HP it lost (the loser's cut is the next step). */
    private double duel() {
        List<Combatant> pair = new ArrayList<>(duelHp.keySet());
        if (pair.size() < 2 || nodes.get(pair.get(0)) == null || nodes.get(pair.get(1)) == null) {
            return 0;
        }
        double ms = SignatureFx.duel(live, nodes.get(pair.get(0)), nodes.get(pair.get(1)));
        for (Combatant c : pair) {
            if (!c.isDead()) {
                Node winner = nodes.get(c);
                int lost = duelHp.get(c) - c.getHealth();
                PauseTransition back = new PauseTransition(Duration.millis(ms));
                back.setOnFinished(e -> Fx.damageNumber(live, winner, lost));
                back.play();
            }
        }
        return ms;
    }

    /** How long one step of effects plays before the next one starts, and how long a trap takes to flip, be read and start its cut. */
    private static final double STEP_MS = 450, TRAP_STEP_MS = 900, LUNGE_MS = 700;
    /** How long an effect's banner shows before the effect itself plays. */
    private static final double TRIGGER_MS = 1000;

    /** An effect about to go off (Seppuku, a turn effect, a Shrine, Gaki): its card's name on a banner, then the effect. */
    private double trigger(Object source) {
        Card card = null;
        String what = "Effect";
        Color colour = Color.web("#c9a227");
        if (source instanceof Minion m) {
            card = m.getSource();
            if (m.isDead()) {
                what = "Seppuku";
                colour = Color.CRIMSON;
            }
        } else if (source instanceof Shrine s) {
            card = s.getSource();
            what = "Shrine";
            colour = Color.web("#9a86c4");
        } else if (source instanceof Card c) {
            card = c;
            what = "Curse";
            colour = Color.web("#6b2d8b");
        }
        if (card == null) {
            return 0;   // an Oath: its reward already has its stamp and banner
        }
        EventBanner.show(table, card.getNameKanji(), card.getNameRomaji(), what, colour);
        Node node = nodes.get(source);
        if (node != null && card.getId().equals("fuma-kotaro")) {
            Bounds box = node.getBoundsInParent();
            SignatureFx.puff(live, box.getCenterX(), box.getCenterY());   // Yogake: he vanishes into smoke
        }
        if (node != null && card.getId().equals("inari-okami")) {
            SignatureFx.foxfire(live, node);   // calls a kitsune at the end of the turn
        }
        return TRIGGER_MS;
    }

    /**
     * Shows the effects from index `from` one step after the other, then runs `then`. A step is a run of the same effect
     * from the same source (an area spell hits everyone at once); a trap is always a step of its own.
     */
    private void showFrom(List<GameEvent> events, int from, Runnable then) {
        if (from >= events.size()) {
            then.run();
            return;
        }
        GameEvent first = events.get(from);
        int next = from;
        double stepMs = 0;
        do {
            stepMs = Math.max(stepMs, show(events.get(next)));
            next++;
        } while (next < events.size() && first.type() != GameEvent.Type.TRAP_TRIGGERED
                && events.get(next).type() == first.type() && events.get(next).source() == first.source());
        int after = next;
        PauseTransition step = new PauseTransition(Duration.millis(stepMs));
        step.setOnFinished(e -> showFrom(events, after, then));
        step.play();
    }

    /** One effect: a slash and the red number for damage, the healing number, the cut for a death, a trap flipping face up.
     *  Returns how long it plays, so the next step waits for it. */
    private double show(GameEvent event) {
        if (event.type() == GameEvent.Type.TRAP_TRIGGERED) {
            revealTrap((TrapCard) event.target());
            return TRAP_STEP_MS;
        }
        if (event.type() == GameEvent.Type.EFFECT_TRIGGERED) {
            return trigger(event.source());
        }
        Node node = nodes.get(event.target());
        if (node == null) {
            return 0;   // not on screen before the action (summoned by it): it simply appears with the redraw
        }
        switch (event.type()) {
            case DAMAGE -> {
                Sound.play(HIT);   // every hit: attacks, minion effects, hero powers, spells
                Fx.slash(live, node);
                Fx.damageNumber(live, node, event.amount());
            }
            case HEAL -> Fx.heal(live, node, event.amount());
            case MINION_DIED -> {
                if (event.target() instanceof Minion m && m.hasKeyword(Keyword.SEPPUKU)) {
                    SignatureFx.seppuku(live, node);   // the blade drives in; its Seppuku effect is the next step
                }
                Fx.deathCut(live, node);
            }
            case MINION_REMOVED -> SignatureFx.vanish(node);   // Kamikakushi: spirited away
            case SILENCED -> {
                silenced.add((Minion) event.target());   // Shike: grey for the rest of the match
                node.setEffect(grey(node.getEffect()));
            }
            case FATIGUE -> EventBanner.show(table, "疲労", "Fatigue", event.amount() + " damage: the deck is empty", Color.web("#8b0000"));
            case ATTACK -> {
                Node attacker = nodes.get(event.source());
                if (attacker != null) {
                    Fx.attack(attacker, node, () -> { });   // Yogake: each enemy minion lunges at another one
                }
                return LUNGE_MS;
            }
            default -> {
                return 0;   // nothing to show (a card added to the hand flies in after the redraw)
            }
        }
        return STEP_MS;
    }

    /**
     * Your minions that were not on screen before the action make their entrance: black smoke for the dead brought back
     * (Kuchiyose, Izanami), white smoke for an Ancestor, sakura petals for a Miko.
     */
    private void arrivals() {
        for (int slot = 0; slot < BoardSide.MAX; slot++) {
            Minion m = you.getSide().getAt(slot);
            if (m == null || nodes.containsKey(m)) {
                continue;   // empty, or already on screen
            }
            double x = SLOT_X + 2 + slot * SLOT_STEP;
            String id = m.getSource().getId();
            if (rising) {
                smoke(x, false);
            } else if (id.equals("ancestor")) {   // RiteCard.ancestorCard()
                smoke(x, true);
            } else if (id.equals(GameEngine.MIKO_ID)) {
                petals(x);
            }
        }
        rising = false;
    }

    /** The arrival smoke's size: wide and tall enough to hide a minion while it appears. */
    private static final double SMOKE_W = 320, SMOKE_H = 220;

    /** Smoke rises over the slot at x and fades: black, or white (the same image brightened). On the table: the redraw clears `live`. */
    private void smoke(double x, boolean white) {
        ImageView smoke = image("/images/fx/black-smoke.png");   // 600 x 198
        smoke.setFitWidth(SMOKE_W);
        smoke.setFitHeight(SMOKE_H);   // stretched taller than the picture, to hide the whole minion
        smoke.setMouseTransparent(true);
        if (white) {
            smoke.setEffect(new ColorAdjust(0, 0, 1, 0));
        }
        smoke.relocate(x + MINION_W / 2 - SMOKE_W / 2, YOUR_Y - 20);
        smoke.setOpacity(0);
        smoke.setId("arrival-smoke");   // render() keeps it in front, so the minion appears from behind it
        table.getChildren().add(smoke);
        Timeline puff = new Timeline(
                new KeyFrame(Duration.millis(400), new KeyValue(smoke.opacityProperty(), 1)),   // thick first, hiding the slot
                new KeyFrame(Duration.millis(1200), new KeyValue(smoke.opacityProperty(), 1), new KeyValue(smoke.translateYProperty(), -20)),
                new KeyFrame(Duration.millis(2600), new KeyValue(smoke.opacityProperty(), 0), new KeyValue(smoke.translateYProperty(), -70)));   // then it clears slowly
        puff.setOnFinished(e -> table.getChildren().remove(smoke));
        puff.play();
    }

    /** The Miko arrives: sakura petals drift down over the slot at x, spinning and swaying, then fade. */
    private void petals(double x) {
        for (int i = 0; i < 16; i++) {
            Ellipse petal = new Ellipse(6, 3.5);
            petal.setFill(Color.web("#f6c1d0"));
            petal.setMouseTransparent(true);
            petal.relocate(x + Math.random() * MINION_W, YOUR_Y - 40 - Math.random() * 60);
            table.getChildren().add(petal);
            Duration time = Duration.millis(2600 + Math.random() * 1200);   // a slow fall
            TranslateTransition fall = new TranslateTransition(time, petal);
            fall.setByX(-30 + Math.random() * 60);
            fall.setByY(180);
            RotateTransition spin = new RotateTransition(time, petal);
            spin.setByAngle(360);
            FadeTransition fade = new FadeTransition(time, petal);
            fade.setFromValue(1);
            fade.setToValue(0);
            fade.setOnFinished(e -> table.getChildren().remove(petal));
            fall.play();
            spin.play();
            fade.play();
        }
    }

    /** You just set a trap: it is shown big, flips, and glides to its place next to your hero. */
    public void showTrapSet(TrapCard trap) {
        int index = you.getTraps().indexOf(trap);
        double toX = TRAPS_X + index * TrapView.SPACING;
        arriving = trap;
        TrapView.set(table, trap, toX, YOUR_TRAPS_Y, () -> {
            arriving = null;
            render();   // now traps() draws it in its place
        });
    }


    /** True from the end of a turn until the pass veil is clicked: banners and stamps wait, so the veil doesn't hide them. */
    private boolean holdingEffects;
    /** The banners and stamps waiting for the veil to lift, in order. */
    private final List<Runnable> heldEffects = new ArrayList<>();
    /** Time between two held effects, so they don't play on top of each other. */
    private static final Duration HELD_GAP = Duration.millis(1900);

    /** From now until the next pass veil is clicked, banners and stamps wait (MatchController calls it as a turn ends). */
    public void holdEffects() {
        holdingEffects = true;
    }

    /** Runs the effect now, or keeps it for when the pass veil lifts. */
    private void runOrHold(Runnable effect) {
        if (holdingEffects) {
            heldEffects.add(effect);
            return;
        }
        effect.run();
    }

    /** The veil has lifted: the waiting effects play one after the other. */
    private void releaseEffects() {
        holdingEffects = false;
        for (int i = 0; i < heldEffects.size(); i++) {
            Runnable effect = heldEffects.get(i);
            PauseTransition wait = new PauseTransition(HELD_GAP.multiply(i));
            wait.setOnFinished(event -> effect.run());
            wait.play();
        }
        heldEffects.clear();
    }

    /** A short announcement across the middle of the table (Curses, Rites, Shrines, Oaths). On `table`, so a redraw doesn't erase it. */
    public void announce(String kanji, String title, String subtitle, Color colour) {
        runOrHold(() -> EventBanner.show(table, kanji, title, subtitle, colour));
    }

    /** An Oath reward (3, 6 or 9 stacks): the red seal slams onto the player's small Oath scroll. */
    public void stampOath(Player p) {
        runOrHold(() -> {
            // Where the player's small scroll is now (the board may have flipped since the reward).
            double zoneY = 107;
            if (p == you) {
                zoneY = 637;
            }
            ImageView seal = image("/images/hud/stamp-seal.png");
            seal.setFitWidth(60);
            seal.setPreserveRatio(true);
            seal.setRotate(-8);
            seal.relocate(1115, zoneY + 30);
            seal.setMouseTransparent(true);
            seal.setOpacity(0);
            seal.setScaleX(3);
            seal.setScaleY(3);
            table.getChildren().add(seal);

            // Slams down from big to small, stays a moment, then fades away.
            KeyFrame land = new KeyFrame(Duration.millis(160),
                    new KeyValue(seal.opacityProperty(), 1),
                    new KeyValue(seal.scaleXProperty(), 1, Interpolator.EASE_IN),
                    new KeyValue(seal.scaleYProperty(), 1, Interpolator.EASE_IN));
            KeyFrame stay = new KeyFrame(Duration.millis(1400), new KeyValue(seal.opacityProperty(), 1));
            KeyFrame gone = new KeyFrame(Duration.millis(1800), new KeyValue(seal.opacityProperty(), 0));
            Timeline slam = new Timeline(land, stay, gone);
            slam.setOnFinished(event -> table.getChildren().remove(seal));
            slam.play();
        });
    }

    /** A hero has died: the first time, it burns into dust before the Victory / Defeat seal; after that, only the seal. */
    private void endGame(boolean won) {
        Hero dead = you.getHero();
        if (won) {
            dead = enemy.getHero();
        }
        Node node = nodes.get(dead);
        if (burnt || node == null) {
            gameOver(won);
            return;
        }
        burnt = true;
        HeroBurn.play(node, () -> gameOver(won));
    }

    /** Game over: a dark veil over the whole table blocks every click, with the 勝利 Victory or 敗北 Defeat seal in the middle. */
    private void gameOver(boolean won) {
        ImageView seal = image(DEFEAT);
        if (won) {
            seal = image(VICTORY);
        }
        seal.setFitHeight(END_H);
        seal.setPreserveRatio(true);
        StackPane end = new StackPane(new Rectangle(W, H, Color.rgb(0, 0, 0, 0.6)), seal);
        end.setOnMousePressed(e -> onGameOver.run());
        table.getChildren().add(end);
    }


    /** Asks "Surrender?" over the table: Yes makes the player at the bottom (whose turn it is) lose, No closes the question. */
    private void askSurrender() {
        Label question = new Label("降参しますか？\nSurrender this match?");
        question.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: 48; -fx-font-weight: bold; -fx-text-alignment: center; -fx-text-fill: #f3ede1;");
        StackPane veil = new StackPane();

        Label yes = darkButton("はい  YES", () -> {
            table.getChildren().remove(veil);
            onSurrender.run();   // the engine records the other player as the winner
            gameOver(false);     // the player at the bottom has lost
        });
        Label no = darkButton("いいえ  NO", () -> table.getChildren().remove(veil));
        HBox answers = new HBox(40, yes, no);
        answers.setAlignment(Pos.CENTER);
        VBox box = new VBox(30, question, answers);
        box.setAlignment(Pos.CENTER);

        veil.getChildren().addAll(new Rectangle(W, H, Color.rgb(0, 0, 0, 0.8)), box);
        table.getChildren().add(veil);
    }

    /** A dark rounded button with light text, for the surrender question. */
    private static Label darkButton(String words, Runnable action) {
        Label button = new Label(words);
        button.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: #f3ede1;"
                + " -fx-background-color: rgba(20,12,8,0.75); -fx-background-radius: 10; -fx-padding: 8 16;");
        HomeView.clickable(button, false, action);
        return button;
    }

    /** No clock in this match (Adventure), or not yet (Oaths not sworn): both clocks hidden. */
    public void hideClocks() {
        yourClock.setVisible(false);
        enemyClock.setVisible(false);
    }

    /** Both chess clocks: the active one is big and bright, the other small and grey (GDD). */
    public void showClocks(String yours, String theirs, boolean yourTurn) {
        yourClock.setVisible(true);
        enemyClock.setVisible(true);
        yourClock.setText(yours);
        enemyClock.setText(theirs);
        clock(yourClock, yourTurn);
        clock(enemyClock, !yourTurn);
    }


    /** How long the TIME'S UP banner stays before the pass veil. */
    private static final Duration TIMEOUT_SHOW = Duration.millis(1600);

    /**
     * Chess clock timeout (GameEvent.CLOCK_TIMEOUT): the hero who ran out of time is slashed and shows the damage,
     * "時間切れ TIME'S UP" covers the table (and blocks clicks), then `then` runs after TIMEOUT_SHOW.
     */
    public void showTimeout(Hero hero, int damage, Runnable then) {
        render();   // the hero's HP after the damage (or the game over, if it killed him)
        Node heroNode = nodes.get(hero);
        if (heroNode != null) {
            Fx.slash(live, heroNode);
            Fx.damageNumber(live, heroNode, damage);
        }

        Label banner = new Label("時間切れ\nTIME'S UP");
        banner.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: 72; -fx-font-weight: bold; -fx-text-alignment: center; -fx-text-fill: #c41e1e;");
        banner.setEffect(new DropShadow(30, Color.BLACK));
        StackPane cover = new StackPane(new Rectangle(W, H, Color.rgb(0, 0, 0, 0.35)), banner);
        table.getChildren().add(cover);

        PauseTransition wait = new PauseTransition(TIMEOUT_SHOW);
        wait.setOnFinished(event -> {
            table.getChildren().remove(cover);
            then.run();
        });
        wait.play();
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
    /** What the match tells the board about playing a hand card: its targets, how many to pick, and the play itself. */
    public void setOnPlay(CardPlayer player) {
        this.player = player;
        render();
    }

    /** What happens when you use a Ninja, Oni or Kannushi power on a target (null for the Miko): true if the engine accepted it. */
    public void setOnPower(Predicate<Combatant> onPower) { this.onPower = onPower; }

    /** What happens when the Samurai picks a blade: true if the engine accepted it. */
    public void setOnKatana(Predicate<KatanaTier> onKatana) { this.onKatana = onKatana; }

    /** Start of your turn: the hero power can be used again. */
    public void resetPower() {
        render();
    }

    /** What happens when you pick an attacker and then a target (the match will call the engine here). */
    public void setOnAttack(BiFunction<Combatant, Combatant, List<GameEvent>> onAttack) { this.onAttack = onAttack; }

    /** Dims, with a slow pulse, the part of your available Ki that a hovered card or power would cost (0 = none). */
    public void previewCost(int cost) { ki.preview(cost); }

    public void setOnEndTurn(Runnable r) { onEndTurn = r; }
    /** What happens when you click "Swear again" on your Oath once Oathbreaker is over. */
    public void setOnReswear(Runnable r) { onReswear = r; }

    /** Start of a match: the class's Oath choice covers the table until one is sworn (GDD, Oaths). */
    public void chooseOath(HeroClass c, Consumer<String> onSworn) { table.getChildren().add(new OathView(c, onSworn)); }


    /** Start of a match: p's opening hand covers the table; `exchange` swaps the cards sent back (engine.mulligan), then onDone. */
    public void mulligan(Player p, Function<List<Card>, List<Card>> exchange, Runnable onDone) {
        table.getChildren().add(new MulliganView(p, exchange, onDone));
    }
    
    
    private final Map<Player, String> oaths = new HashMap<>();
    /** Records the Oath a player swore; it then shows next to their hero. */
    public void swear(Player p, String oath) { oaths.put(p, oath); render(); }

    /**
     * The sworn Oath as a small stamped scroll right of the hero power; hover it to reread it (an enemy Ninja's stays hidden below 3 stacks).
     * A broken Oath is greyed with its Oathbreaker countdown; once the countdown is over, you can click to swear it again.
     */
    private void oath(Player p, double zoneY) {
        if (!oaths.containsKey(p)) return;
        HeroClass c = p.getHero().getHeroClass();
        boolean hidden = p == enemy && c == HeroClass.NINJA && stacksOf(p) < 3;
        StackPane mini = OathView.mini(c, oaths.get(p), hidden);
        place(mini, 1120, zoneY - 15);
        bigOnHover(mini, () -> OathView.sworn(c, oaths.get(p)), 850, 80);

        Oath sworn = p.getOath();
        if (sworn == null || sworn.isSworn()) {
            return;   // a kept Oath: nothing more to draw
        }
        mini.setEffect(new ColorAdjust(0, -1, -0.4, 0));   // grey: the Oath is broken
        if (sworn.getState() == Oath.State.OATHBREAKER) {
            String penalty = OathView.penaltyName(c, oaths.get(p));
            Label countdown = hud(new Label("破約 " + penalty + " · " + sworn.getOathbreakerTurnsLeft()), 0, 0, 200, 30, 15);
            place(countdown, 1060, zoneY + 45);
        } else if (p == you) {
            Label again = hud(new Label("再誓 Swear again"), 0, 0, 170, 30, 15);
            HomeView.clickable(again, false, onReswear);
            place(again, 1060, zoneY + 45);
        }
    }

    /** Your Oath stacks, bottom left: a tag, then 9 marks (the 3 / 6 / 9 reward marks bigger with a gold ring; earned marks are red). A newly earned mark pops in. */
    private void stackCounter() {
        if (!oaths.containsKey(you)) return;
        int n = stacksOf(you);
        int before = stacksShown.getOrDefault(you, n);   // what this player's counter showed last time
        stacksShown.put(you, n);
        place(hud(new Label("誓約  OATH STACKS"), 0, 0, 150, 22, 13), 55, 808);
        for (int i = 1; i <= 9; i++) {
            Circle c = new Circle(i % 3 == 0 ? 11 : 7, i <= n ? Color.web("#c41e1e") : Color.rgb(0, 0, 0, 0.35));
            c.setStroke(i % 3 == 0 ? Color.GOLD : Color.web("#f3ede1"));
            c.setStrokeWidth(2);
            place(c, 40 + i * 30, 840);
            if (i > before && i <= n) {
                pop(c);   // just earned
            }
        }
        place(hud(new Label("誓 " + n + " / 9"), 0, 0, 90, 30, 16), 340, 829);
    }

    /** A just-earned Oath mark: it appears big and glowing gold, then settles into its place. */
    private static void pop(Circle mark) {
        mark.setEffect(new DropShadow(18, Color.GOLD));
        ScaleTransition settle = new ScaleTransition(Duration.millis(450), mark);
        settle.setFromX(2.4);
        settle.setFromY(2.4);
        settle.setToX(1);
        settle.setToY(1);
        settle.setInterpolator(Interpolator.EASE_OUT);
        settle.play();
    }

    /** The player's Oath stacks, read from the engine's Oath (0 before an Oath is sworn). */
    private int stacksOf(Player p) {
        if (p.getOath() == null) {
            return 0;
        }
        return p.getOath().getStacks();
    }

    /** Adventure: the AI's turn is over and you play again at the bottom (the board never flips): End turn works again. */
    public void yourTurn() {
        endTurn.setImage(SWITCH_ON);
        endTurn.setDisable(false);
        selected = null;
        aiming = false;
        held = null;
        render();
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
        aiming = false;
        held = null;
        render();
    }

    /** Hotseat: a dark veil hides the table until the next player clicks, so nobody sees the other's hand (GDD). */
public void passTo(String name, Runnable onReady) {
        Label text = new Label("次の番\nPass to " + name + "\n\nclick when ready");
        text.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: 56; -fx-font-weight: bold; -fx-text-alignment: center; -fx-text-fill: #f3ede1;");
        StackPane veil = new StackPane(new Rectangle(W, H, Color.rgb(0, 0, 0, 0.95)), text);
        veil.setOnMousePressed(e -> { table.getChildren().remove(veil); onReady.run(); releaseEffects(); });
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
        Label count = hud(new Label(String.valueOf(n)), 0, 0, 44, 30, 18);
        if (n == 0 && player != null) {
            int fatigue = player.nextFatigue(p);
            count.setText("-" + fatigue);   // empty deck: the damage of the next draw, in red
            count.setStyle(count.getStyle() + " -fx-text-fill: #ff5a4a;");
            Tooltip.install(count, new Tooltip("Empty deck: the next draw deals " + fatigue + " damage to the hero"));
        }
        place(count, tx + MINION_W / 2 - 22, ty + cardH - 40);
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
            Tooltip.install(back, quick(n + " cards in hand"));
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


    /** A tooltip that shows almost at once (JavaFX's default waits a whole second). */
    static Tooltip quick(String text) {
        Tooltip tip = new Tooltip(text);
        tip.setShowDelay(Duration.millis(100));
        return tip;
    }

    /** One side's 7 fixed slots, left to right. Each minion is drawn in the slot it was dropped on; empty slots stay empty. */
    private void row(Player p, double y) {
        for (int slot = 0; slot < BoardSide.MAX; slot++) {
            BoardEntity entity = p.getSide().entityAt(slot);
            if (entity instanceof Shrine shrine) {
                shrine(shrine, slot, y);   // a Shrine stands here: drawn on its own, it cannot be selected or attacked
                continue;
            }
            Minion minion = p.getSide().getAt(slot);
            if (minion == null) {
                continue;   // empty slot: nothing to draw
            }

            double x = SLOT_X + 2 + slot * SLOT_STEP;
            groundShadow(x, y);

            MinionView view = new MinionView(minion, MINION_W);
            clickable(view, minion, p == you);
            if (silenced.contains(minion)) {
                view.setEffect(grey(view.getEffect()));   // silenced by Shike: no keywords, no effects
            }

            // The big preview opens on the left for the right-hand slots, so it stays on screen.
            boolean rightHandSlot = slot >= 4;
            readable(view, minion, x, y, rightHandSlot);
            place(view, x, y);

            // Keyword overlays. follow() makes them move with the card when it attacks.
            status.follow(view);
            if (minion.hasTorii()) {
                status.gate("torii", minion, x, y);
            }
            if (minion.hasKeyword(Keyword.SHIRODU)) {
                status.gate("shirodu", minion, x, y);
            }
            if (minion.hasKeyword(Keyword.PASSIVE)) {
                status.flag(minion, x, y);   // after the gates, so the flag lies on top of a Shirodu gate
            }
            if (minion.isHidden()) {
                status.shinobi(view, minion, x, y);
            }
            if (minion.hasKeyword(Keyword.SEPPUKU)) {
                status.seppuku(x, y);
            }
            if (minion.getArmour() > 0) {
                status.armour(minion, x, y);
            }
            if (minion.hasKeyword(Keyword.BANZAI)) {
                status.banzai(minion, x, y);
            }
            if (minion.hasStatus(Status.SLEEP)) {
                status.sleep(minion, x, y);
            }
        }
    }

    /** A Shrine in its slot: its card with the turns it has left, a big copy on hover, and its aura glowing on the two slots beside it. */
    private void shrine(Shrine shrine, int slot, double y) {
        double x = SLOT_X + 2 + slot * SLOT_STEP;
        groundShadow(x, y);
        ShrineView view = new ShrineView(shrine, MINION_W);
        place(view, x, y);

        // The big copy opens on the left for the right-hand slots, so it stays on screen (like a minion's).
        double bigW = 280;
        double bigH = bigW * CardView.H / CardView.W;
        double bigX = x + MINION_W + 20;
        if (slot >= 4) {
            bigX = x - 20 - bigW;
        }
        double bigY = Math.max(10, Math.min(H - bigH - 10, y + 80 - bigH / 2));
        bigOnHover(view, () -> new ShrineView(shrine, bigW), bigX, bigY);

        if (slot > 0) {
            auraGlow(slot - 1, y);
        }
        if (slot < BoardSide.MAX - 1) {
            auraGlow(slot + 1, y);
        }
    }

    /** A Shrine's aura on a neighbouring slot: a soft lavender glow behind whatever stands there. */
    private void auraGlow(int slot, double y) {
        double x = SLOT_X + 2 + slot * SLOT_STEP;
        double cardH = MINION_W * CardView.H / CardView.W;
        Rectangle glow = new Rectangle(MINION_W + 16, cardH + 16, Color.web("#b7a6dd", 0.35));   // wider than the card: the rim shows around a minion standing there
        glow.setArcWidth(20);
        glow.setArcHeight(20);
        glow.setEffect(new DropShadow(30, Color.web("#b7a6dd")));
        glow.setMouseTransparent(true);
        glow.setViewOrder(1);   // behind the cards
        glow.relocate(x - 8, y - 8);
        live.getChildren().add(glow);
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
        if (h.hasStatus(Status.SLEEP)) {
            status.follow(box);
            status.sleep(h, W / 2 + wid / 2 - BoardView.MINION_W - 10, zoneY);   // zzz from the hero's top-right corner
        }
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
            int durability = h.getWeapon().getDurability();
            if (durability > 1) {
                Label uses = hud(new Label(String.valueOf(durability)), 0, 0, 34, 34, 18);   // uses left, bottom-right of the rack
                uses.setAlignment(Pos.CENTER);
                StackPane.setAlignment(uses, Pos.BOTTOM_RIGHT);
                slot.getChildren().add(uses);
            }
            box.getProperties().put("weapon", w);
        }
        place(slot, W / 2 - 120 - 195, zoneY + (119 - 83) / 2);
    }

    private void place(Node n, double x, double y) {
        n.relocate(x, y);
        live.getChildren().add(n);
    }

    /** A picture from the resources in a new ImageView; the picture itself is decoded only once (Images keeps it). */
    static ImageView image(String path) {
        return new ImageView(Images.get(path));
    }

    /** A hand card dropped on the board: played at once, or held until its target(s) are clicked when it needs any. */
    private void drop(Card card, int slot) {
        List<Combatant> targets = player.targets(card);
        if (targets.isEmpty()) {
            finishPlay(card, slot, null, null);
            return;
        }
        held = card;
        heldSlot = slot;
        heldTargets = targets;
        firstPick = null;
        selected = null;
        aiming = false;
        render();   // the legal targets glow green; the card waits in the hand until one is clicked
    }

    /** While a card waits for its target(s): each legal one glows green and is picked on click; anything else sends the card back. */
    private void pickable(Node n, Combatant c) {
        if (c == firstPick) {
            n.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.GOLD, 28, 0.45, 0, 0));   // Kettō: the first minion already picked
            n.setOnMouseClicked(e -> cancelHeld());
        } else if (heldTargets.contains(c)) {
            markOnHover(n);
            n.setEffect(new DropShadow(30, Color.LIMEGREEN));
            n.setOnMouseClicked(e -> pick(c));
        } else {
            n.setOnMouseClicked(e -> cancelHeld());
        }
    }

    /** A legal target clicked: Kettō keeps the first one and waits for a second; otherwise the card is played on it. */
    private void pick(Combatant c) {
        if (player.picks(held) == 2 && firstPick == null) {
            firstPick = c;
            render();
            return;
        }
        Combatant first = c;
        Combatant second = null;
        if (firstPick != null) {
            first = firstPick;
            second = c;
        }
        finishPlay(held, heldSlot, first, second);
    }

    /** The held card goes back to the hand: nothing is played, no Ki is spent. */
    private void cancelHeld() {
        held = null;
        render();
    }

    /** Plays the card with its target(s); the Ki impact only shows if the engine accepted it (a refused card stays in the hand). */
    private void finishPlay(Card card, int slot, Combatant first, Combatant second) {
        held = null;
        firstPick = null;
        int cost = costOf(card);   // read before the card leaves the hand
        duelHp.clear();   // Kettō: both duellists' HP before the duel, to show what the winner loses
        if (first != null && second != null) {
            duelHp.put(first, first.getHealth());
            duelHp.put(second, second.getHealth());
        }
        if (player.play(card, slot, first, second)) {
            impact(cost, false);
            heavyLanding(card);
        }
    }

    /** What a hand card costs now (Yōi, Oath discounts); its printed Ki until the match is plugged in. */
    private int costOf(Card card) {
        if (player == null) {
            return card.getKiCost();
        }
        return player.cost(card);
    }

    /** True if the engine would accept this hand card now; false until the match is plugged in. */
    private boolean canPlay(Card card) {
        if (player == null) {
            return false;
        }
        return player.canPlay(card);
    }

    /** What to add to the damage or healing number of a hand card (Ryū, Oaths, Chikara); 0 until the match is plugged in. */
    private int bonusOf(Card card) {
        if (player == null) {
            return 0;
        }
        return player.bonus(card);
    }

    /** Attack selection on one character. Yours that can attack: yellow border (thicker on hover, gold glow when picked),
     *  click to pick or unpick. Once one is picked: legal enemies glow red and attack on click; anything else cancels. */
    private void clickable(Node n, Combatant c, boolean mine) {
        nodes.put(c, n);
        if (held != null) {
            pickable(n, c);   // a played card is waiting for its target(s): no attack selection now
            return;
        }
        if (aiming) {
            aimable(n, c);   // a hero power is waiting for its target: no attack selection now
            return;
        }
        if (mine && c.canAttack()) {
            if (c == selected) floatReady(n);
            n.setEffect(c == selected ? new DropShadow(BlurType.GAUSSIAN, Color.GOLD, 28, 0.45, 0, 0) : border(8));
            n.setOnMouseEntered(e -> { if (c != selected) n.setEffect(border(14)); });
            n.setOnMouseExited(e -> { if (c != selected) n.setEffect(border(8)); });
            n.setOnMouseClicked(e -> { selected = c == selected ? null : c; render(); });
        } else if (!mine && selected != null && canTarget(c)) {
            markOnHover(n);
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

    /** Whether the selected attacker may hit t, by the engine's own rule (Shirodu first, no hidden Shinobi, Stealth…). */
    private boolean canTarget(Combatant t) {
        return player.attackTargets(selected).contains(t);
    }

    /** While aiming: the target rings appear centred on n when the mouse is over it, and go away when it leaves. */
    private void markOnHover(Node n) {
        n.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            Bounds box = table.sceneToLocal(n.localToScene(n.getBoundsInLocal()));
            targetMark.relocate(box.getCenterX() - MARK_W / 2, box.getCenterY() - MARK_W / 2);
            targetMark.setVisible(true);
        });
        n.addEventHandler(MouseEvent.MOUSE_EXITED, e -> targetMark.setVisible(false));
    }

    /** Where the targeting arrow starts: the selected attacker, or your hero for a held card or a hero power; null when nothing aims. */
    private Node arrowSource() {
        if (selected != null) {
            return nodes.get(selected);
        }
        if (held != null || aiming) {
            return nodes.get(you.getHero());
        }
        return null;
    }

    /** Points the targeting arrow from its source to the mouse at (x, y) on the table; hides it when nothing aims. */
    private void pointArrow(double x, double y) {
        Node from = arrowSource();
        if (from == null) {
            arrow.setVisible(false);
            return;
        }
        Bounds box = table.sceneToLocal(from.localToScene(from.getBoundsInLocal()));
        arrow.span(box.getCenterX(), box.getCenterY(), x, y);
        arrow.setVisible(true);
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

    /** A minion of HEAVY_KI or more lands with a heavy thud (sounds/heavy-minion.wav), yours or the AI's. */
    private static void heavyLanding(Card card) {
        if (card instanceof MinionCard && card.getKiCost() >= HEAVY_KI) {
            Sound.play(HEAVY_THUD);
        }
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
        boolean usable = mine && !powerUsed() && you.canAfford(cheapestPower());
        HeroPowerView v = new HeroPowerView(p.getHero().getHeroClass(), mine && powerUsed(), usable, this::previewCost,
                this::pressPower);
        bigOnHover(v, () -> {
            Node b = new HeroPowerView(p.getHero().getHeroClass(), mine && powerUsed(), false, c -> {}, () -> {});
            b.setScaleX(2.5);
            b.setScaleY(2.5);
            return b;
        }, 900, 330);
        place(v, 948 + (158 - HeroPowerView.SIZE) / 2, zoneY + (113 - HeroPowerView.SIZE) / 2);
    }

    /**
     * Your hero power was pressed. Kannushi: the Miko comes at once. Ninja and Oni: pick a target first.
     * Samurai: pick a blade first.
     */
    private void pressPower() {
        HeroClass heroClass = you.getHero().getHeroClass();
        if (heroClass == HeroClass.KANNUSHI) {
            onPower.test(null);   // accepted: the match replays it (the Miko's petals), then redraws
        } else if (heroClass == HeroClass.SAMURAI) {
            chooseBlade();
        } else {
            aiming = true;
            selected = null;   // stop any attack selection
            render();          // the targets now glow
        }
    }

    /** Samurai: the blade picker over the table; the picked blade is equipped if the engine accepts it. */
    private void chooseBlade() {
        KatanaPicker picker = new KatanaPicker(you.getKi(), tier -> {
            onKatana.test(tier);
            render();   // the blade appears on the rack, with the ATK badge
        }, () -> {});
        table.getChildren().add(picker);
    }

    /** The least Ki your power can cost: the Bokken (1) for the Samurai, HERO_POWER_COST for the others. */
    private int cheapestPower() {
        if (you.getHero().getHeroClass() == HeroClass.SAMURAI) {
            return KatanaTier.BOKKEN.getKiCost();
        }
        return GameEngine.HERO_POWER_COST;
    }


    /** True once your hero power was used this turn: the engine's count, so Kuji-in's refresh shows at once. */
    private boolean powerUsed() {
        return player != null && player.powerUsed();
    }

    /** While aiming a hero power: each character it may hit glows violet and takes the power on click; anything else cancels. */
    private void aimable(Node n, Combatant c) {
        if (canPowerTarget(c)) {
            markOnHover(n);
            n.setEffect(new DropShadow(30, Color.web("#b36bff")));
            n.setOnMouseClicked(e -> {
                aiming = false;
                powerHit(c, n);
            });
        } else {
            n.setOnMouseClicked(e -> {
                aiming = false;
                render();
            });
        }
    }

    /** Shuriken and Jikininki may hit any living character, except an enemy minion hidden by Shinobi or an enemy hero in Stealth. */
    private boolean canPowerTarget(Combatant c) {
        boolean enemyCharacter = c == enemy.getHero() || enemy.getSide().getMinions().contains(c);
        if (!enemyCharacter) {
            return true;
        }
        if (c instanceof Minion m && m.isHidden()) {
            return false;
        }
        if (c instanceof Hero h && h.hasStatus(Status.STEALTH)) {
            return false;
        }
        return true;
    }

    /** Shuriken or Jikininki hits `target`: sparks and the red damage number on it, a dead minion is cut, then the board redraws. */
    private void powerHit(Combatant target, Node node) {
        if (!onPower.test(target)) {
            render();   // refused by the engine: nothing happens
            return;
        }
        Node hero = nodes.get(you.getHero());   // the match replays the damage and any death or trap, then redraws
        if (you.getHero().getHeroClass() == HeroClass.ONI) {
            SignatureFx.siphon(live, node, hero);   // Jikininki: a black beam drains the target into your hero
        } else {
            SignatureFx.shuriken(live, hero, node);   // a spinning star thrown from your hero
        }
    }

    /** Plays an attack (GDD): lunge, slash, red damage on both, dead minions cut in half; the board redraws after. */
    private void fight(Combatant attacker, Combatant target) {
        fight(attacker, target, () -> {});
    }

    /**
     * Adventure: the AI attacks, with the same lunge, slash and damage numbers as a click;
     * `then` runs once the board has redrawn (the next AI move).
     */
    public void aiAttack(Combatant attacker, Combatant target, Runnable then) {
        if (nodes.get(attacker) == null || nodes.get(target) == null) {
            onAttack.apply(attacker, target);   // not on screen (should not happen): attack without the animation
            render();
            then.run();
            return;
        }
        fight(attacker, target, then);
    }

    private void fight(Combatant attacker, Combatant target, Runnable then) {
        List<GameEvent> events = onAttack.apply(attacker, target);   // the engine first: a trap may move or cancel the attack
        int attackAt = -1;
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).type() == GameEvent.Type.ATTACK) {
                attackAt = i;
                break;
            }
        }
        if (attackAt < 0) {
            replay(events, 0, 700, then);   // no attack (Otoshiana, or Shikomi andon killed the attacker): only its effects
            return;
        }
        Combatant hit = (Combatant) events.get(attackAt).target();
        if (hit != target) {
            // Kawarimi: the trap and the log first (the redraw puts the log on screen), then the lunge at the log.
            List<GameEvent> after = events.subList(attackAt + 1, events.size());
            replay(events.subList(0, attackAt), 0, 0, () -> lunge(attacker, hit, after, then));
            return;
        }
        List<GameEvent> rest = new ArrayList<>(events);
        rest.remove(attackAt);   // this attack lunges now; any other ATTACK (Yogake's forced fights) lunges in the replay
        lunge(attacker, target, rest, then);
    }

    /** The lunge at `target`, then on impact the hits, deaths and anything else the attack did, step by step, then the redraw. */
    private void lunge(Combatant attacker, Combatant target, List<GameEvent> events, Runnable then) {
        Node a = nodes.get(attacker), t = nodes.get(target);
        if (a == null || t == null) {
            replay(events, 0, 700, then);
            return;
        }
        if (a.getProperties().get("float") instanceof Animation f) { f.stop(); a.setTranslateY(0); }
        Fx.attack(a, t, () -> {
            status.broken(target);
            status.broken(attacker);
            crack(target, t);
            crack(attacker, a);
            impact(attacker instanceof Minion m ? m.getSource().getKiCost() : 0, true);
            replay(events, 0, 700, then);   // the hits, a trap, the deaths, one step after the other, then the redraw
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

    /** What a hit can take away: HP plus armour (heroes, and minions given armour by Yoroi). */
    private static int life(Combatant c) {
        if (c instanceof Hero h) {
            return c.getHealth() + h.getArmour();
        }
        if (c instanceof Minion m) {
            return c.getHealth() + m.getArmour();
        }
        return c.getHealth();
    }

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
        // Removed on the next pulse: MOUSE_EXITED also fires while render() clears the table,
        // and removing a child in the middle of that clear() throws (it froze the AI's turn).
        EventHandler<MouseEvent> hide = e -> {
            delay.stop();
            Node shown = preview;
            if (shown != null) {
                Platform.runLater(() -> live.getChildren().remove(shown));
            }
        };
        n.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> delay.playFromStart());
        n.addEventHandler(MouseEvent.MOUSE_EXITED, hide);
        n.addEventHandler(MouseEvent.MOUSE_PRESSED, hide);
    }

    /** A board card's big copy, as printed on its card (its original stats): on the left for the three right-hand slots, on the right otherwise. */
    private void readable(MinionView v, Minion m, double x, double y, boolean left) {
        double w = 280, h = w * CardView.H / CardView.W;
        bigOnHover(v, () -> new CardView(m.getSource(), w), left ? x - 20 - w : x + MINION_W + 20, Math.max(10, Math.min(H - h - 10, y + 80 - h / 2)));   // the card as printed, not its current state
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