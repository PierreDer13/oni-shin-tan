package fr.onishintan.engine.effect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import fr.onishintan.model.Card;
import fr.onishintan.model.Hero;
import fr.onishintan.model.Keyword;
import fr.onishintan.model.Minion;
import fr.onishintan.model.MinionCard;
import fr.onishintan.model.Player;
import fr.onishintan.engine.GameEngine;
import fr.onishintan.engine.event.GameEvent;

/**
 * The 17 plain class spells (GDD spell table, ids from cards.json): what each one is aimed at,
 * when it can be cast, and its effect, registered in CardEffects under Trigger.BANZAI.
 */
public final class SpellEffects {

    static final int YOROI_ARMOUR = 6;
    static final int HARAE_HEAL = 15;
    static final int KISEKI_BUFF = 3;
    static final int KAGURA_MIKO = 3;

    /** One spell: its target, whether it can be cast now (besides Ki and target), and its effect. */
    public record Rule(SpellTarget target, Predicate<EffectContext> castable, Effect effect) {
        Rule(SpellTarget target, Effect effect) { this(target, ctx -> true, effect); }
    }

    private static final Map<String, Rule> RULES = new HashMap<>();

    private SpellEffects() {}

    public static Rule rule(Card card) {
        return card == null ? null : RULES.get(card.getId());
    }

    /** Puts every spell effect in the engine's CardEffects. */
    public static void register(CardEffects effects) {
        RULES.forEach((id, r) -> effects.register(id, Trigger.BANZAI, r.effect()));
    }

    private static void add(String id, Rule rule) { RULES.put(id, rule); }

    private static boolean hasWeapon(EffectContext ctx) { return ctx.owner().getHero().getWeapon() != null; }

    static {
        // ---------- Ninja ----------
        add("kakuremino", new Rule(SpellTarget.NONE,                    // all friendly minions Shinobi
                ctx -> ctx.owner().getSide().getMinions().forEach(m -> m.giveKeyword(Keyword.SHINOBI))));
        add("kemuri-dama", new Rule(SpellTarget.MINION,                 // any minion Shinobi; a hidden Shirodu stops guarding
                ctx -> ((Minion) ctx.target()).giveKeyword(Keyword.SHINOBI)));
        add("kuji-in", new Rule(SpellTarget.NONE,                       // the hero power can be used again this turn
                ctx -> ctx.engine().refreshHeroPower()));
        add("yoi", new Rule(SpellTarget.NONE,                           // the cards in hand now cost 1 less, no minimum
                ctx -> ctx.engine().discountHand(ctx.owner())));

        // ---------- Samurai ----------
        add("chiburi", new Rule(SpellTarget.NONE, SpellEffects::hasWeapon,
                ctx -> ctx.owner().getHero().getWeapon().addDurability(1)));
        add("iaido", new Rule(SpellTarget.NONE, SpellEffects::hasWeapon, ctx -> {   // weapon gone, its ATK to all minions
            Hero hero = ctx.owner().getHero();
            int atk = hero.getWeapon().getAttack();
            hero.destroyWeapon();
            ctx.engine().publish(GameEvent.of(GameEvent.Type.WEAPON_BROKEN, hero, null));
            for (Player p : List.of(ctx.owner(), ctx.enemy())) {
                for (Minion m : List.copyOf(p.getSide().getMinions())) ctx.engine().dealDamage(null, m, atk);
            }
        }));
        add("ketto", new Rule(SpellTarget.TWO_MINIONS, ctx -> ctx.engine().duel((Minion) ctx.target(), (Minion) ctx.second())));
        add("yoroi", new Rule(SpellTarget.FRIENDLY_MINION, ctx -> {    // 6 armour to your hero and a friendly minion
            ctx.engine().gainArmour(ctx.owner(), YOROI_ARMOUR);
            ctx.engine().gainArmour(ctx.owner(), (Minion) ctx.target(), YOROI_ARMOUR);
        }));

        // ---------- Kannushi ----------
        add("harae", new Rule(SpellTarget.CHARACTER, ctx -> {           // heal 15 and draw
            ctx.engine().heal(ctx.target(), HARAE_HEAL);
            ctx.engine().draw(ctx.owner());
        }));
        add("kiseki", new Rule(SpellTarget.NONE, ctx -> {              // heal all friendly minions; one already full gets +3/+3
            for (Minion m : List.copyOf(ctx.owner().getSide().getMinions())) {
                if (m.getHealth() >= m.getMaxHealth()) m.buff(KISEKI_BUFF, KISEKI_BUFF);
                else ctx.engine().heal(m, m.getMaxHealth());
            }
        }));
        add("senbon-torii", new Rule(SpellTarget.NONE,
                ctx -> ctx.owner().getSide().getMinions().forEach(m -> m.giveKeyword(Keyword.TORII))));
        add("taisha-zukuri", new Rule(SpellTarget.FRIENDLY_MINION,
                ctx -> ((Minion) ctx.target()).giveKeyword(Keyword.TORII)));
        add("kagura", new Rule(SpellTarget.NONE, ctx -> !ctx.owner().getSide().isFull(), ctx -> {   // 3 Miko with Tatakae
            for (int i = 0; i < KAGURA_MIKO; i++) {
                Minion miko = ctx.engine().summon(ctx.owner(), GameEngine.MIKO);
                if (miko == null) break;
                miko.giveKeyword(Keyword.TATAKAE);
            }
        }));

        // ---------- Oni ----------
        add("kuchiyose", new Rule(SpellTarget.NONE,                    // a random dead friendly minion, its Banzai again
                ctx -> !ctx.engine().graveyard(ctx.owner()).isEmpty() && !ctx.owner().getSide().isFull(),
                ctx -> ctx.engine().resurrectRandom(ctx.owner())));
        add("kamikakushi", new Rule(SpellTarget.MINION,                // back into its owner's deck
                ctx -> ctx.engine().shuffleIntoDeck((Minion) ctx.target())));
        add("sakasama", new Rule(SpellTarget.NONE,                     // swap ATK and HP of all enemy minions and their deck copies
                ctx -> ctx.engine().swapEnemyStats(ctx.enemy())));
        add("tamashii-gui", new Rule(SpellTarget.MINION, ctx -> {      // destroy a minion and a random enemy card in hand
            ctx.engine().destroy((Minion) ctx.target());
            ctx.engine().discardRandom(ctx.enemy());
        }));
    }

    /** Sakasama on a printed card: ATK and HP swapped (HP at least 1, ATK at most the 20 cap). */
    public static MinionCard swapped(MinionCard c) {
        return new MinionCard(c.getId(), c.getNameRomaji(), c.getNameKanji(), c.getNameKana(), c.getKiCost(),
                c.getRarity(), c.getHeroClass(), c.getEffectText(), c.getEffectTextJp(),
                Math.min(MinionCard.MAX_BASE_STAT, c.getBaseHealth()), Math.max(1, c.getBaseAttack()), c.getKeywords());
    }
}