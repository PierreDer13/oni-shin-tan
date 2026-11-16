package fr.onishintan.engine;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import fr.onishintan.model.Card;
import fr.onishintan.model.Combatant;
import fr.onishintan.model.HeroClass;
import fr.onishintan.model.Minion;
import fr.onishintan.model.MinionCard;
import fr.onishintan.model.Oath;
import fr.onishintan.model.OathType;
import fr.onishintan.model.Player;
import fr.onishintan.model.Rarity;
import fr.onishintan.model.Shrine;
import fr.onishintan.model.TrapCard;

/**
 * The passive rewards of the 3 and 6 stack tiers (GDD Oath table). Each one holds while its Oath
 * is sworn with at least that many stacks: a break (stacks back to 0, then Oathbreaker) takes them
 * all away at once. The tier 9 effects come with engine-oath-cards.
 */
class OathRewards {

    static final int HONOR_ATK = 2;              // Honor 3: your minions +2 ATK
    static final int HONOR_HEAL = 3;             // Honor 6: a minion hits the enemy hero, heal yours 3
    static final int ALLEGIANCE_BUFF = 2;        // Allegiance 3: end of turn, a random friendly minion +2/+2
    static final int SILENCE_MAX_CARDS = 2;      // Silence 6: played 2 cards or fewer, draw
    static final int DISCIPLINE_KI_SPENT = 4;    // Discipline 3: spent 4+ Ki, +1 Ki next turn
    static final int DISCIPLINE_DRAW_EVERY = 6;  // Discipline 6: a card per 6 Ki spent
    static final int PURITY_HEAL_BONUS = 2;      // Purity 3: healing +2
    static final int PEACE_HP = 2;               // Peace 3: your minions +0/+2
    static final int PEACE_HEAL = 6;             // Peace 6: end of turn, heal your hero 6
    static final int DIVINITY_HEAL = 3;          // Divinity 6: end of turn, heal 3 and a random minion +1/+1
    static final int DIVINITY_BUFF = 1;
    static final int HAVOC_DRAW_AT = 6;          // Havoc 6: dealt 6+ damage this turn, draw (once)

    /** Allegiance 6: summoned when your last minion dies, once per turn. */
    static final MinionCard RETAINER = new MinionCard("retainer", "Retainer", "家来", "けらい", 0,
            Rarity.COMMON, HeroClass.SAMURAI, "", "", 5, 5, null);

    /** Rewards that work once per turn (any player's turn). */
    private enum Once { RETAINER, PURITY_DRAW, HAVOC_DRAW }

    private final GameEngine engine;
    private final Map<Player, Integer> kiNextTurn = new HashMap<>();
    private final Map<Player, Set<Once>> usedThisTurn = new HashMap<>();
    /** The aura bonus each minion carries now: {ATK, HP} (Honor 3, Peace 3). */
    private final Map<Minion, int[]> auras = new IdentityHashMap<>();

    OathRewards(GameEngine engine) {
        this.engine = engine;
    }

    /** True while p's Oath of this type is sworn with at least that many stacks. */
    static boolean has(Player p, OathType type, int stacks) {
        Oath o = p == null ? null : p.getOath();
        return o != null && o.isSworn() && o.getType() == type && o.getStacks() >= stacks;
    }

    // ---------- Ki cost ----------

    /** Silence 3: Ninja cards -1 (min 1). Deception 3: Traps -1 (min 0). Descension 3: 5+ ATK minions -1. */
    int cost(Player p, Card c) {
        int cost = c.getKiCost();
        if (has(p, OathType.SILENCE, 3) && c.getHeroClass() == HeroClass.NINJA) return Math.max(Math.min(cost, 1), cost - 1);
        if (has(p, OathType.DECEPTION, 3) && c instanceof TrapCard) return Math.max(0, cost - 1);
        if (has(p, OathType.DESCENSION, 3) && c instanceof MinionCard mc
                && mc.getBaseAttack() >= TurnTracker.BIG_ATTACK) return Math.max(0, cost - 1);
        return cost;
    }

    /** Ki p gets on top of his refill at the start of his next turn (Justice 3, Deception 6, Discipline 3). */
    int kiNextTurn(Player p) {
        return kiNextTurn.getOrDefault(p, 0);
    }

    // ---------- damage and healing ----------

    /** Effect damage dealt by dealer (spells, Banzai, Seppuku, hero powers, Curses): Havoc 3, Corruption 3, then Justice 6. */
    int effectDamage(Player dealer, boolean fromCurse, Combatant target, int amount) {
        if (amount <= 0) return amount;
        if (has(dealer, OathType.HAVOC, 3)) amount++;
        if (fromCurse && has(dealer, OathType.CORRUPTION, 3)) amount++;
        return combatDamage(dealer, target, amount);
    }

    /** Justice 6: double damage to the enemy minions that hit the dealer's hero during the last enemy turn. */
    int combatDamage(Player dealer, Combatant target, int amount) {
        if (target instanceof Minion m && has(dealer, OathType.JUSTICE, 6)
                && engine.getTurnTracker().getHeroAttackers(dealer).contains(m)) return amount * 2;
        return amount;
    }

    /** Purity 3: healing done during a Purity player's turn gets +2. */
    int heal(int amount) {
        return amount > 0 && has(engine.active(), OathType.PURITY, 3) ? amount + PURITY_HEAL_BONUS : amount;
    }

    /** Purity 6: once per turn, a friendly minion healed back to full HP draws a card. */
    void healed(Combatant target, int restored) {
        Player p = engine.active();
        if (restored > 0 && target instanceof Minion m && m.getHealth() >= m.getMaxHealth()
                && p.getSide().slotOf(m) >= 0 && has(p, OathType.PURITY, 6) && once(p, Once.PURITY_DRAW)) {
            engine.draw(p);
        }
    }

    /** Descension 6: a 5+ ATK minion attacks through Shirodu and Torii. */
    boolean pierces(Combatant attacker) {
        return attacker instanceof Minion m && m.getAttack() >= TurnTracker.BIG_ATTACK
                && has(engine.ownerOf(m), OathType.DESCENSION, 6);
    }

    // ---------- events ----------

    /** Every event, after the rule handlers: the triggered rewards, then the auras. */
    void onEvent(GameEvent e) {
        switch (e.type()) {
            case TURN_STARTED -> startTurn((Player) e.source());
            case ATTACK -> honorHeal(e.source(), e.target());
            case MINION_DIED -> minionDied((Player) e.source());
            case TRAP_TRIGGERED -> trapTriggered((Player) e.source());
            case CURSE_REMOVED -> curseRemoved((Player) e.source());
            case DAMAGE -> havocDraw();
            case SHRINE_PLACED -> {                                        // Divinity 3
                if (has((Player) e.source(), OathType.DIVINITY, 3)) ((Shrine) e.target()).extend(1);
            }
            case OATH_BROKEN -> kiNextTurn.remove((Player) e.source());    // the Ki earned goes with the Oath
            default -> { }
        }
        refreshAuras();
    }

    private void startTurn(Player p) {
        usedThisTurn.clear();
        Integer ki = kiNextTurn.remove(p);
        if (ki != null) p.gainTemporaryKi(ki);
    }

    /** Honor 6: a friendly minion hits the enemy hero, heal your hero 3. */
    private void honorHeal(Object attacker, Object target) {
        Player p = engine.active();
        if (attacker instanceof Minion && target == engine.getState().opponentOf(p).getHero()
                && has(p, OathType.HONOR, 6)) {
            engine.heal(p.getHero(), HONOR_HEAL);
        }
    }

    /** Justice 3: an enemy minion dies, +1 Ki next turn. Allegiance 6: your last minion dies, a Retainer. */
    private void minionDied(Player owner) {
        Player killer = engine.getState().opponentOf(owner);
        if (has(killer, OathType.JUSTICE, 3)) addKi(killer, 1);
        if (owner.getSide().size() == 0 && has(owner, OathType.ALLEGIANCE, 6) && once(owner, Once.RETAINER)) {
            engine.summon(owner, RETAINER);
        }
    }

    /** Deception 6: the enemy sets off one of your Traps, draw a card and +1 Ki next turn. */
    private void trapTriggered(Player owner) {
        if (!has(owner, OathType.DECEPTION, 6)) return;
        engine.draw(owner);
        addKi(owner, 1);
    }

    /** Corruption 6: the enemy pays to remove one of your Curses, draw a card. */
    private void curseRemoved(Player holder) {
        Player giver = engine.getState().opponentOf(holder);
        if (has(giver, OathType.CORRUPTION, 6)) engine.draw(giver);
    }

    /** Havoc 6: once the active player has dealt 6+ damage this turn, he draws a card (once). */
    private void havocDraw() {
        Player p = engine.active();
        if (has(p, OathType.HAVOC, 6) && engine.getTurnTracker().getDamageDealt() >= HAVOC_DRAW_AT
                && once(p, Once.HAVOC_DRAW)) {
            engine.draw(p);
        }
    }

    /** End of p's turn, before his Oath is checked: Allegiance 3, Silence 6, Discipline 3 and 6, Peace 6, Divinity 6. */
    void endTurn(Player p) {
        TurnTracker turn = engine.getTurnTracker();
        if (has(p, OathType.ALLEGIANCE, 3)) randomMinion(p).ifPresent(m -> m.buff(ALLEGIANCE_BUFF, ALLEGIANCE_BUFF));
        if (has(p, OathType.SILENCE, 6) && turn.getCardsPlayed() <= SILENCE_MAX_CARDS) engine.draw(p);
        if (has(p, OathType.DISCIPLINE, 3) && turn.getKiSpent() >= DISCIPLINE_KI_SPENT) addKi(p, 1);
        if (has(p, OathType.DISCIPLINE, 6)) {
            for (int i = 0; i < turn.getKiSpent() / DISCIPLINE_DRAW_EVERY; i++) engine.draw(p);
        }
        if (has(p, OathType.PEACE, 6)) engine.heal(p.getHero(), PEACE_HEAL);
        if (has(p, OathType.DIVINITY, 6)) {
            engine.heal(p.getHero(), DIVINITY_HEAL);
            randomMinion(p).ifPresent(m -> m.buff(DIVINITY_BUFF, DIVINITY_BUFF));
        }
    }

    // ---------- auras ----------

    /** Honor 3 and Peace 3: every minion carries exactly the aura its owner's Oath gives now. */
    void refreshAuras() {
        GameState s = engine.getState();
        Set<Minion> onBoard = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Player p : List.of(s.getFirst(), s.getSecond())) {
            int atk = has(p, OathType.HONOR, 3) ? HONOR_ATK : 0;
            int hp = has(p, OathType.PEACE, 3) ? PEACE_HP : 0;
            for (Minion m : p.getSide().getMinions()) {
                if (m.isDead()) continue;                  // about to leave the board
                onBoard.add(m);
                int[] now = auras.getOrDefault(m, new int[2]);
                int dAtk = atk - now[0];
                int dHp = hp - now[1];
                if (dAtk == 0 && dHp == 0) continue;
                if (dAtk > 0 || dHp > 0) m.buff(Math.max(dAtk, 0), Math.max(dHp, 0));
                if (dAtk < 0 || dHp < 0) m.unbuff(Math.max(-dAtk, 0), Math.max(-dHp, 0));
                auras.put(m, new int[] {atk, hp});
            }
        }
        auras.keySet().retainAll(onBoard);
    }

    // ---------- helpers ----------

    private void addKi(Player p, int n) {
        kiNextTurn.merge(p, n, Integer::sum);
    }

    /** True the first time this turn, false after. */
    private boolean once(Player p, Once what) {
        return usedThisTurn.computeIfAbsent(p, k -> EnumSet.noneOf(Once.class)).add(what);
    }

    private Optional<Minion> randomMinion(Player p) {
        List<Minion> minions = p.getSide().getMinions();
        return minions.isEmpty() ? Optional.empty() : Optional.of(minions.get(engine.rng().nextInt(minions.size())));
    }
}