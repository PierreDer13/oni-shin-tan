package fr.onishintan.ui.match;

import fr.onishintan.engine.GameEngine;
import fr.onishintan.engine.GameEvent;
import fr.onishintan.model.Card;
import fr.onishintan.model.Combatant;
import fr.onishintan.model.HeroClass;
import fr.onishintan.model.OathType;
import fr.onishintan.model.Player;
import fr.onishintan.ui.MockCards;

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
    /** Set to true when the engine announces a draw, so the card can fly in after the pass veil. */
    private boolean drewThisTurn;

    /** A hotseat match on this board; at the end, onEnd gets whether the first player won. */
    public MatchController(String boardName, HeroClass yours, HeroClass theirs, Consumer<Boolean> onEnd) {
        // 1. The two players, with test decks from MockCards until real decks are saved.
        first = MockCards.player("Kayne", yours, 0);
        second = MockCards.player(theirs.name(), theirs, 0);
        giveGusokuArmour(first);
        giveGusokuArmour(second);

        // 2. The engine: shuffle and deal the opening hands, then the first player's turn 1 (Ki and a draw).
        engine = new GameEngine(first, second, new Random());
        engine.getBus().subscribe(GameEvent.Type.CARD_DRAWN, event -> drewThisTurn = true);
        engine.startMatch();
        engine.beginFirstTurn();

        // 3. The table, with each action plugged into the engine.
        board = new BoardView(boardName, second, PORTRAIT.get(theirs), first, PORTRAIT.get(yours));
        board.showClocks("7:00", "7:00", true);
        board.setOnPlay(this::playCard);
        board.setOnAttack(this::attack);
        board.setOnPower(this::useHeroPower);
        board.setOnTrade(this::trade);
        board.setOnEndTurn(this::endTurn);
        board.setOnGameOver(() -> onEnd.accept(!first.getHero().isDead()));

        // 4. Each player swears an Oath behind a pass veil, then the first player's hand is dealt.
        chooseOaths(yours, theirs);
    }

    /** The table to show on screen. */
    public BoardView getBoard() {
        return board;
    }

    /** Oath choice (GDD): the first player picks, the veil passes to the second, who picks; then the first player's hand is dealt. */
    private void chooseOaths(HeroClass yours, HeroClass theirs) {
        board.chooseOath(yours, firstOath -> {
            swear(first, firstOath);
            board.chooseOath(theirs, secondOath -> {
                swear(second, secondOath);
                board.passTo(first.getName(), board::dealHand);
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
        engine.playCard(card, slot, null);   // null = no target yet; choosing targets for spells and Banzai comes later
        board.render();
    }

    /** An attacker and its target: the engine deals the damage both ways and removes the dead. BoardView animates, then redraws. */
    private void attack(Combatant attacker, Combatant target) {
        engine.attack(attacker, target);
    }

    /** Hero power. Pierre's engine doesn't have powers yet (engine-hero-powers), so for now it only pays the Ki. */
    private void useHeroPower() {
        engine.active().spendKi(HeroPowerView.COST);
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
        drewThisTurn = false;
        engine.endTurn();

        // A broken Oath can kill its own hero: then the match is over and there is no pass veil.
        if (engine.getState().isOver()) {
            board.render();
            return;
        }

        // Flip the board so the next player sits at the bottom, then hide it until they click.
        board.swapSides();
        board.passTo(engine.active().getName(), () -> {
            if (drewThisTurn) {
                board.dealDrawnCard();   // the card they drew flies in from their deck
            }
        });
    }

    /** Samurai passive Gusoku: start with 10 armour (GDD). Here until the engine applies hero passives. */
    private static void giveGusokuArmour(Player player) {
        if (player.getHero().getHeroClass() == HeroClass.SAMURAI) {
            player.getHero().gainArmour(10);
        }
    }
}