package fr.onishintan.engine.system;

import fr.onishintan.engine.GameEngine;
import fr.onishintan.engine.effect.SpellEffects;
import fr.onishintan.engine.effect.Trigger;
import fr.onishintan.engine.event.GameEvent;
import fr.onishintan.model.Minion;
import fr.onishintan.model.MinionCard;
import fr.onishintan.model.Player;
import fr.onishintan.model.Shrine;
import fr.onishintan.model.TrapTrigger;
import fr.onishintan.model.Weapon;
import fr.onishintan.model.WeaponCard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** What the effects do to the board: summon, equip, transform, bounce, fight, destroy, bring back the dead, Shrines. */
public class BoardActions {

    private final GameEngine engine;
    /** Kuchiyose: the cards of each player's dead minions, in death order. */
    private final Map<Player, List<MinionCard>> graveyards = new HashMap<>();

    public BoardActions(GameEngine engine) {
        this.engine = engine;
    }

    /** A minion summoned by a rule (Retainer, Kagura's Miko, Kuchiyose) in p's leftmost free slot. Null if his side is full. */
    public Minion summon(Player p, MinionCard card) {
        int slot = p.getSide().firstFree();
        if (slot < 0) return null;
        Minion m = new Minion(card);
        p.getSide().place(m, slot);
        engine.publish(GameEvent.of(GameEvent.Type.MINION_SUMMONED, p, m));
        engine.traps().trigger(engine.getState().opponentOf(p), TrapTrigger.ON_ENEMY_SUMMON, m);
        return m;
    }

    /** Daimyō, Jujutsu shihan, Takemikazuchi: the hero equips this weapon (replaces the current one). */
    public void equip(Player p, WeaponCard card) {
        p.getHero().equip(new Weapon(card));
        engine.publish(GameEvent.of(GameEvent.Type.WEAPON_EQUIPPED, p, p.getHero()));
    }

    /** Kitsune no bakashiya: m becomes a fresh copy of that card in the same slot (no Banzai). */
    public void transform(Minion m, MinionCard into) {
        Player owner = engine.ownerOf(m);
        if (owner == null) return;
        int slot = owner.getSide().slotOf(m);
        owner.getSide().remove(m);
        engine.publish(GameEvent.of(GameEvent.Type.MINION_REMOVED, owner, m));
        Minion copy = new Minion(into);
        owner.getSide().place(copy, slot);
        engine.publish(GameEvent.of(GameEvent.Type.MINION_SUMMONED, owner, copy));
    }

    /** Fūjin: back to its owner's hand (lost if the hand is full), without dying. */
    public void bounce(Minion m) {
        Player owner = engine.ownerOf(m);
        if (owner == null) return;
        owner.getSide().remove(m);
        engine.addToHand(owner, m.getSource());
        engine.publish(GameEvent.of(GameEvent.Type.MINION_REMOVED, owner, m));
    }

    /** Yogake: two minions hit each other with their ATK, like an attack but with no attacker's turn. */
    public void fight(Minion a, Minion b) {
        int x = a.getAttack(), y = b.getAttack();
        engine.publish(GameEvent.of(GameEvent.Type.ATTACK, a, b));
        engine.publish(new GameEvent(GameEvent.Type.DAMAGE, a, b, b.takeDamage(x)));
        engine.publish(new GameEvent(GameEvent.Type.DAMAGE, b, a, a.takeDamage(y)));
        engine.combat().removeDead();
    }

    /** Kettō: a random one of the two survives on half its HP (rounded up), the other dies. */
    public void duel(Minion a, Minion b) {
        boolean first = engine.rng().nextBoolean();
        Minion winner = first ? a : b;
        winner.halveHealth();
        destroy(first ? b : a);
    }

    /** Destroyed outright, whatever its HP or Torii (Banishment, Abyss). */
    public void destroy(Minion m) {
        m.destroy();
        engine.combat().removeDead();
    }

    /** MINION_DIED: Kuchiyose keeps the card of the dead minion. */
    public void recordDeath(Player owner, Minion m) {
        graveyards.computeIfAbsent(owner, k -> new ArrayList<>()).add(m.getSource());
    }

    public List<MinionCard> graveyard(Player p) {
        return graveyards.getOrDefault(p, List.of());
    }

    /** Kuchiyose: a random dead minion of p's comes back in his leftmost free slot, and its Banzai runs again. */
    public void resurrectRandom(Player p) {
        List<MinionCard> dead = graveyards.get(p);
        if (dead == null || dead.isEmpty()) return;
        MinionCard card = dead.remove(engine.rng().nextInt(dead.size()));
        Minion m = summon(p, card);
        if (m != null) engine.runTrigger(Trigger.BANZAI, p, m, null);
    }

    /** Kamikakushi: the minion leaves the board without dying (no Seppuku), its card goes into its owner's deck. */
    public void shuffleIntoDeck(Minion m) {
        Player owner = engine.ownerOf(m);
        if (owner == null) return;
        owner.getSide().remove(m);
        owner.getDeck().insertRandom(m.getSource(), engine.rng());
        engine.publish(GameEvent.of(GameEvent.Type.MINION_REMOVED, owner, m));
    }

    /** Sakasama: every enemy minion swaps ATK and HP; the copies of those cards in his deck get the swapped stats. */
    public void swapEnemyStats(Player enemy) {
        Set<String> ids = new HashSet<>();
        for (Minion m : enemy.getSide().getMinions()) {
            m.swapStats();
            ids.add(m.getSource().getId());
        }
        enemy.getDeck().replaceAll(c -> c instanceof MinionCard mc && ids.contains(mc.getId()) ? SpellEffects.swapped(mc) : c);
    }

    /** Start of the owner's turn: Shrines lose a turn, the expired ones leave the board. */
    public void tickShrines(Player owner) {
        for (Shrine s : owner.getSide().tickShrines()) {
            engine.publish(GameEvent.of(GameEvent.Type.SHRINE_EXPIRED, owner, s));
        }
    }

    /** Profanity: every Shrine of p leaves the board. */
    public void destroyShrines(Player p) {
        for (Shrine s : List.copyOf(p.getSide().shrines())) {
            p.getSide().remove(s);
            engine.publish(GameEvent.of(GameEvent.Type.SHRINE_EXPIRED, p, s));
        }
    }
}
