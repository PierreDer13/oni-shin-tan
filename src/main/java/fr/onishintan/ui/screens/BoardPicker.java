package fr.onishintan.ui.screens;

import fr.onishintan.ui.app.Images;

import java.util.function.Consumer;

import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/** The board picker: the battlefields a match can be played on, two by two, as clickable pictures with their names. */
public final class BoardPicker {
    /** Each board: its id (images/boards/<id>.png) and its name, in the order shown. */
    private static final String[][] BOARDS = {
            { "izakaya", "居酒屋  Izakaya" },
            { "town", "町  Machi" },
            { "ship", "台風  Taifū" },
            { "fuji", "富士  Fuji" } };
    /** Size of each board's picture (the boards are 1600 x 900), and how round its corners are. */
    private static final double PICTURE_W = 480, PICTURE_H = 270, CORNER = 24;
    private static final Color GOLD = Color.web("#ffd84a");

    private BoardPicker() {
        // Only a static helper: nobody creates a BoardPicker.
    }

    /** The boards two by two; clicking one gives its id to onChosen. */
    public static GridPane grid(Consumer<String> onChosen) {
        GridPane grid = new GridPane();
        grid.setHgap(40);
        grid.setVgap(24);
        grid.setAlignment(Pos.CENTER);
        for (int i = 0; i < BOARDS.length; i++) {
            int column = i % 2;
            int row = i / 2;
            grid.add(choice(BOARDS[i][0], BOARDS[i][1], onChosen), column, row);
        }
        return grid;
    }

    /** One board: its picture with round corners and a shadow, its name under it; a gold border while hovered; clicking it chooses it. */
    private static VBox choice(String id, String name, Consumer<String> onChosen) {
        ImageView picture = new ImageView(Images.get("/images/boards/" + id + ".png", PICTURE_W, PICTURE_H));
        picture.setClip(roundedRectangle());

        // The gold border, shown only while the mouse is on the board.
        Rectangle border = roundedRectangle();
        border.setFill(Color.TRANSPARENT);
        border.setStroke(GOLD);
        border.setStrokeWidth(5);
        border.setVisible(false);

        Group framed = new Group(picture, border);
        framed.setEffect(new DropShadow(15, Color.BLACK));   // on the group, so the round clip doesn't cut the shadow off

        Label label = new Label(name);
        label.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: #f3ede1;");

        VBox choice = new VBox(10, framed, label);
        choice.setAlignment(Pos.TOP_CENTER);
        // addEventHandler, so HomeView.clickable's own hover (grow and shadow) keeps working too.
        choice.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> border.setVisible(true));
        choice.addEventHandler(MouseEvent.MOUSE_EXITED, event -> border.setVisible(false));
        HomeView.clickable(choice, false, () -> onChosen.accept(id));
        return choice;
    }

    /** A rectangle the size of a board's picture, with round corners: the picture's clip and the hover border. */
    private static Rectangle roundedRectangle() {
        Rectangle rectangle = new Rectangle(PICTURE_W, PICTURE_H);
        rectangle.setArcWidth(CORNER);
        rectangle.setArcHeight(CORNER);
        return rectangle;
    }
}