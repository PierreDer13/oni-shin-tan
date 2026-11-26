package fr.onishintan.engine.system;

import fr.onishintan.engine.GameEngine;
import fr.onishintan.engine.GameState;
import fr.onishintan.engine.event.GameEvent;
import fr.onishintan.model.Combatant;
import fr.onishintan.model.GodaiElement;
import fr.onishintan.model.Keyword;
import fr.onishintan.model.HeroClass;
import fr.onishintan.model.KatanaTier;
import fr.onishintan.model.Minion;
import fr.onishintan.model.Player;
import fr.onishintan.model.TrapTrigger;
import fr.onishintan.model.Weapon;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import static fr.onishintan.engine.GameEngine.HERO_POWER_COST;
import static fr.onishintan.engine.GameEngine.JIKININKI_DRAIN;
import static fr.onishintan.engine.GameEngine.MIKO;
import static fr.onishintan.engine.GameEngine.MIKO_BONUS_EVERY;
import static fr.onishintan.engine.GameEngine.MIKO_ID;
import static fr.onishintan.engine.GameEngine.SHURIKEN_DAMAGE;

/** The hero powers: Shuriken (Ninja), Miko (Kannushi), Jikininki (Oni), Yatsu-kubi (Orochi), the katanas (Samurai). */
public class HeroPowers {

    private final GameEngine engine;

    /** Godai numbers: Earth's HP and armour, Water's heal, Fire's damage, Wind's ATK. */
    public static final int GODAI_EARTH_HP = 4, GODAI_ARMOUR = 4, GODAI_HEAL = 8, GODAI_FIRE = 6, GODAI_WIND_ATK = 3;
    /** The players whose hero power is Godai for the rest of the match (Takeda Shingen). */
    private final Set<Player> godai = Collections.newSetFromMap(new IdentityHashMap<>());

    public HeroPowers(GameEngine engine) {
        this.engine = engine;
    }

    private GameState state() {
        return engine.getState();
    }

    /**
     * Ninja Shuriken (6 damage), Kannushi Miko (3/3 in the leftmost free slot),
     * Oni Jikininki (drain 4, heal own hero), Orochi Yatsu-kubi (8 x 1 damage, random enemies).
     * Samurai: useKatana. Once per turn.
     */
    public boolean useHeroPower(Combatant target) {
        Player p = state().active();
        if (hasGodai(p)) return false;   // Takeda Shingen: this player's power is Godai now (useGodai)
        if (state().isOver() || engine.turns().isPowerUsedThisTurn() || !p.canAfford(HERO_POWER_COST)) return false;
        switch (p.getHero().getHeroClass()) {
            case NINJA -> {
                if (!engine.targeting().isPowerTarget(target)) return false;
                payPower(p, HERO_POWER_COST);
                int dealt = target.takeDamage(engine.getRewards().effectDamage(p, false, target, SHURIKEN_DAMAGE + engine.ryu(p) + engine.getMinionEffects().shurikenBonus(p)));
                engine.publish(new GameEvent(GameEvent.Type.DAMAGE, p.getHero(), target, dealt));
            }
            case KANNUSHI -> {
                int slot = p.getSide().firstFree();
                if (slot < 0) return false;
                payPower(p, HERO_POWER_COST);
                Minion miko = new Minion(MIKO);
                p.getSide().place(miko, slot);
                engine.publish(GameEvent.of(GameEvent.Type.MINION_SUMMONED, p, miko));
                engine.traps().trigger(state().opponent(), TrapTrigger.ON_ENEMY_SUMMON, miko);
                p.addMikoCall();
                if (p.getMikoCalls() % MIKO_BONUS_EVERY == 0) {
                    p.getSide().getMinions().stream().filter(HeroPowers::isMiko).forEach(m -> m.buff(1, 1));
                }
            }
            case ONI -> {
                if (!engine.targeting().isPowerTarget(target)) return false;
                payPower(p, HERO_POWER_COST);
                int dealt = target.takeDamage(engine.getRewards().effectDamage(p, false, target, JIKININKI_DRAIN + engine.ryu(p)));
                engine.publish(new GameEvent(GameEvent.Type.DAMAGE, p.getHero(), target, dealt));
                int healed = p.getHero().heal(dealt);
                engine.publish(new GameEvent(GameEvent.Type.HEAL, p.getHero(), p.getHero(), healed));
            }
            case OROCHI -> {
                payPower(p, HERO_POWER_COST);
                engine.orochi().yatsuKubi(p);
            }
            default -> { return false; }   // SAMURAI picks a blade: useKatana(tier)
        }
        engine.combat().removeDead();
        engine.checkGameOver();
        return true;
    }

    /** Samurai power: pay the blade's Ki and equip it (replaces the current weapon). */
    public boolean useKatana(KatanaTier tier) {
        Player p = state().active();
        if (state().isOver() || tier == null || hasGodai(p) || p.getHero().getHeroClass() != HeroClass.SAMURAI
                || engine.turns().isPowerUsedThisTurn() || !p.canAfford(tier.getKiCost())) return false;
        payPower(p, tier.getKiCost());
        p.getHero().equip(new Weapon(tier.toCard()));
        engine.publish(GameEvent.of(GameEvent.Type.WEAPON_EQUIPPED, p, p.getHero()));
        return true;
    }

    /** Takeda Shingen: p's hero power becomes Godai for the rest of the match (GODAI_GAINED, once). */
    public void grantGodai(Player p) {
        if (godai.add(p)) engine.publish(GameEvent.of(GameEvent.Type.GODAI_GAINED, p, p.getHero()));
    }

    /** True once p's hero power is Godai. */
    public boolean hasGodai(Player p) {
        return godai.contains(p);
    }

    /**
     * True if this element may land on t for the active player. Earth: a friendly minion, or null (armour alone) when he has none.
     * Water: a friendly character. Fire: an enemy character the powers can reach. Wind: a friendly minion. Void: any reachable minion.
     */
    public boolean isGodaiTarget(GodaiElement element, Combatant t) {
        Player me = state().active();
        boolean friendlyMinion = t instanceof Minion m && me.getSide().slotOf(m) >= 0 && !m.isDead();
        return switch (element) {
            case CHI -> t == null ? me.getSide().getMinions().isEmpty() : friendlyMinion;
            case SUI -> friendlyMinion || t == me.getHero();
            case KA -> t != null && !friendlyMinion && t != me.getHero() && engine.targeting().isPowerTarget(t);
            case FU -> friendlyMinion;
            case KU -> t instanceof Minion && engine.targeting().isPowerTarget(t);
        };
    }

    /** Godai: the chosen element on its target, for HERO_POWER_COST Ki, once per turn. */
    public boolean useGodai(GodaiElement element, Combatant target) {
        Player p = state().active();
        if (state().isOver() || element == null || !hasGodai(p) || engine.turns().isPowerUsedThisTurn()
                || !p.canAfford(HERO_POWER_COST) || !isGodaiTarget(element, target)) return false;
        payPower(p, HERO_POWER_COST);
        switch (element) {
            case CHI -> {                                   // Earth: a wall, and armour for the hero
                if (target instanceof Minion m) {
                    m.buff(0, GODAI_EARTH_HP);
                    m.giveKeyword(Keyword.SHIRODU);
                }
                engine.gainArmour(p, GODAI_ARMOUR);
            }
            case SUI -> {                                   // Water: heal, then draw
                engine.heal(target, GODAI_HEAL);
                engine.draw(p);
            }
            case KA -> engine.dealDamage(p.getHero(), target, GODAI_FIRE);   // Fire
            case FU -> {                                    // Wind: stronger, and it may strike again
                Minion m = (Minion) target;
                m.buff(GODAI_WIND_ATK, 0);
                m.giveKeyword(Keyword.TATAKAE);
                m.refreshAttack();
            }
            case KU -> {                                    // Void: emptied, back to its printed card
                Minion m = (Minion) target;
                engine.getMinionEffects().silence(m);
                m.resetToPrinted();
            }
        }
        engine.combat().removeDead();
        engine.checkGameOver();
        return true;
    }

    private void payPower(Player p, int cost) {
        p.spendKi(cost);
        engine.turns().markPowerUsed();
        engine.publish(new GameEvent(GameEvent.Type.HERO_POWER_USED, p, p.getHero(), cost));
    }

    public static boolean isMiko(Minion m) {
        return MIKO_ID.equals(m.getSource().getId());
    }
}
