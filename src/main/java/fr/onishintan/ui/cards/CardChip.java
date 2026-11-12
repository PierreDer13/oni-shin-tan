package fr.onishintan.ui.cards;

import fr.onishintan.model.Card;
import fr.onishintan.model.HeroClass;
import fr.onishintan.model.MinionCard;
import fr.onishintan.model.Rarity;
import fr.onishintan.model.WeaponCard;

import java.util.Map;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * One card of a deck list, like the Card Curve's chips: the class colour stripe on the left; the kanji name
 * (gold for a legendary or mythic card) with the stats on the right (ATK/HP, ATK/DUR for a weapon, nothing for a spell);
 * under it the romaji name and the number of copies (x1, x2, x3).
 */
public class CardChip extends VBox {
    /** Stripe colour per class: the Card Curve's, readable on the dark chip. */
    private static final Map<HeroClass, String> CLASS_COLOUR = Map.of(
            HeroClass.NINJA, "#7f95c8",
            HeroClass.SAMURAI, "#e06a5c",
            HeroClass.KANNUSHI, "#b7a6dd",
            HeroClass.ONI, "#c9c2b6",
            HeroClass.NEUTRAL, "#6fb27d");
    private static final String CREAM = "#ece6da";
    private static final String SOFT = "#aaa293";
    private static final String GOLD = "#d4ab4a";
    private static final String FONT = "-fx-font-family: 'Yu Mincho'; -fx-font-weight: bold;";

    private final Card card;

    /** A chip `width` wide for this card, of which the deck holds `copies`. */
    public CardChip(Card card, int copies, double width) {
        super(2);
        this.card = card;
        setPrefWidth(width);
        setMaxWidth(width);
        setPadding(new Insets(5, 6, 5, 8));
        setStyle("-fx-background-color: #232220; -fx-background-radius: 5; -fx-border-radius: 5;"
                + " -fx-border-color: #3a3833 #3a3833 #3a3833 " + CLASS_COLOUR.get(card.getHeroClass()) + ";"
                + " -fx-border-width: 1 1 1 4;");

        // Top line: the kanji name, the stats on the right.
        Label kanji = label(card.getNameKanji(), kanjiSize(), kanjiColour());
        kanji.setTextOverrun(OverrunStyle.CLIP);
        HBox.setHgrow(kanji, Priority.ALWAYS);
        kanji.setMaxWidth(Double.MAX_VALUE);
        Label stats = label(stats(), 10, CREAM);
        stats.setMinWidth(Region.USE_PREF_SIZE);
        HBox top = new HBox(4, kanji, stats);
        top.setAlignment(Pos.CENTER_LEFT);

        // Bottom line: the romaji name (cut with "…" if too long), the copies on the right.
        Label romaji = label(card.getNameRomaji(), 10, SOFT);
        romaji.setTextOverrun(OverrunStyle.ELLIPSIS);
        HBox.setHgrow(romaji, Priority.ALWAYS);
        romaji.setMaxWidth(Double.MAX_VALUE);
        Label count = label("x" + copies, 11, CREAM);
        count.setMinWidth(Region.USE_PREF_SIZE);
        HBox bottom = new HBox(4, romaji, count);
        bottom.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(top, bottom);
    }

    /** The card this chip shows. */
    public Card getCard() {
        return card;
    }

    /** ATK/HP for a minion, ATK/durability for a weapon, nothing for a spell. */
    private String stats() {
        if (card instanceof MinionCard minion) {
            return minion.getBaseAttack() + "/" + minion.getBaseHealth();
        }
        if (card instanceof WeaponCard weapon) {
            return weapon.getAttack() + "/" + weapon.getDurability();
        }
        return "";
    }

    /** Short kanji names big, long ones (4 characters or more) a little smaller so they fit. */
    private double kanjiSize() {
        if (card.getNameKanji().length() >= 4) {
            return 12;
        }
        return 15;
    }

    /** Gold for a legendary or mythic card, cream otherwise. */
    private String kanjiColour() {
        if (card.getRarity() == Rarity.LEGENDARY || card.getRarity() == Rarity.MYTHIC) {
            return GOLD;
        }
        return CREAM;
    }

    /** A bold Yu Mincho label of this size and colour. */
    private static Label label(String words, double size, String colour) {
        Label label = new Label(words);
        label.setStyle(FONT + " -fx-font-size: " + size + "; -fx-text-fill: " + colour + ";");
        return label;
    }
}