package fr.onishintan.ui.screens;

import fr.onishintan.data.SaveManager;
import fr.onishintan.model.HeroClass;
import fr.onishintan.ui.app.Images;
import fr.onishintan.ui.app.Profile;
import fr.onishintan.ui.app.Rei;
import fr.onishintan.ui.app.SceneRouter;
import fr.onishintan.ui.app.Screen;
import fr.onishintan.ui.app.ScreenGrid;

import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Profile: your name (type to change it), your Rei rank with its stone, your games per class,
 * and the seven Rei stones from Ishikage to Ten'nyū, lit up to your rank and dimmed above it.
 * The BACK sign goes back to the home screen.
 */
public class ProfileView extends Pane {
    private static final String PAPER = "#f3ede1";
    private static final double STONE_W = 120, STONE_GAP = 30;
    private static final List<HeroClass> CLASSES = List.of(HeroClass.SAMURAI, HeroClass.NINJA, HeroClass.KANNUSHI, HeroClass.ONI);

    private final Pane grid = new Pane();

    public ProfileView(SceneRouter router) {
        ScreenGrid.fitToWindow(this, grid);
        setStyle("-fx-background-color: black;");
        SaveManager.Stats stats = Profile.stats();

        ImageView background = new ImageView(Images.get("/images/profile/archive-room.png"));   // the archive room
        background.setFitWidth(ScreenGrid.W);
        background.setFitHeight(ScreenGrid.H);
        Rectangle veil = new Rectangle(ScreenGrid.W, ScreenGrid.H, Color.rgb(0, 0, 0, 0.35));
        Label title = text("プロフィール   PROFILE", 40);
        title.relocate(560, 30);
        grid.getChildren().addAll(background, veil, title, HomeView.back(() -> router.goTo(Screen.HOME)),
                name(), rank(stats.rei()), record(stats), stones(stats.rei()));
    }

    /** Your name: type a new one and press Enter, or click elsewhere, to keep it. */
    private TextField name() {
        TextField field = new TextField(Profile.name());
        field.setFont(Font.font("Yu Mincho", FontWeight.BOLD, 30));
        field.setStyle("-fx-background-color: rgba(20,12,8,0.85); -fx-text-fill: " + PAPER + ";"
                + " -fx-border-color: #c9a227; -fx-border-radius: 6; -fx-background-radius: 6;");
        field.setPrefWidth(420);
        field.relocate(120, 130);
        field.setOnAction(e -> Profile.rename(field.getText()));
        field.focusedProperty().addListener((property, was, focused) -> {
            if (!focused) {
                Profile.rename(field.getText());
            }
        });
        return field;
    }

    /** Your Rei: the stone of your rank, its name, and your points out of 6000. */
    private VBox rank(int rei) {
        Rei.Stone stone = Rei.stoneOf(rei);
        VBox box = new VBox(8, stoneImage(stone, 220), text(stone.kanji() + "  " + stone.name(), 30), text(rei + " / " + Rei.MAX + " Rei", 24));
        box.setAlignment(Pos.TOP_CENTER);
        box.setPrefWidth(420);
        box.relocate(120, 210);
        return box;
    }

    /** Games in all, then for each class: played, won and lost. */
    private VBox record(SaveManager.Stats stats) {
        VBox box = new VBox(18,
                text("戦績   RECORD", 32),
                text(stats.played() + " games   ·   " + stats.totalWins() + " won   ·   " + stats.totalLosses() + " lost", 24));
        for (HeroClass c : CLASSES) {
            String line = c.getKanji() + "  " + c.getDisplayName() + "   " + stats.playedOf(c) + " played   ·   "
                    + stats.winsOf(c) + " won   ·   " + stats.lossesOf(c) + " lost";
            box.getChildren().add(text(line, 22));
        }
        box.relocate(780, 150);
        return box;
    }

    /** The seven Rei stones from the lowest to the highest: yours and those below are lit, those above are dimmed. */
    private HBox stones(int rei) {
        HBox row = new HBox(STONE_GAP);
        for (Rei.Stone stone : Rei.STONES) {
            ImageView picture = stoneImage(stone, STONE_W);
            if (rei < stone.from()) {
                picture.setEffect(new ColorAdjust(0, -1, -0.6, 0));   // not reached yet: grey and dark
            }
            Tooltip.install(picture, new Tooltip(stone.name() + " " + stone.kanji() + ": from " + stone.from() + " Rei"));
            row.getChildren().add(picture);
        }
        double width = Rei.STONES.size() * STONE_W + (Rei.STONES.size() - 1) * STONE_GAP;
        row.relocate((ScreenGrid.W - width) / 2, 700);
        return row;
    }

    /** One stone's picture, `width` wide. */
    private static ImageView stoneImage(Rei.Stone stone, double width) {
        ImageView picture = new ImageView(Images.get("/images/rei-stones/" + stone.id() + ".png"));
        picture.setFitWidth(width);
        picture.setPreserveRatio(true);
        return picture;
    }

    /** Light text with a dark shadow, readable over the background. */
    private static Label text(String words, double size) {
        Label label = new Label(words);
        label.setFont(Font.font("Yu Mincho", FontWeight.BOLD, size));
        label.setTextFill(Color.web(PAPER));
        label.setEffect(new DropShadow(6, Color.BLACK));
        return label;
    }
}
