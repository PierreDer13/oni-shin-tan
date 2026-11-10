package fr.onishintan.ui;
import fr.onishintan.ui.match.BoardView;
import fr.onishintan.ui.match.HeroPowerView;
import fr.onishintan.ui.cards.CardView;
import fr.onishintan.ui.screens.HomeView;
import fr.onishintan.ui.screens.ClassSelectView;
import fr.onishintan.ui.app.SceneRouter;
import fr.onishintan.ui.app.Screen;
import fr.onishintan.ui.app.Sound;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Optional;
import java.util.function.Consumer;

import fr.onishintan.engine.GameState;
import fr.onishintan.engine.TurnManager;
import fr.onishintan.model.Card;
import fr.onishintan.model.HeroClass;
import fr.onishintan.model.Minion;
import fr.onishintan.model.MinionCard;
import fr.onishintan.model.Player;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.layout.FlowPane;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

public class OniShinTanApp extends Application {
    private MediaPlayer music;

    @Override
    public void start(Stage stage) {
        SceneRouter router = new SceneRouter(stage);
        music = Sound.music("music", 0.35);
        router.register(Screen.HOME, () -> new HomeView(router));
        router.register(Screen.CLASS_SELECT, () -> new ClassSelectView(router));
        router.register(Screen.INVENTORY, () -> {
            FlowPane grid = new FlowPane(20, 20);
            grid.setAlignment(Pos.CENTER);
            MockCards.ALL.forEach(c -> grid.getChildren().add(new CardView(c, 230)));
            return grid;
        });
        router.register(Screen.MATCH, () -> match("izakaya", HeroClass.SAMURAI, HeroClass.ONI, won -> router.goTo(Screen.HOME)));
        router.goTo(Screen.HOME);
        stage.setTitle("Oni Shin Tan");
        stage.show();
    }

    private static final Map<HeroClass, String> PORTRAIT = Map.of(HeroClass.SAMURAI, "yasuke", HeroClass.NINJA, "hattori-hanzo",
            HeroClass.KANNUSHI, "kada-no-azumamaro", HeroClass.ONI, "shuten-doji");

    /** A hotseat match: you play first, at the bottom; at the end, onEnd gets whether you won. */
    public static BoardView match(String boardName, HeroClass yours, HeroClass theirs, Consumer<Boolean> onEnd) {
        Player p1 = MockCards.player("Kayne", yours, 0), p2 = MockCards.player(theirs.name(), theirs, 0);
        for (Player p : List.of(p1, p2)) if (p.getHero().getHeroClass() == HeroClass.SAMURAI) p.getHero().gainArmour(10);   // Gusoku (GDD), until the engine applies it
        TurnManager turns = new TurnManager(new GameState(p1, p2), new Random());
        turns.startMatch();
        turns.startTurn();
        BoardView board = new BoardView(boardName, p2, PORTRAIT.get(theirs), p1, PORTRAIT.get(yours));
        board.showClocks("7:00", "7:00", true);
        board.setOnPlay((card, slot) -> {
            Player you = turns.getState().active();
            if (!(card instanceof MinionCard m) || you.getSide().isFull()) return;
            you.spendKi(card.getKiCost());
            you.removeFromHand(card);
            you.getSide().place(new Minion(m), slot);
            board.render();
        });
        board.setOnAttack((attacker, target) -> {
            target.takeDamage(attacker.getAttack());
            if (target instanceof Minion) attacker.takeDamage(target.getAttack());
            attacker.markAttacked();
            p1.getSide().removeDead();
            p2.getSide().removeDead();
        });
        board.setOnPower(() -> turns.getState().active().spendKi(HeroPowerView.COST));
        board.setOnTrade(card -> { turns.swapCard(card); board.render(); });
        board.setOnEndTurn(() -> {
            // Mock: the Oath counts as kept until the engine checks Oaths.
            board.addStack(turns.getState().active());

            // End the turn. The engine starts the next player's turn and gives back the card they drew,
            // or nothing if their deck is empty or their hand is full (then the card is burned).
            Optional<Card> drawnCard = turns.endTurn();

            // Flip the board so the next player sits at the bottom, then hide it behind the "Pass to" veil.
            board.swapSides();
            String nextPlayer = turns.getState().active().getName();
            board.passTo(nextPlayer, () -> {
                // The next player clicked the veil: now show them the card they drew.
                if (drawnCard.isPresent()) {
                    board.dealDrawnCard();
                }
            });
        });
        board.setOnGameOver(() -> onEnd.accept(!p1.getHero().isDead()));
        board.chooseOath(yours, o1 -> { board.swear(p1, o1); board.chooseOath(theirs, o2 -> { board.swear(p2, o2); board.passTo(p1.getName(), board::dealHand); }); board.passTo(p2.getName(), () -> {}); });
        return board;
    }

    public static void main(String[] args) { launch(args); }
}