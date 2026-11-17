package fr.onishintan.ui.screens;

import fr.onishintan.ui.app.AudioSettings;
import fr.onishintan.ui.app.ScreenGrid;
import fr.onishintan.ui.app.Sound;

import javafx.beans.property.DoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

/**
 * The settings, laid over the current screen (home or a match) on the 1600 x 900 grid: music and effect volumes,
 * the window size and, in a match, Surrender. Close, or a click on the dark veil around the panel, takes it away.
 */
public class SettingsPanel extends StackPane {
    private static final double W = ScreenGrid.W, H = ScreenGrid.H;
    private static final String FONT = "-fx-font-family: 'Yu Mincho'; -fx-font-weight: bold; -fx-text-fill: #f3ede1;";
    /** Width of each volume scroll. */
    private static final double BAR_W = 620;
    /** The window sizes offered (width, height); Fullscreen comes after them. */
    private static final int[][] SIZES = { { 1280, 720 }, { 1600, 900 }, { 1920, 1080 } };

    /** onSurrender: what Surrender does in a match; null on the home screen, where there is no Surrender button. */
    public SettingsPanel(Runnable onSurrender) {
        Rectangle veil = new Rectangle(W, H, Color.rgb(0, 0, 0, 0.6));
        veil.setOnMousePressed(e -> close());

        GridPane rows = new GridPane();
        rows.setHgap(30);
        rows.setVgap(24);
        rows.setAlignment(Pos.CENTER);
        addRow(rows, 0, "音楽  Music", volume(AudioSettings.MUSIC, false));
        addRow(rows, 1, "効果音  Effects", volume(AudioSettings.EFFECTS, true));
        addRow(rows, 2, "画面  Screen", sizes());

        HBox actions = new HBox(30);
        actions.setAlignment(Pos.CENTER);
        if (onSurrender != null) {
            actions.getChildren().add(button("降参  Surrender", () -> {
                close();
                onSurrender.run();
            }));
        }
        actions.getChildren().add(button("閉じる  Close", this::close));

        VBox panel = new VBox(36, label("設定  Settings", 44), rows, actions);
        panel.setAlignment(Pos.CENTER);
        panel.setMaxSize(1100, 560);
        // A dark rounded box until the painted settings background exists.
        panel.setStyle("-fx-background-color: rgba(20,12,8,0.92); -fx-background-radius: 16; -fx-padding: 40;");
        getChildren().addAll(veil, panel);
        setPrefSize(W, H);
    }

    /** One row: the name on the left, its control on the right. */
    private static void addRow(GridPane rows, int row, String name, Node control) {
        rows.add(label(name, 28), 0, row);
        rows.add(control, 1, row);
    }

    /** A painted volume scroll; for the effects, a click plays when the mouse is let go, to hear the new level. */
    private static VolumeBar volume(DoubleProperty volume, boolean clickOnRelease) {
        Runnable onRelease = () -> {};
        if (clickOnRelease) {
            onRelease = () -> Sound.play(HomeView.CLICK);
        }
        return new VolumeBar(volume, BAR_W, onRelease);
    }

    /** One button per window size, then Fullscreen. */
    private HBox sizes() {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        for (int[] size : SIZES) {
            row.getChildren().add(button(size[0] + "×" + size[1], () -> resizeWindow(size[0], size[1])));
        }
        row.getChildren().add(button("Fullscreen", () -> stage().setFullScreen(true)));
        return row;
    }

    /** Leaves fullscreen and gives the window this size, centred on the screen. */
    private void resizeWindow(double width, double height) {
        Stage stage = stage();
        stage.setFullScreen(false);
        stage.setWidth(width);
        stage.setHeight(height);
        stage.centerOnScreen();
    }

    /** The game's window. */
    private Stage stage() {
        return (Stage) getScene().getWindow();
    }

    /** Takes the panel off the screen it was laid over. */
    private void close() {
        ((Pane) getParent()).getChildren().remove(this);
    }

    /** A light text button on a faint paper background; it clicks and runs its action when pressed. */
    private static Label button(String words, Runnable action) {
        Label button = label(words, 22);
        button.setStyle(button.getStyle() + " -fx-background-color: rgba(243,237,225,0.15); -fx-background-radius: 8; -fx-padding: 6 14;");
        HomeView.clickable(button, false, action);
        return button;
    }

    /** A bold light Yu Mincho label of this size. */
    private static Label label(String words, int size) {
        Label label = new Label(words);
        label.setStyle(FONT + " -fx-font-size: " + size + ";");
        return label;
    }
}
