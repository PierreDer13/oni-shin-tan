package fr.onishintan.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/** A player's draw pile. The top of the deck is the end of the list. */
public class Deck {

    public static final int SIZE = 40;

    /** The cards left, read-only (Deception 9 looks for a Trap in it). */
    public List<Card> getCards() { return Collections.unmodifiableList(cards); }

    /** Replaces cards in place (Sakasama: the enemy's deck copies take the new stats). */
    public void replaceAll(java.util.function.UnaryOperator<Card> f) { cards.replaceAll(f); }

    /** Takes this exact card out of the deck. */
    public boolean remove(Card card) {
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i) == card) {
                cards.remove(i);
                return true;
            }
        }
        return false;
    }

    private final List<Card> cards;

    public Deck(List<Card> cards) {
        this.cards = new ArrayList<>(cards);
    }

    public void shuffle(Random rng) { Collections.shuffle(cards, rng); }

    /** Takes the top card, or empty when the deck is out. */
    public Optional<Card> draw() {
        return cards.isEmpty() ? Optional.empty() : Optional.of(cards.remove(cards.size() - 1));
    }

    /** Puts a card at a random place (Korori, Tatari, putting a card back). */
    public void insertRandom(Card card, Random rng) {
        cards.add(rng.nextInt(cards.size() + 1), card);
    }

    public int size() { return cards.size(); }
    public boolean isEmpty() { return cards.isEmpty(); }

    /**
     * Deck-builder rules: exactly 50 cards, all of the hero's class or NEUTRAL,
     * at most 3 copies of a card (1 for legendary and mythic).
     * Returns the list of problems; an empty list means the deck is legal.
     */
    public List<String> validate(HeroClass heroClass) {
        List<String> problems = new ArrayList<>();
        if (cards.size() != SIZE) {
            problems.add("Deck has " + cards.size() + " cards, needs " + SIZE);
        }
        Map<String, Integer> copies = new HashMap<>();
        for (Card c : cards) {
            if (c.getHeroClass() != heroClass && c.getHeroClass() != HeroClass.NEUTRAL) {
                problems.add(c.getNameRomaji() + " is a " + c.getHeroClass() + " card");
            }
            int n = copies.merge(c.getId(), 1, Integer::sum);
            if (n == c.getRarity().getMaxCopies() + 1) {
                problems.add("Too many copies of " + c.getNameRomaji()
                        + " (max " + c.getRarity().getMaxCopies() + ")");
            }
        }
        return problems;
    }

    public boolean isLegal(HeroClass heroClass) { return validate(heroClass).isEmpty(); }
}