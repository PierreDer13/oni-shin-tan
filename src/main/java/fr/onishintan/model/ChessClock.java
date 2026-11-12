package fr.onishintan.model;

/**
 * Chess clock of one player: a 7 min bank that only runs during his turn,
 * +2 s at the end of each turn, +30 s after a timeout.
 * Times are passed in (nowMs) so tests can drive the clock.
 */
public class ChessClock {
    public static final long BANK_MS = 7 * 60_000L;
    public static final long INCREMENT_MS = 2_000L;
    public static final long TIMEOUT_BONUS_MS = 30_000L;

    private long bankMs = BANK_MS;
    private long startedAt = -1;            // -1 = paused

    public void start(long nowMs) {
        if (startedAt < 0) startedAt = nowMs;
    }

    public void pause(long nowMs) {
        if (startedAt < 0) return;
        bankMs = Math.max(0, bankMs - (nowMs - startedAt));
        startedAt = -1;
    }

    public long remaining(long nowMs) {
        if (startedAt < 0) return bankMs;
        return Math.max(0, bankMs - (nowMs - startedAt));
    }

    public void add(long ms) { bankMs += ms; }

    public boolean isRunning() { return startedAt >= 0; }
}