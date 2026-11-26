package fr.onishintan.ui.lan;

import fr.onishintan.model.Card;
import fr.onishintan.model.Combatant;
import fr.onishintan.model.Minion;
import fr.onishintan.model.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * How a LAN game names what a move points at, the same way on both computers:
 * "h0" / "h1" is the first / second player's hero, "m1.3" the second player's minion in slot 3, "-" nothing.
 * Cards in a hand are named by their place in it.
 */
public final class LanCodec {
    private final Player first;
    private final Player second;

    public LanCodec(Player first, Player second) {
        this.first = first;
        this.second = second;
    }

    /** The name of a hero or minion, or "-" for none. */
    public String ref(Combatant c) {
        if (c == null) {
            return "-";
        }
        if (c == first.getHero()) {
            return "h0";
        }
        if (c == second.getHero()) {
            return "h1";
        }
        int slot = first.getSide().slotOf((Minion) c);
        if (slot >= 0) {
            return "m0." + slot;
        }
        return "m1." + second.getSide().slotOf((Minion) c);
    }

    /** The hero or minion a name points at, or null for "-". */
    public Combatant combatant(String ref) {
        if (ref.equals("-")) {
            return null;
        }
        Player owner = first;
        if (ref.charAt(1) == '1') {
            owner = second;
        }
        if (ref.charAt(0) == 'h') {
            return owner.getHero();
        }
        return owner.getSide().getAt(Integer.parseInt(ref.substring(3)));
    }

    /** The places in `hand` of the picked cards, like "0,3"; "-" when none (two copies of a card get two places). */
    public static String places(List<Card> hand, List<Card> picked) {
        List<String> places = new ArrayList<>();
        boolean[] used = new boolean[hand.size()];
        for (Card card : picked) {
            for (int i = 0; i < hand.size(); i++) {
                if (!used[i] && hand.get(i) == card) {
                    used[i] = true;
                    places.add(String.valueOf(i));
                    break;
                }
            }
        }
        if (places.isEmpty()) {
            return "-";
        }
        return String.join(",", places);
    }

    /** The cards of `hand` at these places (the reverse of places()). */
    public static List<Card> cards(List<Card> hand, String places) {
        List<Card> cards = new ArrayList<>();
        if (places.equals("-")) {
            return cards;
        }
        for (String place : places.split(",")) {
            cards.add(hand.get(Integer.parseInt(place)));
        }
        return cards;
    }
}
