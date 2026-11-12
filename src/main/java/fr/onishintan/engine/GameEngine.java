package fr.onishintan.engine;

import fr.onishintan.model.Card;
import fr.onishintan.model.ChessClock;
import fr.onishintan.model.Combatant;
import fr.onishintan.model.Hero;
import fr.onishintan.model.Minion;
import fr.onishintan.model.MinionCard;
import fr.onishintan.model.Player;
import fr.onishintan.model.Shrine;
import fr.onishintan.model.ShrineCard;
import fr.onishintan.model.SpellCard;
import fr.onishintan.model.Oath;
import fr.onishintan.model.OathType;
import fr.onishintan.model.TrapCard;
import fr.onishintan.model.TrapTrigger;
import fr.onishintan.model.Weapon;
import fr.onishintan.model.WeaponCard;
import fr.onishintan.model.HeroClass;
import fr.onishintan.model.KatanaTier;
import fr.onishintan.model.Rarity;
import fr.onishintan.model.Status;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.LongSupplier;

/**
 * The only entry point the UI uses. Every rule goes through here, and every change
 * is announced to the listeners as a GameEvent.
 * Methods marked TODO are agreed signatures, filled in later branches.
 */
public class GameEngine {

    private final GameState state;
    private final TurnManager turns;
    private final CombatResolver combat;
    private final EventBus bus = new EventBus();
    private final CardEffects cardEffects = new CardEffects();
    private final List<GameListener> listeners = new ArrayList<>();
    private final TurnTracker turnTracker = new TurnTracker();
    private final OathRules oathRules = new OathRules();

    private LongSupplier now = System::currentTimeMillis;   // replaced in tests

    /** Tests drive the clocks with a fake time. */
    public void setTimeSource(LongSupplier now) {
        this.now = Objects.requireNonNull(now);
    }


    public GameEngine(Player first, Player second, Random rng) {
        this.state = new GameState(first, second);
        this.turns = new TurnManager(state, Objects.requireNonNull(rng));
        this.combat = new CombatResolver(state, this::publish);

        // Rule handlers: they run before the UI hears the event
        bus.subscribe(GameEvent.Type.MINION_DIED,
                e -> runTrigger(Trigger.SEPPUKU, (Player) e.source(), (Minion) e.target(), null));
        bus.subscribe(GameEvent.Type.TURN_STARTED,
                e -> runForBoard(Trigger.TURN_START, (Player) e.source()));
        bus.subscribe(GameEvent.Type.TURN_ENDED,
                e -> runForBoard(Trigger.TURN_END, (Player) e.source()));
        bus.subscribe(GameEvent.Type.TURN_STARTED,
             e -> turnTracker.reset());
        bus.subscribe(GameEvent.Type.CARD_PLAYED,
             e -> turnTracker.cardPlayed((Card) e.target()));
        bus.subscribe(GameEvent.Type.TURN_ENDED,
             e -> tickShrines((Player) e.source()));
    }

    // ---------- listeners ----------

    public void addListener(GameListener listener) {
        listeners.add(Objects.requireNonNull(listener));
    }

    public void removeListener(GameListener listener) {
        listeners.remove(listener);
    }

    void publish(GameEvent event) {
        bus.publish(event);
        for (GameListener l : List.copyOf(listeners)) l.onEvent(event, state);
    }

    // ---------- match flow ----------

    /** Shuffles and deals 4 / 5 cards. The mulligan comes next, then beginFirstTurn(). */
    public void startMatch() {
        turns.startMatch();
        publish(GameEvent.of(GameEvent.Type.MATCH_STARTED));
    }

    /** After the mulligan: the first player's turn 1 starts. */
    public void beginFirstTurn() {
        Player p = state.active();
        turns.startTurn().forEach(card -> publishDraw(p, card));
        publish(GameEvent.of(GameEvent.Type.TURN_STARTED, p, null));
        p.getClock().start(now.getAsLong());
    }

    /** Ends the active turn (end-of-turn effects, then the Oath) and starts the opponent's. */
    public void endTurn() {
        Player ending = state.active();
        ending.getClock().pause(now.getAsLong());
        ending.getClock().add(ChessClock.INCREMENT_MS);
        publish(GameEvent.of(GameEvent.Type.TURN_ENDED, ending, null));
        resolveOath(ending);
        if (state.isOver()) return;           // an Oath break can kill its own hero
        List<Optional<Card>> drawn = turns.endTurn();
        Player next = state.active();
        drawn.forEach(card -> publishDraw(next, card));
        publish(GameEvent.of(GameEvent.Type.TURN_STARTED, next, null));
        next.getClock().start(now.getAsLong());
    }

    /** Time left in a player's bank, in ms (the UI shows it). */
    public long remainingMs(Player p) {
        return p.getClock().remaining(now.getAsLong());
    }

    /**
     * The UI calls this about every 200 ms. Bank empty: the active hero takes
     * half his HP (rounded up), gets +30 s, and his turn ends.
     * @return true if a timeout happened
     */
    public boolean checkClock() {
        if (state.isOver()) return false;
        Player p = state.active();
        if (p.getClock().remaining(now.getAsLong()) > 0) return false;
        int damage = (p.getHero().getHealth() + 1) / 2;
        p.getClock().add(ChessClock.TIMEOUT_BONUS_MS);
        publish(new GameEvent(GameEvent.Type.CLOCK_TIMEOUT, p, p.getHero(), damage));
        dealDamage(p.getClock(), p.getHero(), damage);
        if (!state.isOver()) endTurn();
        return true;
    }

    /** Drag a card onto the deck: once per turn, cost Player.TRADE_COST. */
    public boolean trade(Card card) {
        Player p = state.active();
        boolean ok = turns.swapCard(card);
        if (ok) publish(GameEvent.of(GameEvent.Type.CARD_TRADED, p, card));
        return ok;
    }

    
    // ---------- oaths (PvP only) ----------

    /** Before the mulligan. False if already sworn or the Oath is not of the hero's class. */
    public boolean swearOath(Player p, OathType type) {
        if (p.getOath() != null || type.getHeroClass() != p.getHero().getHeroClass()) return false;
        Oath oath = new Oath(type);
        p.setOath(oath);
        publish(GameEvent.of(GameEvent.Type.OATH_SWORN, p, oath));
        return true;
    }

    /** After 2 turns of Oathbreaker: the same Oath again, from 0 stacks. */
    public boolean reswearOath(Player p) {
        Oath oath = p.getOath();
        if (oath == null || oath.getState() != Oath.State.LAPSED) return false;
        oath.reswear();
        publish(GameEvent.of(GameEvent.Type.OATH_SWORN, p, oath));
        return true;
    }

    /** End of the owner's turn: Oathbreaker countdown, or the rule check. */
    private void resolveOath(Player p) {
        Oath oath = p.getOath();
        if (oath == null) return;

        if (oath.getState() == Oath.State.OATHBREAKER) {
            if (oath.tickOathbreaker()) publish(GameEvent.of(GameEvent.Type.OATH_RESTORED, p, oath));
            return;
        }
        if (!oath.isSworn()) return;

        switch (oathRules.get(oath.getType()).check(p, turnTracker, state)) {
            case KEPT -> {
                if (oath.addStack()) {
                    int stacks = oath.getStacks();
                    publish(new GameEvent(GameEvent.Type.OATH_STACK, p, oath, stacks));
                    if (stacks % 3 == 0) publish(new GameEvent(GameEvent.Type.OATH_REWARD, p, oath, stacks));
                }
            }
            case BROKEN -> {
                int damage = oath.getType().getBreakBase() + oath.breakOath();
                publish(new GameEvent(GameEvent.Type.OATH_BROKEN, p, oath, damage));
                dealDamage(oath, p.getHero(), damage);
            }
            case UNABLE -> { }   // no stack, the Oath holds
        }
    }

    // ---------- combat ----------

    /** Enemy characters this attacker may hit (Shirodu, Shinobi, Stealth). */
    public List<Combatant> legalTargets(Combatant attacker) {
        return combat.legalTargets(attacker);
    }

    /**
     * Attack; the defender's traps go off first (they may kill the attacker),
     * then publishes ATTACK, DAMAGE, WEAPON_BROKEN, MINION_DIED, then GAME_OVER if a hero died.
     */
    public boolean attack(Combatant attacker, Combatant target) {
        TrapTrigger when = target instanceof Hero
                ? TrapTrigger.ON_ENEMY_ATTACK_HERO : TrapTrigger.ON_ENEMY_ATTACK_MINION;
        triggerTraps(state.opponent(), when, attacker);
        if (state.isOver()) return false;
        boolean ok = combat.resolve(attacker, target);
        if (ok) checkGameOver();
        return ok;
    }

    // ---------- playing cards ----------

    /** True if the active player has the card in hand and enough Ki. */
    public boolean canPlay(Card card) {
        Player p = state.active();
        return card != null && p.getHand().contains(card) && p.canAfford(card.getKiCost());
    }

    /**
     * slot 0-6 for a minion (ignored otherwise), target may be null.
     * A trap is set face down and publishes TRAP_SET only.
     * Otherwise publishes CARD_PLAYED, then MINION_SUMMONED / SPELL_CAST / WEAPON_EQUIPPED,
     * runs Banzai and the enemy's traps, removes the dead and checks game over.
     */
    public boolean playCard(Card card, int slot, Combatant target) {
        if (state.isOver() || !canPlay(card)) return false;
        Player p = state.active();

        // A minion needs its chosen slot to be free: check before spending anything
        if ((card instanceof MinionCard || card instanceof ShrineCard) && !p.getSide().isFree(slot)) {            return false;
        }

        // A trap is set face down: TRAP_SET instead of CARD_PLAYED. Only its owner's UI shows the card
        if (card instanceof TrapCard trap) {
            if (!p.canSetTrap(trap)) return false;
            p.spendKi(trap.getKiCost());
            p.removeFromHand(trap);
            p.setTrap(trap);
            turnTracker.trapSet(trap);
            publish(new GameEvent(GameEvent.Type.TRAP_SET, p, trap, p.trapCount()));
            return true;
        }

        p.spendKi(card.getKiCost());
        p.removeFromHand(card);
        publish(GameEvent.of(GameEvent.Type.CARD_PLAYED, p, card));

        if (card instanceof MinionCard mc) {
            Minion m = new Minion(mc);
            p.getSide().place(m, slot);
            publish(GameEvent.of(GameEvent.Type.MINION_SUMMONED, p, m));
            runTrigger(Trigger.BANZAI, p, m, target);
            triggerTraps(state.opponent(), TrapTrigger.ON_ENEMY_SUMMON, m);
        } else if (card instanceof WeaponCard wc) {
            p.getHero().equip(new Weapon(wc));
            publish(GameEvent.of(GameEvent.Type.WEAPON_EQUIPPED, p, p.getHero()));
            } else if (card instanceof ShrineCard sc) {
            Shrine shrine = new Shrine(sc);
            p.getSide().place(shrine, slot);
            publish(new GameEvent(GameEvent.Type.SHRINE_PLACED, p, shrine, slot));
        } else if (card instanceof SpellCard) {
            publish(GameEvent.of(GameEvent.Type.SPELL_CAST, p, card));
            triggerTraps(state.opponent(), TrapTrigger.ON_ENEMY_SPELL, null);
            cardEffects.get(card.getId(), Trigger.BANZAI)
                    .ifPresent(effect -> effect.apply(new EffectContext(this, p, null, target)));
        }

        combat.removeDead();
        checkGameOver();
        return true;
    }

    public static final int HERO_POWER_COST = 2;     // GDD, proposed for Shuriken, Miko, Jikininki
    public static final int SHURIKEN_DAMAGE = 6;
    public static final int JIKININKI_DRAIN = 4;
    static final MinionCard MIKO = new MinionCard("power-miko", "Miko", "巫女", "みこ", 0,
            Rarity.COMMON, HeroClass.KANNUSHI, "", "", 3, 3, null);

    /**
     * Ninja Shuriken (6 damage), Kannushi Miko (3/3 in the leftmost free slot),
     * Oni Jikininki (drain 4, heal own hero). Samurai: useKatana. Once per turn.
     */
    public boolean useHeroPower(Combatant target) {
        Player p = state.active();
        if (state.isOver() || turns.isPowerUsedThisTurn() || !p.canAfford(HERO_POWER_COST)) return false;
        switch (p.getHero().getHeroClass()) {
            case NINJA -> {
                if (!isPowerTarget(target)) return false;
                payPower(p, HERO_POWER_COST);
                int dealt = target.takeDamage(SHURIKEN_DAMAGE);
                publish(new GameEvent(GameEvent.Type.DAMAGE, p.getHero(), target, dealt));
            }
            case KANNUSHI -> {
                int slot = p.getSide().firstFree();
                if (slot < 0) return false;
                payPower(p, HERO_POWER_COST);
                Minion miko = new Minion(MIKO);
                p.getSide().place(miko, slot);
                publish(GameEvent.of(GameEvent.Type.MINION_SUMMONED, p, miko));
                triggerTraps(state.opponent(), TrapTrigger.ON_ENEMY_SUMMON, miko);
            }
            case ONI -> {
                if (!isPowerTarget(target)) return false;
                payPower(p, HERO_POWER_COST);
                int dealt = target.takeDamage(JIKININKI_DRAIN);
                publish(new GameEvent(GameEvent.Type.DAMAGE, p.getHero(), target, dealt));
                int healed = p.getHero().heal(dealt);
                publish(new GameEvent(GameEvent.Type.HEAL, p.getHero(), p.getHero(), healed));
            }
            default -> { return false; }   // SAMURAI picks a blade: useKatana(tier)
        }
        combat.removeDead();
        checkGameOver();
        return true;
    }

    /** Samurai power: pay the blade's Ki and equip it (replaces the current weapon). */
    public boolean useKatana(KatanaTier tier) {
        Player p = state.active();
        if (state.isOver() || tier == null || p.getHero().getHeroClass() != HeroClass.SAMURAI
                || turns.isPowerUsedThisTurn() || !p.canAfford(tier.getKiCost())) return false;
        payPower(p, tier.getKiCost());
        p.getHero().equip(new Weapon(tier.toCard()));
        publish(GameEvent.of(GameEvent.Type.WEAPON_EQUIPPED, p, p.getHero()));
        return true;
    }

    public boolean isHeroPowerUsed() { return turns.isPowerUsedThisTurn(); }

    private void payPower(Player p, int cost) {
        p.spendKi(cost);
        turns.markPowerUsed();
        publish(new GameEvent(GameEvent.Type.HERO_POWER_USED, p, p.getHero(), cost));
    }

    /** Any living character on the board, except a hidden enemy minion or a stealthed enemy hero. */
    private boolean isPowerTarget(Combatant t) {
        if (t == null || t.isDead()) return false;
        Player enemy = state.opponent();
        if (t instanceof Minion m) {
            if (enemy.getSide().slotOf(m) >= 0) return !m.isHidden();
            return state.active().getSide().slotOf(m) >= 0;
        }
        if (t == enemy.getHero()) return !t.hasStatus(Status.STEALTH);
        return t == state.active().getHero();
    }

    // ---------- triggers and effects ----------

    /** Runs the effect a minion has for this trigger, if any. */
    void runTrigger(Trigger trigger, Player owner, Minion source, Combatant target) {
        if (source == null) return;
        cardEffects.get(source.getSource().getId(), trigger)
                .ifPresent(effect -> effect.apply(new EffectContext(this, owner, source, target)));
    }

    /** Runs a trigger for every minion on that player's side (copy: the board may change). */
    private void runForBoard(Trigger trigger, Player owner) {
        for (Minion m : List.copyOf(owner.getSide().getMinions())) {
            runTrigger(trigger, owner, m, null);
        }
    }

    /** End of the owner's turn: Shrines lose a turn, the expired ones leave the board. */
    private void tickShrines(Player owner) {
        for (Shrine s : owner.getSide().tickShrines()) {
            publish(GameEvent.of(GameEvent.Type.SHRINE_EXPIRED, owner, s));
        }
    }

    /** One trap per action: the first of the owner's traps waiting for it goes off, then is gone. */
    private void triggerTraps(Player owner, TrapTrigger when, Combatant target) {
        Optional<TrapCard> trap = owner.takeTrap(when);
        if (trap.isEmpty()) return;
        publish(GameEvent.of(GameEvent.Type.TRAP_TRIGGERED, owner, trap.get()));
        cardEffects.get(trap.get().getId(), Trigger.TRAP)
                .ifPresent(effect -> effect.apply(new EffectContext(this, owner, null, target)));
        combat.removeDead();
        checkGameOver();
    }

    /** Effect damage: publishes DAMAGE, removes the dead, checks game over. */
    void dealDamage(Object source, Combatant target, int amount) {
        if (target == null || amount <= 0) return;
        int taken = target.takeDamage(amount);
        publish(new GameEvent(GameEvent.Type.DAMAGE, source, target, taken));
        combat.removeDead();
        checkGameOver();
    }

    void heal(Combatant target, int amount) {
        int restored = target.heal(amount);
        if (restored > 0) publish(new GameEvent(GameEvent.Type.HEAL, null, target, restored));
            }

    void gainArmour(Player owner, int amount) {
        Hero hero = owner.getHero();
        hero.gainArmour(amount);
        publish(new GameEvent(GameEvent.Type.ARMOUR_GAINED, owner, hero, amount));
    }

    void draw(Player p) {
        publishDraw(p, p.draw());
    }

    public CardEffects getCardEffects() { return cardEffects; }
    public EventBus getBus() { return bus; }

    // ---------- reading ----------

    public GameState getState() { return state; }
    public Player active() { return state.active(); }
    public boolean isSwapUsedThisTurn() { return turns.isSwapUsedThisTurn(); }
    public TurnTracker getTurnTracker() { return turnTracker; }

    // ---------- helpers ----------

    private void publishDraw(Player p, Optional<Card> drawn) {
        if (drawn.isPresent()) {
            publish(GameEvent.of(GameEvent.Type.CARD_DRAWN, p, drawn.get()));
        } else if (!p.getDeck().isEmpty() || p.getHand().size() >= Player.MAX_HAND) {
            publish(GameEvent.of(GameEvent.Type.CARD_BURNED, p, null));
        }
    }

    /** Called after every action that can kill a hero (combat, spells). */
    void checkGameOver() {
        if (state.checkGameOver()) {
            publish(GameEvent.of(GameEvent.Type.GAME_OVER, null, state.getWinner().orElse(null)));
        }
    }
}