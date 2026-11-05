package fr.onishintan.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** One side of a match: hero, deck, hand, board and Ki. The chess clock comes with the engine. */
public class Player {
    public static final int KI_CAP = 12;
    public static final int MAX_HAND = 12;

    private final String name;
    private final Hero hero;
    private final Deck deck;
    private final List<Card> hand = new ArrayList<>();
    private final BoardSide side = new BoardSide();
    private int ki;
    private int maxKi;

    public Player(String name, Hero hero, Deck deck) {
        this.name = Objects.requireNonNull(name);
        this.hero = Objects.requireNonNull(hero);
        this.deck = Objects.requireNonNull(deck);
    }

    /**
     * Draws the top card. Returns it if it reached the hand; empty if the deck
     * was out or the hand was full (the card is then burned, as in the GDD).
     */
    public Optional<Card> draw() {
        Optional<Card> top = deck.draw();
        if (top.isEmpty() || hand.size() >= MAX_HAND) return Optional.empty();
        hand.add(top.get());
        return top;
    }

    /** Start of turn: max Ki +1 (cap 12), then Ki refilled. */
    public void refillKi() {
        maxKi = Math.min(KI_CAP, maxKi + 1);
        ki = maxKi;
    }

    public boolean canAfford(int cost) { return cost <= ki; }

    public void spendKi(int cost) {
        if (!canAfford(cost)) throw new IllegalStateException("Not enough Ki: " + ki + " < " + cost);
        ki -= cost;
    }

    /** Extra Ki for this turn only (Izakaya even roll, second-player coin). */
    public void gainTemporaryKi(int n) { ki += n; }

    public boolean removeFromHand(Card c) { return hand.remove(c); }
    public void addToHand(Card c) { if (hand.size() < MAX_HAND) hand.add(c); }

    public String getName() { return name; }
    public Hero getHero() { return hero; }
    public Deck getDeck() { return deck; }
    public List<Card> getHand() { return Collections.unmodifiableList(hand); }
    public BoardSide getSide() { return side; }
    public int getKi() { return ki; }
    public int getMaxKi() { return maxKi; }
}