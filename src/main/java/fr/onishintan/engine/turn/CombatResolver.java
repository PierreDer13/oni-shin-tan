package fr.onishintan.engine.turn;

import fr.onishintan.engine.event.GameEvent.Type;
import fr.onishintan.model.Combatant;
import fr.onishintan.model.Hero;
import fr.onishintan.model.Minion;
import fr.onishintan.model.Player;
import fr.onishintan.model.Status;
import java.util.function.Predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import fr.onishintan.engine.GameState;
import fr.onishintan.engine.event.GameEvent;
/**
 * Attacks between characters (GDD, Combat):
 * - a visible Shirodu on the enemy side must be attacked first;
 * - a hidden Shinobi minion, or a hero under Stealth (Mutsubushi), cannot be targeted;
 * - both sides hit at once, but only a minion strikes back (a hero being hit does not);
 * - Torii is handled inside Minion.takeDamage.
 */
public class CombatResolver {

    private final GameState state;
    private final Consumer<GameEvent> publish;


    /** What a hit deals once the Oath rewards are counted (Justice 6). */
    public interface DamageRule {
        int apply(Combatant source, Combatant target, int amount);
    }

    private DamageRule damageRule = (source, target, amount) -> amount;
    private Predicate<Combatant> piercing = c -> false;

    /** Set by GameEngine. */
    public void setDamageRule(DamageRule rule) {
        damageRule = Objects.requireNonNull(rule);
    }

    /** Attackers that ignore Shirodu and Torii (Descension 6). Set by GameEngine. */
    public void setPiercing(Predicate<Combatant> rule) {
        piercing = Objects.requireNonNull(rule);
    }

    public CombatResolver(GameState state, Consumer<GameEvent> publish) {
        this.state = Objects.requireNonNull(state);
        this.publish = Objects.requireNonNull(publish);
    }

    /** Enemy characters the attacker may hit. Empty if the attacker is not the active player's. */
    public List<Combatant> legalTargets(Combatant attacker) {
        List<Combatant> targets = new ArrayList<>();
        if (attacker == null || !belongsToActive(attacker)) return targets;

        Player enemy = state.opponent();
        if (!piercing.test(attacker)) {
            for (Minion m : enemy.getSide().shirodus()) {
                if (!m.isHidden()) targets.add(m);      // a hidden Shirodu does not guard
            }
            if (!targets.isEmpty()) return targets;
        }
        if (!targets.isEmpty()) return targets;

        for (Minion m : enemy.getSide().getMinions()) {
            if (!m.isHidden()) targets.add(m);
        }
        Hero enemyHero = enemy.getHero();
        if (!enemyHero.hasStatus(Status.STEALTH)) targets.add(enemyHero);
        return targets;
    }

    public boolean canAttack(Combatant attacker, Combatant target) {
        return attacker != null && target != null
                && attacker.canAttack()
                && legalTargets(attacker).contains(target);
    }

    /** Resolves the attack. Returns false (and changes nothing) if it is not allowed. */
    public boolean resolve(Combatant attacker, Combatant target) {
        if (!canAttack(attacker, target)) return false;

        boolean hadWeapon = attacker instanceof Hero h && h.getWeapon() != null;
        int dealt = damageRule.apply(attacker, target, attacker.getAttack());
        int back = (target instanceof Minion) ? damageRule.apply(target, attacker, target.getAttack()) : 0;

        publish.accept(GameEvent.of(Type.ATTACK, attacker, target));

        if (target instanceof Minion m && piercing.test(attacker)) m.takeDamageIgnoringTorii(dealt);
        else target.takeDamage(dealt);
        publish.accept(new GameEvent(Type.DAMAGE, attacker, target, dealt));
        if (back > 0) {
            attacker.takeDamage(back);
            publish.accept(new GameEvent(Type.DAMAGE, target, attacker, back));
        }

        attacker.markAttacked();   // weapon durability -1 (drops it at 0), Shinobi revealed
        if (hadWeapon && ((Hero) attacker).getWeapon() == null) {
            publish.accept(GameEvent.of(Type.WEAPON_BROKEN, attacker, null));
        }

        removeDead();
        return true;
    }

    /** Removes dead minions on both sides and announces each death. */
    public void removeDead() {
        for (Player p : List.of(state.active(), state.opponent())) {
            for (Minion dead : p.getSide().removeDead()) {
                publish.accept(GameEvent.of(Type.MINION_DIED, p, dead));
            }
        }
    }

    private boolean belongsToActive(Combatant c) {
        Player me = state.active();
        return c == me.getHero() || me.getSide().getMinions().contains(c);
    }
}