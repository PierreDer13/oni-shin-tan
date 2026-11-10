package fr.onishintan.ui.match;
import fr.onishintan.ui.screens.HomeView;

import fr.onishintan.model.HeroClass;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.util.Duration;

/** Oaths on scrolls (GDD, Oaths): the choice at the start of a match, the stamped scroll to reread, the small one by the hero. */
final class OathView extends StackPane {
    private static final Image SCROLL = new Image(OathView.class.getResourceAsStream("/images/hud/oath-scroll.png"));
    private static final String INK = "#1f1b17", RED = "#c41e1e", BRUSH = "Kaushan Script", JP = "Yuji Syuku";
    static {   // the brush fonts of the scrolls (OFL, Google Fonts)
        Font.loadFont(OathView.class.getResourceAsStream("/fonts/KaushanScript-Regular.ttf"), 12);
        Font.loadFont(OathView.class.getResourceAsStream("/fonts/YujiSyuku-Regular.ttf"), 12);
    }
    /** name, kanji, rule, 3 / 6 / 9 rewards, break. */
    private static final Map<HeroClass, String[][]> OATHS = Map.of(
            HeroClass.SAMURAI, new String[][] {
                { "Honor", "名誉", "Each turn, if one of your minions can attack, at least one must attack the enemy hero.", "Minions +2 ATK.", "When a minion attacks the enemy hero, heal your hero 3.", "The first minion played each turn gains +4 ATK and Tatakae.", "Dishonour: 10 + your hero is Asleep for 3 turns." },
                { "Allegiance", "忠義", "End your turn with at least 2 friendly minions.", "End of turn, a random friendly minion gets +2/+2.", "After your last minion dies, summon a 5/5 Retainer (once per turn).", "All friendly minions get +2/+2 and Shirodu.", "Banishment: 6 + destroy your strongest minion." },
                { "Justice", "正義", "When an enemy minion attacks your hero, you must attack it on your next turn, if possible.", "Killing an enemy minion gives +1 Ki next turn.", "Your minions deal double damage to minions that attacked your hero last turn.", "Oath card: deal your strongest minion's ATK to all enemy minions.", "Persecution: 8 + all enemy minions get +2 ATK for 3 turns." } },
            HeroClass.NINJA, new String[][] {
                { "Silence", "沈黙", "Play at most 2 cards per turn.", "Ninja cards cost 1 less Ki (min 1).", "End of turn, if you played 2 or fewer cards, draw a card.", "End of turn, duplicate the biggest minion you played this turn.", "Revelation: 8 + reveal your hand, discard a random card." },
                { "Deception", "欺瞞", "End each of your turns with at least 1 Trap in play.", "Your Traps cost 1 less Ki (min 0).", "When an enemy triggers a Trap, draw a card and +1 Ki next turn.", "Every Trap you play also casts another, different Trap.", "Exposed: 6 + your opponent steals all your Traps." },
                { "Discipline", "規律", "End each turn with 0 Ki, unless you have no legal card to play.", "If you spent 4+ Ki, +1 Ki next turn.", "Draw a card for every 6 Ki you spend in a turn.", "Every card you play gives an extra Ki.", "Disappointment: 6 + you can't draw for 2 turns." } },
            HeroClass.KANNUSHI, new String[][] {
                { "Purity", "清浄", "End your turn with every friendly minion at full HP, if you can heal them.", "Your healing heals 2 more.", "Restoring a minion to full HP draws a card (once per turn).", "End of turn, fully heal all your minions.", "Defilement: 8 + your minions take 2 and can't be healed for 2 turns." },
                { "Peace", "平和", "Your minions never attack the enemy hero unless the enemy has no minions.", "Minions +0/+2.", "End of turn, heal your hero 6.", "The first damage your hero takes each turn hits the enemy hero instead.", "Disturbance: 15." },
                { "Divinity", "神性", "From turn 3, end every turn with a Shrine in play.", "Shrines last 1 extra turn.", "End of turn, heal 3 and give a random friendly minion +1/+1.", "Your Shrines' effects are doubled.", "Profanity: 8 + destroy your Shrines; none next turn." } },
            HeroClass.ONI, new String[][] {
                { "Havoc", "破壊", "Deal at least 3 damage (minions or hero) every turn.", "Your damage effects deal +1.", "If you dealt 6+ damage in a turn, draw a card.", "Oath card Wreak Havoc: 8 damage to every enemy minion and the enemy hero.", "Failure: 10 + the enemy's minions are fully healed." },
                { "Descension", "降臨", "Play a minion with 5+ ATK every turn, if you can afford one.", "Your 5+ ATK minions cost 1 less Ki.", "Your 5+ ATK minions ignore Shirodu and Torii.", "Your biggest minion has double ATK and HP.", "Abyss: 8 + destroy your strongest minion." },
                { "Corruption", "堕落", "Give the enemy at least 1 Curse every turn, if you have a Curse card.", "Your Curses deal +1 damage.", "When the enemy removes a Curse, draw a card.", "Every Curse in the enemy's hand triggers at the start and end of every turn.", "Exorcism: 8 + the enemy discards all Curses; you can't curse for 2 turns." } });

    OathView(HeroClass c, Consumer<String> onSworn) {
        HBox row = new HBox(80);
        row.setAlignment(Pos.CENTER);
        for (String[] o : OATHS.get(c)) {
            StackPane s = scroll(o);
            Node stamp = s.getChildren().get(3);
            stamp.setOpacity(0);
            HomeView.clickable(s, false, () -> {
                setMouseTransparent(true);   // one Oath only
                stamp.setScaleX(3);
                stamp.setScaleY(3);
                Timeline t = new Timeline(new KeyFrame(Duration.millis(160), new KeyValue(stamp.opacityProperty(), 1),
                        new KeyValue(stamp.scaleXProperty(), 1, Interpolator.EASE_IN), new KeyValue(stamp.scaleYProperty(), 1, Interpolator.EASE_IN)),
                        new KeyFrame(Duration.millis(1100)));
                t.setOnFinished(e -> { ((Pane) getParent()).getChildren().remove(this); onSworn.accept(o[0]); });
                t.play();
            });
            row.getChildren().add(s);
        }
        VBox box = new VBox(20, label("誓いを立てよ   SWEAR YOUR OATH", 34, "#f3ede1", JP), row);
        box.setAlignment(Pos.CENTER);
        getChildren().addAll(new Rectangle(BoardView.W, BoardView.H, Color.rgb(0, 0, 0, 0.5)), box);
    }

    /** A sworn Oath's full scroll, stamped, to reread it. */
    static StackPane sworn(HeroClass c, String name) { return scroll(find(c, name)); }

    /** The small scroll next to the hero: the Oath's kanji ("?" while hidden) and its stamp. */
    static StackPane mini(HeroClass c, String name, boolean hidden) {
        ImageView bg = new ImageView(SCROLL);
        bg.setFitHeight(150);
        bg.setPreserveRatio(true);
        bg.setEffect(BoardView.SHADOW);
        ImageView stamp = image("stamp-seal", 26);
        StackPane m = new StackPane(bg, label(hidden ? "?" : find(c, name)[1], 22, INK, JP), stamp);
        StackPane.setAlignment(stamp, Pos.BOTTOM_CENTER);
        StackPane.setMargin(stamp, new Insets(0, 0, 14, 0));
        return m;
    }

    private static String[] find(HeroClass c, String name) {
        return Arrays.stream(OATHS.get(c)).filter(o -> o[0].equals(name)).findFirst().orElseThrow();
    }

    /** One Oath on a scroll in brush lettering, with the painted frame where the 承認 seal lands (children: bg, text, frame, stamp). */
    private static StackPane scroll(String[] o) {
        ImageView bg = new ImageView(SCROLL);
        bg.setFitHeight(740);
        bg.setPreserveRatio(true);
        bg.setEffect(BoardView.SHADOW);
        VBox text = new VBox(8, label("Oath of\n" + o[0], 30, INK, BRUSH), label(o[1], 54, INK, JP), label(o[2], 14, INK, BRUSH),
                label("3 — " + o[3] + "\n6 — " + o[4] + "\n9 — " + o[5], 13, INK, BRUSH), label("Break — " + o[6], 13, "#8f1d1d", BRUSH));
        text.setAlignment(Pos.TOP_CENTER);
        text.setMaxWidth(190);
        text.setPadding(new Insets(50, 0, 0, 0));
        ImageView frame = image("stamp-frame", 120), stamp = image("stamp-seal", 140);
        stamp.setRotate(-6);
        StackPane s = new StackPane(bg, text, frame, stamp);
        StackPane.setAlignment(text, Pos.TOP_CENTER);
        StackPane.setAlignment(frame, Pos.BOTTOM_CENTER);
        StackPane.setAlignment(stamp, Pos.BOTTOM_CENTER);
        StackPane.setMargin(frame, new Insets(0, 0, 45, 0));
        StackPane.setMargin(stamp, new Insets(0, 0, 34, 0));
        return s;
    }

    /** A HUD image from /images/hud/, w wide. */
    private static ImageView image(String name, double w) {
        ImageView v = new ImageView(new Image(OathView.class.getResourceAsStream("/images/hud/" + name + ".png")));
        v.setFitWidth(w);
        v.setPreserveRatio(true);
        return v;
    }

    private static Label label(String s, double size, String color, String font) {
        Label l = new Label(s);
        l.setStyle("-fx-font-family: '" + font + "'; -fx-font-size: " + size + "; -fx-text-fill: " + color + "; -fx-text-alignment: center;");
        l.setWrapText(true);
        return l;
    }
}