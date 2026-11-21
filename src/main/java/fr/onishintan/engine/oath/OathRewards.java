package fr.onishintan.engine.oath;

import java.util.ArrayList;
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
import fr.onishintan.model.Keyword;
import fr.onishintan.model.Minion;
import fr.onishintan.model.MinionCard;
import fr.onishintan.model.Oath;
import fr.onishintan.model.OathType;
import fr.onishintan.model.Player;
import fr.onishintan.model.Rarity;
import fr.onishintan.model.Shrine;
import fr.onishintan.model.SpellCard;
import fr.onishintan.model.TrapCard;
import fr.onishintan.engine.GameEngine;
import fr.onishintan.engine.effect.Trigger;
import fr.onishintan.engine.event.GameEvent;
import fr.onishintan.engine.turn.TurnTracker;

/**
 * The rewards of the 3, 6 and 9 stack tiers (GDD Oath table). Each one holds while its Oath
 * is sworn with at least that many stacks: a break (stacks back to 0, then Oathbreaker) takes them
 * all away at once, the Oath cards leave the hand and the Honor 9 bonus is taken back.
 */
public class OathRewards {

    static final int HONOR_ATK = 2;              // Honor 3: your minions +2 ATK
    static final int HONOR_HEAL = 3;             // Honor 6: a minion hits the enemy hero, heal yours 3
    static final int ALLEGIANCE_BUFF = 2;        // Allegiance 3: end of turn, a random friendly minion +2/+2
    static final int SILENCE_MAX_CARDS = 2;      // Silence 6: played 2 cards or fewer, draw
    static final int DISCIPLINE_KI_SPENT = 4;    // Discipline 3: spent 4+ Ki, +1 Ki next turn
    static final int DISCIPLINE_DRAW_EVERY = 6;  // Discipline 6: a card per 6 Ki spent
    public static final int PURITY_HEAL_BONUS = 2;      // Purity 3: healing +2
    static final int PEACE_HP = 2;               // Peace 3: your minions +0/+2
    static final int PEACE_HEAL = 6;             // Peace 6: end of turn, heal your hero 6
    static final int DIVINITY_HEAL = 3;          // Divinity 6: end of turn, heal 3 and a random minion +1/+1
    static final int DIVINITY_BUFF = 1;
    static final int HAVOC_DRAW_AT = 6;          // Havoc 6: dealt 6+ damage this turn, draw (once)
    static final int HONOR_FIRST_ATK = 4;        // Honor 9: first minion played each turn +4 ATK and Tatakae
    static final int ALLEGIANCE_ALL = 2;         // Allegiance 9: all friendly minions +2/+2 and Shirodu
    static final int WREAK_HAVOC_DAMAGE = 8;     // Havoc 9 Oath card

    /** Justice 9: Oath card added to the hand (0 Ki, decided 16 Nov), taken back on a break. */
    public static final SpellCard JUSTICE_CARD = new SpellCard("justice-oath", "Justice", "正義", "せいぎ", 0,
            Rarity.LEGENDARY, HeroClass.SAMURAI, "Oath card: deal your strongest minion's ATK to all enemy minions.", "");
    /** Havoc 9: Oath card added to the hand (0 Ki), taken back on a break. */
    public static final SpellCard WREAK_HAVOC = new SpellCard("wreak-havoc", "Wreak Havoc", "破壊", "はかい", 0,
            Rarity.LEGENDARY, HeroClass.ONI, "Oath card: deal 8 damage to every enemy minion and the enemy hero.", "");

    /** Allegiance 6: summoned when your last minion dies, once per turn. */
    public static final MinionCard RETAINER = new MinionCard("retainer", "Retainer", "家来", "けらい", 0,
            Rarity.COMMON, HeroClass.SAMURAI, "", "", 5, 5, null);

    /** Rewards that work once per turn (any player's turn). */
    private enum Once { RETAINER, PURITY_DRAW, HAVOC_DRAW, HONOR_FIRST, SILENCE_FIRST, DEFLECT }

    private final GameEngine engine;
    private final Map<Player, Integer> kiNextTurn = new HashMap<>();
    private final Map<Player, Set<Once>> usedThisTurn = new HashMap<>();
    /** The aura bonus each minion carries now: {ATK, HP, Shirodu given 0/1} (Honor 3, Peace 3, Allegiance 9, Descension 9). */
    private final Map<Minion, int[]> auras = new IdentityHashMap<>();
    /** Honor 9: the minions that got +4 ATK and Tatakae, and whether they had Tatakae before. */
    private final Map<Minion, Boolean> honored = new IdentityHashMap<>();

    private boolean minionCardPlayed;    // a MinionCard was played, its MINION_SUMMONED comes next
    private boolean settingExtraTrap;    // Deception 9: the free Trap does not chain

    public OathRewards(GameEngine engine) {
        this.engine = engine;
        engine.getCardEffects()
                .register(JUSTICE_CARD.getId(), Trigger.BANZAI, ctx -> {
                    int atk = OathPenalties.strongest(ctx.owner()).map(Minion::getAttack).orElse(0);
                    for (Minion m : List.copyOf(ctx.enemy().getSide().getMinions())) ctx.engine().dealDamage(null, m, atk);
                })
                .register(WREAK_HAVOC.getId(), Trigger.BANZAI, ctx -> {
                    for (Minion m : List.copyOf(ctx.enemy().getSide().getMinions())) {
                        ctx.engine().dealDamage(null, m, WREAK_HAVOC_DAMAGE);
                    }
                    ctx.engine().dealDamage(null, ctx.enemy().getHero(), WREAK_HAVOC_DAMAGE);
                });
    }

    /** True while p's Oath of this type is sworn with at least that many stacks. */
    public static boolean has(Player p, OathType type, int stacks) {
        Oath o = p == null ? null : p.getOath();
        return o != null && o.isSworn() && o.getType() == type && o.getStacks() >= stacks;
    }

    // ---------- Ki cost ----------

    /** Silence 3: Ninja cards -1 (min 1). Deception 3: Traps -1 (min 0). Descension 3: 5+ ATK minions -1. */
    public int cost(Player p, Card c) {
        int cost = c.getKiCost();
        if (has(p, OathType.SILENCE, 3) && c.getHeroClass() == HeroClass.NINJA) return Math.max(Math.min(cost, 1), cost - 1);
        if (has(p, OathType.DECEPTION, 3) && c instanceof TrapCard) return Math.max(0, cost - 1);
        if (has(p, OathType.DESCENSION, 3) && c instanceof MinionCard mc
                && mc.getBaseAttack() >= TurnTracker.BIG_ATTACK) return Math.max(0, cost - 1);
        return cost;
    }

    /** Ki p gets on top of his refill at the start of his next turn (Justice 3, Deception 6, Discipline 3). */
    public int kiNextTurn(Player p) {
        return kiNextTurn.getOrDefault(p, 0);
    }

    // ---------- damage and healing ----------

    /** Effect damage dealt by dealer (spells, Banzai, Seppuku, hero powers, Curses): Havoc 3, Corruption 3, then Justice 6. */
    public int effectDamage(Player dealer, boolean fromCurse, Combatant target, int amount) {
        if (amount <= 0) return amount;
        if (has(dealer, OathType.HAVOC, 3)) amount++;
        if (fromCurse && has(dealer, OathType.CORRUPTION, 3)) amount++;
        return combatDamage(dealer, target, amount);
    }

    /** Justice 6: double damage to the enemy minions that hit the dealer's hero during the last enemy turn. Then Peace 9. */
    public int combatDamage(Player dealer, Combatant target, int amount) {
        if (target instanceof Minion m && has(dealer, OathType.JUSTICE, 6)
                && engine.getTurnTracker().getHeroAttackers(dealer).contains(m)) amount *= 2;
        return deflect(target, amount);
    }

    /** Peace 9: the first damage a Peace hero would take each turn hits the enemy hero instead. */
    private int deflect(Combatant target, int amount) {
        if (amount <= 0) return amount;
        Player q = engine.ownerOf(target);
        if (q == null || target != q.getHero() || !has(q, OathType.PEACE, 9) || !once(q, Once.DEFLECT)) return amount;
        announce(q);
        engine.dealDamage(q.getOath(), engine.getState().opponentOf(q).getHero(), amount);
        return 0;
    }

    /** Divinity 9: Shrine effects doubled. Read by the Shrine auras (engine-family-effects). */
    public int shrineFactor(Player p) {
        return has(p, OathType.DIVINITY, 9) ? 2 : 1;
    }

    /** Purity 3: healing done during a Purity player's turn gets +2. */
    public int heal(int amount) {
        return amount > 0 && has(engine.active(), OathType.PURITY, 3) ? amount + PURITY_HEAL_BONUS : amount;
    }

    /** Purity 6: once per turn, a friendly minion healed back to full HP draws a card. */
    public void healed(Combatant target, int restored) {
        Player p = engine.active();
        if (restored > 0 && target instanceof Minion m && m.getHealth() >= m.getMaxHealth()
                && p.getSide().slotOf(m) >= 0 && has(p, OathType.PURITY, 6) && once(p, Once.PURITY_DRAW)) {
            announce(p);
            engine.draw(p);
        }
    }

    /** Descension 6: a 5+ ATK minion attacks through Shirodu and Torii. */
    public boolean pierces(Combatant attacker) {
        return attacker instanceof Minion m && m.getAttack() >= TurnTracker.BIG_ATTACK
                && has(engine.ownerOf(m), OathType.DESCENSION, 6);
    }

    // ---------- events ----------

    /** Every event, after the rule handlers: the triggered rewards, then the auras. */
    public void onEvent(GameEvent e) {
        switch (e.type()) {
            case TURN_STARTED -> startTurn((Player) e.source());
            case TURN_ENDED -> corruptionAtTurnEnd((Player) e.source());
            case CARD_PLAYED -> cardPlayed((Player) e.source(), (Card) e.target());
            case TRAP_SET -> trapSet((Player) e.source(), (TrapCard) e.target());
            case MINION_SUMMONED -> minionSummoned((Player) e.source(), (Minion) e.target());
            case OATH_REWARD -> { if (e.amount() == 9) oathCard((Player) e.source(), (Oath) e.target()); }
            case ATTACK -> honorHeal(e.source(), e.target());
            case MINION_DIED -> minionDied((Player) e.source());
            case TRAP_TRIGGERED -> trapTriggered((Player) e.source());
            case CURSE_REMOVED -> curseRemoved((Player) e.source());
            case DAMAGE -> havocDraw();
            case SHRINE_PLACED -> {                                        // Divinity 3
                if (has((Player) e.source(), OathType.DIVINITY, 3)) ((Shrine) e.target()).extend(1);
            }
            case OATH_BROKEN -> broken((Player) e.source());
            default -> { }
        }
        refreshAuras();
    }

    private void startTurn(Player p) {
        usedThisTurn.clear();
        Integer ki = kiNextTurn.remove(p);
        if (ki != null) {
            announce(p);
            p.gainTemporaryKi(ki);
        }
        for (Player giver : players()) {                                     // Corruption 9: start of every turn
            if (has(giver, OathType.CORRUPTION, 9) && !engine.getState().isOver()) {
                announce(giver);
                engine.runHandCurses(engine.getState().opponentOf(giver));
            }
        }
    }

    /** Corruption 9: the Curses also act at the end of the giver's own turn (the holder's end already runs them). */
    private void corruptionAtTurnEnd(Player ending) {
        for (Player giver : players()) {
            Player holder = engine.getState().opponentOf(giver);
            if (holder != ending && has(giver, OathType.CORRUPTION, 9) && !engine.getState().isOver()) {
                announce(giver);
                engine.runHandCurses(holder);
            }
        }
    }

    /** Discipline 9: every card played gives 1 Ki. */
    private void cardPlayed(Player p, Card c) {
        if (c instanceof MinionCard) minionCardPlayed = true;
        if (has(p, OathType.DISCIPLINE, 9)) {
            announce(p);
            p.gainTemporaryKi(1);
        }
    }

    /** A Trap set is a card played too (Discipline 9). Deception 9: set another, different Trap from the hand or deck, free. */
    private void trapSet(Player p, TrapCard trap) {
        if (settingExtraTrap) return;
        if (has(p, OathType.DISCIPLINE, 9)) {
            announce(p);
            p.gainTemporaryKi(1);
        }
        if (!has(p, OathType.DECEPTION, 9)) return;
        List<TrapCard> other = new ArrayList<>();
        for (Card c : p.getHand()) if (c instanceof TrapCard t && !t.getId().equals(trap.getId()) && p.canSetTrap(t)) other.add(t);
        for (Card c : p.getDeck().getCards()) if (c instanceof TrapCard t && !t.getId().equals(trap.getId()) && p.canSetTrap(t)) other.add(t);
        if (other.isEmpty()) return;
        announce(p);
        settingExtraTrap = true;
        try {
            engine.setExtraTrap(p, other.get(engine.rng().nextInt(other.size())));
        } finally {
            settingExtraTrap = false;
        }
    }

    /**
     * A minion played from the hand. Honor 9: the first one each turn gets +4 ATK and Tatakae.
     * Silence 9: the first one each turn is cloned at once (a fresh copy of its card, no Banzai) in the leftmost free slot.
     */
    private void minionSummoned(Player p, Minion m) {
        if (!minionCardPlayed) return;
        minionCardPlayed = false;
        if (has(p, OathType.HONOR, 9) && once(p, Once.HONOR_FIRST)) {
            announce(p);
            honored.put(m, m.hasKeyword(Keyword.TATAKAE));
            m.buff(HONOR_FIRST_ATK, 0);
            m.giveKeyword(Keyword.TATAKAE);
        }
        if (has(p, OathType.SILENCE, 9) && once(p, Once.SILENCE_FIRST)) {
            announce(p);
            engine.summon(p, m.getSource());
        }
    }

    /** Justice 9 and Havoc 9: the Oath card goes to the hand. */
    private void oathCard(Player p, Oath oath) {
        if (oath.getType() == OathType.JUSTICE) engine.addToHand(p, JUSTICE_CARD);
        if (oath.getType() == OathType.HAVOC) engine.addToHand(p, WREAK_HAVOC);
    }

    /** A break: the Ki earned for next turn, the Oath cards in hand and the Honor 9 bonus go with the Oath. */
    private void broken(Player p) {
        kiNextTurn.remove(p);
        while (p.removeFromHand(JUSTICE_CARD)) { }
        while (p.removeFromHand(WREAK_HAVOC)) { }
        for (Map.Entry<Minion, Boolean> e : List.copyOf(honored.entrySet())) {
            Minion m = e.getKey();
            Player owner = engine.ownerOf(m);
            if (owner != null && owner != p) continue;
            honored.remove(m);
            if (owner == null) continue;                                     // already dead
            m.unbuff(HONOR_FIRST_ATK, 0);
            if (!e.getValue()) m.removeKeyword(Keyword.TATAKAE);
        }
    }

    /** Honor 6: a friendly minion hits the enemy hero, heal your hero 3. */
    private void honorHeal(Object attacker, Object target) {
        Player p = engine.active();
        if (attacker instanceof Minion && target == engine.getState().opponentOf(p).getHero()
                && has(p, OathType.HONOR, 6)) {
            announce(p);
            engine.heal(p.getHero(), HONOR_HEAL);
        }
    }

    /** Justice 3: an enemy minion dies, +1 Ki next turn. Allegiance 6: your last minion dies, a Retainer. */
    private void minionDied(Player owner) {
        Player killer = engine.getState().opponentOf(owner);
        if (has(killer, OathType.JUSTICE, 3)) {
            announce(killer);
            addKi(killer, 1);
        }
        if (owner.getSide().size() == 0 && has(owner, OathType.ALLEGIANCE, 6) && once(owner, Once.RETAINER)) {
            announce(owner);
            engine.summon(owner, RETAINER);
        }
    }

    /** Deception 6: the enemy sets off one of your Traps, draw a card and +1 Ki next turn. */
    private void trapTriggered(Player owner) {
        if (!has(owner, OathType.DECEPTION, 6)) return;
        announce(owner);
        engine.draw(owner);
        addKi(owner, 1);
    }

    /** Corruption 6: the enemy pays to remove one of your Curses, draw a card. */
    private void curseRemoved(Player holder) {
        Player giver = engine.getState().opponentOf(holder);
        if (has(giver, OathType.CORRUPTION, 6)) {
            announce(giver);
            engine.draw(giver);
        }
    }

    /** Havoc 6: once the active player has dealt 6+ damage this turn, he draws a card (once). */
    private void havocDraw() {
        Player p = engine.active();
        if (has(p, OathType.HAVOC, 6) && engine.getTurnTracker().getDamageDealt() >= HAVOC_DRAW_AT
                && once(p, Once.HAVOC_DRAW)) {
            announce(p);
            engine.draw(p);
        }
    }

    /** End of p's turn, before his Oath is checked: Allegiance 3, Silence 6, Discipline 3 and 6, Peace 6, Divinity 6, Purity 9. */
    public void endTurn(Player p) {
        TurnTracker turn = engine.getTurnTracker();
        if (has(p, OathType.ALLEGIANCE, 3) && !p.getSide().getMinions().isEmpty()) {
            announce(p);
            randomMinion(p).ifPresent(m -> m.buff(ALLEGIANCE_BUFF, ALLEGIANCE_BUFF));
        }
        if (has(p, OathType.SILENCE, 6) && turn.getCardsPlayed() <= SILENCE_MAX_CARDS) {
            announce(p);
            engine.draw(p);
        }
        if (has(p, OathType.DISCIPLINE, 3) && turn.getKiSpent() >= DISCIPLINE_KI_SPENT) {
            announce(p);
            addKi(p, 1);
        }
        if (has(p, OathType.DISCIPLINE, 6) && turn.getKiSpent() >= DISCIPLINE_DRAW_EVERY) {
            announce(p);
            for (int i = 0; i < turn.getKiSpent() / DISCIPLINE_DRAW_EVERY; i++) engine.draw(p);
        }
        if (has(p, OathType.PEACE, 6)) {
            announce(p);
            engine.heal(p.getHero(), PEACE_HEAL);
        }
        if (has(p, OathType.DIVINITY, 6)) {
            announce(p);
            engine.heal(p.getHero(), DIVINITY_HEAL);
            randomMinion(p).ifPresent(m -> m.buff(DIVINITY_BUFF, DIVINITY_BUFF));
        }
        if (has(p, OathType.PURITY, 9) && !p.getSide().getMinions().isEmpty()) {   // fully heal all your minions
            announce(p);
            for (Minion m : List.copyOf(p.getSide().getMinions())) engine.heal(m, m.getMaxHealth());
        }
    }

    // ---------- auras ----------

    /**
     * Honor 3, Peace 3, Allegiance 9 (+2/+2 and Shirodu) and Descension 9 (biggest minion: double ATK and HP):
     * every minion carries exactly the aura its owner's Oath gives now.
     */
    public void refreshAuras() {
        Set<Minion> onBoard = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Player p : players()) {
            boolean all = has(p, OathType.ALLEGIANCE, 9);
            int atk = (has(p, OathType.HONOR, 3) ? HONOR_ATK : 0) + (all ? ALLEGIANCE_ALL : 0);
            int hp = (has(p, OathType.PEACE, 3) ? PEACE_HP : 0) + (all ? ALLEGIANCE_ALL : 0);
            Minion biggest = has(p, OathType.DESCENSION, 9) ? biggestWithoutAura(p) : null;
            for (Minion m : p.getSide().getMinions()) {
                if (m.isDead()) continue;                  // about to leave the board
                onBoard.add(m);
                int[] now = auras.getOrDefault(m, new int[3]);
                int wantAtk = atk, wantHp = hp;
                if (m == biggest) {                        // its own stats once more
                    wantAtk += m.getAttack() - now[0];
                    wantHp += m.getMaxHealth() - now[1];
                }
                int wantShirodu = all && (now[2] == 1 || !m.hasKeyword(Keyword.SHIRODU)) ? 1 : 0;
                int dAtk = wantAtk - now[0];
                int dHp = wantHp - now[1];
                if (dAtk == 0 && dHp == 0 && wantShirodu == now[2]) continue;
                if (dAtk > 0 || dHp > 0) m.buff(Math.max(dAtk, 0), Math.max(dHp, 0));
                if (dAtk < 0 || dHp < 0) m.unbuff(Math.max(-dAtk, 0), Math.max(-dHp, 0));
                if (wantShirodu > now[2]) m.giveKeyword(Keyword.SHIRODU);
                if (wantShirodu < now[2]) m.removeKeyword(Keyword.SHIRODU);
                auras.put(m, new int[] {wantAtk, wantHp, wantShirodu});
            }
        }
        auras.keySet().retainAll(onBoard);
    }

    /** Descension 9: highest ATK without the aura, then HP, then the leftmost. */
    private Minion biggestWithoutAura(Player p) {
        Minion best = null;
        int bestAtk = -1, bestHp = -1;
        for (Minion m : p.getSide().getMinions()) {        // left to right: a tie keeps the leftmost
            if (m.isDead()) continue;
            int[] a = auras.getOrDefault(m, new int[3]);
            int atk = m.getAttack() - a[0], hp = m.getMaxHealth() - a[1];
            if (atk > bestAtk || (atk == bestAtk && hp > bestHp)) {
                best = m;
                bestAtk = atk;
                bestHp = hp;
            }
        }
        return best;
    }

    // ---------- helpers ----------

    private List<Player> players() {
        return List.of(engine.getState().getFirst(), engine.getState().getSecond());
    }

    /** EFFECT_TRIGGERED with the Oath as source, just before its reward acts. */
    private void announce(Player p) {
        engine.announce(p.getOath(), p);
    }

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