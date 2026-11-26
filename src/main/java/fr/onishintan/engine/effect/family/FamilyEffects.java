package fr.onishintan.engine.effect.family;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.onishintan.model.Card;
import fr.onishintan.model.Combatant;
import fr.onishintan.model.CurseCard;
import fr.onishintan.model.HeroClass;
import fr.onishintan.model.Keyword;
import fr.onishintan.model.Minion;
import fr.onishintan.model.MinionCard;
import fr.onishintan.model.Player;
import fr.onishintan.model.Rarity;
import fr.onishintan.model.Shrine;

import fr.onishintan.engine.GameEngine;
import fr.onishintan.engine.effect.minion.MinionEffects;
import fr.onishintan.engine.effect.CardEffects;
import fr.onishintan.engine.effect.Effect;
import fr.onishintan.engine.effect.EffectContext;
import fr.onishintan.engine.effect.Effects;
import fr.onishintan.engine.effect.Trigger;
import fr.onishintan.engine.event.GameEvent;
/**
 * The effects of the card families of cards.json, by card id: the 6 Ninja Traps, the 6 Kannushi
 * Shrine auras (doubled by Divinity 9), the 6 Oni Curses and the Banzai that the Rite of Gokitō
 * leaves on later Ancestors (the Rite stacking itself is in GameEngine.performRite).
 */
public final class FamilyEffects {

    /** Kawarimi: the 0/9 log that takes the hit (Shirodu, so it is a legal target). */
    static final MinionCard KAWARIMI_LOG = new MinionCard("kawarimi-log", "Kawarimi log", "丸太", "まるた", 0,
            Rarity.COMMON, HeroClass.NINJA, "Takes the hit", "", 0, 9, EnumSet.of(Keyword.SHIRODU));

    static final int MAKIBISHI_DAMAGE = 6;
    static final int OYASUMI_SLEEPERS = 2;
    static final int SHIKOMI_ANDON_DAMAGE = 14;
    static final int GAGOZE_DAMAGE = 4;
    static final int HINOEUMA_DAMAGE = 8;
    static final int TATARI_DAMAGE = 5;
    static final int SHRINE_HEAL = 3;

    private final GameEngine engine;
    private final MinionEffects minions;
    /** The aura bonus each minion carries now: {ATK, HP}. */
    private final Map<Minion, int[]> auras = new IdentityHashMap<>();
    /** The keywords an aura gave a minion that did not have them (taken back when the aura ends). */
    private final Map<Minion, Set<Keyword>> lent = new IdentityHashMap<>();

    public FamilyEffects(GameEngine engine, MinionEffects minions) {
        this.engine = engine;
        this.minions = minions;
        register(engine.getCardEffects());
    }

    // ---------- the effects, by card id ----------

    private void register(CardEffects fx) {
        // Traps: owner = the trap's owner, target = the attacker or the summoned minion
        trap(fx, "kawarimi", ctx -> {
            Minion log = engine.summon(ctx.owner(), KAWARIMI_LOG);
            if (log != null) engine.redirectAttack(log);
        });
        trap(fx, "makibishi", ctx -> engine.dealDamage(null, ctx.target(), MAKIBISHI_DAMAGE));
        trap(fx, "oyasumi", ctx -> {
            List<Minion> theirs = new ArrayList<>(ctx.enemy().getSide().getMinions());
            Collections.shuffle(theirs, engine.rng());
            theirs.stream().limit(OYASUMI_SLEEPERS).forEach(minions::sleep);
        });
        trap(fx, "otoshiana", ctx -> {
            engine.cancelAttack();
            if (ctx.target() instanceof Minion m) minions.sleep(m);
        });
        trap(fx, "shikomi-andon", ctx -> engine.dealDamage(null, ctx.target(), SHIKOMI_ANDON_DAMAGE));
        trap(fx, "dondengaeshi", ctx -> {
            if (ctx.target() instanceof Minion m) engine.summon(ctx.owner(), m.getSource());
        });

        // Curses: owner = the cursed player
        curse(fx, "bakeneko", ctx -> { });                      // acts on the cost (costUp), nothing at the end of the turn
        curse(fx, "gagoze", ctx -> {
            List<Minion> theirs = ctx.owner().getSide().getMinions();
            if (!theirs.isEmpty()) engine.dealDamage(null, theirs.get(engine.rng().nextInt(theirs.size())), GAGOZE_DAMAGE);
        });
        curse(fx, "hinoeuma", ctx -> engine.dealDamage(null, ctx.owner().getHero(), HINOEUMA_DAMAGE));
        curse(fx, "tatari", ctx -> engine.dealDamage(null, ctx.owner().getHero(), TATARI_DAMAGE));
        curse(fx, "gaki", ctx -> { });                          // acts at the start of the holder's turn (gakiTurnStart)
        curse(fx, "gokiburi", this::gokiburi);

        // Rite of Gokitō: every later Ancestor has Banzai: draw a card (one draw per Gokitō performed)
        fx.register("rite-of-gokito", Trigger.BANZAI, Effects.draw(1));
    }

    private static void trap(CardEffects fx, String id, Effect e) { fx.register(id, Trigger.TRAP, e); }
    private static void curse(CardEffects fx, String id, Effect e) { fx.register(id, Trigger.CURSE, e); }

    /** Gokiburi: eats the card just left of it in its holder's hand (nothing if it is the leftmost). */
    private void gokiburi(EffectContext ctx) {
        Player p = ctx.owner();
        List<Card> hand = p.getHand();
        int at = -1;
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i) instanceof CurseCard c && "gokiburi".equals(c.getId()) && p.isHeldCurse(c)) { at = i; break; }
        }
        if (at <= 0) return;
        Card eaten = hand.get(at - 1);
        if (eaten instanceof CurseCard c && p.isHeldCurse(c)) p.removeCurse(c);
        else p.removeFromHand(eaten);
        engine.publish(GameEvent.of(GameEvent.Type.CARD_DISCARDED, p, eaten));
    }

    // ---------- hooks called by GameEngine ----------

    /** Bakeneko: while p holds it, his minions cost 1 more Ki (one per Bakeneko held). */
    public int costUp(Player p, Card card) {
        return card instanceof MinionCard ? held(p, "bakeneko") : 0;
    }

    /** Gaki: at the start of his turn, its holder loses 1 Ki (one per Gaki held). */
    private void gakiTurnStart(Player p) {
        int n = Math.min(held(p, "gaki"), p.getKi());
        if (n <= 0) return;
        p.getHand().stream().filter(c -> c instanceof CurseCard && "gaki".equals(c.getId()) && p.isHeldCurse(c))
                .findFirst().ifPresent(g -> engine.announce(g, p));
        p.spendKi(n);
    }

    private static int held(Player p, String id) {
        return (int) p.getHand().stream().filter(c -> c instanceof CurseCard && id.equals(c.getId()) && p.isHeldCurse(c)).count();
    }

    /**
     * Fushimi Inari: on the opponent's turns, a minion next to it cannot die from a single hit:
     * the hit leaves it at 1 HP at least.
     */
    public int capDamage(Combatant target, int amount) {
        if (!(target instanceof Minion m) || amount <= 0 || m.getToriiStacks() > 0) return amount;
        Player owner = engine.ownerOf(m);
        if (owner == null || owner == engine.getState().active() || !nextTo(owner, m, "fushimi-inari")) return amount;
        return Math.min(amount, m.getArmour() + m.getHealth() - 1);
    }

    /** Meiji Ōtorii: a minion next to it heals 3 after it attacks (still alive), as its card says. */
    public void afterAttack(Combatant attacker) {
        if (!(attacker instanceof Minion m) || m.isDead()) return;
        Player owner = engine.ownerOf(m);
        if (owner == null) return;
        List<Shrine> meiji = shrinesAround(owner, m).stream().filter(s -> "meiji-otorii".equals(s.getSource().getId())).toList();
        if (meiji.isEmpty()) return;
        engine.announce(meiji.get(0), owner);
        engine.heal(m, SHRINE_HEAL * meiji.size() * factor(owner));
    }

    /** End of the Shrine owner's turn, before the Shrines tick: Heiwa no Torii heals, Itsukushima gives Torii. */
    private void endTurn(Player p) {
        for (Shrine s : p.getSide().shrines()) {
            List<Minion> next = p.getSide().adjacent(p.getSide().slotOf(s));
            String id = s.getSource().getId();
            if (next.isEmpty() || !(id.equals("heiwa-no-torii") || id.equals("itsukushima"))) continue;
            engine.announce(s, p);
            switch (id) {
                case "heiwa-no-torii" -> next.forEach(m -> engine.heal(m, SHRINE_HEAL * factor(p)));
                case "itsukushima" -> next.forEach(m -> { for (int i = 0; i < factor(p); i++) m.giveKeyword(Keyword.TORII); });
                default -> { }
            }
        }
    }

    public void onEvent(GameEvent e) {
        switch (e.type()) {
            case TURN_STARTED -> gakiTurnStart((Player) e.source());
            default -> { }
        }
        refreshAuras();
    }

    /** Subscribed before the Shrines tick, so a Shrine still acts at the end of its last turn. */
    public void onTurnEnded(GameEvent e) {
        endTurn((Player) e.source());
    }

    // ---------- auras ----------

    /**
     * The stat and keyword auras of the Shrines on the minions next to them:
     * Heiwa no Torii +0/+3, Meiji Ōtorii +3/+0, Kumano Hongū Taisha +2/+4 and Shirodu,
     * Motonosumi Jinja +3/+3 and Tatakae up to 2 slots away. Divinity 9 doubles the stats.
     */
    void refreshAuras() {
        Set<Minion> onBoard = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Player p : List.of(engine.getState().getFirst(), engine.getState().getSecond())) {
            int f = factor(p);
            for (Minion m : p.getSide().getMinions()) {
                if (m.isDead()) continue;
                onBoard.add(m);
                int wantAtk = 0, wantHp = 0;
                Set<Keyword> wantKw = EnumSet.noneOf(Keyword.class);
                for (Shrine s : shrinesAround(p, m)) {
                    switch (s.getSource().getId()) {
                        case "heiwa-no-torii" -> wantHp += 3 * f;
                        case "meiji-otorii" -> wantAtk += 3 * f;
                        case "kumano-hongu-taisha" -> { wantAtk += 2 * f; wantHp += 4 * f; wantKw.add(Keyword.SHIRODU); }
                        case "motonosumi-shrine" -> { wantAtk += 3 * f; wantHp += 3 * f; wantKw.add(Keyword.TATAKAE); }
                        default -> { }
                    }
                }
                applyStats(m, wantAtk, wantHp);
                applyKeywords(m, wantKw);
            }
        }
        auras.keySet().retainAll(onBoard);
        lent.keySet().retainAll(onBoard);
    }

    private void applyStats(Minion m, int wantAtk, int wantHp) {
        int[] now = auras.getOrDefault(m, new int[2]);
        int dAtk = wantAtk - now[0], dHp = wantHp - now[1];
        if (dAtk == 0 && dHp == 0) return;
        if (dAtk > 0 || dHp > 0) m.buff(Math.max(dAtk, 0), Math.max(dHp, 0));
        if (dAtk < 0 || dHp < 0) m.unbuff(Math.max(-dAtk, 0), Math.max(-dHp, 0));
        auras.put(m, new int[] {wantAtk, wantHp});
    }

    private void applyKeywords(Minion m, Set<Keyword> want) {
        Set<Keyword> mine = lent.computeIfAbsent(m, k -> EnumSet.noneOf(Keyword.class));
        for (Keyword k : List.copyOf(mine)) {
            if (!want.contains(k)) { m.removeKeyword(k); mine.remove(k); }
        }
        for (Keyword k : want) {
            if (!m.hasKeyword(k)) { m.giveKeyword(k); mine.add(k); }
        }
    }

    // ---------- helpers ----------

    private int factor(Player p) {
        return engine.shrineFactor(p);
    }

    /** The Shrines in the slots next to this minion of p's. */
    private static final String MOTONOSUMI = "motonosumi-shrine";
    /** Motonosumi Jinja reaches 2 slots on each side; every other Shrine only its two neighbours. */
    static final int MOTONOSUMI_REACH = 2;

    /** The Shrines whose aura reaches this minion: next to it, or up to 2 slots away for Motonosumi Jinja. */
    private static List<Shrine> shrinesAround(Player p, Minion m) {
        int slot = p.getSide().slotOf(m);
        List<Shrine> out = new ArrayList<>(4);
        for (int d = -MOTONOSUMI_REACH; d <= MOTONOSUMI_REACH; d++) {
            if (d == 0) continue;
            if (p.getSide().entityAt(slot + d) instanceof Shrine sh
                    && (Math.abs(d) == 1 || MOTONOSUMI.equals(sh.getSource().getId()))) {
                out.add(sh);
            }
        }
        return out;
    }

    private static int shrinesNextTo(Player p, Minion m, String id) {
        return (int) shrinesAround(p, m).stream().filter(s -> id.equals(s.getSource().getId())).count();
    }

    private static boolean nextTo(Player p, Minion m, String id) {
        return shrinesNextTo(p, m, id) > 0;
    }
}