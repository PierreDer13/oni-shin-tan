package fr.onishintan.engine.oath;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import fr.onishintan.model.Minion;
import fr.onishintan.model.Oath;
import fr.onishintan.model.Player;
import fr.onishintan.model.Status;
import fr.onishintan.engine.GameEngine;

/**
 * The extra penalty of each broken Oath (GDD Oath table, "Break" column), applied after the damage,
 * and the penalties that last some of the Oathbreaker's own turns.
 */
public class OathPenalties {

    /** A penalty that lasts some of its player's turns. */
    public enum Lasting {
        HERO_ASLEEP(3),   // Dishonour: the hero cannot attack
        PERSECUTION(3),   // Persecution: the enemy minions keep +2 ATK
        NO_DRAW(2),       // Disappointment: no card drawn
        NO_HEAL(2),       // Defilement: your minions cannot be healed
        NO_SHRINE(1),     // Profanity: no Shrine can be played
        NO_CURSE(2);      // Exorcism: no Curse can be given

        final int turns;

        Lasting(int turns) { this.turns = turns; }
    }

    static final int PERSECUTION_ATK = 2;
    static final int DEFILEMENT_DAMAGE = 2;

    private final GameEngine engine;
    private final Map<Player, EnumMap<Lasting, Integer>> left = new HashMap<>();
    private final Map<Player, List<Minion>> persecuted = new HashMap<>();

    public OathPenalties(GameEngine engine) {
        this.engine = engine;
    }

    public boolean has(Player p, Lasting l) {
        return turnsLeft(p, l) > 0;
    }

    int turnsLeft(Player p, Lasting l) {
        EnumMap<Lasting, Integer> m = left.get(p);
        return m == null ? 0 : m.getOrDefault(l, 0);
    }

    private void start(Player p, Lasting l) {
        left.computeIfAbsent(p, k -> new EnumMap<>(Lasting.class)).put(l, l.turns);
    }

    /** p just broke this Oath: its own penalty, after the break damage. */
    public void apply(Player p, Oath oath) {
        Player enemy = engine.getState().opponentOf(p);
        switch (oath.getType()) {
            case HONOR -> {                                          // Dishonour
                p.getHero().addStatus(Status.SLEEP);
                start(p, Lasting.HERO_ASLEEP);
            }
            case ALLEGIANCE, DESCENSION -> strongest(p).ifPresent(engine::destroy);   // Banishment, Abyss
            case JUSTICE -> {                                        // Persecution
                List<Minion> buffed = new ArrayList<>(enemy.getSide().getMinions());
                buffed.forEach(m -> m.buff(PERSECUTION_ATK, 0));
                persecuted.put(p, buffed);
                start(p, Lasting.PERSECUTION);
            }
            case SILENCE -> engine.revealAndDiscard(p);              // Revelation
            case DECEPTION -> engine.stealTraps(p, enemy);           // Exposed
            case DISCIPLINE -> start(p, Lasting.NO_DRAW);            // Disappointment
            case PURITY -> {                                         // Defilement
                for (Minion m : List.copyOf(p.getSide().getMinions())) engine.dealDamage(oath, m, DEFILEMENT_DAMAGE);
                start(p, Lasting.NO_HEAL);
            }
            case PEACE -> { }                                        // Disturbance: the damage only
            case DIVINITY -> {                                       // Profanity
                engine.destroyShrines(p);
                start(p, Lasting.NO_SHRINE);
            }
            case HAVOC -> enemy.getSide().getMinions().forEach(m -> engine.heal(m, m.getMaxHealth()));   // Failure
            case CORRUPTION -> {                                     // Exorcism
                engine.purgeCurses(enemy);
                start(p, Lasting.NO_CURSE);
            }
        }
    }

    /** End of p's turn, before his Oath is checked: each of his lasting penalties loses a turn. */
    public void tick(Player p) {
        EnumMap<Lasting, Integer> m = left.get(p);
        if (m == null) return;
        for (Lasting l : List.copyOf(m.keySet())) {
            int n = m.get(l) - 1;
            if (n > 0) {
                m.put(l, n);
            } else {
                m.remove(l);
                end(p, l);
            }
        }
    }

    private void end(Player p, Lasting l) {
        switch (l) {
            case HERO_ASLEEP -> p.getHero().removeStatus(Status.SLEEP);
            case PERSECUTION -> {
                Player enemy = engine.getState().opponentOf(p);
                for (Minion m : persecuted.remove(p)) {
                    if (enemy.getSide().slotOf(m) >= 0) m.buff(-PERSECUTION_ATK, 0);   // still alive
                }
            }
            default -> { }
        }
    }

    /** Highest ATK, then highest HP, then the leftmost. */
    static Optional<Minion> strongest(Player p) {
        return p.getSide().getMinions().stream()
                .max(Comparator.comparingInt(Minion::getAttack)
                        .thenComparingInt(Minion::getHealth)
                        .thenComparingInt(m -> -p.getSide().slotOf(m)));
    }
}