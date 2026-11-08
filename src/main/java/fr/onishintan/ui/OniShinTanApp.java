package fr.onishintan.ui;

import fr.onishintan.model.HeroClass;
import fr.onishintan.model.Minion;
import fr.onishintan.model.MinionCard;
import fr.onishintan.model.Player;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.layout.FlowPane;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import javafx.scene.media.Media;

public class OniShinTanApp extends Application {
    private MediaPlayer music;

    @Override
    public void start(Stage stage) {
        SceneRouter router = new SceneRouter(stage);
        music = new MediaPlayer(new Media(getClass().getResource("/sounds/music.mp3").toExternalForm()));
        music.setCycleCount(MediaPlayer.INDEFINITE);
        music.setVolume(0.35);
        music.play();
        router.register(Screen.HOME, () -> new HomeView(router));
        router.register(Screen.INVENTORY, () -> {
            FlowPane grid = new FlowPane(20, 20);
            grid.setAlignment(Pos.CENTER);
            MockCards.ALL.forEach(c -> grid.getChildren().add(new CardView(c, 230)));
            return grid;
        });
        router.register(Screen.MATCH, () -> {
            Player enemy = MockCards.player("Pierre", HeroClass.ONI, 7);
            Player you = MockCards.player("Kayne", HeroClass.SAMURAI, 3);
            you.getSide().getMinions().get(0).takeDamage(2);
            for (int i = 0; i < Player.KI_CAP; i++) { you.refillKi(); enemy.refillKi(); }
            for (int i = 0; i < 7; i++) { if (i < 6) you.draw(); enemy.draw(); }
            enemy.gainTemporaryKi(1);
            you.getSide().getMinions().forEach(Minion::onTurnStart);
            BoardView board = new BoardView("izakaya", enemy, "shuten-doji", you, "yasuke");
            board.showClocks("1:30", "1:12", true);
            board.setOnPlay((card, slot) -> {
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
                you.getSide().removeDead();
                enemy.getSide().removeDead();
            });
            return board;
        });
        router.goTo(Screen.HOME);
        stage.setTitle("Oni Shin Tan");
        stage.show();
    }

    public static void main(String[] args) { launch(args); }
}