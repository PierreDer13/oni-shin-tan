package fr.onishintan.engine;

import fr.onishintan.model.BoardSide;
import fr.onishintan.model.Card;
import fr.onishintan.model.Combatant;
import fr.onishintan.model.Hero;
import fr.onishintan.model.Minion;
import fr.onishintan.model.MinionCard;
import fr.onishintan.model.Player;
import fr.onishintan.model.SpellCard;
import fr.onishintan.model.Weapon;
import fr.onishintan.model.WeaponCard;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

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

    /** Shuffles and deals 6 / 7 cards. The mulligan comes next, then beginFirstTurn(). */
    public void startMatch() {
        turns.startMatch();
        publish(GameEvent.of(GameEvent.Type.MATCH_STARTED));
    }

    /** After the mulligan: the first player's turn 1 starts. */
    public void beginFirstTurn() {
        Player p = state.active();
        publishDraw(p, turns.startTurn());
        publish(GameEvent.of(GameEvent.Type.TURN_STARTED, p, null));
    }

    /** Ends the active turn and starts the opponent's. */
    public void endTurn() {
        Player ending = state.active();
        publish(GameEvent.of(GameEvent.Type.TURN_ENDED, ending, null));
        Optional<Card> drawn = turns.endTurn();
        Player next = state.active();
        publishDraw(next, drawn);
        publish(GameEvent.of(GameEvent.Type.TURN_STARTED, next, null));
    }

    /** Drag a card onto the deck: once per turn, cost Player.TRADE_COST. */
    public boolean trade(Card card) {
        Player p = state.active();
        boolean ok = turns.swapCard(card);
        if (ok) publish(GameEvent.of(GameEvent.Type.CARD_TRADED, p, card));
        return ok;
    }

    // ---------- combat ----------

    /** Enemy characters this attacker may hit (Shirodu, Shinobi, Stealth). */
    public List<Combatant> legalTargets(Combatant attacker) {
        return combat.legalTargets(attacker);
    }

    /** Attack; publishes ATTACK, DAMAGE, WEAPON_BROKEN, MINION_DIED, then GAME_OVER if a hero died. */
    public boolean attack(Combatant attacker, Combatant target) {
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
     * Publishes CARD_PLAYED, then MINION_SUMMONED / SPELL_CAST / WEAPON_EQUIPPED,
     * runs Banzai, removes the dead and checks game over.
     */
    public boolean playCard(Card card, int slot, Combatant target) {
        if (state.isOver() || !canPlay(card)) return false;
        Player p = state.active();

        // A minion needs room on the board: check before spending anything
        if (card instanceof MinionCard
                && (slot < 0 || slot >= BoardSide.MAX || p.getSide().isFull())) {
            return false;
        }

        p.spendKi(card.getKiCost());
        p.removeFromHand(card);
        publish(GameEvent.of(GameEvent.Type.CARD_PLAYED, p, card));

        if (card instanceof MinionCard mc) {
            Minion m = new Minion(mc);
            p.getSide().place(m, slot);
            publish(GameEvent.of(GameEvent.Type.MINION_SUMMONED, p, m));
            runTrigger(Trigger.BANZAI, p, m, target);
        } else if (card instanceof WeaponCard wc) {
            p.getHero().equip(new Weapon(wc));
            publish(GameEvent.of(GameEvent.Type.WEAPON_EQUIPPED, p, p.getHero()));
        } else if (card instanceof SpellCard) {
            publish(GameEvent.of(GameEvent.Type.SPELL_CAST, p, card));
            cardEffects.get(card.getId(), Trigger.BANZAI)
                    .ifPresent(effect -> effect.apply(new EffectContext(this, p, null, target)));
        }

        combat.removeDead();
        checkGameOver();
        return true;
    }

    /** Once per turn, cost 2 Ki for now. target may be null. TODO engine-hero-powers */
    public boolean useHeroPower(Combatant target) {
        throw new UnsupportedOperationException("TODO engine-hero-powers");
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