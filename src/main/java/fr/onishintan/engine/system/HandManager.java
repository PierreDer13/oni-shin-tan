package fr.onishintan.engine.system;

import fr.onishintan.adventure.Upgrade;
import fr.onishintan.engine.GameEngine;
import fr.onishintan.engine.event.GameEvent;
import fr.onishintan.engine.oath.OathPenalties;
import fr.onishintan.engine.oath.OathRewards;
import fr.onishintan.model.Card;
import fr.onishintan.model.CurseCard;
import fr.onishintan.model.OathType;
import fr.onishintan.model.Player;
import fr.onishintan.model.SpellCard;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The hands: draws and fatigue, cards added or thrown away, Yōi discounts, the bonus numbers shown on the cards. */
public class HandManager {

    private static final java.util.regex.Pattern DAMAGE_NUMBER = java.util.regex.Pattern.compile("(\\d+) damage");
    private static final java.util.regex.Pattern HEAL_NUMBER =
            java.util.regex.Pattern.compile("(?i)\\b(heal|restore)[^0-9;.]*?(\\d+)");

    private final GameEngine engine;
    private final Map<Player, Integer> fatigue = new HashMap<>();   // empty-deck draws so far, per player
    /** Yōi: Ki taken off the cards each player had in hand, by card. */
    private final Map<Player, Map<Card, Integer>> handDiscounts = new HashMap<>();

    public HandManager(GameEngine engine) {
        this.engine = engine;
    }

    public void draw(Player p) {
        if (engine.getPenalties().has(p, OathPenalties.Lasting.NO_DRAW)) return;     // Disappointment
        publishDraw(p, p.draw());
    }

    /** An effect puts c in p's hand: CARD_ADDED so the hand redraws, or CARD_BURNED if the hand is full (the card is lost). */
    public boolean addToHand(Player p, Card c) {
        if (c == null) return false;
        if (p.getHand().size() >= Player.MAX_HAND) {
            engine.publish(GameEvent.of(GameEvent.Type.CARD_BURNED, p, c));
            return false;
        }
        p.addToHand(c);
        engine.publish(GameEvent.of(GameEvent.Type.CARD_ADDED, p, c));
        return true;
    }

    public void publishDraw(Player p, Optional<Card> drawn) {
        if (drawn.isPresent()) {
            engine.publish(GameEvent.of(GameEvent.Type.CARD_DRAWN, p, drawn.get()));
        } else if (p.drewFromEmptyDeck()) {
            fatigue(p);
        } else {
            engine.publish(GameEvent.of(GameEvent.Type.CARD_BURNED, p, null));
        }
    }

    /** Fatigue (decided 18 Nov): each draw from an empty deck deals 1, then 2, then 3... to the drawer's own hero, with no bonus. */
    private void fatigue(Player p) {
        if (engine.getState().isOver()) return;
        int damage = fatigue.merge(p, 1, Integer::sum);
        engine.publish(new GameEvent(GameEvent.Type.FATIGUE, p, p.getHero(), damage));
        engine.dealDamage(p.getDeck(), p.getHero(), damage);
    }

    /** The damage p's next draw from an empty deck will deal (the UI can show it on the empty deck). */
    public int nextFatigue(Player p) {
        return fatigue.getOrDefault(p, 0) + 1;
    }

    /** Yōi: every card now in p's hand (not a held Curse) costs 1 less, until it leaves the hand. */
    public void discountHand(Player p) {
        Map<Card, Integer> off = handDiscounts.computeIfAbsent(p, k -> new IdentityHashMap<>());
        for (Card c : p.getHand()) if (!p.isHeldCurse(c)) off.merge(c, 1, Integer::sum);
    }

    /** A Yōi discount leaves with its card (played or set). */
    public void useDiscount(Player p, Card c) {
        Map<Card, Integer> off = handDiscounts.get(p);
        if (off != null) off.computeIfPresent(c, (k, v) -> v > 1 ? v - 1 : null);
    }

    /** The Ki Yōi takes off this card of p's now. */
    public int discountOf(Player p, Card card) {
        Map<Card, Integer> off = handDiscounts.get(p);
        return off == null ? 0 : off.getOrDefault(card, 0);
    }

    /** Revelation: the hand is shown to the opponent, then a random card of it is thrown away. */
    public void revealAndDiscard(Player p) {
        engine.publish(new GameEvent(GameEvent.Type.HAND_REVEALED, p, List.copyOf(p.getHand()), p.getHand().size()));
        discardRandom(p);
    }

    /** A random card of p's hand is thrown away (Revelation, Tamashii-gui). */
    public void discardRandom(Player p) {
        if (p.getHand().isEmpty()) return;
        Card c = p.getHand().get(engine.rng().nextInt(p.getHand().size()));
        if (c instanceof CurseCard curse && p.isHeldCurse(curse)) p.removeCurse(curse);
        else p.removeFromHand(c);
        engine.publish(GameEvent.of(GameEvent.Type.CARD_DISCARDED, p, c));
    }

    /**
     * What to add to the damage or healing number written on card, as it would be dealt by p now (0 if none):
     * Ryu +4, Havoc 3 +1 and Corruption 3 +1 (Curses) on damage, Purity 3 +2 on healing.
     * A Curse held in p's hand deals the giver's damage, so its bonus is the opponent's. The UI shows it in green.
     * Chikara (Adventure) adds 3 to spell damage.
     * Justice 6 (double damage on some targets) depends on the target and is not counted.
     */
    public int bonusOf(Player p, Card card) {
        if (p == null || card == null) return 0;
        String text = card.getEffectText() == null ? "" : card.getEffectText();
        boolean heldCurse = card instanceof CurseCard cc && p.isHeldCurse(cc);
        Player dealer = heldCurse ? engine.getState().opponentOf(p) : p;
        if (DAMAGE_NUMBER.matcher(text).find()) {
            int bonus = engine.ryu(dealer);
            if (OathRewards.has(dealer, OathType.HAVOC, 3)) bonus++;
            if (card instanceof CurseCard && OathRewards.has(dealer, OathType.CORRUPTION, 3)) bonus++;
            if (card instanceof SpellCard && engine.has(dealer, Upgrade.CHIKARA)) bonus += Upgrade.CHIKARA_BONUS;
            return bonus;
        }
        if (HEAL_NUMBER.matcher(text).find()) {
            int bonus = engine.ryu(dealer);
            if (OathRewards.has(dealer, OathType.PURITY, 3)) bonus += OathRewards.PURITY_HEAL_BONUS;
            return bonus;
        }
        return 0;
    }
}
