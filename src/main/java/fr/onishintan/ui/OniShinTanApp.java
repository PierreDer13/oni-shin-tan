package fr.onishintan.ui;

import fr.onishintan.model.HeroClass;
import fr.onishintan.model.Player;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.layout.FlowPane;

public class OniShinTanApp extends Application {
        @Override
    public void start(Stage stage) {
        SceneRouter router = new SceneRouter(stage);
        router.register(Screen.HOME, () -> {
            VBox menu = new VBox(10);
            menu.setAlignment(Pos.CENTER);
            for (Screen s : Screen.values()) {
                if (s == Screen.HOME) continue;
                Button b = new Button(s.name());
                b.setOnAction(e -> router.goTo(s));
                menu.getChildren().add(b);
            }
            return menu;
        });
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
            for (int i = 0; i < 5; i++) { you.refillKi(); enemy.refillKi(); }
            you.spendKi(2);
            BoardView board = new BoardView("izakaya", enemy, "shuten-doji", you, "yasuke");
            board.showClocks("1:30", "1:12", true);
            return board;
        });
        
        router.goTo(Screen.HOME);
        stage.setTitle("Oni Shin Tan");
        stage.show();
    }

    public static void main(String[] args) { launch(args); }
}