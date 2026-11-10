package fr.onishintan.engine;

import fr.onishintan.model.Card;
import fr.onishintan.model.MinionCard;
import fr.onishintan.model.OathType;
import fr.onishintan.model.Player;

import java.util.EnumMap;
import java.util.Map;

/**
 * The rule of each Oath. Not written yet = KEPT, the same as the old UI mock
 * (+1 stack per turn); each one is filled in once its card family exists.
 */
public class OathRules {
    private static final OathRule NOT_YET = (p, t, s) -> OathRule.Verdict.KEPT;

    private final Map<OathType, OathRule> rules = new EnumMap<>(OathType.class);

    public OathRules() {
        rules.put(OathType.ALLEGIANCE, OathRules::allegiance);
        rules.put(OathType.SILENCE, OathRules::silence);
    }

    public OathRule get(OathType type) {
        return rules.getOrDefault(type, NOT_YET);
    }

    /** Allegiance: end your turn with at least 2 friendly minions. */
    private static OathRule.Verdict allegiance(Player p, TurnTracker turn, GameState state) {
        if (p.getSide().size() >= 2) return OathRule.Verdict.KEPT;
        // Broken only if a minion was still playable: one in hand, affordable, and a free slot
        boolean couldPlay = !p.getSide().isFull() && p.getHand().stream()
                .anyMatch(c -> c instanceof MinionCard && p.canAfford(c.getKiCost()));
        return couldPlay ? OathRule.Verdict.BROKEN : OathRule.Verdict.UNABLE;
    }

    /** Silence: play at most 2 cards per turn. */
    private static OathRule.Verdict silence(Player p, TurnTracker turn, GameState state) {
        return turn.getCardsPlayed() <= 2 ? OathRule.Verdict.KEPT : OathRule.Verdict.BROKEN;
    }
}