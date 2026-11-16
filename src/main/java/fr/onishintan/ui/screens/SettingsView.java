package fr.onishintan.ui.screens;

import fr.onishintan.ui.app.AudioSettings;
import fr.onishintan.ui.app.Images;
import fr.onishintan.ui.app.SceneRouter;
import fr.onishintan.ui.app.Screen;
import fr.onishintan.ui.app.ScreenGrid;
import fr.onishintan.ui.app.Sound;

import javafx.beans.property.DoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/** Settings: the music and sound-effect volumes as two sliders (AudioSettings keeps them while the game runs). BACK goes home. */
public class SettingsView extends Pane {
    private static final double W = ScreenGrid.W, H = ScreenGrid.H;
    private static final String FONT = "-fx-font-family: 'Yu Mincho'; -fx-font-weight: bold; -fx-text-fill: #f3ede1;";
    /** Width of each volume scroll. */
    private static final double BAR_W = 760;

    /** Everything is placed on this 1600 x 900 grid, which is scaled to fit the window. */
    private final Pane grid = new Pane();

    public SettingsView(SceneRouter router) {
        ScreenGrid.fitToWindow(this, grid);

        // The wooden shelves under a dark veil, like the other menus.
        ImageView background = new ImageView(Images.get("/images/deck-builder/shelves.png", W, H));
        Rectangle veil = new Rectangle(W, H, Color.rgb(0, 0, 0, 0.6));

        Label title = label("設定  Settings", 44);
        HBox titleRow = new HBox(title);
        titleRow.setAlignment(Pos.CENTER);
        titleRow.setPrefWidth(W);
        titleRow.relocate(0, 120);

        GridPane rows = new GridPane();
        rows.setHgap(30);
        rows.setVgap(20);
        rows.setAlignment(Pos.CENTER);
        rows.setPrefWidth(W);
        rows.relocate(0, 250);
        addRow(rows, 0, "音楽  Music", AudioSettings.MUSIC, false);
        addRow(rows, 1, "効果音  Effects", AudioSettings.EFFECTS, true);

        ImageView back = HomeView.back(() -> router.goTo(Screen.HOME));
        grid.getChildren().addAll(background, veil, titleRow, rows, back);
    }

    /**
     * One row: the name and the painted volume scroll (VolumeBar), which shows its own value in %.
     * clickOnRelease: a click plays when the mouse is let go, to hear the new level (for the effects).
     */
    private void addRow(GridPane rows, int row, String name, DoubleProperty volume, boolean clickOnRelease) {
        Runnable onRelease = () -> {};
        if (clickOnRelease) {
            onRelease = () -> Sound.play(HomeView.CLICK);
        }
        VolumeBar bar = new VolumeBar(volume, BAR_W, onRelease);

        rows.add(label(name, 28), 0, row);
        rows.add(bar, 1, row);
    }

    /** A bold light Yu Mincho label of this size. */
    private static Label label(String words, int size) {
        Label label = new Label(words);
        label.setStyle(FONT + " -fx-font-size: " + size + ";");
        return label;
    }
}