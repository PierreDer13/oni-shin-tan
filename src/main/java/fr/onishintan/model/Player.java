package fr.onishintan.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * One side of a match: hero, deck, hand, board and Ki. The chess clock comes
 * with the engine.
 */
public class Player {
    public static final int KI_CAP = 12;
    public static final int MAX_HAND = 12;

    /** Ki cost of a trade: free, once per turn, whatever the card's own Ki cost (decided 10 Nov). */
    public static final int TRADE_COST = 0;

    private final String name;
    private final Hero hero;
    private final Deck deck;
    private final List<Card> hand = new ArrayList<>();
    private final BoardSide side = new BoardSide();
    private int ki;
    private int maxKi;
    private Oath oath;          // null in Adventure, or before the Oath is sworn
    private final List<TrapCard> traps = new ArrayList<>();   // face down, no limit (GDD)
    private final Set<Card> heldCurses = Collections.newSetFromMap(new IdentityHashMap<>());   // Curses the enemy put in this hand    
    private final List<RiteCard> rites = new ArrayList<>();   // Rites performed this match, in order
    private final ChessClock clock = new ChessClock();


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
        if (top.isEmpty() || hand.size() >= MAX_HAND)
            return Optional.empty();
        hand.add(top.get());
        return top;
    }

    /**
     * Puts a card from the hand back into the deck at a random place, then draws
     * one.
     * The once-per-turn rule lives in TurnManager.swapCard.
     * 
     * @return false if the card is not in the hand or the player lacks the Ki
     */
    public boolean trade(Card card, Random rng) {
        if (card == null || !hand.contains(card) || ki < TRADE_COST) {
            return false;
        }
        hand.remove(card);
        ki -= TRADE_COST;
        deck.insertRandom(card, rng);
        draw();
        return true;
    }

    /** Trade with a fresh Random: what Kayne's mock calls. The engine passes its own rng. */
    public boolean trade(Card card) {
        return trade(card, new Random());
    }

    /** Start of turn: max Ki +1 (cap 12), then Ki refilled. */
    public void refillKi() {
        maxKi = Math.min(KI_CAP, maxKi + 1);
        ki = maxKi;
    }

    public boolean canAfford(int cost) {
        return cost <= ki;
    }

    public void spendKi(int cost) {
        if (!canAfford(cost))
            throw new IllegalStateException("Not enough Ki: " + ki + " < " + cost);
        ki -= cost;
    }

    /** Extra Ki for this turn only (Izakaya even roll, second-player coin). */
    public void gainTemporaryKi(int n) {
        ki += n;
    }

    public boolean removeFromHand(Card c) {
        return hand.remove(c);
    }

    public void addToHand(Card c) {
        if (hand.size() < MAX_HAND)
            hand.add(c);
    }

    /** An enemy Curse lands in this hand. False if the hand is full (the Curse is lost). */
    public boolean receiveCurse(CurseCard curse) {
        if (hand.size() >= MAX_HAND) return false;
        hand.add(curse);
        heldCurses.add(curse);
        return true;
    }

    /** True for a Curse the enemy gave: it cannot be played, only removed. */
    public boolean isHeldCurse(Card c) { return heldCurses.contains(c); }

    public boolean removeCurse(CurseCard curse) {
        heldCurses.remove(curse);
        return hand.remove(curse);
    }

    /** After a Rite: its lasting bonus applies to every Ancestor summoned later. */
    public void addRite(RiteCard rite) { rites.add(rite); }

    public List<RiteCard> getRites() { return Collections.unmodifiableList(rites); }


    /** False if the same trap is already set: one copy per trap (GDD). */
    public boolean canSetTrap(TrapCard trap) {
        return traps.stream().noneMatch(t -> t.getId().equals(trap.getId()));
    }

    public boolean setTrap(TrapCard trap) {
        if (!canSetTrap(trap)) return false;
        traps.add(trap);
        return true;
    }

    /** Removes and returns the first set trap with this trigger, if any. */
    public Optional<TrapCard> takeTrap(TrapTrigger when) {
        for (TrapCard t : traps) {
            if (t.getTrigger() == when) {
                traps.remove(t);
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }

    /** What the enemy may see: how many, not which. */
    public int trapCount() { return traps.size(); }

    /** The owner's traps. The enemy (UI or AI) must only use trapCount(). */
    public List<TrapCard> getTraps() { return Collections.unmodifiableList(traps); }

    public String getName() {
        return name;
    }

    public Hero getHero() {
        return hero;
    }

    public Deck getDeck() {
        return deck;
    }

    public List<Card> getHand() {
        return Collections.unmodifiableList(hand);
    }

    public BoardSide getSide() {
        return side;
    }

    public int getKi() {
        return ki;
    }

    public int getMaxKi() {
        return maxKi;
    }

    public ChessClock getClock() {
        return clock;
    }


    /** PvP only. The engine checks the class and the timing (GameEngine.swearOath). */
    public void setOath(Oath oath) {
        this.oath = oath;
    }

    public Oath getOath() {
        return oath;
    }
}