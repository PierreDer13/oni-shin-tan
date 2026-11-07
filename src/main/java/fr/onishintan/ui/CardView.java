package fr.onishintan.ui;

import fr.onishintan.model.Card;
import fr.onishintan.model.Keyword;
import fr.onishintan.model.MinionCard;
import fr.onishintan.model.WeaponCard;
import java.io.InputStream;
import javafx.geometry.HPos;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
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

    private final Card card;
    private final Pane face = new Pane();

    public CardView(Card card, double width) {
        this.card = card;
        face.getTransforms().add(new Scale(width / W, width / W));
        getChildren().add(face);
        render();
    }

        /** Rebuilds the card face from the card's data. */
    public void render() {
        face.getChildren().setAll(new ImageView(art()));
        text(String.valueOf(card.getKiCost()), 56, 25, 18, 100, 100);                    // Ki circle
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
            stat(m.getBaseAttack(), 27);
            stat(m.getBaseHealth(), 402);
        }
        if (card instanceof WeaponCard w) { stat(w.getAttack(), 27); stat(w.getDurability(), 402); }
        text(desc, 17, 154 + 24 * kw, 32, 208 - 24 * kw, 210).setFont(Font.font(SERIF, 17)); // description, top middle
    }

    /** Pink glow on cards the player can afford (used by the hand). */
    public void setPlayable(boolean playable) { setEffect(playable ? new DropShadow(40, Color.HOTPINK) : null); }

    public Card getCard() { return card; }

    /** The card's art, or the blank frame of its class while the art is missing. */
    private Image art() {
        InputStream in = getClass().getResourceAsStream("/images/cards/" + card.getId() + ".png");
        if (in == null) in = getClass().getResourceAsStream("/images/frames/" + card.getHeroClass().name().toLowerCase() + "-base-card.png");
        return new Image(in);
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

    private void stat(int value, double x) { text(String.valueOf(value), 56, x, 595, 100, 100); }

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
}