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
import fr.onishintan.model.Shrine;
import fr.onishintan.model.ShrineCard;
import fr.onishintan.model.SpellCard;
import fr.onishintan.model.Oath;
import fr.onishintan.model.OathType;
import fr.onishintan.model.TrapCard;
import fr.onishintan.model.TrapTrigger;
import fr.onishintan.model.Weapon;
import fr.onishintan.model.WeaponCard;
import fr.onishintan.model.GodaiElement;
import fr.onishintan.model.Keyword;
import fr.onishintan.model.HeroClass;
import fr.onishintan.model.KatanaTier;
import fr.onishintan.model.Rarity;
import fr.onishintan.model.CurseCard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.LongSupplier;

import fr.onishintan.engine.effect.CardEffects;
import fr.onishintan.engine.effect.family.FamilyEffects;
import fr.onishintan.engine.effect.minion.MinionEffects;
import fr.onishintan.engine.event.EventBus;
import fr.onishintan.engine.event.GameListener;
import fr.onishintan.engine.oath.OathPenalties;
import fr.onishintan.engine.oath.OathRewards;
import fr.onishintan.engine.oath.OathRules;
import fr.onishintan.engine.turn.CombatResolver;
import fr.onishintan.engine.turn.TurnManager;
import fr.onishintan.engine.turn.TurnTracker;
import fr.onishintan.engine.effect.Effect;
import fr.onishintan.engine.effect.EffectContext;
import fr.onishintan.engine.effect.SpellEffects;
import fr.onishintan.engine.effect.SpellTarget;
import fr.onishintan.engine.effect.Trigger;
import fr.onishintan.engine.event.GameEvent;
import fr.onishintan.engine.system.BoardActions;
import fr.onishintan.engine.system.Curses;
import fr.onishintan.engine.system.HandManager;
import fr.onishintan.engine.system.HeroPowers;
import fr.onishintan.engine.system.MatchClock;
import fr.onishintan.engine.system.OrochiBoss;
import fr.onishintan.engine.system.Rites;
import fr.onishintan.engine.system.Targeting;
import fr.onishintan.engine.system.Traps;
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
   // empty-deck draws so far, per player
    private List<GameEvent> heard;          // events waiting for the listeners while a rule handler runs (publish)
    /** Otoshiana: a trap cancelled the attack being resolved. */
    private boolean attackCancelled;
    /** Every card the match may create by id (tokens, Curses): set by the UI, later by cards.json. */
    private final Map<String, Card> catalog = new HashMap<>();
    /** Adventure upgrades of each player (Inochi, Chikara, Hayasa): none outside the Adventure. */
    private final Map<Player, Set<Upgrade>> upgrades = new HashMap<>();
    /** True while a spell card's effect is resolving (Chikara: +3 spell damage). */
    private boolean castingSpell;
    /** Whose effect is resolving (a spell, a Banzai, a trigger, a trap): its damage gets that player's bonuses. */
    private Player effectOwner;
    private final Random rng;
    private final MatchClock clock = new MatchClock(this);
    private final HeroPowers powers = new HeroPowers(this);
    private final OrochiBoss orochi = new OrochiBoss(this);
    private final Curses curses = new Curses(this);
    private final Rites rites = new Rites(this);
    private final Traps traps = new Traps(this);
    private final HandManager hand = new HandManager(this);
    private final BoardActions board = new BoardActions(this);
    private final Targeting targeting = new Targeting(this);

   // replaced in tests

    /** Tests drive the clocks with a fake time. */
    public void setTimeSource(LongSupplier now) { clock.setTimeSource(now); }

    /** The players who already used their mulligan (once each, before the first turn). */
    private final Set<Player> mulliganDone = new HashSet<>();

    /** The clocks run only once both players have sworn an Oath (PvP). */
    public boolean clocksEnabled() { return clock.clocksEnabled(); }

    /** Hides the table (pass veil): the active clock stops. */
    public void pauseClock() { clock.pauseClock(); }

    /** The active player clicked the veil: his clock runs again. */
    public void resumeClock() { clock.resumeClock(); }

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
        bus.subscribe(GameEvent.Type.MINION_DIED, e -> board.recordDeath((Player) e.source(), (Minion) e.target()));
        bus.subscribe(GameEvent.Type.CARD_PLAYED, e -> hand.useDiscount((Player) e.source(), (Card) e.target()));
        bus.subscribe(GameEvent.Type.TRAP_SET, e -> hand.useDiscount((Player) e.source(), (Card) e.target()));
        bus.subscribe(GameEvent.Type.TURN_ENDED, familyEffects::onTurnEnded);   // Heiwa no Torii, Itsukushima: before the tick
        // A Shrine loses 1 durability at the start of its owner's turn (not the turn it is placed)
        bus.subscribe(GameEvent.Type.TURN_STARTED,
             e -> board.tickShrines((Player) e.source()));        
        bus.subscribe(GameEvent.Type.TURN_ENDED,
             e -> runHandCurses((Player) e.source()));
        bus.subscribe(GameEvent.Type.CARD_DRAWN,
             e -> curses.onCurseDrawn((Player) e.source(), (Card) e.target()));
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
            if (e.source() instanceof Player p && e.target() instanceof Minion m && HeroPowers.isMiko(m)) {
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
        // Yamata no Orochi: his mythic comes by itself at the start of his turns 10, 15, 20...
        bus.subscribe(GameEvent.Type.TURN_STARTED, e -> orochiMythic((Player) e.source()));
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
    public void publish(GameEvent event) {
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
        turns.startTurn().forEach(card -> hand.publishDraw(p, card));
        if (state.isOver()) return;           // fatigue can kill
        publish(GameEvent.of(GameEvent.Type.TURN_STARTED, p, null));
        clock.markTurnsBegun();
        clock.startClock(p);
    }

    
    /**
     * Mulligan, once per player between startMatch() and beginFirstTurn(): the chosen cards of p's opening hand
     * are replaced. The new cards are drawn first, then the old ones go back into the deck at random places,
     * so a card sent back cannot come straight back. Cards not in p's hand are ignored; an empty list keeps the hand.
     * Publishes MULLIGAN_DONE (target = the new cards, amount = how many were replaced).
     * @return the new cards, or an empty list if the mulligan is refused (already done, or the first turn began)
     */
    public List<Card> mulligan(Player p, List<Card> toReplace) {
        if (p == null || clock.turnsBegun() || mulliganDone.contains(p)
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
        long t = clock.now();
        p.getClock().pause(t);
        winner.getClock().pause(t);
        state.declareWinner(winner);
        publish(GameEvent.of(GameEvent.Type.GAME_OVER, p, winner));
        return true;
    }

    /** Ends the active turn (end-of-turn effects, then the Oath) and starts the opponent's. */
    public void endTurn() {
        Player ending = state.active();
        ending.getClock().pause(clock.now());
        if (clocksEnabled()) ending.getClock().add(ChessClock.INCREMENT_MS);
        publish(GameEvent.of(GameEvent.Type.TURN_ENDED, ending, null));
        rewards.endTurn(ending);              // end-of-turn Oath rewards (Allegiance 3, Peace 6...)
        if (state.isOver()) return;
        penalties.tick(ending);               // his lasting Oath penalties lose one of his turns
        resolveOath(ending);
        if (state.isOver()) return;           // an Oath break can kill its own hero
        List<Optional<Card>> drawn = turns.endTurn();
        Player next = state.active();
        drawn.forEach(card -> hand.publishDraw(next, card));
        if (state.isOver()) return;           // fatigue can kill
        publish(GameEvent.of(GameEvent.Type.TURN_STARTED, next, null));
        clock.startClock(next);
    }

    /** Time left in a player's bank, in ms (the UI shows it). */
    public long remainingMs(Player p) { return clock.remainingMs(p); }

    /** Bank empty: the active hero takes half his HP and gets +30 s (MatchClock). */
    public boolean checkClock() { return clock.checkClock(); }

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
        clock.startClock(state.active());   // the second Oath sworn: the active clock starts
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
        traps.trigger(state.opponent(), when, attacker);
        if (state.isOver() || attacker.isDead()) return false;      // Shikomi andon may kill the attacker first
        if (attackCancelled) {                                      // Otoshiana: the attack is spent, nothing hits
            attacker.markAttacked();
            return true;
        }
        if (redirected != null && !redirected.isDead()) target = redirected;   // Kawarimi: the log takes the hit
        boolean ok = combat.resolve(attacker, target);
        if (ok) familyEffects.afterAttack(attacker);                // Meiji Ōtorii
        if (ok) minionEffects.followHero(attacker, target);       // Kosho strikes what its hero struck
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
        if (!targeting.spellAimedRight(card, target, second)) return false;
        Player p = state.active();
        if (card instanceof CurseCard && p.isHeldCurse(card)) return false;   // removed with removeCurse, never played

        // A minion needs its chosen slot to be free: check before spending anything
        if ((card instanceof MinionCard || card instanceof ShrineCard) && !p.getSide().isFree(slot)) {
        return false;
        }

        // A Banzai with legal targets needs one of them (checked before anything is spent)
        if (card instanceof MinionCard && !targeting.banzaiAimedRight(card, target)) return false;

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
            traps.trigger(state.opponent(), TrapTrigger.ON_ENEMY_SUMMON, m);
        } else if (card instanceof WeaponCard wc) {
            p.getHero().equip(new Weapon(wc));
            publish(GameEvent.of(GameEvent.Type.WEAPON_EQUIPPED, p, p.getHero()));
            } else if (card instanceof ShrineCard sc) {
            Shrine shrine = new Shrine(sc);
            p.getSide().place(shrine, slot);
            publish(new GameEvent(GameEvent.Type.SHRINE_PLACED, p, shrine, slot));
        } else if (card instanceof CurseCard cc) {
            curses.giveCurse(p, state.opponent(), cc);        
        } else if (card instanceof RiteCard rc) {
            rites.performRite(p, rc, slot, target);
        } else if (card instanceof SpellCard) {
            publish(GameEvent.of(GameEvent.Type.SPELL_CAST, p, card));
            traps.trigger(state.opponent(), TrapTrigger.ON_ENEMY_SPELL, null);
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

    public static final int HERO_POWER_COST = 2;     // GDD, proposed for Shuriken, Miko, Jikininki, Yatsu-kubi
    public static final int SHURIKEN_DAMAGE = 6;
    public static final int JIKININKI_DRAIN = 4;
    public static final String MIKO_ID = "miko";       // same id as the Miko card: art images/cards/Kannushi/miko.png
    public static final int MIKO_BONUS_EVERY = 3;      // every 3rd Kannushi power use: all Miko +1/+1
    public static final MinionCard MIKO = new MinionCard(MIKO_ID, "Miko", "巫女", "みこ", 0,
            Rarity.COMMON, HeroClass.KANNUSHI, "", "", 3, 3, null);

    /** Yamata no Orochi's power, 八つ首 Yatsu-kubi (Eight Heads): 1 damage to a random enemy character, 8 times. */
    public static final int YATSU_KUBI_HITS = 8;
    /** Every this many HP Orochi has lost, each Yatsu-kubi hit does 1 more damage. */
    public static final int YATSU_KUBI_RAGE_STEP = 50;

    /** How much each Yatsu-kubi hit does now (the UI shows it on the power). */
    public static int yatsuKubiDamage(Player p) { return OrochiBoss.yatsuKubiDamage(p); }

    /** Ninja Shuriken, Kannushi Miko, Oni Jikininki, Orochi Yatsu-kubi; Samurai: useKatana. Once per turn. */
    public boolean useHeroPower(Combatant target) { return powers.useHeroPower(target); }

    // ---------- Yamata no Orochi's mythic ----------

    public static final String OROCHI_MYTHIC_ID = "yamata-no-orochi";
    public static final int OROCHI_MYTHIC_FIRST = 10;
    public static final int OROCHI_MYTHIC_EVERY = 5;

    /** True on Orochi's turns 10, 15, 20, 25... */
    static boolean isOrochiMythicTurn(int round) { return OrochiBoss.isMythicTurn(round); }

    /** Start of an OROCHI turn 10, 15, 20...: his mythic arrives without his hand. */
    public void orochiMythic(Player p) { orochi.orochiMythic(p); }

    /** Samurai power: pay the blade's Ki and equip it (replaces the current weapon). */
    public boolean useKatana(KatanaTier tier) { return powers.useKatana(tier); }

    // ---------- Uragiri ----------

    /** Uragiri: true while p's Oath is broken, from the break until he swears it again. */
    public boolean isOathbreaker(Player p) {
        return p.getOath() != null && !p.getOath().isSworn();
    }

    /** Izanagi: the player who may now take a card from the enemy hand, or null. */
    public Player pendingSteal() { return minionEffects.pendingSteal(); }

    /** Izanagi: takes this card from the enemy hand. False if it cannot be taken. */
    public boolean steal(Card card) { return minionEffects.steal(card); }

    /** Takeda Shingen: p's hero power becomes Godai. */
    public void grantGodai(Player p) { powers.grantGodai(p); }

    /** True once p's hero power is Godai. */
    public boolean hasGodai(Player p) { return powers.hasGodai(p); }

    /** True if this Godai element may land on that target now (null: Earth with no friendly minion). */
    public boolean isGodaiTarget(GodaiElement element, Combatant target) { return powers.isGodaiTarget(element, target); }

    /** Godai: the element on its target; false if refused. */
    public boolean useGodai(GodaiElement element, Combatant target) { return powers.useGodai(element, target); }

    public boolean isHeroPowerUsed() { return turns.isPowerUsedThisTurn(); }

        

    // ---------- triggers and effects ----------

    /** Runs the effect a minion has for this trigger, if any. */
    public void runTrigger(Trigger trigger, Player owner, Minion source, Combatant target) {
        if (source == null || minionEffects.isSilenced(source)) return;   // Shike
        if (source.getSource().getKeywords().contains(Keyword.URAGIRI) && !isOathbreaker(owner)) return;   // Uragiri: only while the Oath is broken
        Optional<Effect> effect = cardEffects.get(source.getSource().getId(), trigger);
        if (effect.isEmpty()) return;
        if (trigger != Trigger.BANZAI) announce(source, owner);            // a Banzai is announced by its CARD_PLAYED
        applyAs(effect.get(), new EffectContext(this, owner, source, target));
    }

    /** EFFECT_TRIGGERED, just before a triggered effect acts: the UI shows its banner and keeps it in order in the replay. */
    public void announce(Object source, Player owner) {
        publish(GameEvent.of(GameEvent.Type.EFFECT_TRIGGERED, source, owner));
    }

    /** Runs a trigger for every minion on that player's side (copy: the board may change). */
    private void runForBoard(Trigger trigger, Player owner) {
        for (Minion m : List.copyOf(owner.getSide().getMinions())) {
            runTrigger(trigger, owner, m, null);
        }
    }

    // ---------- curses (Oni) ----------

    /** The active player pays the removal cost to get rid of a Curse in his hand. */
    public boolean removeCurse(CurseCard curse) { return curses.removeCurse(curse); }

    /** Every hand Curse the holder holds acts, left to right. */
    public void runHandCurses(Player holder) { curses.runHandCurses(holder); }

    // ---------- rites (Samurai) ----------

    /**
     * Runs an effect as its owner's, so its damage gets the owner's bonuses (Ryū, Oaths, Chikara)
     * and not the active player's: an enemy trap or death effect set off by your spell stays the enemy's.
     */
    public void applyAs(Effect effect, EffectContext ctx) {
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
        if (curses.cursed() != null) return state.opponentOf(curses.cursed());
        if (effectOwner != null) return effectOwner;
        return state.active();
    }

    /**
     * Effect damage: publishes DAMAGE, removes the dead, checks game over.
     * Oath rewards count, except on an Oath's own damage (break, Defilement) and the clock's.
     */
    public void dealDamage(Object source, Combatant target, int amount) {
        if (target == null || amount <= 0) return;
        if (!(source instanceof Oath) && !(source instanceof ChessClock) && !(source instanceof Deck)) {   // fatigue: no bonus
            Player dealer = dealer();
            amount = rewards.effectDamage(dealer, curses.cursed() != null, target, amount + ryu(dealer) + chikaraSpell(dealer));
        }
        amount = familyEffects.capDamage(target, amount);               // Fushimi Inari
        int taken = target.takeDamage(amount);
        publish(new GameEvent(GameEvent.Type.DAMAGE, source, target, taken));
        combat.removeDead();
        checkGameOver();
    }

    public void heal(Combatant target, int amount) {
        if (healBlocked(target)) return;                                 // Defilement
        int restored = target.heal(rewards.heal(amount + ryu(state.active())));   // Ryū, Purity 3
        if (restored > 0) publish(new GameEvent(GameEvent.Type.HEAL, null, target, restored));
        rewards.healed(target, restored);                                 // Purity 6
    }

    public void gainArmour(Player owner, int amount) {
        Hero hero = owner.getHero();
        int n = amount + ryu(owner);
        hero.gainArmour(n);
        publish(new GameEvent(GameEvent.Type.ARMOUR_GAINED, owner, hero, n));
    }

    /** Yoroi: armour on a minion of owner's. */
    public void gainArmour(Player owner, Minion m, int amount) {
        int n = amount + ryu(owner);
        m.gainArmour(n);
        publish(new GameEvent(GameEvent.Type.ARMOUR_GAINED, owner, m, n));
    }

    public void draw(Player p) { hand.draw(p); }

    /** An effect puts c in p's hand: CARD_ADDED, or CARD_BURNED if the hand is full. */
    public boolean addToHand(Player p, Card c) { return hand.addToHand(p, c); }

    // ---------- hand display ----------

    /** What to add to the damage or healing number written on card, as dealt by p now (the UI shows it in green). */
    public int bonusOf(Player p, Card card) { return hand.bonusOf(p, card); }

    // ---------- Oath rewards and penalties ----------

    /** What this card costs p now, Oath rewards (Silence 3, Deception 3, Descension 3) and Hayasa included. The UI shows this. */
    public int costOf(Player p, Card card) {
        int yoi = hand.discountOf(p, card);
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

    public boolean has(Player p, Upgrade u) {
        Set<Upgrade> set = upgrades.get(p);
        return set != null && set.contains(u);
    }

    /** Chikara: +3 to the damage of p's spell being cast. */
    private int chikaraSpell(Player p) {
        return castingSpell && has(p, Upgrade.CHIKARA) ? Upgrade.CHIKARA_BONUS : 0;
    }

    /** Kawarimi: the attack being resolved hits this minion instead. */
    public void redirectAttack(Minion m) { redirected = m; }

    /** Otoshiana: the attack being resolved is cancelled. */
    public void cancelAttack() { attackCancelled = true; }

    /** Divinity 9 doubles the Shrine auras. */
    public int shrineFactor(Player p) { return rewards.shrineFactor(p); }

    /** The player whose hero or minion this is, or null if it is not on the board. */
    public Player ownerOf(Combatant c) {
        for (Player p : List.of(state.getFirst(), state.getSecond())) {
            if (c == p.getHero() || (c instanceof Minion m && p.getSide().slotOf(m) >= 0)) return p;
        }
        return null;
    }

    /** A minion summoned by a rule in p's leftmost free slot. Null if his side is full. */
    public Minion summon(Player p, MinionCard card) { return board.summon(p, card); }

    // ---------- plain spells (engine-spell-mechanics) ----------

    public static final String RYU_ID = "ryu";
    public static final int RYU_BONUS = 4;

    /** What this plain spell is aimed at (NONE for any other card). */
    public SpellTarget spellTarget(Card card) { return targeting.spellTarget(card); }

    /** The characters the active player may aim this plain spell at. */
    public List<Combatant> spellTargets(Card card) { return targeting.spellTargets(card); }

    // ---------- minion effects (engine-minion-effects) ----------

    /** What this minion's Banzai is aimed at (NONE when it needs no target). */
    public SpellTarget banzaiTarget(Card card) { return targeting.banzaiTarget(card); }

    /** The legal targets of this minion's Banzai now. */
    public List<Combatant> banzaiTargets(Card card) { return targeting.banzaiTargets(card); }

    /** The cards the engine may create by id: tokens (Sakana uri, Koi, Kitsune no tsukai) and Curses (Oiwa). */
    public void setCatalog(java.util.Collection<? extends Card> cards) {
        catalog.clear();
        for (Card c : cards) catalog.put(c.getId(), c);
    }

    public Card catalogCard(String id) { return catalog.get(id); }

    public List<CurseCard> deckCurses() { return curses.deckCurses(); }

    /** Oiwa: one copy of a deck Curse shuffled into to's deck. */
    public void giveDeckCurse(Player from, Player to, CurseCard curse) { curses.giveDeckCurse(from, to, curse); }

    /** The hero equips this weapon (replaces the current one). */
    public void equip(Player p, WeaponCard card) { board.equip(p, card); }

    /** Kitsune no bakashiya: m becomes a fresh copy of that card in the same slot. */
    public void transform(Minion m, MinionCard into) { board.transform(m, into); }

    /** Fūjin: back to its owner's hand, without dying. */
    public void bounce(Minion m) { board.bounce(m); }

    /** Yogake: two minions hit each other with their ATK. */
    public void fight(Minion a, Minion b) { board.fight(a, b); }

    public MinionEffects getMinionEffects() { return minionEffects; }

    /** Ryū on p's side: +4 to his damage, healing and armour effects. */
    public int ryu(Player p) {
        if (p == null) return 0;
        for (Minion m : p.getSide().getMinions()) {
            if (!m.isDead() && RYU_ID.equals(m.getSource().getId())) return RYU_BONUS;
        }
        return 0;
    }

    /** Kuji-in. */
    public void refreshHeroPower() { turns.resetPower(); }

    /** Yōi: every card now in p's hand costs 1 less, until it leaves the hand. */
    public void discountHand(Player p) { hand.discountHand(p); }

    /** Kettō: a random one of the two survives on half its HP, the other dies. */
    public void duel(Minion a, Minion b) { board.duel(a, b); }

    public List<MinionCard> graveyard(Player p) { return board.graveyard(p); }

    /** Kuchiyose: a random dead minion of p's comes back. */
    public void resurrectRandom(Player p) { board.resurrectRandom(p); }

    /** Kamikakushi: the minion leaves the board without dying, its card goes into its owner's deck. */
    public void shuffleIntoDeck(Minion m) { board.shuffleIntoDeck(m); }

    /** Sakasama: every enemy minion swaps ATK and HP. */
    public void swapEnemyStats(Player enemy) { board.swapEnemyStats(enemy); }

    /** Deception 9: a second Trap set for free, from the hand or the deck. */
    public void setExtraTrap(Player p, TrapCard trap) { traps.setExtraTrap(p, trap); }

    public Random rng() { return rng; }

    public OathRewards getRewards() { return rewards; }

    public OathPenalties getPenalties() { return penalties; }

    /** Destroyed outright, whatever its HP or Torii (Banishment, Abyss). */
    public void destroy(Minion m) { board.destroy(m); }

    /** Revelation: the hand is shown to the opponent, then a random card of it is thrown away. */
    public void revealAndDiscard(Player p) { hand.revealAndDiscard(p); }

    /** A random card of p's hand is thrown away (Revelation, Tamashii-gui). */
    public void discardRandom(Player p) { hand.discardRandom(p); }

    /** Exposed: every Trap of from, set or in hand, goes to thief. */
    public void stealTraps(Player from, Player thief) { traps.stealTraps(from, thief); }

    /** Profanity: every Shrine of p leaves the board. */
    public void destroyShrines(Player p) { board.destroyShrines(p); }

    /** Exorcism: every Curse the holder was given is thrown away for free. */
    public void purgeCurses(Player holder) { curses.purgeCurses(holder); }

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

    /** The engine's parts, for the classes of engine.system and the effects. */
    public TurnManager turns() { return turns; }
    public CombatResolver combat() { return combat; }
    public Map<String, Card> catalog() { return catalog; }
    public Targeting targeting() { return targeting; }
    public Traps traps() { return traps; }
    public OrochiBoss orochi() { return orochi; }

    public GameState getState() { return state; }
    public Player active() { return state.active(); }
    public boolean isSwapUsedThisTurn() { return turns.isSwapUsedThisTurn(); }
    public TurnTracker getTurnTracker() { return turnTracker; }

    // ---------- helpers ----------

    /** The damage p's next draw from an empty deck will deal. */
    public int nextFatigue(Player p) { return hand.nextFatigue(p); }

    /** Called after every action that can kill a hero (combat, spells). */
    public void checkGameOver() {
        if (state.checkGameOver()) {
            publish(GameEvent.of(GameEvent.Type.GAME_OVER, null, state.getWinner().orElse(null)));
        }
    }
}