package fr.onishintan.engine;

import fr.onishintan.model.Card;
import fr.onishintan.model.Minion;
import fr.onishintan.model.Player;
import fr.onishintan.model.Status;

import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/**
 * Runs the match order from the GDD:
 * start of match (shuffle, opening hands), then for each turn
 * start (max Ki +1, Ki refilled, reset characters, draw 1), main phase, end.
 * Mulligan, chess clock and start/end-of-turn effects plug in later.
 */
public class TurnManager {
    public static final int OPENING_HAND_FIRST = 6;
    public static final int OPENING_HAND_SECOND = 7;
    /** Extra Ki on the second player's first turn only (GDD, proposed). */
    public static final int SECOND_PLAYER_BONUS_KI = 1;

    private final GameState state;
    private final Random rng;
    private boolean started;
    private boolean swapUsedThisTurn;

    public TurnManager(GameState state, Random rng) {
        this.state = Objects.requireNonNull(state, "state");
        this.rng = Objects.requireNonNull(rng, "rng");
    }

    /**
     * Shuffles both decks and deals the opening hands (6 cards first player, 7 second).
     * Does not start the first turn: the mulligan comes in between, then startTurn().
     */
    public void startMatch() {
        if (started) throw new IllegalStateException("Match already started");
        started = true;
        Player first = state.getFirst();
        Player second = state.getSecond();
        first.getDeck().shuffle(rng);
        second.getDeck().shuffle(rng);
        for (int i = 0; i < OPENING_HAND_FIRST; i++) first.draw();
        for (int i = 0; i < OPENING_HAND_SECOND; i++) second.draw();
        state.begin();
    }

    /**
     * Start of the active player's turn. Returns the card drawn,
     * or empty if the deck was out or the hand full (card burned).
     */
    public Optional<Card> startTurn() {
        checkPlaying();
        Player p = state.active();

        // 1. Ki: max +1 (cap 12) and refill; the second player gets +1 on their first turn
        p.refillKi();
        if (state.getRound() == 1 && !state.isFirst(p)) p.gainTemporaryKi(SECOND_PLAYER_BONUS_KI);

        // 2. Characters can act again; fresh minions lose their summoning sickness
        p.getHero().onTurnStart();
        p.getHero().removeStatus(Status.STEALTH);   // Mutsubushi lasts until its owner's next turn
        for (Minion m : p.getSide().getMinions()) m.onTurnStart();
        swapUsedThisTurn = false;

        // 3. Start-of-turn effects: added with features/engine-events

        // 4. Draw 1 card. Empty deck: fatigue still to be defined in the GDD, nothing happens for now
        return p.draw();
    }

    /** Ends the active player's turn and starts the opponent's. Returns the card they drew. */
    public Optional<Card> endTurn() {
        checkPlaying();
        // End-of-turn effects (engine-events) and the clock's +2 s (engine-clock) go here
        state.nextTurn();
        return startTurn();
    }

    /**
     * Once per turn, the active player may put a card from hand back into the deck
     * (random place) and draw one. Returns false if not allowed.
     */
    public boolean swapCard(Card card) {
        checkPlaying();
        Player p = state.active();
        if (swapUsedThisTurn || !p.getHand().contains(card)) return false;
        p.removeFromHand(card);
        p.getDeck().insertRandom(card, rng);
        p.draw();
        swapUsedThisTurn = true;
        return true;
    }

    public boolean isSwapUsedThisTurn() { return swapUsedThisTurn; }
    public GameState getState() { return state; }

    private void checkPlaying() {
        if (!started) throw new IllegalStateException("Match not started");
        if (state.checkGameOver()) throw new IllegalStateException("Match is over");
    }
}