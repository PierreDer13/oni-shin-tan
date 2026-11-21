package fr.onishintan.engine.system;

import fr.onishintan.engine.GameEngine;
import fr.onishintan.engine.event.GameEvent;
import fr.onishintan.model.ChessClock;
import fr.onishintan.model.Player;

import java.util.Objects;
import java.util.function.LongSupplier;

/** The chess clocks of a PvP match: time source, pause (pass veil), start of a turn, timeout. */
public class MatchClock {

    private final GameEngine engine;
    private LongSupplier now = System::currentTimeMillis;   // replaced in tests
    /** True once beginFirstTurn() ran: the clocks may start from then on. */
    private boolean turnsBegun;
    /** True while the table is hidden (Oath choice, hotseat pass veil): no clock runs. */
    private boolean clockPaused;

    public MatchClock(GameEngine engine) {
        this.engine = engine;
    }

    /** Tests drive the clocks with a fake time. */
    public void setTimeSource(LongSupplier now) {
        this.now = Objects.requireNonNull(now);
    }

    /** The engine's time, in ms. */
    public long now() {
        return now.getAsLong();
    }

    public boolean turnsBegun() {
        return turnsBegun;
    }

    public void markTurnsBegun() {
        turnsBegun = true;
    }

    /**
     * The clocks run only once both players have sworn an Oath (PvP).
     * Adventure has no Oath, so its matches have no clock.
     */
    public boolean clocksEnabled() {
        return engine.getState().getFirst().getOath() != null && engine.getState().getSecond().getOath() != null;
    }

    /** Hides the table (pass veil): the active clock stops, using the engine's time. */
    public void pauseClock() {
        clockPaused = true;
        engine.getState().active().getClock().pause(now.getAsLong());
    }

    /** The active player clicked the veil: his clock runs again. */
    public void resumeClock() {
        clockPaused = false;
        startClock(engine.getState().active());
    }

    /** Starts p's clock, unless the clocks are off (no Oaths) or paused. */
    public void startClock(Player p) {
        if (turnsBegun && clocksEnabled() && !clockPaused && !engine.getState().isOver()) p.getClock().start(now.getAsLong());
    }

    /** Time left in a player's bank, in ms (the UI shows it). */
    public long remainingMs(Player p) {
        return p.getClock().remaining(now.getAsLong());
    }

    /**
     * The UI calls this about every 200 ms. Bank empty: the active hero takes
     * half his HP (rounded up) and gets +30 s; his turn goes on.
     * @return true if a timeout happened
     */
    public boolean checkClock() {
        if (engine.getState().isOver()) return false;
        Player p = engine.getState().active();
        if (!p.getClock().isRunning()) return false;   // no Oaths yet, Adventure, or paused
        if (p.getClock().remaining(now.getAsLong()) > 0) return false;
        int damage = (p.getHero().getHealth() + 1) / 2;
        p.getClock().add(ChessClock.TIMEOUT_BONUS_MS);
        engine.publish(new GameEvent(GameEvent.Type.CLOCK_TIMEOUT, p, p.getHero(), damage));
        engine.dealDamage(p.getClock(), p.getHero(), damage);
        return true;
    }
}
