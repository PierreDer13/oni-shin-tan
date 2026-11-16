package fr.onishintan.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import fr.onishintan.model.Card;
import fr.onishintan.model.Combatant;
import fr.onishintan.model.CurseCard;
import fr.onishintan.model.HeroClass;
import fr.onishintan.model.KatanaTier;
import fr.onishintan.model.Keyword;
import fr.onishintan.model.Minion;
import fr.onishintan.model.MinionCard;
import fr.onishintan.model.Player;
import fr.onishintan.model.Rarity;
import fr.onishintan.model.Status;
import fr.onishintan.model.WeaponCard;

/**
 * The effects of the minions of MockCards (Card Curve), by card id: Banzai (with the target the UI
 * must ask for), Seppuku, end-of-turn passives, auras (Ronin, Jōnin), Tennō's cost cut, Hakahori,
 * Rokurokubi, Fūma Kotarō's Yogake, Jorōgumo's sleep and Shike's silence.
 */
final class MinionEffects {

    /** A Banzai that needs a target: its kind, and a filter on top of it (Shinigami: damaged only). */
    record Aim(SpellTarget target, Predicate<Combatant> filter) {
        Aim(SpellTarget target) { this(target, c -> true); }
    }

    static final String RYU = GameEngine.RYU_ID;
    static final WeaponCard FUTSU_NO_MITAMA = new WeaponCard("futsu-no-mitama", "Futsu-no-mitama", "布都御魂", "ふつのみたま",
            0, Rarity.LEGENDARY, HeroClass.SAMURAI, "", "", 10, 2);

    private static final Map<String, Aim> AIMS = new HashMap<>();
    static {
        AIMS.put("unsui", new Aim(SpellTarget.FRIENDLY_CHARACTER));
        AIMS.put("shike", new Aim(SpellTarget.ENEMY_MINION));
        AIMS.put("ikareru-hyakusho", new Aim(SpellTarget.MINION));
        AIMS.put("yamabushi", new Aim(SpellTarget.CHARACTER));
        AIMS.put("onmyoji", new Aim(SpellTarget.FRIENDLY_MINION));
        AIMS.put("maiko", new Aim(SpellTarget.ENEMY_MINION));
        AIMS.put("onnagata", new Aim(SpellTarget.ENEMY_MINION));
        AIMS.put("yumihiki", new Aim(SpellTarget.CHARACTER));
        AIMS.put("jorogumo", new Aim(SpellTarget.ENEMY_MINION));
        AIMS.put("shinigami", new Aim(SpellTarget.ENEMY_MINION, c -> c.getHealth() < c.getMaxHealth()));
        AIMS.put("kitsune-no-bakashiya", new Aim(SpellTarget.MINION));
        AIMS.put("fujin", new Aim(SpellTarget.ENEMY_MINION));
        AIMS.put("susanoo", new Aim(SpellTarget.ENEMY_MINION));
    }

    private final GameEngine engine;
    private final Set<Minion> silenced = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<Minion> asleep = Collections.newSetFromMap(new IdentityHashMap<>());
    /** The aura bonus each minion carries now: {ATK, HP} (Ronin, Jōnin). */
    private final Map<Minion, int[]> auras = new IdentityHashMap<>();

    MinionEffects(GameEngine engine) {
        this.engine = engine;
        register(engine.getCardEffects());
    }

    /** The Banzai target this minion card asks for, or null when its Banzai needs none. */
    static Aim aim(Card card) {
        return card == null ? null : AIMS.get(card.getId());
    }

    boolean isSilenced(Minion m) { return silenced.contains(m); }

    private boolean active(Minion m, String id) {
        return !m.isDead() && !silenced.contains(m) && id.equals(m.getSource().getId());
    }

    private long count(Player p, String id) {
        return p.getSide().getMinions().stream().filter(m -> active(m, id)).count();
    }

    /** Rokurokubi attacks past Shirodu and Torii. */
    boolean pierces(Combatant c) {
        return c instanceof Minion m && active(m, "rokurokubi");
    }

    /** Tennō: your other minions cost 1 less Ki (one per Tennō on your side). */
    int costCut(Player p, Card c) {
        return c instanceof MinionCard && !"tenno".equals(c.getId()) ? (int) count(p, "tenno") : 0;
    }

    // ---------- the effects, by card id ----------

    private void register(CardEffects fx) {
        // Banzai with a target (the target may be null when there was no legal one)
        banzai(fx, "unsui", aimed((ctx, t) -> engine.heal(t, 6)));
        banzai(fx, "shike", aimed((ctx, t) -> silence((Minion) t)));
        banzai(fx, "ikareru-hyakusho", aimed((ctx, t) -> engine.dealDamage(ctx.source(), t, 4)));
        banzai(fx, "yamabushi", aimed((ctx, t) -> engine.heal(t, 12)));
        banzai(fx, "onmyoji", aimed((ctx, t) -> ((Minion) t).giveKeyword(Keyword.TORII)));
        banzai(fx, "maiko", aimed((ctx, t) -> ((Minion) t).buff(-3, 0)));
        banzai(fx, "onnagata", aimed((ctx, t) -> ctx.source().buff(t.getAttack() - ctx.source().getAttack(), 0)));
        banzai(fx, "yumihiki", aimed((ctx, t) -> engine.dealDamage(ctx.source(), t, 4)));
        banzai(fx, "jorogumo", aimed((ctx, t) -> sleep((Minion) t)));
        banzai(fx, "shinigami", aimed((ctx, t) -> engine.destroy((Minion) t)));
        banzai(fx, "kitsune-no-bakashiya", aimed((ctx, t) -> engine.transform(ctx.source(), ((Minion) t).getSource())));
        banzai(fx, "fujin", aimed((ctx, t) -> engine.bounce((Minion) t)));
        banzai(fx, "susanoo", aimed((ctx, t) -> engine.dealDamage(ctx.source(), t, 6)));

        // Banzai without a target
        banzai(fx, "katana-kaji", ctx -> { if (ctx.owner().getHero().getWeapon() != null) ctx.owner().getHero().getWeapon().addAttack(3); });
        banzai(fx, "sakana-uri", ctx -> addToHand(ctx.owner(), "koi"));
        banzai(fx, "monogoi", ctx -> { if (ctx.owner().getHand().isEmpty()) { engine.draw(ctx.owner()); engine.draw(ctx.owner()); } });
        banzai(fx, "taiko-uchi", ctx -> others(ctx).forEach(m -> m.buff(2, 0)));
        banzai(fx, "daimyo", ctx -> engine.equip(ctx.owner(), KatanaTier.KATANA.toCard()));
        banzai(fx, "shogun", ctx -> ctx.owner().getSide().getMinions().forEach(m -> m.buff(4, 4)));
        banzai(fx, "kappa", Effects.damageEnemyHero(4));
        banzai(fx, "nue", ctx -> {
            for (Player p : List.of(ctx.owner(), ctx.enemy())) {
                for (Minion m : List.copyOf(p.getSide().getMinions())) if (m != ctx.source()) engine.dealDamage(ctx.source(), m, 6);
            }
        });
        banzai(fx, "enma", ctx -> {
            for (Player p : List.of(ctx.owner(), ctx.enemy())) {
                for (Minion m : List.copyOf(p.getSide().getMinions())) if (m != ctx.source() && m.getAttack() <= 8) engine.destroy(m);
            }
        });
        banzai(fx, "raijin", this::raijin);
        banzai(fx, "amaterasu-omikami", ctx -> engine.heal(ctx.owner().getHero(), ctx.owner().getHero().getMaxHealth()));
        banzai(fx, "tsukuyomi", ctx -> others(ctx).forEach(m -> m.giveKeyword(Keyword.SHINOBI)));
        banzai(fx, "takemikazuchi", ctx -> engine.equip(ctx.owner(), FUTSU_NO_MITAMA));
        banzai(fx, "okuninushi", ctx -> {
            engine.heal(ctx.owner().getHero(), 10);
            for (Minion m : List.copyOf(ctx.owner().getSide().getMinions())) engine.heal(m, 10);
        });
        banzai(fx, "izanami", ctx -> {                  // the 2 highest-cost dead minions come back (no Banzai)
            List<MinionCard> dead = new ArrayList<>(engine.graveyard(ctx.owner()));
            dead.sort(Comparator.comparingInt(MinionCard::getKiCost).reversed());
            for (MinionCard c : dead.subList(0, Math.min(2, dead.size()))) {
                engine.graveyard(ctx.owner()).remove(c);
                engine.summon(ctx.owner(), c);
            }
        });
        banzai(fx, "kiji", ctx -> {                     // a copy of a random card of the opponent's hand (not a Curse he holds)
            List<Card> pool = ctx.enemy().getHand().stream().filter(c -> !ctx.enemy().isHeldCurse(c)).toList();
            if (!pool.isEmpty()) ctx.owner().addToHand(pool.get(engine.rng().nextInt(pool.size())));
        });

        // Seppuku
        seppuku(fx, "inasaku-noka", ctx -> engine.heal(ctx.owner().getHero(), 8));
        seppuku(fx, "sendaiku", ctx -> summon(ctx.owner(), "sakana-uri"));
        seppuku(fx, "jujutsu-shihan", ctx -> engine.equip(ctx.owner(), KatanaTier.HONJO_MASAMUNE.toCard()));
        seppuku(fx, "noppera-bo", ctx -> ctx.owner().addToHand(ctx.source().getSource()));
        seppuku(fx, "oiwa", ctx -> {                    // 2 random deck Curses into the enemy deck
            List<CurseCard> curses = engine.deckCurses();
            for (int i = 0; i < 2 && !curses.isEmpty(); i++) {
                engine.giveDeckCurse(ctx.owner(), ctx.enemy(), curses.get(engine.rng().nextInt(curses.size())));
            }
        });
        seppuku(fx, "koi", ctx -> engine.draw(ctx.owner()));

        // End of the owner's turn
        turnEnd(fx, "jushoku", ctx -> engine.heal(ctx.owner().getHero(), 6));
        turnEnd(fx, "goze", ctx -> { for (Minion m : List.copyOf(ctx.owner().getSide().getMinions())) engine.heal(m, 3); });
        turnEnd(fx, "komuso", ctx -> engine.heal(ctx.owner().getHero(), 4));
        turnEnd(fx, "hatamoto", ctx -> engine.gainArmour(ctx.owner(), 3));
        turnEnd(fx, "inari-okami", ctx -> summon(ctx.owner(), "kitsune-no-tsukai"));
    }

    private interface Aimed { void apply(EffectContext ctx, Combatant target); }

    private static Effect aimed(Aimed a) {
        return ctx -> { if (ctx.target() != null && !ctx.target().isDead()) a.apply(ctx, ctx.target()); };
    }

    private static void banzai(CardEffects fx, String id, Effect e) { fx.register(id, Trigger.BANZAI, e); }
    private static void seppuku(CardEffects fx, String id, Effect e) { fx.register(id, Trigger.SEPPUKU, e); }
    private static void turnEnd(CardEffects fx, String id, Effect e) { fx.register(id, Trigger.TURN_END, e); }

    private static List<Minion> others(EffectContext ctx) {
        return ctx.owner().getSide().getMinions().stream().filter(m -> m != ctx.source()).toList();
    }

    private void addToHand(Player p, String id) {
        Card c = engine.catalogCard(id);
        if (c != null) p.addToHand(c);
    }

    private void summon(Player p, String id) {
        if (engine.catalogCard(id) instanceof MinionCard mc) engine.summon(p, mc);
    }

    /** Raijin: 12 damage, 1 at a time on random enemy characters, dealt as one hit per character. */
    private void raijin(EffectContext ctx) {
        List<Combatant> enemies = new ArrayList<>(ctx.enemy().getSide().getMinions());
        enemies.add(ctx.enemy().getHero());
        Map<Combatant, Integer> hits = new IdentityHashMap<>();
        for (int i = 0; i < 12; i++) hits.merge(enemies.get(engine.rng().nextInt(enemies.size())), 1, Integer::sum);
        for (Combatant c : enemies) if (hits.containsKey(c)) engine.dealDamage(ctx.source(), c, hits.get(c));
    }

    /** Shike: the minion loses its keywords and its effects (Banzai, Seppuku, passives) for good. */
    private void silence(Minion m) {
        silenced.add(m);
        m.clearKeywords();
        asleep.remove(m);
        m.removeStatus(Status.SLEEP);
    }

    // ---------- events ----------

    void onEvent(GameEvent e) {
        switch (e.type()) {
            case MINION_DIED -> hakahori((Player) e.source(), (Minion) e.target());
            case ATTACK -> yogake(e.source());
            case TURN_ENDED -> wake((Player) e.source());
            default -> { }
        }
        refreshAuras();
    }

    /** Hakahori: +2 ATK when another friendly minion dies. */
    private void hakahori(Player owner, Minion dead) {
        for (Minion m : owner.getSide().getMinions()) if (m != dead && active(m, "hakahori")) m.buff(2, 0);
    }

    /** Fūma Kotarō, Yogake: when it reveals itself by attacking, each enemy minion attacks another random enemy minion. */
    private void yogake(Object attacker) {
        if (!(attacker instanceof Minion f) || !f.isHidden() || !active(f, "fuma-kotaro")) return;
        Player enemy = engine.getState().opponentOf(engine.ownerOf(f));
        List<Minion> theirs = List.copyOf(enemy.getSide().getMinions());
        if (theirs.size() < 2) return;
        for (Minion a : theirs) {
            if (a.isDead()) continue;
            List<Minion> others = theirs.stream().filter(b -> b != a && !b.isDead()).toList();
            if (others.isEmpty()) break;
            engine.fight(a, others.get(engine.rng().nextInt(others.size())));
        }
    }

    /** Jorōgumo, Oyasumi, Otoshiana: the minion falls asleep until the end of its owner's turn. */
    void sleep(Minion m) {
        if (m == null || m.isDead()) return;
        m.addStatus(Status.SLEEP);
        asleep.add(m);
    }

    /** Jorōgumo: the sleeper wakes at the end of its owner's turn. */
    private void wake(Player p) {
        for (Minion m : List.copyOf(asleep)) {
            if (p.getSide().slotOf(m) >= 0 || m.isDead()) {
                m.removeStatus(Status.SLEEP);
                asleep.remove(m);
            }
        }
    }

    /** Ronin (+6/+6 while it is your only minion) and Jōnin (your other Shinobi minions +3 ATK). */
    void refreshAuras() {
        Set<Minion> onBoard = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Player p : List.of(engine.getState().getFirst(), engine.getState().getSecond())) {
            List<Minion> side = p.getSide().getMinions();
            for (Minion m : side) {
                if (m.isDead()) continue;
                onBoard.add(m);
                int wantAtk = 0, wantHp = 0;
                if (active(m, "ronin") && side.size() == 1) { wantAtk += 6; wantHp += 6; }
                if (m.hasKeyword(Keyword.SHINOBI)) {
                    for (Minion j : side) if (j != m && active(j, "jonin")) wantAtk += 3;
                }
                int[] now = auras.getOrDefault(m, new int[2]);
                int dAtk = wantAtk - now[0], dHp = wantHp - now[1];
                if (dAtk == 0 && dHp == 0) continue;
                if (dAtk > 0 || dHp > 0) m.buff(Math.max(dAtk, 0), Math.max(dHp, 0));
                if (dAtk < 0 || dHp < 0) m.unbuff(Math.max(-dAtk, 0), Math.max(-dHp, 0));
                auras.put(m, new int[] {wantAtk, wantHp});
            }
        }
        auras.keySet().retainAll(onBoard);
    }
}
