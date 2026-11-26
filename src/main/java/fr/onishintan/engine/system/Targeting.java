package fr.onishintan.engine.system;

import fr.onishintan.engine.GameEngine;
import fr.onishintan.engine.GameState;
import fr.onishintan.engine.effect.EffectContext;
import fr.onishintan.engine.effect.SpellEffects;
import fr.onishintan.engine.effect.SpellTarget;
import fr.onishintan.engine.effect.minion.MinionEffects;
import fr.onishintan.model.Card;
import fr.onishintan.model.Combatant;
import fr.onishintan.model.Minion;
import fr.onishintan.model.Player;
import fr.onishintan.model.Status;

import java.util.ArrayList;
import java.util.List;

/** Who may be aimed at: hero powers, plain spells, Banzai. */
public class Targeting {

    private final GameEngine engine;

    public Targeting(GameEngine engine) {
        this.engine = engine;
    }

    private GameState state() {
        return engine.getState();
    }

    /** Any living character on the board, except a hidden enemy minion or a stealthed enemy hero. */
    public boolean isPowerTarget(Combatant t) {
        if (t == null || t.isDead()) return false;
        Player enemy = state().opponent();
        if (t instanceof Minion m) {
            if (enemy.getSide().slotOf(m) >= 0) return !m.isHidden();
            return state().active().getSide().slotOf(m) >= 0;
        }
        if (t == enemy.getHero()) return !t.hasStatus(Status.STEALTH);
        return t == state().active().getHero();
    }

    /** What this plain spell is aimed at (NONE for any other card). */
    public SpellTarget spellTarget(Card card) {
        SpellEffects.Rule r = SpellEffects.rule(card);
        return r == null ? SpellTarget.NONE : r.target();
    }

    /** The characters the active player may aim this plain spell at (empty when it needs no target). */
    public List<Combatant> spellTargets(Card card) {
        return targets(spellTarget(card));
    }

    /** The characters of that kind the active player may aim at. */
    private List<Combatant> targets(SpellTarget kind) {
        List<Combatant> out = new ArrayList<>();
        Player p = state().active();
        switch (kind) {
            case NONE -> { }
            case FRIENDLY_MINION -> out.addAll(p.getSide().getMinions());
            case FRIENDLY_CHARACTER -> {
                out.add(p.getHero());
                out.addAll(p.getSide().getMinions());
            }
            case ENEMY_MINION -> {
                for (Minion m : state().opponent().getSide().getMinions()) if (isPowerTarget(m)) out.add(m);
            }
            case MINION, TWO_MINIONS -> {
                for (Player q : List.of(p, state().opponent())) {
                    for (Minion m : q.getSide().getMinions()) if (isPowerTarget(m)) out.add(m);
                }
            }
            case CHARACTER -> {
                for (Player q : List.of(p, state().opponent())) {
                    if (isPowerTarget(q.getHero())) out.add(q.getHero());
                    for (Minion m : q.getSide().getMinions()) if (isPowerTarget(m)) out.add(m);
                }
            }
        }
        out.removeIf(Combatant::isDead);
        return out;
    }

    /** True for any card but a plain spell; a plain spell needs its legal target(s) and its condition. */
    public boolean spellAimedRight(Card card, Combatant target, Combatant second) {
        SpellEffects.Rule r = SpellEffects.rule(card);
        if (r == null) return true;
        List<Combatant> legal = spellTargets(card);
        boolean aimed = switch (r.target()) {
            case NONE -> true;
            case TWO_MINIONS -> target != null && second != null && target != second
                    && legal.contains(target) && legal.contains(second);
            default -> target != null && legal.contains(target);
        };
        return aimed && r.castable().test(new EffectContext(engine, state().active(), null, target, second));
    }

    /** What this minion's Banzai is aimed at (NONE when it needs no target). */
    public SpellTarget banzaiTarget(Card card) {
        MinionEffects.Aim a = MinionEffects.aim(card);
        return a == null ? SpellTarget.NONE : a.target();
    }

    /** The legal targets of this minion's Banzai now. Empty: play it without a target. */
    public List<Combatant> banzaiTargets(Card card) {
        MinionEffects.Aim a = MinionEffects.aim(card);
        if (a == null) return List.of();
        List<Combatant> out = targets(a.target());
        out.removeIf(c -> !a.filter().test(c));
        return out;
    }

    /** A Banzai with legal targets needs one of them; with none, it is played without a target. */
    public boolean banzaiAimedRight(Card card, Combatant target) {
        List<Combatant> legal = banzaiTargets(card);
        return legal.isEmpty() || (target != null && legal.contains(target));
    }
}
