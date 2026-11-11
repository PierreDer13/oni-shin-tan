package fr.onishintan.engine;

import fr.onishintan.model.Card;
import fr.onishintan.model.Minion;
import fr.onishintan.model.Player;
import fr.onishintan.model.Status;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/**
 * Runs the match order from the GDD:
 * start of match (shuffle, opening hands), then for each turn
 * start (max Ki +1, Ki refilled, reset characters, draw 2), main phase, end.
 * Mulligan, chess clock and start/end-of-turn effects plug in later.
 */
public class TurnManager {
    public static final int OPENING_HAND_FIRST = 4;
    public static final int OPENING_HAND_SECOND = 5;
    /** Extra Ki on the second player's first turn only (GDD, proposed). */
    public static final int SECOND_PLAYER_BONUS_KI = 1;
    /** Cards drawn at the start of every turn (GDD, 10 Nov). */
    public static final int DRAW_PER_TURN = 2;

    private final GameState state;
    private final Random rng;
    private boolean started;
    private boolean swapUsedThisTurn;

    public TurnManager(GameState state, Random rng) {
        this.state = Objects.requireNonNull(state, "state");
        this.rng = Objects.requireNonNull(rng, "rng");
    }

    /**
     * Shuffles both decks and deals the opening hands (4 cards first player, 5
     * second).
     * Does not start the first turn: the mulligan comes in between, then
     * startTurn().
     */
    public void startMatch() {
        if (started)
            throw new IllegalStateException("Match already started");
        started = true;
        Player first = state.getFirst();
        Player second = state.getSecond();
        first.getDeck().shuffle(rng);
        second.getDeck().shuffle(rng);
        for (int i = 0; i < OPENING_HAND_FIRST; i++)
            first.draw();
        for (int i = 0; i < OPENING_HAND_SECOND; i++)
            second.draw();
        state.begin();
    }

    /**
     * Start of the active player's turn. Returns one entry per draw (DRAW_PER_TURN):
     * the card drawn, or empty if the deck was out or the hand full (card burned).
     */
    public List<Optional<Card>> startTurn() {
        checkPlaying();
        Player p = state.active();

        // 1. Ki: max +1 (cap 12) and refill; the second player gets +1 on their first
        // turn
        p.refillKi();
        if (state.getRound() == 1 && !state.isFirst(p))
            p.gainTemporaryKi(SECOND_PLAYER_BONUS_KI);

        // 2. Characters can act again; fresh minions lose their summoning sickness
        p.getHero().onTurnStart();
        p.getHero().removeStatus(Status.STEALTH); // Mutsubushi lasts until its owner's next turn
        for (Minion m : p.getSide().getMinions())
            m.onTurnStart();
        swapUsedThisTurn = false;

        // 3. Start-of-turn effects: added with features/engine-events

        // 4. Draw DRAW_PER_TURN cards. Empty deck: fatigue still to be defined in the GDD
        List<Optional<Card>> drawn = new ArrayList<>(DRAW_PER_TURN);
        for (int i = 0; i < DRAW_PER_TURN; i++)
            drawn.add(p.draw());
        return drawn;
    }

    /**
     * Ends the active player's turn and starts the opponent's. Returns the cards
     * they drew (one entry per draw, see startTurn).
     */
    public List<Optional<Card>> endTurn() {
        checkPlaying();
        // End-of-turn effects (engine-events) and the clock's +2 s (engine-clock) go
        // here
        state.nextTurn();
        return startTurn();
    }

    /**
     * Once per turn, the active player may put a card from hand back into the deck
     * (random place) and draw one, whatever the card's Ki cost. Returns false if
     * not allowed.
     */
    public boolean swapCard(Card card) {
        checkPlaying();
        if (swapUsedThisTurn) {
            return false;
        }
        boolean ok = state.active().trade(card, rng);
        if (ok) {
            swapUsedThisTurn = true;
        }
        return ok;
    }

    public boolean isSwapUsedThisTurn() {
        return swapUsedThisTurn;
    }

    public GameState getState() {
        return state;
    }

    private void checkPlaying() {
        if (!started)
            throw new IllegalStateException("Match not started");
        if (state.checkGameOver())
            throw new IllegalStateException("Match is over");
    }
}