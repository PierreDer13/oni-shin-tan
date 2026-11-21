package fr.onishintan.engine.system;

import fr.onishintan.engine.GameEngine;
import fr.onishintan.engine.effect.EffectContext;
import fr.onishintan.engine.effect.Trigger;
import fr.onishintan.engine.event.GameEvent;
import fr.onishintan.model.Combatant;
import fr.onishintan.model.Keyword;
import fr.onishintan.model.Minion;
import fr.onishintan.model.Player;
import fr.onishintan.model.RiteCard;
import fr.onishintan.model.TrapTrigger;

/** The Samurai's Rites: they summon Ancestors that carry the bonuses of every Rite performed before. */
public class Rites {

    private final GameEngine engine;

    public Rites(GameEngine engine) {
        this.engine = engine;
    }

    /**
     * Summons the Rite's Ancestors (first on the chosen slot if free, then leftmost free;
     * no room = the rest are lost), each with the Rite's own keywords and the lasting
     * bonuses of the Rites before it, then records this Rite for the next Ancestors.
     */
    public void performRite(Player p, RiteCard rite, int slot, Combatant target) {
        for (int i = 0; i < rite.getAncestors(); i++) {
            int s = (i == 0 && p.getSide().isFree(slot)) ? slot : p.getSide().firstFree();
            if (s < 0) break;
            Minion a = new Minion(RiteCard.ancestorCard());
            for (Keyword k : rite.getNowKeywords()) a.giveKeyword(k);
            for (RiteCard past : p.getRites()) {
                a.buff(past.getBonusAttack(), past.getBonusHealth());
                for (Keyword k : past.getBonusKeywords()) a.giveKeyword(k);
            }
            p.getSide().place(a, s);
            engine.publish(GameEvent.of(GameEvent.Type.MINION_SUMMONED, p, a));
            for (RiteCard past : p.getRites()) {
                engine.getCardEffects().get(past.getId(), Trigger.BANZAI)
                        .ifPresent(effect -> engine.applyAs(effect, new EffectContext(engine, p, a, target)));
            }
            engine.traps().trigger(engine.getState().opponent(), TrapTrigger.ON_ENEMY_SUMMON, a);
            if (engine.getState().isOver()) return;
        }
        p.addRite(rite);
        engine.publish(GameEvent.of(GameEvent.Type.RITE_PERFORMED, p, rite));
    }
}
