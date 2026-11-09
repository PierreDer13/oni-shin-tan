package fr.onishintan.ui;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

import fr.onishintan.engine.GameState;
import fr.onishintan.engine.TurnManager;
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
    static BoardView match(String boardName, HeroClass yours, HeroClass theirs, Consumer<Boolean> onEnd) {
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
        board.getEndTurn().setOnAction(e -> {
            turns.endTurn();
            board.swapSides();
            board.passTo(turns.getState().active().getName());
        });
        board.setOnGameOver(() -> onEnd.accept(!p1.getHero().isDead()));
        return board;
    }

    public static void main(String[] args) { launch(args); }
}