package fr.onishintan.engine.system;

import fr.onishintan.engine.GameEngine;
import fr.onishintan.engine.GameState;
import fr.onishintan.engine.effect.Trigger;
import fr.onishintan.engine.event.GameEvent;
import fr.onishintan.model.Combatant;
import fr.onishintan.model.HeroClass;
import fr.onishintan.model.Minion;
import fr.onishintan.model.MinionCard;
import fr.onishintan.model.Player;
import fr.onishintan.model.TrapTrigger;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static fr.onishintan.engine.GameEngine.OROCHI_MYTHIC_EVERY;
import static fr.onishintan.engine.GameEngine.OROCHI_MYTHIC_FIRST;
import static fr.onishintan.engine.GameEngine.OROCHI_MYTHIC_ID;
import static fr.onishintan.engine.GameEngine.YATSU_KUBI_HITS;
import static fr.onishintan.engine.GameEngine.YATSU_KUBI_RAGE_STEP;

/** Yamata no Orochi, the final boss: his power Yatsu-kubi and his mythic on turns 10, 15, 20... */
public class OrochiBoss {

    private final GameEngine engine;

    public OrochiBoss(GameEngine engine) {
        this.engine = engine;
    }

    private GameState state() {
        return engine.getState();
    }

    /** How much each Yatsu-kubi hit does now: 1, +1 for every 50 HP Orochi has lost (the UI shows it on the power). */
    public static int yatsuKubiDamage(Player p) {
        int lost = p.getHero().getMaxHealth() - p.getHero().getHealth();
        return 1 + lost / YATSU_KUBI_RAGE_STEP;
    }

    /** True on Orochi's turns 10, 15, 20, 25... (his own turns: one per round). */
    public static boolean isMythicTurn(int round) {
        return round >= OROCHI_MYTHIC_FIRST && (round - OROCHI_MYTHIC_FIRST) % OROCHI_MYTHIC_EVERY == 0;
    }

    /** Yatsu-kubi: 8 hits on random enemy characters (1 damage each, +1 per 50 HP he has lost), dealt as one hit per character (like Raijin). */
    public void yatsuKubi(Player p) {
        Player enemy = state().opponentOf(p);
        List<Combatant> targets = new ArrayList<>(enemy.getSide().getMinions());
        targets.add(enemy.getHero());
        Map<Combatant, Integer> hits = new IdentityHashMap<>();
        int damage = yatsuKubiDamage(p);   // angrier as he is wounded
        for (int i = 0; i < YATSU_KUBI_HITS; i++) hits.merge(targets.get(engine.rng().nextInt(targets.size())), damage, Integer::sum);
        for (Combatant c : targets) {
            if (hits.containsKey(c) && !state().isOver()) engine.dealDamage(p.getHero(), c, hits.get(c));
        }
    }

    /**
     * Start of an OROCHI turn 10, 15, 20...: his mythic arrives without his hand. It costs 10 Ki (all he has if less),
     * comes even if he has one already, replaces his weakest minion when his side is full,
     * then CARD_PLAYED, MINION_SUMMONED and its Banzai, like a card played.
     */
    public void orochiMythic(Player p) {
        if (state().isOver() || p.getHero().getHeroClass() != HeroClass.OROCHI || !isMythicTurn(state().getRound())) return;
        if (!(engine.catalogCard(OROCHI_MYTHIC_ID) instanceof MinionCard card)) return;
        int slot = p.getSide().firstFree();
        if (slot < 0) {
            Minion weakest = p.getSide().getMinions().stream()
                    .min(java.util.Comparator.comparingInt((Minion m) -> m.getAttack() + m.getHealth())).orElse(null);
            if (weakest == null) return;
            slot = p.getSide().slotOf(weakest);
            p.getSide().remove(weakest);
            engine.publish(GameEvent.of(GameEvent.Type.MINION_REMOVED, p, weakest));
        }
        int paid = Math.min(card.getKiCost(), p.getKi());
        p.spendKi(paid);
        engine.publish(new GameEvent(GameEvent.Type.CARD_PLAYED, p, card, paid));
        Minion m = new Minion(card);
        p.getSide().place(m, slot);
        engine.publish(GameEvent.of(GameEvent.Type.MINION_SUMMONED, p, m));
        engine.runTrigger(Trigger.BANZAI, p, m, null);
        engine.traps().trigger(state().opponentOf(p), TrapTrigger.ON_ENEMY_SUMMON, m);
    }
}
