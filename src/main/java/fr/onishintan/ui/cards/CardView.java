package fr.onishintan.ui.cards;

import fr.onishintan.model.Card;
import fr.onishintan.model.Keyword;
import fr.onishintan.model.MinionCard;
import fr.onishintan.model.Rarity;
import fr.onishintan.model.SpellCard;
import fr.onishintan.model.WeaponCard;
import fr.onishintan.ui.app.Images;
import fr.onishintan.ui.fx.CurseFumes;

import java.io.InputStream;
import java.util.Map;
import javafx.geometry.HPos;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Scale;
import javafx.scene.text.Text;

/** One card in the karuta layout: drawn at the art's size (530 x 742), then scaled to the width asked. */
public class CardView extends Group {
    public static final double W = 530, H = 742;
    private static final String SERIF = "Yu Mincho";
    private static final Color INK = Color.web("#1f1b17");
    private static final Color DAMAGED = Color.web("#c41e1e");
    /** The edge tags (SPELL on the left, the family on the right), in card coordinates (530 x 742):
     *  width, top, height per letter and the extra height around the letters (SPELL: 30 + 5 x 24 = 150). */
    private static final double SIDE_TAG_W = 40, SIDE_TAG_Y = 345, SIDE_TAG_LETTER_H = 24, SIDE_TAG_PADDING = 30;
    /** Engine tokens drawn with another card's art: token id -> card id (the Kannushi hero power summons "power-miko"). */
    private static final Map<String, String> ART_OF = Map.of("power-miko", "miko");

    private final Card card;
    private final Pane face = new Pane();

    public CardView(Card card, double width) {
        this.card = card;
        face.getTransforms().add(new Scale(width / W, width / W));
        Rectangle corners = new Rectangle(W, H);
        corners.setArcWidth(30);
        corners.setArcHeight(30);
        face.setClip(corners);
        getChildren().add(face);
        render();
    }

        /** Rebuilds the card face from the card's data. */
    public void render() {
        face.getChildren().setAll(artView());
        text(String.valueOf(card.getKiCost()), 50, 25, 24, 100, 82);                     // Ki circle
        text("気 Ki", 14, 25, 88, 100, 20);
        String kanji = card.getNameKanji();
        vertical(kanji, Math.min(56, 200 / kanji.length()), 370, 30, 135, 210);           // Japanese name, top right
        String name = card.getNameRomaji();
        Text probe = new Text(name);
        probe.setFont(Font.font(SERIF, FontWeight.BOLD, 24));
        text(name, Math.min(24, 24 * 84 / probe.getLayoutBounds().getWidth()), 222, 240, 86, 34); // English name, in the gap
        String desc = card.getEffectText();
        int kw = 0;
        if (card instanceof MinionCard m) {
            for (Keyword k : m.getKeywords()) {
                vertical(k.getDisplayName().toUpperCase(), 18, 150 + 24 * kw++, 32, 24, 210);
                desc = desc.replaceAll(k.getDisplayName() + "([.:]\\s*|$)", "");
            }
        }
        int[] s = stats();
        if (s != null) { stat(s[0], "攻撃 ATK", 27, INK); stat(s[1], card instanceof WeaponCard ? "耐久 DUR" : "体力 HP", 402, damaged() ? DAMAGED : INK); }
        text(desc, 22, 154 + 24 * kw, 32, 208 - 24 * kw, 210).setFont(Font.font(SERIF, 22)); // description, top middle
        if (card.getRarity() == Rarity.LEGENDARY)
            rarityTag("伝説 LEGENDARY", 190, "#8a6a1c, #f7e08a 30%, #fff6c8 50%, #d4ab4a 70%, #8a6a1c", "#3a2a05", Color.GOLD);
        if (card.getRarity() == Rarity.MYTHIC)
            rarityTag("神話 MYTHIC", 150, "#9fb8d9, #e9f7ff 30%, #ffffff 50%, #c8e6ff 65%, #b6a8ff 85%, #9fb8d9", "#2b3e63", Color.web("#bfe9ff"));
        if (card instanceof SpellCard) {
            spellTag();
        }
    }

    /** Pink outline hugging the border of cards the player can afford (used by the hand). */
    public void setPlayable(boolean playable) {
        if (!playable) {
            setEffect(null);
            return;
        }
        // small radius keeps the pink close to the edge, high spread turns it into a solid band
        setEffect(new DropShadow(BlurType.GAUSSIAN, Color.HOTPINK, 12, 0.85, 0, 0));
    }


    /** Darkens a Curse the enemy put in your hand and stamps its removal cost on it (used by the hand). */
    public void showAsHeldCurse(int removalCost) {
        face.getChildren().add(new CurseSeal(removalCost));

        // Dark fumes rising from it: drawn at the card's scale, but outside its rounded clip so the smoke escapes above it.
        CurseFumes fumes = new CurseFumes();
        fumes.getTransforms().setAll(face.getTransforms());
        getChildren().add(fumes);
    }

    public Card getCard() { return card; }

    /** ATK and HP (durability for weapons) shown at the bottom, or null for spells. MinionView shows live values. */
    /** Whether the HP should show in red (a damaged minion). Printed cards never are; MinionView decides. */
    protected boolean damaged() { return false; }

    /** True for minions on the board: bigger, heavier numbers and no captions. */
    protected boolean onBoard() { return false; }

    protected int[] stats() {
        if (card instanceof MinionCard m) return new int[] { m.getBaseAttack(), m.getBaseHealth() };
        if (card instanceof WeaponCard w) return new int[] { w.getAttack(), w.getDurability() };
        return null;
    }

    /**
     * The card's art (decoded once, at the card's size, then reused), or the blank frame of its class while the art is missing.
     * A few engine tokens are drawn with another card's art (ART_OF): the hero power's Miko uses the Miko card's.
     */
    private Image art() {
        String folder = card.getHeroClass().name().charAt(0) + card.getHeroClass().name().substring(1).toLowerCase();
        String artId = ART_OF.getOrDefault(card.getId(), card.getId());
        Image art = Images.get("/images/cards/" + folder + "/" + artId + ".png", W, H);
        if (art == null) {
            art = Images.get("/images/frames/" + card.getHeroClass().name().toLowerCase() + "-base-card.png", W, H);
        }
        return art;
    }


    /**
     * The card's art fitted to the card's size (W x H), whatever the PNG's own size is.
     * Some art is drawn at twice the size (1061 x 1483); without this only its top-left quarter would show.
     */
    private ImageView artView() {
        ImageView view = new ImageView(art());
        view.setFitWidth(W);
        view.setFitHeight(H);
        view.setSmooth(true);
        return view;
    }

    /** Text written top to bottom, one character per line; extra columns go right to left. */
    private void vertical(String s, double size, double x, double y, double w, double h) {
        FlowPane col = new FlowPane(Orientation.VERTICAL);
        col.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        col.setAlignment(Pos.TOP_CENTER);
        col.setColumnHalignment(HPos.CENTER);
        col.setVgap(-size * 0.3);
        col.setPrefSize(w, h);
        col.relocate(x, y);
        for (String c : s.split("")) {
            Label l = new Label(c);
            l.setFont(Font.font(SERIF, FontWeight.BOLD, size));
            l.setTextFill(INK);
            col.getChildren().add(l);
        }
        face.getChildren().add(col);
    }

    private void stat(int value, String caption, double x, Color color) {
        if (!onBoard()) {
            text(String.valueOf(value), 50, x, 600, 100, 82).setTextFill(color);
            text(caption, 14, x, 665, 100, 20);
            return;
        }
        Label n = text(String.valueOf(value), 72, x, 604, 100, 82);
        n.setTextFill(color);
        n.setEffect(new DropShadow(BlurType.ONE_PASS_BOX, color, 3, 1.0, 0, 0));
    }

    /** A centred bold label in the box (x, y, w, h), in card coordinates. */
    private Label text(String s, double size, double x, double y, double w, double h) {
        Label l = new Label(s);
        l.setFont(Font.font(SERIF, FontWeight.BOLD, size));
        l.setTextFill(INK);
        l.setAlignment(Pos.CENTER);
        l.setTextAlignment(TextAlignment.CENTER);
        l.setWrapText(true);
        l.setPrefSize(w, h);
        l.relocate(x, y);
        face.getChildren().add(l);
        return l;
    }

    /** Legendary / mythic tag across the top of the card: a shiny gradient pill with a matching glow (GDD). */
    private void rarityTag(String tag, double width, String gradient, String ink, Color glow) {
        Label t = text(tag, 18, (W - width) / 2, 0, width, 34);
        t.setTextFill(Color.web(ink));
        t.setStyle("-fx-background-color: linear-gradient(to right, " + gradient + "); -fx-background-radius: 6;");
        t.setEffect(new DropShadow(18, glow));
    }

    /** Every spell: a purple SPELL tag on the left edge; a family spell also gets its family tag (TRAP, SHRINE, RITUAL or CURSE) on the right. */
    private void spellTag() {
        sideTag("SPELL", "#5b2380, #8e44ad 50%, #5b2380", Color.web("#8e44ad"), false);
        CardFamily family = CardFamily.of(card);
        if (family != null) {
            sideTag(family.getWord(), family.getGradient(), family.getGlow(), true);
        }
    }

    /**
     * A tag on one of the card's edges, one letter under the other.
     * @param word     the word to write
     * @param gradient the background colours, top to bottom (CSS list)
     * @param onRight  true for the right edge, false for the left
     */
    private void sideTag(String word, String gradient, Color glow, boolean onRight) {
        VBox tag = new VBox(-4);
        tag.setAlignment(Pos.CENTER);
        double height = SIDE_TAG_PADDING + SIDE_TAG_LETTER_H * word.length();
        tag.setPrefSize(SIDE_TAG_W, height);

        String corners = "0 8 8 0";   // rounded on the inner side only
        if (onRight) {
            tag.relocate(W - SIDE_TAG_W, SIDE_TAG_Y);
            corners = "8 0 0 8";
        } else {
            tag.relocate(0, SIDE_TAG_Y);
        }
        tag.setStyle("-fx-background-color: linear-gradient(to bottom, " + gradient + "); -fx-background-radius: " + corners + ";");
        tag.setEffect(new DropShadow(10, glow));

        for (String letter : word.split("")) {
            Label label = new Label(letter);
            label.setFont(Font.font(SERIF, FontWeight.BOLD, 20));
            label.setTextFill(Color.web("#f3ede1"));
            tag.getChildren().add(label);
        }
        face.getChildren().add(tag);
    }
}