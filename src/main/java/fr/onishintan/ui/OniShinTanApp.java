package fr.onishintan.ui;

import java.util.Random;

import fr.onishintan.engine.GameState;
import fr.onishintan.engine.TurnManager;
import fr.onishintan.model.HeroClass;
import fr.onishintan.model.Minion;
import fr.onishintan.model.MinionCard;
import fr.onishintan.model.Player;
import fr.onishintan.model.Player;
import fr.onishintan.model.Rarity;
import fr.onishintan.model.Weapon;
import fr.onishintan.model.WeaponCard;

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
                router.register(Screen.MATCH, () -> {
            Player p1 = MockCards.player("Kayne", HeroClass.SAMURAI, 0), p2 = MockCards.player("Pierre", HeroClass.ONI, 0);
            p1.getHero().gainArmour(10);   // Samurai passive Gusoku (GDD), until the engine applies it
            TurnManager turns = new TurnManager(new GameState(p1, p2), new Random());
            turns.startMatch();
            turns.startTurn();
            BoardView board = new BoardView("izakaya", p2, "shuten-doji", p1, "yasuke");
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
            return board;
        });
        router.goTo(Screen.HOME);
        stage.setTitle("Oni Shin Tan");
        stage.show();
    }

    public static void main(String[] args) { launch(args); }
}