package fr.onishintan.engine;

import fr.onishintan.model.Player;

import java.util.Objects;
import java.util.Optional;

/**
 * Everything that changes during a match: the two players, whose turn it is,
 * the round number and the winner. Rules live in TurnManager and GameEngine.
 */
public class GameState {
    private final Player[] players;   // players[0] plays first
    private int current;              // index of the active player
    private int round;                // 0 before the first turn; one round = both players' turns
    private boolean over;
    private Player winner;            // null while playing, or on a draw

    public GameState(Player first, Player second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        if (first == second) throw new IllegalArgumentException("A player cannot face themself");
        this.players = new Player[] { first, second };
    }

    /** Round 1, first player to act. Called once by TurnManager.startMatch(). */
    void begin() {
        current = 0;
        round = 1;
    }

    /** Hands the turn to the other player; a new round starts when the first player is up again. */
    void nextTurn() {
        current = 1 - current;
        if (current == 0) round++;
    }

    /**
     * Checks both heroes. Once one is at 0 HP the match is over:
     * the other player wins, or nobody if both died at once (draw).
     */
    public boolean checkGameOver() {
        if (over) return true;
        boolean firstDead = players[0].getHero().isDead();
        boolean secondDead = players[1].getHero().isDead();
        if (!firstDead && !secondDead) return false;
        over = true;
        if (firstDead != secondDead) winner = firstDead ? players[1] : players[0];
        return true;
    }

    /** Ends the match at once (concede, chess-clock penalty). */
    public void declareWinner(Player p) {
        if (p != players[0] && p != players[1]) throw new IllegalArgumentException("Not in this match: " + p.getName());
        over = true;
        winner = p;
    }

    public Player active() { return players[current]; }
    public Player opponent() { return players[1 - current]; }

    public Player opponentOf(Player p) {
        if (p == players[0]) return players[1];
        if (p == players[1]) return players[0];
        throw new IllegalArgumentException("Not in this match: " + p.getName());
    }

    public Player getFirst() { return players[0]; }
    public Player getSecond() { return players[1]; }
    public boolean isFirst(Player p) { return p == players[0]; }

    public int getRound() { return round; }
    public boolean isOver() { return over; }
    public Optional<Player> getWinner() { return Optional.ofNullable(winner); }
}