package fr.onishintan.engine.oath;

import static fr.onishintan.engine.oath.OathRule.Verdict.BROKEN;
import static fr.onishintan.engine.oath.OathRule.Verdict.KEPT;
import static fr.onishintan.engine.oath.OathRule.Verdict.UNABLE;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.ToIntBiFunction;
import java.util.stream.Stream;

import fr.onishintan.model.Card;
import fr.onishintan.model.Combatant;
import fr.onishintan.model.CurseCard;
import fr.onishintan.model.Minion;
import fr.onishintan.model.MinionCard;
import fr.onishintan.model.OathType;
import fr.onishintan.model.Player;
import fr.onishintan.model.RiteCard;
import fr.onishintan.model.ShrineCard;
import fr.onishintan.model.TrapCard;

import fr.onishintan.engine.turn.CombatResolver;
import fr.onishintan.engine.GameState;
import fr.onishintan.engine.turn.TurnTracker;
/**
 * The rule of each Oath, checked at the end of its owner's turn (GDD, Oaths table).
 * KEPT = +1 stack, UNABLE = no legal way to keep it (no stack, the Oath holds), BROKEN = broken on purpose.
 */
public class OathRules {
    private final CombatResolver combat;
    private final Map<OathType, OathRule> rules = new EnumMap<>(OathType.class);
    private ToIntBiFunction<Player, Card> cost = (p, c) -> c.getKiCost();

    public OathRules(CombatResolver combat) {
        this.combat = Objects.requireNonNull(combat);
        rules.put(OathType.HONOR, this::honor);
        rules.put(OathType.ALLEGIANCE, this::allegiance);
        rules.put(OathType.JUSTICE, this::justice);
        rules.put(OathType.SILENCE, this::silence);
        rules.put(OathType.DECEPTION, this::deception);
        rules.put(OathType.DISCIPLINE, this::discipline);
        rules.put(OathType.PURITY, this::purity);
        rules.put(OathType.PEACE, this::peace);
        rules.put(OathType.DIVINITY, this::divinity);
        rules.put(OathType.HAVOC, this::havoc);
        rules.put(OathType.DESCENSION, this::descension);
        rules.put(OathType.CORRUPTION, this::corruption);
    }

    /** What a card costs its player now (Oath rewards). Set by GameEngine. */
    public void setCost(ToIntBiFunction<Player, Card> cost) {
        this.cost = Objects.requireNonNull(cost);
    }

    public OathRule get(OathType type) {
        return rules.get(type);
    }

    // ---------- Samurai ----------

    /** Honor: each turn, if one of your minions can attack, at least one attacks the enemy hero. */
    private OathRule.Verdict honor(Player p, TurnTracker turn, GameState state) {
        if (turn.getHeroAttacks() > 0) return KEPT;
        Combatant enemyHero = state.opponentOf(p).getHero();
        boolean couldHit = p.getSide().getMinions().stream()
                .anyMatch(m -> m.canAttack() && combat.legalTargets(m).contains(enemyHero));
        return turn.getHeroSkipped() > 0 || couldHit ? BROKEN : UNABLE;
    }

    /** Allegiance: end your turn with at least 2 friendly minions. */
    private OathRule.Verdict allegiance(Player p, TurnTracker turn, GameState state) {
        if (p.getSide().size() >= 2) return KEPT;
        return couldPlay(p, c -> c instanceof MinionCard) ? BROKEN : UNABLE;
    }

    /** Justice: an enemy minion that attacked your hero must be attacked this turn, if possible. */
    private OathRule.Verdict justice(Player p, TurnTracker turn, GameState state) {
        Player enemy = state.opponentOf(p);
        boolean unable = false;
        for (Minion m : turn.getHeroAttackers(p)) {
            if (!enemy.getSide().getMinions().contains(m) || turn.hasAttacked(m)) continue;   // dead, or hit back
            if (characters(p).anyMatch(c -> c.canAttack() && combat.legalTargets(c).contains(m))) return BROKEN;
            unable = true;
        }
        return unable ? UNABLE : KEPT;
    }

    // ---------- Ninja ----------

    /** Silence: play at most 2 cards per turn. */
    private OathRule.Verdict silence(Player p, TurnTracker turn, GameState state) {
        return turn.getCardsPlayed() <= 2 ? KEPT : BROKEN;
    }

    /** Deception: end each turn with at least 1 Trap set. */
    private OathRule.Verdict deception(Player p, TurnTracker turn, GameState state) {
        if (p.trapCount() > 0) return KEPT;
        return couldPlay(p, c -> c instanceof TrapCard) ? BROKEN : UNABLE;
    }

    /** Discipline: end each turn with 0 Ki, unless no legal card to play (the turn still counts). */
    private OathRule.Verdict discipline(Player p, TurnTracker turn, GameState state) {
        if (p.getKi() == 0 || !couldPlay(p, c -> true)) return KEPT;
        return BROKEN;
    }

    // ---------- Kannushi ----------

    /** Purity: every damaged minion you can heal is back to full HP. Which cards heal comes with the card data. */
    private OathRule.Verdict purity(Player p, TurnTracker turn, GameState state) {
        boolean allFull = p.getSide().getMinions().stream().allMatch(m -> m.getHealth() >= m.getMaxHealth());
        return allFull ? KEPT : UNABLE;
    }

    /** Peace: your minions never attack the enemy hero unless the enemy has no minions. */
    private OathRule.Verdict peace(Player p, TurnTracker turn, GameState state) {
        return turn.getHeroAttacksPastMinions() > 0 ? BROKEN : KEPT;
    }

    /** Divinity: from turn 3, end every turn with a Shrine in play. */
    private OathRule.Verdict divinity(Player p, TurnTracker turn, GameState state) {
        if (state.getRound() < 3 || !p.getSide().shrines().isEmpty()) return KEPT;
        return couldPlay(p, c -> c instanceof ShrineCard) ? BROKEN : UNABLE;
    }

    // ---------- Oni ----------

    /** Havoc: deal at least 3 damage every turn. */
    private OathRule.Verdict havoc(Player p, TurnTracker turn, GameState state) {
        if (turn.getDamageDealt() >= 3) return KEPT;
        boolean couldHit = characters(p).anyMatch(c -> c.canAttack() && !combat.legalTargets(c).isEmpty());
        return couldHit ? BROKEN : UNABLE;
    }

    /** Descension: play a 5+ ATK minion every turn, if you can afford one. */
    private OathRule.Verdict descension(Player p, TurnTracker turn, GameState state) {
        if (turn.getBigMinionsPlayed() > 0) return KEPT;
        boolean couldPlayBig = couldPlay(p, c -> c instanceof MinionCard mc && mc.getBaseAttack() >= TurnTracker.BIG_ATTACK);
        return couldPlayBig ? BROKEN : UNABLE;
    }

    /** Corruption: give the enemy at least 1 Curse every turn, if you have a Curse card. */
    private OathRule.Verdict corruption(Player p, TurnTracker turn, GameState state) {
        if (turn.getCursesGiven() > 0) return KEPT;
        return couldPlay(p, c -> c instanceof CurseCard) ? BROKEN : UNABLE;
    }

    // ---------- helpers ----------

    /** The player's minions and hero. */
    private static Stream<Combatant> characters(Player p) {
        return Stream.concat(p.getSide().getMinions().stream(), Stream.of(p.getHero()));
    }

    /** True if a card of that kind in hand could still be played now (Ki, slot, trap duplicate, not a held Curse). */
    private boolean couldPlay(Player p, Predicate<Card> kind) {
        return p.getHand().stream().filter(kind).anyMatch(c -> playableNow(p, c));
    }

    private boolean playableNow(Player p, Card c) {
        if (!p.canAfford(cost.applyAsInt(p, c)) || p.isHeldCurse(c)) return false;
        if (c instanceof TrapCard t) return p.canSetTrap(t);
        if (c instanceof MinionCard || c instanceof ShrineCard || c instanceof RiteCard) return !p.getSide().isFull();
        return true;
    }
}