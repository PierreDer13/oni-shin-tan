package fr.onishintan.ui.match;

import fr.onishintan.engine.GameEngine;
import fr.onishintan.engine.GameEvent;
import fr.onishintan.model.Card;
import fr.onishintan.model.CurseCard;
import fr.onishintan.model.CurseZone;
import fr.onishintan.model.Combatant;
import fr.onishintan.model.Hero;
import fr.onishintan.model.HeroClass;
import fr.onishintan.model.KatanaTier;
import fr.onishintan.model.OathType;
import fr.onishintan.model.Player;
import fr.onishintan.model.RiteCard;
import fr.onishintan.model.Shrine;
import fr.onishintan.model.TrapCard;
import fr.onishintan.ui.MockCards;
import javafx.scene.paint.Color;

import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Runs one hotseat match. Pierre's GameEngine applies every rule; the BoardView only draws the table.
 * Each click on the board calls one engine method, then the board redraws from the players' new state.
 */
public class MatchController {
    /** Each class's hero portrait (images/heroes/<name>.png). */
    private static final Map<HeroClass, String> PORTRAIT = Map.of(
            HeroClass.SAMURAI, "yasuke",
            HeroClass.NINJA, "hattori-hanzo",
            HeroClass.KANNUSHI, "kada-no-azumamaro",
            HeroClass.ONI, "shuten-doji");

    private final GameEngine engine;
    private final BoardView board;
    /** Plays first and starts at the bottom of the screen. */
    private final Player first;
    private final Player second;
    /** Checks the engine's clock and redraws both times every 200 ms. */
    private final ClockTicker clockTicker = new ClockTicker(this::tickClock);
    /** The last clock timeout (from CLOCK_TIMEOUT): the hero who ran out of time and the damage he took. */
    private Hero timedOutHero;
    private int timeoutDamage;
    /** How many cards the engine drew during endTurn(), so they can fly in after the pass veil. */
    private int cardsDrawnThisTurn;
    /** False until the second player's first turn: then their whole opening hand is dealt, not only the 2 cards drawn. */
    private boolean secondHandDealt;

    /** A hotseat match on this board; at the end, onEnd gets whether the first player won. */
    public MatchController(String boardName, HeroClass yours, HeroClass theirs, Consumer<Boolean> onEnd) {
        // 1. The two players, with test decks from MockCards until real decks are saved.
        first = MockCards.player("Kayne", yours, 0);
        second = MockCards.player(theirs.name(), theirs, 0);
        giveGusokuArmour(first);
        giveGusokuArmour(second);

        // 2. The engine: shuffle and deal the opening hands, then the first player's turn 1 (Ki and a draw).
        engine = new GameEngine(first, second, new Random());
        engine.getBus().subscribe(GameEvent.Type.CARD_DRAWN, event -> cardsDrawnThisTurn++);
        engine.startMatch();
        engine.beginFirstTurn();
        pauseClock();   // the engine starts the first player's clock: it waits until both Oaths are sworn

        // 3. The table, with each action plugged into the engine.
        board = new BoardView(boardName, second, PORTRAIT.get(theirs), first, PORTRAIT.get(yours));
        showClocks();
        board.setOnPlay(this::playCard);
        board.setOnAttack(this::attack);
        board.setOnPower(this::useHeroPower);
        board.setOnKatana(this::useKatana);
        board.setOnTrade(this::trade);
        board.setOnEndTurn(this::endTurn);
        board.setOnGameOver(() -> onEnd.accept(!first.getHero().isDead()));

        // A trap that goes off is shown to both players (the engine has already applied its effect).
        engine.getBus().subscribe(GameEvent.Type.TRAP_TRIGGERED, event -> board.revealTrap((TrapCard) event.target()));
        // A trap you set is shown big, then flips and glides to its place by your hero.
        engine.getBus().subscribe(GameEvent.Type.TRAP_SET, event -> board.showTrapSet((TrapCard) event.target()));
        // A clock timeout: remember who ran out and the damage, for the TIME'S UP animation once checkClock() returns.
        engine.getBus().subscribe(GameEvent.Type.CLOCK_TIMEOUT, event -> {
            timedOutHero = (Hero) event.target();
            timeoutDamage = event.amount();
        });

        // Curses, Rites and Shrines get a banner across the table.
        announceFamilyEvents();

        // 4. Each player swears an Oath behind a pass veil, then the first player's hand is dealt.
        chooseOaths(yours, theirs);
    }

    /** The table to show on screen. */
    public BoardView getBoard() {
        return board;
    }


    /** Banner colours: the glow of each family's kanji. */
    private static final Color CURSE_RED = Color.web("#c41e1e");
    private static final Color PURIFIED = Color.web("#f3ede1");
    private static final Color RITE_CRIMSON = Color.web("#dc143c");
    private static final Color SHRINE_LAVENDER = Color.web("#b7a6dd");

    /** Curses, Rites and Shrines are announced across the table, so both players see what happened. */
    private void announceFamilyEvents() {
        engine.getBus().subscribe(GameEvent.Type.CURSE_GIVEN, event -> {
            CurseCard curse = (CurseCard) event.target();
            String where = "into the enemy's hand";
            if (curse.getZone() == CurseZone.DECK) {
                where = event.amount() + " shuffled into the enemy's deck";
            }
            board.announce("呪", curse.getNameRomaji(), where, CURSE_RED);
        });
        engine.getBus().subscribe(GameEvent.Type.CURSE_TRIGGERED, event -> {
            CurseCard curse = (CurseCard) event.target();
            board.announce("祟", curse.getNameRomaji(), "the curse strikes", CURSE_RED);
        });
        engine.getBus().subscribe(GameEvent.Type.CURSE_REMOVED, event -> {
            CurseCard curse = (CurseCard) event.target();
            board.announce("祓", curse.getNameRomaji(), "purified", PURIFIED);
        });
        engine.getBus().subscribe(GameEvent.Type.RITE_PERFORMED, event -> {
            RiteCard rite = (RiteCard) event.target();
            String rise = rite.getAncestors() + " Ancestors rise";
            if (rite.getAncestors() == 1) {
                rise = "1 Ancestor rises";
            }
            board.announce("儀", rite.getNameRomaji(), rise, RITE_CRIMSON);
        });
        engine.getBus().subscribe(GameEvent.Type.SHRINE_PLACED, event -> {
            Shrine shrine = (Shrine) event.target();
            board.announce("社", shrine.getSource().getNameRomaji(), "a shrine stands", SHRINE_LAVENDER);
        });
    }

    /** Oath choice (GDD): the first player picks, the veil passes to the second, who picks; then the first player's hand is dealt. */
    private void chooseOaths(HeroClass yours, HeroClass theirs) {
        board.chooseOath(yours, firstOath -> {
            swear(first, firstOath);
            board.chooseOath(theirs, secondOath -> {
                swear(second, secondOath);
                board.passTo(first.getName(), () -> {
                    resumeClock();          // both Oaths are sworn: the first player's clock starts now
                    clockTicker.start();
                    board.dealHand();
                });
            });
            board.passTo(second.getName(), () -> {});
        });
    }

    /** The engine records the Oath (it checks the rule at the end of each turn); the board shows its scroll. */
    private void swear(Player player, String oathName) {
        engine.swearOath(player, OathType.fromName(oathName));
        board.swear(player, oathName);
    }

    /** A card dropped on a slot: the engine checks the Ki and the slot, plays it and runs its Banzai. */
    private void playCard(Card card, int slot) {
        Player active = engine.active();
        if (card instanceof CurseCard curse && active.isHeldCurse(curse)) {
            engine.removeCurse(curse);   // a Curse the enemy gave you: dropping it on the board pays its removal Ki
        } else {
            engine.playCard(card, slot, null);   // null = no target yet; choosing targets for spells and Banzai comes later
        }
        board.render();
    }

    /** An attacker and its target: the engine deals the damage both ways and removes the dead. BoardView animates, then redraws. */
    private void attack(Combatant attacker, Combatant target) {
        engine.attack(attacker, target);
    }

    /** Hero power with a target (Ninja Shuriken, Oni Jikininki) or without (Kannushi Miko: target is null). True if the engine accepted it. */
    private boolean useHeroPower(Combatant target) {
        return engine.useHeroPower(target);
    }

    /** Samurai power: equip the chosen blade. True if the engine accepted it (enough Ki, not used yet this turn). */
    private boolean useKatana(KatanaTier tier) {
        return engine.useKatana(tier);
    }

    /** A hand card dropped on the deck: the engine shuffles it in and draws another (once per turn). True if it was accepted. */
    private boolean trade(Card card) {
        return engine.trade(card);
    }

    /**
     * End-turn switch: the engine runs the end-of-turn effects and checks the Oath (a stack, or a break),
     * then starts the next player's turn with its draw.
     */
    private void endTurn() {
        cardsDrawnThisTurn = 0;
        engine.endTurn();
        afterTurnEnded();
    }

    /**
     * What the table does once the engine has ended a turn (End turn pressed, or a clock timeout):
     * flip the board to the next player and hide it behind the pass veil. Their clock waits until they click.
     */
    private void afterTurnEnded() {
        // A broken Oath can kill its own hero: then the match is over and there is no pass veil.
        if (engine.getState().isOver()) {
            board.render();
            return;
        }

        // Flip the board so the next player sits at the bottom, then hide it until they click.
        board.swapSides();
        pauseClock();
        int drawn = cardsDrawnThisTurn;   // kept now: the clock ticks reset the counter while the veil is up
        board.passTo(engine.active().getName(), () -> {
            resumeClock();
            if (!secondHandDealt) {
                // The second player's first turn: their opening hand and the cards they just drew all fly in.
                secondHandDealt = true;
                board.dealHand();
            } else if (drawn > 0) {
                board.dealDrawnCards(drawn);   // the cards they drew fly in from their deck
            }
        });
    }

    /**
     * Every ClockTicker.TICK: the engine checks the active player's bank (a timeout hits the hero and ends the turn),
     * then both times are redrawn. On a timeout the TIME'S UP animation plays, then the pass veil.
     */
    private void tickClock() {
        if (engine.getState().isOver()) {
            clockTicker.stop();
            return;
        }
        cardsDrawnThisTurn = 0;   // a timeout ends the turn inside checkClock(): count only the cards drawn by it
        boolean timedOut = engine.checkClock();
        showClocks();
        if (!timedOut) {
            return;
        }
        clockTicker.stop();
        board.showTimeout(timedOutHero, timeoutDamage, () -> {
            if (engine.getState().isOver()) {
                return;   // the timeout killed the hero: showTimeout already shows the game over
            }
            afterTurnEnded();
            clockTicker.start();
        });
    }

    /** Both chess clocks as m:ss. The active player always sits at the bottom, so the bottom clock is theirs. */
    private void showClocks() {
        if (!engine.clocksEnabled()) {
            board.hideClocks();   // no clock yet (Oaths not sworn) or none at all (Adventure)
            return;
        }
        Player active = engine.active();
        Player waiting = second;
        if (active == second) {
            waiting = first;
        }
        String yours = ClockTicker.format(engine.remainingMs(active));
        String theirs = ClockTicker.format(engine.remainingMs(waiting));
        board.showClocks(yours, theirs, true);
    }

    /** Stops the active player's clock while the table is hidden (Oath choice, pass veil), on the engine's own time. */
    private void pauseClock() {
        engine.pauseClock();
    }

    /** Starts the active player's clock again once they clicked the pass veil. */
    private void resumeClock() {
        engine.resumeClock();
    }

    /** Samurai passive Gusoku: start with 10 armour (GDD). Here until the engine applies hero passives. */
    private static void giveGusokuArmour(Player player) {
        if (player.getHero().getHeroClass() == HeroClass.SAMURAI) {
            player.getHero().gainArmour(10);
        }
    }
}