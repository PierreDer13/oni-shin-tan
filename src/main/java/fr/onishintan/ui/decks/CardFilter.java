package fr.onishintan.ui.decks;

import fr.onishintan.model.Card;
import fr.onishintan.model.HeroClass;
import fr.onishintan.model.MinionCard;
import fr.onishintan.model.SpellCard;

import java.text.Normalizer;

/**
 * What the deck editor's collection shows: a search on name and effect, a Ki cost and a card type.
 * A part left empty ("", ANY_KI, Type.ALL) lets every card through.
 */
public class CardFilter {
    /** Ki value meaning "any cost". */
    static final int ANY_KI = -1;
    /** The last Ki button: this cost or more ("7+"). */
    static final int KI_OR_MORE = 7;

    /** The card types you can pick. */
    enum Type { ALL, MINIONS, SPELLS }

    private String search = "";
    private int ki = ANY_KI;
    private Type type = Type.ALL;
    /** The class to show, or null for every class. */
    private HeroClass heroClass;

    /** Sets the search text; capitals and accents are ignored ("jushoku" finds Jūshoku). */
    void setSearch(String text) {
        search = plain(text.trim());
    }

    /** Sets the Ki cost: ANY_KI, one cost, or KI_OR_MORE for that cost and more. */
    void setKi(int ki) {
        this.ki = ki;
    }

    /** Sets the card type to show. */
    void setType(Type type) {
        this.type = type;
    }

    /** Sets the class to show; null shows every class (the Inventory's class row). */
    public void setHeroClass(HeroClass heroClass) {
        this.heroClass = heroClass;
    }

    /** True when the card passes the search, the Ki, the type and the class. */
    public boolean matches(Card card) {
        return matchesSearch(card) && matchesKi(card) && matchesType(card) && matchesClass(card);
    }

    /** The search text is in the card's romaji name, its kanji name or its effect text. */
    private boolean matchesSearch(Card card) {
        if (search.isEmpty()) {
            return true;
        }
        boolean inName = plain(card.getNameRomaji()).contains(search);
        boolean inKanji = plain(card.getNameKanji()).contains(search);
        boolean inEffect = plain(card.getEffectText()).contains(search);
        return inName || inKanji || inEffect;
    }

    /** The card costs the Ki picked (or at least KI_OR_MORE for the last button). */
    private boolean matchesKi(Card card) {
        if (ki == ANY_KI) {
            return true;
        }
        if (ki == KI_OR_MORE) {
            return card.getKiCost() >= KI_OR_MORE;
        }
        return card.getKiCost() == ki;
    }

    /** The card is of the type picked. Traps are spells. */
    private boolean matchesType(Card card) {
        if (type == Type.MINIONS) {
            return card instanceof MinionCard;
        }
        if (type == Type.SPELLS) {
            return card instanceof SpellCard;
        }
        return true;
    }

    /** The card is of the class picked (any class when none is picked). */
    private boolean matchesClass(Card card) {
        if (heroClass == null) {
            return true;
        }
        return card.getHeroClass() == heroClass;
    }

    /** Lower case without accents: "Jūshoku" becomes "jushoku". */
    private static String plain(String text) {
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
        String withoutAccents = decomposed.replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase();
    }
}