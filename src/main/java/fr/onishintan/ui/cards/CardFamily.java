package fr.onishintan.ui.cards;

import fr.onishintan.model.Card;
import fr.onishintan.model.TrapCard;
import fr.onishintan.model.CurseCard;
import fr.onishintan.model.RiteCard;
import fr.onishintan.model.ShrineCard;

import javafx.scene.paint.Color;

/**
 * The class spell families, each with the tag written on the card's left edge.
 * Found by the card's class: TrapCard, ShrineCard, RiteCard, CurseCard (Pierre's engine).
 */
public enum CardFamily {
    TRAP("TRAP", "#0b1430, #1c2b4f 50%, #0b1430", "#1c2b4f"),          // Ninja: dark blue
    SHRINE("SHRINE", "#5e4b8b, #9a86c4 50%, #5e4b8b", "#b7a6dd"),      // Kannushi: lavender
    RITUAL("RITUAL", "#5c0a14, #b0172b 50%, #5c0a14", "#dc143c"),      // Samurai: crimson
    CURSE("CURSE", "#000000, #2b2622 50%, #000000", "#000000");        // Oni: black

    private final String word;
    private final String gradient;
    private final String glow;

    CardFamily(String word, String gradient, String glow) {
        this.word = word;
        this.gradient = gradient;
        this.glow = glow;
    }

    /**
     * The family of a card.
     * @return TRAP, SHRINE, RITUAL or CURSE, or null for a minion or a plain spell
     */
    public static CardFamily of(Card card) {
        if (card instanceof TrapCard) {
            return TRAP;
        }
        if (card instanceof ShrineCard) {
            return SHRINE;
        }
        if (card instanceof RiteCard) {
            return RITUAL;
        }
        if (card instanceof CurseCard) {
            return CURSE;
        }
        return null;
    }

    /** The word on the tag ("TRAP", "SHRINE", …). */
    public String getWord() {
        return word;
    }

    /** The tag's background colours, top to bottom, as a CSS gradient list. */
    public String getGradient() {
        return gradient;
    }

    /** The colour of the soft glow around the tag. */
    public Color getGlow() {
        return Color.web(glow);
    }
}
