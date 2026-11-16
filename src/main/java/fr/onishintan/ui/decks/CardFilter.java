package fr.onishintan.ui.decks;

import fr.onishintan.model.Card;
import fr.onishintan.model.HeroClass;
import fr.onishintan.model.MinionCard;
import fr.onishintan.model.SpellCard;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.Set;

/**
 * What the deck editor and the Inventory show: a search on name and effect, the Ki costs picked on the soroban, a card type and a class.
 * A part left empty ("", no Ki picked, Type.ALL, no class) lets every card through.
 */
public class CardFilter {
    /** The last Ki button: this cost or more ("7+"). */
    static final int KI_OR_MORE = 7;

    /** The card types you can pick. */
    enum Type { ALL, MINIONS, SPELLS }

    private String search = "";
    /** The Ki costs picked on the soroban; none picked = any cost. */
    private final Set<Integer> kiValues = new HashSet<>();
    private Type type = Type.ALL;
    /** The class to show, or null for every class. */
    private HeroClass heroClass;

    /** Sets the search text; capitals and accents are ignored ("jushoku" finds Jūshoku). */
    void setSearch(String text) {
        search = plain(text.trim());
    }

    /** Picks this Ki cost, or unpicks it if it was picked (KI_OR_MORE = 7 and more). Returns true if it is picked now. */
    boolean toggleKi(int ki) {
        if (kiValues.remove(ki)) {
            return false;
        }
        kiValues.add(ki);
        return true;
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

    /** The card costs one of the Ki picked on the soroban (7+ covers 7 and more); with none picked, any cost passes. */
    private boolean matchesKi(Card card) {
        if (kiValues.isEmpty()) {
            return true;
        }
        int cost = card.getKiCost();
        if (cost >= KI_OR_MORE) {
            return kiValues.contains(KI_OR_MORE);
        }
        return kiValues.contains(cost);
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