package fr.onishintan.engine;

import fr.onishintan.adventure.Upgrade;
import fr.onishintan.model.Card;
import fr.onishintan.model.ChessClock;
import fr.onishintan.model.Combatant;
import fr.onishintan.model.Deck;
import fr.onishintan.model.Hero;
import fr.onishintan.model.Minion;
import fr.onishintan.model.MinionCard;
import fr.onishintan.model.Player;
import fr.onishintan.model.RiteCard;
import fr.onishintan.model.Keyword;
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
import fr.onishintan.model.CurseCard;
import fr.onishintan.model.CurseZone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
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
    private final OathRules oathRules;
    private final OathPenalties penalties = new OathPenalties(this);    
    private final OathRewards rewards = new OathRewards(this);
    private final MinionEffects minionEffects = new MinionEffects(this);
    private final FamilyEffects familyEffects = new FamilyEffects(this, minionEffects);
    /** Kawarimi: the minion an attack is turned onto by a trap, null otherwise. */
    private Minion redirected;
    private final Map<Player, Integer> fatigue = new HashMap<>();   // empty-deck draws so far, per player
    private List<GameEvent> heard;          // events waiting for the listeners while a rule handler runs (publish)
    /** Otoshiana: a trap cancelled the attack being resolved. */
    private boolean attackCancelled;
    /** Every card the match may create by id (tokens, Curses): set by the UI, later by cards.json. */
    private final Map<String, Card> catalog = new HashMap<>();
    /** The holder of the hand Curse acting right now (fireCurse), null otherwise. */
    private Player cursed;
    /** Kuchiyose: the cards of each player's dead minions, in death order. */
    private final Map<Player, List<MinionCard>> graveyards = new HashMap<>();
    /** Yōi: Ki taken off the cards each player had in hand, by card. */
    private final Map<Player, Map<Card, Integer>> handDiscounts = new HashMap<>();
    /** Adventure upgrades of each player (Inochi, Chikara, Hayasa): none outside the Adventure. */
    private final Map<Player, Set<Upgrade>> upgrades = new HashMap<>();
    /** True while a spell card's effect is resolving (Chikara: +3 spell damage). */
    private boolean castingSpell;
    /** Whose effect is resolving (a spell, a Banzai, a trigger, a trap): its damage gets that player's bonuses. */
    private Player effectOwner;
    private final Random rng;

    private LongSupplier now = System::currentTimeMillis;   // replaced in tests

    /** Tests drive the clocks with a fake time. */
    public void setTimeSource(LongSupplier now) {
        this.now = Objects.requireNonNull(now);
    }

    /** True once beginFirstTurn() ran: the clocks may start from then on. */
    private boolean turnsBegun;
    /** The players who already used their mulligan (once each, before the first turn). */
    private final Set<Player> mulliganDone = new HashSet<>();

    /** True while the table is hidden (Oath choice, hotseat pass veil): no clock runs. */
    private boolean clockPaused;

    /**
     * The clocks run only once both players have sworn an Oath (PvP).
     * Adventure has no Oath, so its matches have no clock.
     */
    public boolean clocksEnabled() {
        return state.getFirst().getOath() != null && state.getSecond().getOath() != null;
    }

    /** Hides the table (pass veil): the active clock stops, using the engine's time. */
    public void pauseClock() {
        clockPaused = true;
        state.active().getClock().pause(now.getAsLong());
    }

    /** The active player clicked the veil: his clock runs again. */
    public void resumeClock() {
        clockPaused = false;
        startClock(state.active());
    }

    /** Starts p's clock, unless the clocks are off (no Oaths) or paused. */
    private void startClock(Player p) {
        if (turnsBegun && clocksEnabled() && !clockPaused && !state.isOver()) p.getClock().start(now.getAsLong());
    }



    public GameEngine(Player first, Player second, Random rng) {
        this.state = new GameState(first, second);
        this.rng = Objects.requireNonNull(rng);
        this.turns = new TurnManager(state, this.rng);
        this.combat = new CombatResolver(state, this::publish);
        this.oathRules = new OathRules(combat);
        turns.setDrawRule(p -> !penalties.has(p, OathPenalties.Lasting.NO_DRAW));
        oathRules.setCost(this::costOf);
        SpellEffects.register(cardEffects);
        combat.setPiercing(c -> rewards.pierces(c) || minionEffects.pierces(c));
        combat.setDamageRule((source, target, amount) ->
                familyEffects.capDamage(target, rewards.combatDamage(ownerOf(source), target, amount)));   // Fushimi Inari


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
             e -> turnTracker.cardPlayed((Card) e.target(), e.amount()));
        bus.subscribe(GameEvent.Type.HERO_POWER_USED,
             e -> turnTracker.addKiSpent(e.amount()));
        // Kuchiyose reads the dead; a Yōi discount leaves with its card
        bus.subscribe(GameEvent.Type.MINION_DIED, e -> graveyards
             .computeIfAbsent((Player) e.source(), k -> new ArrayList<>()).add(((Minion) e.target()).getSource()));
        bus.subscribe(GameEvent.Type.CARD_PLAYED, e -> useDiscount((Player) e.source(), (Card) e.target()));
        bus.subscribe(GameEvent.Type.TRAP_SET, e -> useDiscount((Player) e.source(), (Card) e.target()));
        bus.subscribe(GameEvent.Type.TURN_ENDED, familyEffects::onTurnEnded);   // Heiwa no Torii, Itsukushima: before the tick
        // A Shrine loses 1 durability at the start of its owner's turn (not the turn it is placed)
        bus.subscribe(GameEvent.Type.TURN_STARTED,
             e -> tickShrines((Player) e.source()));        
        bus.subscribe(GameEvent.Type.TURN_ENDED,
             e -> runHandCurses((Player) e.source()));
        bus.subscribe(GameEvent.Type.CARD_DRAWN,
             e -> onCurseDrawn((Player) e.source(), (Card) e.target()));
        // What the Oath rules read at the end of the turn
        bus.subscribe(GameEvent.Type.ATTACK,
             e -> onAttack((Combatant) e.source(), (Combatant) e.target()));
        bus.subscribe(GameEvent.Type.DAMAGE,
             e -> onDamage(e.target(), e.amount()));
        bus.subscribe(GameEvent.Type.CURSE_GIVEN,
             e -> turnTracker.curseGiven());
        bus.subscribe(GameEvent.Type.TURN_STARTED,
             e -> turnTracker.clearHeroAttackers(state.opponentOf((Player) e.source())));
        // A Miko arrives with the bonus of every 3 Kannushi power uses so far
        bus.subscribe(GameEvent.Type.MINION_SUMMONED, e -> {
            if (e.source() instanceof Player p && e.target() instanceof Minion m && isMiko(m)) {
                int bonus = p.getMikoCalls() / MIKO_BONUS_EVERY;
                if (bonus > 0) m.buff(bonus, bonus);
            }
        });
        // Chikara: +3 ATK to every minion summoned and every weapon equipped
        bus.subscribe(GameEvent.Type.MINION_SUMMONED, e -> {
            if (e.source() instanceof Player p && e.target() instanceof Minion m && has(p, Upgrade.CHIKARA)) {
                m.buff(Upgrade.CHIKARA_BONUS, 0);
            }
        });
        bus.subscribe(GameEvent.Type.WEAPON_EQUIPPED, e -> {
            if (e.source() instanceof Player p && has(p, Upgrade.CHIKARA) && p.getHero().getWeapon() != null) {
                p.getHero().getWeapon().addAttack(Upgrade.CHIKARA_BONUS);
            }
        });
        // Oath rewards (3 and 6 stacks): last, so they see what the rule handlers recorded
        for (GameEvent.Type t : GameEvent.Type.values()) bus.subscribe(t, rewards::onEvent);
        for (GameEvent.Type t : GameEvent.Type.values()) bus.subscribe(t, minionEffects::onEvent);
        for (GameEvent.Type t : GameEvent.Type.values()) bus.subscribe(t, familyEffects::onEvent);
    }

    // ---------- listeners ----------

    public void addListener(GameListener listener) {
        listeners.add(Objects.requireNonNull(listener));
    }

    public void removeListener(GameListener listener) {
        listeners.remove(listener);
    }

    /**
     * The rule handlers run first; the listeners (UI) hear the events after them, in cause-then-consequence
     * order: an event comes before the events its handlers published (MINION_DIED before its Seppuku,
     * TURN_ENDED before the end-of-turn effects), and nothing is heard until the outermost event is handled.
     */
    void publish(GameEvent event) {
        List<GameEvent> outer = heard;
        heard = new ArrayList<>();
        heard.add(event);
        List<GameEvent> mine;
        try {
            bus.publish(event);
        } finally {
            mine = heard;
            heard = outer;
        }
        if (outer != null) {                       // published by a rule handler: heard after its cause
            outer.addAll(mine);
            return;
        }
        for (GameEvent e : mine) {
            for (GameListener l : List.copyOf(listeners)) l.onEvent(e, state);
        }
    }

    /** Honor, Peace and Justice: who attacked what this turn. */
    private void onAttack(Combatant attacker, Combatant target) {
        Player enemy = state.opponent();
        turnTracker.attacked(target);
        if (!(attacker instanceof Minion m)) return;
        if (target instanceof Hero) {
            turnTracker.minionHitHero(enemy.getSide().size() > 0);
            turnTracker.markHeroAttacker(enemy, m);
        } else if (combat.legalTargets(m).contains(enemy.getHero())) {
            turnTracker.minionSkippedHero();
        }
    }

    /** Havoc: damage taken by the enemy's hero and minions during this turn. */
    private void onDamage(Object target, int amount) {
        Player enemy = state.opponent();
        if (target == enemy.getHero() || (target instanceof Minion m && enemy.getSide().getMinions().contains(m))) {
            turnTracker.addDamage(amount);
        }
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
        if (state.isOver()) return;           // fatigue can kill
        publish(GameEvent.of(GameEvent.Type.TURN_STARTED, p, null));
        turnsBegun = true;
        startClock(p);
    }

    
    /**
     * Mulligan, once per player between startMatch() and beginFirstTurn(): the chosen cards of p's opening hand
     * are replaced. The new cards are drawn first, then the old ones go back into the deck at random places,
     * so a card sent back cannot come straight back. Cards not in p's hand are ignored; an empty list keeps the hand.
     * Publishes MULLIGAN_DONE (target = the new cards, amount = how many were replaced).
     * @return the new cards, or an empty list if the mulligan is refused (already done, or the first turn began)
     */
    public List<Card> mulligan(Player p, List<Card> toReplace) {
        if (p == null || turnsBegun || mulliganDone.contains(p)
                || (p != state.getFirst() && p != state.getSecond())) return List.of();
        mulliganDone.add(p);
        List<Card> back = new ArrayList<>();
        if (toReplace != null) {
            for (Card c : toReplace) if (c != null && !back.contains(c) && p.getHand().contains(c)) back.add(c);
        }
        List<Card> fresh = new ArrayList<>();
        for (int i = 0; i < back.size(); i++) p.getDeck().draw().ifPresent(fresh::add);
        for (Card c : back) {
            p.removeFromHand(c);
            p.getDeck().insertRandom(c, rng);
        }
        fresh.forEach(p::addToHand);
        publish(new GameEvent(GameEvent.Type.MULLIGAN_DONE, p, List.copyOf(fresh), back.size()));
        return fresh;
    }

    /** True once p used his mulligan (the UI hides the mulligan screen). */
    public boolean hasMulliganed(Player p) {
        return mulliganDone.contains(p);
    }

    /**
     * p gives up (Surrender button): the opponent wins at once, both clocks stop and GAME_OVER is published.
     * @param p the player who concedes (source of GAME_OVER)
     * @return false if the match was already over
     */
    public boolean concede(Player p) {
        if (state.isOver()) return false;
        Player winner = state.opponentOf(p);   // throws if p is not in this match
        long t = now.getAsLong();
        p.getClock().pause(t);
        winner.getClock().pause(t);
        state.declareWinner(winner);
        publish(GameEvent.of(GameEvent.Type.GAME_OVER, p, winner));
        return true;
    }

    /** Ends the active turn (end-of-turn effects, then the Oath) and starts the opponent's. */
    public void endTurn() {
        Player ending = state.active();
        ending.getClock().pause(now.getAsLong());
        if (clocksEnabled()) ending.getClock().add(ChessClock.INCREMENT_MS);
        publish(GameEvent.of(GameEvent.Type.TURN_ENDED, ending, null));
        rewards.endTurn(ending);              // end-of-turn Oath rewards (Allegiance 3, Peace 6...)
        if (state.isOver()) return;
        penalties.tick(ending);               // his lasting Oath penalties lose one of his turns
        resolveOath(ending);
        if (state.isOver()) return;           // an Oath break can kill its own hero
        List<Optional<Card>> drawn = turns.endTurn();
        Player next = state.active();
        drawn.forEach(card -> publishDraw(next, card));
        if (state.isOver()) return;           // fatigue can kill
        publish(GameEvent.of(GameEvent.Type.TURN_STARTED, next, null));
        startClock(next);
    }

    /** Time left in a player's bank, in ms (the UI shows it). */
    public long remainingMs(Player p) {
        return p.getClock().remaining(now.getAsLong());
    }

    /**
     * The UI calls this about every 200 ms. Bank empty: the active hero takes
     * half his HP (rounded up) and gets +30 s; his turn goes on.
     * @return true if a timeout happened
     */
    public boolean checkClock() {
        if (state.isOver()) return false;
        Player p = state.active();
        if (!p.getClock().isRunning()) return false;   // no Oaths yet, Adventure, or paused
        if (p.getClock().remaining(now.getAsLong()) > 0) return false;
        int damage = (p.getHero().getHealth() + 1) / 2;
        p.getClock().add(ChessClock.TIMEOUT_BONUS_MS);
        publish(new GameEvent(GameEvent.Type.CLOCK_TIMEOUT, p, p.getHero(), damage));
        dealDamage(p.getClock(), p.getHero(), damage);
        return true;
    }

    /** Drag a card onto the deck: once per turn, cost Player.TRADE_COST. */
    public boolean trade(Card card) {
        if (!isTradeable(card)) return false;
        Player p = state.active();
        boolean ok = turns.swapCard(card);
        if (ok) publish(GameEvent.of(GameEvent.Type.CARD_TRADED, p, card));
        return ok;
    }
    
    /** A Curse can never be traded: neither one of yours to give, nor one the enemy put in your hand. */
    public static boolean isTradeable(Card card) {
        return card != null && !(card instanceof CurseCard);
    }


    
    // ---------- oaths (PvP only) ----------

    /** Before the mulligan. False if already sworn or the Oath is not of the hero's class. */
    public boolean swearOath(Player p, OathType type) {
        if (p.getOath() != null || type.getHeroClass() != p.getHero().getHeroClass()) return false;
        Oath oath = new Oath(type);
        p.setOath(oath);
        publish(GameEvent.of(GameEvent.Type.OATH_SWORN, p, oath));
        startClock(state.active());   // the second Oath sworn: the active clock starts
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
                if (!state.isOver()) penalties.apply(p, oath);                
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
        if (state.isOver() || attacker == null || !combat.canAttack(attacker, target)) return false;   // an illegal attack springs no trap
        redirected = null;
        attackCancelled = false;
        triggerTraps(state.opponent(), when, attacker);
        if (state.isOver() || attacker.isDead()) return false;      // Shikomi andon may kill the attacker first
        if (attackCancelled) {                                      // Otoshiana: the attack is spent, nothing hits
            attacker.markAttacked();
            return true;
        }
        if (redirected != null && !redirected.isDead()) target = redirected;   // Kawarimi: the log takes the hit
        boolean ok = combat.resolve(attacker, target);
        if (ok) familyEffects.afterAttack(attacker);                // Meiji Ōtorii
        if (ok) checkGameOver();
        return ok;
    }

    // ---------- playing cards ----------

    /** True if the active player has the card in hand and enough Ki, and no Oath penalty forbids it. */
    public boolean canPlay(Card card) {
        Player p = state.active();
        if (card == null || !p.getHand().contains(card) || !p.canAfford(costOf(p, card))) return false;
        if (card instanceof ShrineCard && penalties.has(p, OathPenalties.Lasting.NO_SHRINE)) return false;   // Profanity
        SpellEffects.Rule rule = SpellEffects.rule(card);
        if (rule != null) {                                                                              // a plain spell
            if (rule.target() == SpellTarget.NONE && !rule.castable().test(new EffectContext(this, p, null, null))) return false;
            if (rule.target() != SpellTarget.NONE && spellTargets(card).size() < (rule.target() == SpellTarget.TWO_MINIONS ? 2 : 1)) return false;
        }
        return !(card instanceof CurseCard && !p.isHeldCurse(card)
                && penalties.has(p, OathPenalties.Lasting.NO_CURSE));                                        // Exorcism
    }

    /**
     * slot 0-6 for a minion (ignored otherwise), target may be null.
     * A trap is set face down and publishes TRAP_SET only.
     * Otherwise publishes CARD_PLAYED, then MINION_SUMMONED / SPELL_CAST / WEAPON_EQUIPPED,
     * runs Banzai and the enemy's traps, removes the dead and checks game over.
     */
    public boolean playCard(Card card, int slot, Combatant target) {
        return playCard(card, slot, target, null);
    }

    /**
     * Same, with a second target for Kettō (SpellTarget.TWO_MINIONS). A plain spell whose target is
     * missing or illegal is refused before any Ki is spent.
     */
    public boolean playCard(Card card, int slot, Combatant target, Combatant second) {
        if (state.isOver() || !canPlay(card)) return false;
        if (!spellAimedRight(card, target, second)) return false;
        Player p = state.active();
        if (card instanceof CurseCard && p.isHeldCurse(card)) return false;   // removed with removeCurse, never played

        // A minion needs its chosen slot to be free: check before spending anything
        if ((card instanceof MinionCard || card instanceof ShrineCard) && !p.getSide().isFree(slot)) {
        return false;
        }

        // A Banzai with legal targets needs one of them (checked before anything is spent)
        if (card instanceof MinionCard && !banzaiAimedRight(card, target)) return false;

        // A Rite needs at least one free slot for its first Ancestor
        if (card instanceof RiteCard && p.getSide().isFull()) return false;

        // A trap is set face down: TRAP_SET instead of CARD_PLAYED. Only its owner's UI shows the card
        if (card instanceof TrapCard trap) {
            if (!p.canSetTrap(trap)) return false;
            int paid = costOf(p, trap);
            p.spendKi(paid);
            p.removeFromHand(trap);
            p.setTrap(trap);
            turnTracker.trapSet(trap, paid);
            publish(new GameEvent(GameEvent.Type.TRAP_SET, p, trap, p.trapCount()));
            return true;
        }

        int paid = costOf(p, card);
        p.spendKi(paid);
        p.removeFromHand(card);
        publish(new GameEvent(GameEvent.Type.CARD_PLAYED, p, card, paid));   // amount = Ki paid

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
        } else if (card instanceof CurseCard cc) {
            giveCurse(p, state.opponent(), cc);        
        } else if (card instanceof RiteCard rc) {
            performRite(p, rc, slot, target);
        } else if (card instanceof SpellCard) {
            publish(GameEvent.of(GameEvent.Type.SPELL_CAST, p, card));
            triggerTraps(state.opponent(), TrapTrigger.ON_ENEMY_SPELL, null);
            castingSpell = true;
            try {
                cardEffects.get(card.getId(), Trigger.BANZAI)
                        .ifPresent(effect -> applyAs(effect, new EffectContext(this, p, null, target, second)));
            } finally {
                castingSpell = false;
            }
        }

        combat.removeDead();
        checkGameOver();
        return true;
    }

    public static final int HERO_POWER_COST = 2;     // GDD, proposed for Shuriken, Miko, Jikininki
    public static final int SHURIKEN_DAMAGE = 6;
    public static final int JIKININKI_DRAIN = 4;
    public static final String MIKO_ID = "miko";       // same id as the Miko card: art images/cards/Kannushi/miko.png
    public static final int MIKO_BONUS_EVERY = 3;      // every 3rd Kannushi power use: all Miko +1/+1
    static final MinionCard MIKO = new MinionCard(MIKO_ID, "Miko", "巫女", "みこ", 0,
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
                int dealt = target.takeDamage(rewards.effectDamage(p, false, target, SHURIKEN_DAMAGE + ryu(p)));
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
                p.addMikoCall();
                if (p.getMikoCalls() % MIKO_BONUS_EVERY == 0) {
                    p.getSide().getMinions().stream().filter(GameEngine::isMiko).forEach(m -> m.buff(1, 1));
                }
            }
            case ONI -> {
                if (!isPowerTarget(target)) return false;
                payPower(p, HERO_POWER_COST);
                int dealt = target.takeDamage(rewards.effectDamage(p, false, target, JIKININKI_DRAIN + ryu(p)));
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

    private static boolean isMiko(Minion m) {
        return MIKO_ID.equals(m.getSource().getId());
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
        if (source == null || minionEffects.isSilenced(source)) return;   // Shike
        Optional<Effect> effect = cardEffects.get(source.getSource().getId(), trigger);
        if (effect.isEmpty()) return;
        if (trigger != Trigger.BANZAI) announce(source, owner);            // a Banzai is announced by its CARD_PLAYED
        applyAs(effect.get(), new EffectContext(this, owner, source, target));
    }

    /** EFFECT_TRIGGERED, just before a triggered effect acts: the UI shows its banner and keeps it in order in the replay. */
    void announce(Object source, Player owner) {
        publish(GameEvent.of(GameEvent.Type.EFFECT_TRIGGERED, source, owner));
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

    // ---------- curses (Oni) ----------

    private void giveCurse(Player from, Player to, CurseCard curse) {
        if (curse.getZone() == CurseZone.HAND) {
            if (!to.receiveCurse(curse)) return;          // hand full: the Curse is lost
        } else {
            for (int i = 0; i < curse.getCopies(); i++) to.getDeck().insertRandom(curse, rng);
        }
        publish(new GameEvent(GameEvent.Type.CURSE_GIVEN, from, curse, curse.getCopies()));
    }

    /** The active player pays the removal cost to get rid of a Curse in his hand. Refused = nothing paid. */
    public boolean removeCurse(CurseCard curse) {
        Player p = state.active();
        if (state.isOver() || !p.isHeldCurse(curse) || !p.canAfford(curse.getRemovalCost())) return false;
        p.spendKi(curse.getRemovalCost());
        p.removeCurse(curse);
        publish(GameEvent.of(GameEvent.Type.CURSE_REMOVED, p, curse));
        return true;
    }

    /** End of the holder's turn: every hand Curse he holds acts, left to right. */
    /** End of the holder's turn (and Corruption 9 at every turn start and end): every hand Curse he holds acts, left to right. */
    void runHandCurses(Player holder) {
        for (Card c : List.copyOf(holder.getHand())) {
            if (c instanceof CurseCard curse && holder.isHeldCurse(curse)) fireCurse(holder, curse);
            if (state.isOver()) return;
        }
    }

    /** A deck Curse acts as soon as it is drawn, then is gone. */
    private void onCurseDrawn(Player p, Card card) {
        if (!(card instanceof CurseCard curse) || curse.getZone() != CurseZone.DECK) return;
        p.removeFromHand(curse);
        fireCurse(p, curse);
    }

    private void fireCurse(Player victim, CurseCard curse) {
        publish(GameEvent.of(GameEvent.Type.CURSE_TRIGGERED, victim, curse));
        Player before = cursed;
        cursed = victim;                      // its damage is the cursing player's (Corruption 3)
        cardEffects.get(curse.getId(), Trigger.CURSE)
                .ifPresent(effect -> effect.apply(new EffectContext(this, victim, null, null)));
        cursed = before;        combat.removeDead();
        checkGameOver();
    }

    // ---------- rites (Samurai) ----------

    /**
     * Summons the Rite's Ancestors (first on the chosen slot if free, then leftmost free;
     * no room = the rest are lost), each with the Rite's own keywords and the lasting
     * bonuses of the Rites before it, then records this Rite for the next Ancestors.
     */
    private void performRite(Player p, RiteCard rite, int slot, Combatant target) {
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
            publish(GameEvent.of(GameEvent.Type.MINION_SUMMONED, p, a));
            for (RiteCard past : p.getRites()) {
                cardEffects.get(past.getId(), Trigger.BANZAI)
                        .ifPresent(effect -> applyAs(effect, new EffectContext(this, p, a, target)));
            }
            triggerTraps(state.opponent(), TrapTrigger.ON_ENEMY_SUMMON, a);
            if (state.isOver()) return;
        }
        p.addRite(rite);
        publish(GameEvent.of(GameEvent.Type.RITE_PERFORMED, p, rite));
    }

    /** One trap per action: the first of the owner's traps waiting for it goes off, then is gone. */
    private void triggerTraps(Player owner, TrapTrigger when, Combatant target) {
        Optional<TrapCard> trap = owner.takeTrap(when);
        if (trap.isEmpty()) return;
        publish(GameEvent.of(GameEvent.Type.TRAP_TRIGGERED, owner, trap.get()));
        cardEffects.get(trap.get().getId(), Trigger.TRAP)
                .ifPresent(effect -> applyAs(effect, new EffectContext(this, owner, null, target)));
        combat.removeDead();
        checkGameOver();
    }

    /**
     * Runs an effect as its owner's, so its damage gets the owner's bonuses (Ryū, Oaths, Chikara)
     * and not the active player's: an enemy trap or death effect set off by your spell stays the enemy's.
     */
    private void applyAs(Effect effect, EffectContext ctx) {
        Player before = effectOwner;
        effectOwner = ctx.owner();
        try {
            effect.apply(ctx);
        } finally {
            effectOwner = before;
        }
    }

    /** Who deals effect damage now: the cursing player (Corruption 3), else the effect's owner, else the active player. */
    private Player dealer() {
        if (cursed != null) return state.opponentOf(cursed);
        if (effectOwner != null) return effectOwner;
        return state.active();
    }

    /**
     * Effect damage: publishes DAMAGE, removes the dead, checks game over.
     * Oath rewards count, except on an Oath's own damage (break, Defilement) and the clock's.
     */
    void dealDamage(Object source, Combatant target, int amount) {
        if (target == null || amount <= 0) return;
        if (!(source instanceof Oath) && !(source instanceof ChessClock) && !(source instanceof Deck)) {   // fatigue: no bonus
            Player dealer = dealer();
            amount = rewards.effectDamage(dealer, cursed != null, target, amount + ryu(dealer) + chikaraSpell(dealer));
        }
        amount = familyEffects.capDamage(target, amount);               // Fushimi Inari
        int taken = target.takeDamage(amount);
        publish(new GameEvent(GameEvent.Type.DAMAGE, source, target, taken));
        combat.removeDead();
        checkGameOver();
    }

    void heal(Combatant target, int amount) {
        if (healBlocked(target)) return;                                 // Defilement
        int restored = target.heal(rewards.heal(amount + ryu(state.active())));   // Ryū, Purity 3
        if (restored > 0) publish(new GameEvent(GameEvent.Type.HEAL, null, target, restored));
        rewards.healed(target, restored);                                 // Purity 6
    }

    void gainArmour(Player owner, int amount) {
        Hero hero = owner.getHero();
        int n = amount + ryu(owner);
        hero.gainArmour(n);
        publish(new GameEvent(GameEvent.Type.ARMOUR_GAINED, owner, hero, n));
    }

    /** Yoroi: armour on a minion of owner's. */
    void gainArmour(Player owner, Minion m, int amount) {
        int n = amount + ryu(owner);
        m.gainArmour(n);
        publish(new GameEvent(GameEvent.Type.ARMOUR_GAINED, owner, m, n));
    }

    void draw(Player p) {
        if (penalties.has(p, OathPenalties.Lasting.NO_DRAW)) return;     // Disappointment
        publishDraw(p, p.draw());
    }

    /** An effect puts c in p's hand: CARD_ADDED so the hand redraws, or CARD_BURNED if the hand is full (the card is lost). */
    boolean addToHand(Player p, Card c) {
        if (c == null) return false;
        if (p.getHand().size() >= Player.MAX_HAND) {
            publish(GameEvent.of(GameEvent.Type.CARD_BURNED, p, c));
            return false;
        }
        p.addToHand(c);
        publish(GameEvent.of(GameEvent.Type.CARD_ADDED, p, c));
        return true;
    }

    // ---------- hand display ----------

    private static final java.util.regex.Pattern DAMAGE_NUMBER = java.util.regex.Pattern.compile("(\\d+) damage");
    private static final java.util.regex.Pattern HEAL_NUMBER =
            java.util.regex.Pattern.compile("(?i)\\b(heal|restore)[^0-9;.]*?(\\d+)");

    /**
     * What to add to the damage or healing number written on card, as it would be dealt by p now (0 if none):
     * Ryu +4, Havoc 3 +1 and Corruption 3 +1 (Curses) on damage, Purity 3 +2 on healing.
     * A Curse held in p's hand deals the giver's damage, so its bonus is the opponent's. The UI shows it in green.
     * Chikara (Adventure) adds 3 to spell damage.
     * Justice 6 (double damage on some targets) depends on the target and is not counted.
     */
    public int bonusOf(Player p, Card card) {
        if (p == null || card == null) return 0;
        String text = card.getEffectText() == null ? "" : card.getEffectText();
        boolean heldCurse = card instanceof CurseCard cc && p.isHeldCurse(cc);
        Player dealer = heldCurse ? state.opponentOf(p) : p;
        if (DAMAGE_NUMBER.matcher(text).find()) {
            int bonus = ryu(dealer);
            if (OathRewards.has(dealer, OathType.HAVOC, 3)) bonus++;
            if (card instanceof CurseCard && OathRewards.has(dealer, OathType.CORRUPTION, 3)) bonus++;
            if (card instanceof SpellCard && has(dealer, Upgrade.CHIKARA)) bonus += Upgrade.CHIKARA_BONUS;
            return bonus;
        }
        if (HEAL_NUMBER.matcher(text).find()) {
            int bonus = ryu(dealer);
            if (OathRewards.has(dealer, OathType.PURITY, 3)) bonus += OathRewards.PURITY_HEAL_BONUS;
            return bonus;
        }
        return 0;
    }

    // ---------- Oath rewards and penalties ----------

    /** What this card costs p now, Oath rewards (Silence 3, Deception 3, Descension 3) and Hayasa included. The UI shows this. */
    public int costOf(Player p, Card card) {
        Map<Card, Integer> off = handDiscounts.get(p);
        int yoi = off == null ? 0 : off.getOrDefault(card, 0);
        int cost = Math.max(0, rewards.cost(p, card) + familyEffects.costUp(p, card)
                - yoi - minionEffects.costCut(p, card));   // Bakeneko, Yōi, Tennō
        return has(p, Upgrade.HAYASA) && cost > Upgrade.HAYASA_MIN ? cost - 1 : cost;   // Hayasa: -1, minimum 1
    }

    // ---------- Adventure upgrades ----------

    /**
     * Gives p his Adventure upgrades for this battle; call it before the first turn.
     * Inochi raises the hero's max and current HP by 30 at once; Chikara and Hayasa apply from then on.
     */
    public void setUpgrades(Player p, Set<Upgrade> chosen) {
        Set<Upgrade> set = chosen == null || chosen.isEmpty() ? EnumSet.noneOf(Upgrade.class) : EnumSet.copyOf(chosen);
        Set<Upgrade> before = upgrades.getOrDefault(p, Set.of());
        if (set.contains(Upgrade.INOCHI) && !before.contains(Upgrade.INOCHI)) p.getHero().raiseMaxHealth(Upgrade.INOCHI_HP);
        upgrades.put(p, set);
    }

    /** p's Adventure upgrades (empty outside the Adventure). */
    public Set<Upgrade> upgradesOf(Player p) {
        return Collections.unmodifiableSet(upgrades.getOrDefault(p, EnumSet.noneOf(Upgrade.class)));
    }

    boolean has(Player p, Upgrade u) {
        Set<Upgrade> set = upgrades.get(p);
        return set != null && set.contains(u);
    }

    /** Chikara: +3 to the damage of p's spell being cast. */
    private int chikaraSpell(Player p) {
        return castingSpell && has(p, Upgrade.CHIKARA) ? Upgrade.CHIKARA_BONUS : 0;
    }

    /** Kawarimi: the attack being resolved hits this minion instead. */
    void redirectAttack(Minion m) { redirected = m; }

    /** Otoshiana: the attack being resolved is cancelled. */
    void cancelAttack() { attackCancelled = true; }

    /** Divinity 9 doubles the Shrine auras. */
    int shrineFactor(Player p) { return rewards.shrineFactor(p); }

    /** The player whose hero or minion this is, or null if it is not on the board. */
    Player ownerOf(Combatant c) {
        for (Player p : List.of(state.getFirst(), state.getSecond())) {
            if (c == p.getHero() || (c instanceof Minion m && p.getSide().slotOf(m) >= 0)) return p;
        }
        return null;
    }

    /** A minion summoned by a rule (Retainer, Kagura's Miko, Kuchiyose) in p's leftmost free slot. Null if his side is full. */
    Minion summon(Player p, MinionCard card) {
        int slot = p.getSide().firstFree();
        if (slot < 0) return null;
        Minion m = new Minion(card);
        p.getSide().place(m, slot);
        publish(GameEvent.of(GameEvent.Type.MINION_SUMMONED, p, m));
        triggerTraps(state.opponentOf(p), TrapTrigger.ON_ENEMY_SUMMON, m);
        return m;
    }

    // ---------- plain spells (engine-spell-mechanics) ----------

    public static final String RYU_ID = "ryu";
    public static final int RYU_BONUS = 4;

    /** What this plain spell is aimed at (NONE for any other card). */
    public SpellTarget spellTarget(Card card) {
        SpellEffects.Rule r = SpellEffects.rule(card);
        return r == null ? SpellTarget.NONE : r.target();
    }

    /** The characters the active player may aim this plain spell at (empty when it needs no target). */
    public List<Combatant> spellTargets(Card card) {
        return targets(spellTarget(card));
    }

    /** The characters of that kind the active player may aim at. */
    private List<Combatant> targets(SpellTarget kind) {
        List<Combatant> out = new ArrayList<>();
        Player p = state.active();
        switch (kind) {
            case NONE -> { }
            case FRIENDLY_MINION -> out.addAll(p.getSide().getMinions());
            case FRIENDLY_CHARACTER -> {
                out.add(p.getHero());
                out.addAll(p.getSide().getMinions());
            }
            case ENEMY_MINION -> {
                for (Minion m : state.opponent().getSide().getMinions()) if (isPowerTarget(m)) out.add(m);
            }
            case MINION, TWO_MINIONS -> {
                for (Player q : List.of(p, state.opponent())) {
                    for (Minion m : q.getSide().getMinions()) if (isPowerTarget(m)) out.add(m);
                }
            }
            case CHARACTER -> {
                for (Player q : List.of(p, state.opponent())) {
                    if (isPowerTarget(q.getHero())) out.add(q.getHero());
                    for (Minion m : q.getSide().getMinions()) if (isPowerTarget(m)) out.add(m);
                }
            }
        }
        out.removeIf(Combatant::isDead);
        return out;
    }

    /** True for any card but a plain spell; a plain spell needs its legal target(s) and its condition. */
    private boolean spellAimedRight(Card card, Combatant target, Combatant second) {
        SpellEffects.Rule r = SpellEffects.rule(card);
        if (r == null) return true;
        List<Combatant> legal = spellTargets(card);
        boolean aimed = switch (r.target()) {
            case NONE -> true;
            case TWO_MINIONS -> target != null && second != null && target != second
                    && legal.contains(target) && legal.contains(second);
            default -> target != null && legal.contains(target);
        };
        return aimed && r.castable().test(new EffectContext(this, state.active(), null, target, second));
    }

    // ---------- minion effects (engine-minion-effects) ----------

    /** What this minion's Banzai is aimed at (NONE when it needs no target). */
    public SpellTarget banzaiTarget(Card card) {
        MinionEffects.Aim a = MinionEffects.aim(card);
        return a == null ? SpellTarget.NONE : a.target();
    }

    /** The legal targets of this minion's Banzai now. Empty: play it without a target. */
    public List<Combatant> banzaiTargets(Card card) {
        MinionEffects.Aim a = MinionEffects.aim(card);
        if (a == null) return List.of();
        List<Combatant> out = targets(a.target());
        out.removeIf(c -> !a.filter().test(c));
        return out;
    }

    /** A Banzai with legal targets needs one of them; with none, it is played without a target. */
    private boolean banzaiAimedRight(Card card, Combatant target) {
        List<Combatant> legal = banzaiTargets(card);
        return legal.isEmpty() || (target != null && legal.contains(target));
    }

    /** The cards the engine may create by id: tokens (Sakana uri, Koi, Kitsune no tsukai) and Curses (Oiwa). */
    public void setCatalog(java.util.Collection<? extends Card> cards) {
        catalog.clear();
        for (Card c : cards) catalog.put(c.getId(), c);
    }

    Card catalogCard(String id) { return catalog.get(id); }

    List<CurseCard> deckCurses() {
        return catalog.values().stream().filter(c -> c instanceof CurseCard cc && cc.getZone() == CurseZone.DECK)
                .map(c -> (CurseCard) c).sorted(java.util.Comparator.comparing(Card::getId)).toList();
    }

    /** Oiwa: one copy of a deck Curse shuffled into to's deck. */
    void giveDeckCurse(Player from, Player to, CurseCard curse) {
        to.getDeck().insertRandom(curse, rng);
        publish(new GameEvent(GameEvent.Type.CURSE_GIVEN, from, curse, 1));
    }

    /** Daimyō, Jujutsu shihan, Takemikazuchi: the hero equips this weapon (replaces the current one). */
    void equip(Player p, WeaponCard card) {
        p.getHero().equip(new Weapon(card));
        publish(GameEvent.of(GameEvent.Type.WEAPON_EQUIPPED, p, p.getHero()));
    }

    /** Kitsune no bakashiya: m becomes a fresh copy of that card in the same slot (no Banzai). */
    void transform(Minion m, MinionCard into) {
        Player owner = ownerOf(m);
        if (owner == null) return;
        int slot = owner.getSide().slotOf(m);
        owner.getSide().remove(m);
        publish(GameEvent.of(GameEvent.Type.MINION_REMOVED, owner, m));
        Minion copy = new Minion(into);
        owner.getSide().place(copy, slot);
        publish(GameEvent.of(GameEvent.Type.MINION_SUMMONED, owner, copy));
    }

    /** Fūjin: back to its owner's hand (lost if the hand is full), without dying. */
    void bounce(Minion m) {
        Player owner = ownerOf(m);
        if (owner == null) return;
        owner.getSide().remove(m);
        addToHand(owner, m.getSource());
        publish(GameEvent.of(GameEvent.Type.MINION_REMOVED, owner, m));
    }

    /** Yogake: two minions hit each other with their ATK, like an attack but with no attacker's turn. */
    void fight(Minion a, Minion b) {
        int x = a.getAttack(), y = b.getAttack();
        publish(GameEvent.of(GameEvent.Type.ATTACK, a, b));
        publish(new GameEvent(GameEvent.Type.DAMAGE, a, b, b.takeDamage(x)));
        publish(new GameEvent(GameEvent.Type.DAMAGE, b, a, a.takeDamage(y)));
        combat.removeDead();
    }

    MinionEffects getMinionEffects() { return minionEffects; }

    /** Ryū on p's side: +4 to his damage, healing and armour effects. */
    int ryu(Player p) {
        if (p == null) return 0;
        for (Minion m : p.getSide().getMinions()) {
            if (!m.isDead() && RYU_ID.equals(m.getSource().getId())) return RYU_BONUS;
        }
        return 0;
    }

    /** Kuji-in. */
    void refreshHeroPower() { turns.resetPower(); }

    /** Yōi: every card now in p's hand (not a held Curse) costs 1 less, until it leaves the hand. */
    void discountHand(Player p) {
        Map<Card, Integer> off = handDiscounts.computeIfAbsent(p, k -> new IdentityHashMap<>());
        for (Card c : p.getHand()) if (!p.isHeldCurse(c)) off.merge(c, 1, Integer::sum);
    }

    private void useDiscount(Player p, Card c) {
        Map<Card, Integer> off = handDiscounts.get(p);
        if (off != null) off.computeIfPresent(c, (k, v) -> v > 1 ? v - 1 : null);
    }

    /** Kettō: a random one of the two survives on half its HP (rounded up), the other dies. */
    void duel(Minion a, Minion b) {
        boolean first = rng.nextBoolean();
        Minion winner = first ? a : b;
        winner.halveHealth();
        destroy(first ? b : a);
    }

    List<MinionCard> graveyard(Player p) {
        return graveyards.getOrDefault(p, List.of());
    }

    /** Kuchiyose: a random dead minion of p's comes back in his leftmost free slot, and its Banzai runs again. */
    void resurrectRandom(Player p) {
        List<MinionCard> dead = graveyards.get(p);
        if (dead == null || dead.isEmpty()) return;
        MinionCard card = dead.remove(rng.nextInt(dead.size()));
        Minion m = summon(p, card);
        if (m != null) runTrigger(Trigger.BANZAI, p, m, null);
    }

    /** Kamikakushi: the minion leaves the board without dying (no Seppuku), its card goes into its owner's deck. */
    void shuffleIntoDeck(Minion m) {
        Player owner = ownerOf(m);
        if (owner == null) return;
        owner.getSide().remove(m);
        owner.getDeck().insertRandom(m.getSource(), rng);
        publish(GameEvent.of(GameEvent.Type.MINION_REMOVED, owner, m));
    }

    /** Sakasama: every enemy minion swaps ATK and HP; the copies of those cards in his deck get the swapped stats. */
    void swapEnemyStats(Player enemy) {
        Set<String> ids = new HashSet<>();
        for (Minion m : enemy.getSide().getMinions()) {
            m.swapStats();
            ids.add(m.getSource().getId());
        }
        enemy.getDeck().replaceAll(c -> c instanceof MinionCard mc && ids.contains(mc.getId()) ? SpellEffects.swapped(mc) : c);
    }

    /** Deception 9: a second Trap set for free, from the hand or the deck (not a card played, no TurnTracker count). */
    void setExtraTrap(Player p, TrapCard trap) {
        if (!p.removeFromHand(trap)) p.getDeck().remove(trap);
        if (!p.setTrap(trap)) return;
        publish(new GameEvent(GameEvent.Type.TRAP_SET, p, trap, p.trapCount()));
    }

    Random rng() { return rng; }

    OathRewards getRewards() { return rewards; }

    OathPenalties getPenalties() { return penalties; }

    /** Destroyed outright, whatever its HP or Torii (Banishment, Abyss). */
    void destroy(Minion m) {
        m.destroy();
        combat.removeDead();
    }

    /** Revelation: the hand is shown to the opponent, then a random card of it is thrown away. */
    void revealAndDiscard(Player p) {
        publish(new GameEvent(GameEvent.Type.HAND_REVEALED, p, List.copyOf(p.getHand()), p.getHand().size()));
        discardRandom(p);
    }

    /** A random card of p's hand is thrown away (Revelation, Tamashii-gui). */
    void discardRandom(Player p) {
        if (p.getHand().isEmpty()) return;
        Card c = p.getHand().get(rng.nextInt(p.getHand().size()));
        if (c instanceof CurseCard curse && p.isHeldCurse(curse)) p.removeCurse(curse);
        else p.removeFromHand(c);
        publish(GameEvent.of(GameEvent.Type.CARD_DISCARDED, p, c));
    }

    /** Exposed: every Trap of from, set or in hand, goes to thief (a copy he already has set, or a full hand, loses it). */
    void stealTraps(Player from, Player thief) {
        int stolen = 0;
        for (TrapCard t : from.takeAllTraps()) {
            if (thief.setTrap(t)) stolen++;
        }
        for (Card c : List.copyOf(from.getHand())) {
            if (c instanceof TrapCard t) {
                from.removeFromHand(t);
                if (addToHand(thief, t)) stolen++;
            }
        }
        publish(new GameEvent(GameEvent.Type.TRAPS_STOLEN, from, thief, stolen));
    }

    /** Profanity: every Shrine of p leaves the board. */
    void destroyShrines(Player p) {
        for (Shrine s : List.copyOf(p.getSide().shrines())) {
            p.getSide().remove(s);
            publish(GameEvent.of(GameEvent.Type.SHRINE_EXPIRED, p, s));
        }
    }

    /** Exorcism: every Curse the holder was given is thrown away for free. */
    void purgeCurses(Player holder) {
        for (Card c : List.copyOf(holder.getHand())) {
            if (c instanceof CurseCard curse && holder.isHeldCurse(curse)) {
                holder.removeCurse(curse);
                publish(GameEvent.of(GameEvent.Type.CURSE_REMOVED, holder, curse));
            }
        }
    }

    /** Defilement: the minions of a player under NO_HEAL cannot be healed. */
    private boolean healBlocked(Combatant target) {
        if (!(target instanceof Minion m)) return false;
        for (Player p : List.of(state.getFirst(), state.getSecond())) {
            if (p.getSide().slotOf(m) >= 0) return penalties.has(p, OathPenalties.Lasting.NO_HEAL);
        }
        return false;
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
        } else if (p.drewFromEmptyDeck()) {
            fatigue(p);
        } else {
            publish(GameEvent.of(GameEvent.Type.CARD_BURNED, p, null));
        }
    }

    /** Fatigue (decided 18 Nov): each draw from an empty deck deals 1, then 2, then 3... to the drawer's own hero, with no bonus. */
    private void fatigue(Player p) {
        if (state.isOver()) return;
        int damage = fatigue.merge(p, 1, Integer::sum);
        publish(new GameEvent(GameEvent.Type.FATIGUE, p, p.getHero(), damage));
        dealDamage(p.getDeck(), p.getHero(), damage);
    }

    /** The damage p's next draw from an empty deck will deal (the UI can show it on the empty deck). */
    public int nextFatigue(Player p) {
        return fatigue.getOrDefault(p, 0) + 1;
    }

    /** Called after every action that can kill a hero (combat, spells). */
    void checkGameOver() {
        if (state.checkGameOver()) {
            publish(GameEvent.of(GameEvent.Type.GAME_OVER, null, state.getWinner().orElse(null)));
        }
    }
}