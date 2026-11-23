package fr.onishintan.ui.match.dialog;

import fr.onishintan.model.GodaiElement;
import fr.onishintan.ui.match.BoardView;

import java.util.function.Consumer;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * Takeda Shingen's hero power (Godai): a stone gorinto rises out of the table, one stone per element,
 * from Earth (the cube at the base) to Void (the jewel at the top). Hovering a stone lights it in its element's colour
 * and names its effect; clicking it picks that element. A click outside the stones closes it without using the power.
 */
public final class GodaiPicker extends Pane {
    private static final String PICTURE = "/images/hud/godai-picker.png";
    /** The picture's size (887 x 1774) and the height it is drawn at on the table. */
    private static final double PICTURE_W = 887, PICTURE_H = 1774, SHOWN_H = 620;
    /** Measured on the picture (its own pixels): where each stone starts and ends, from Earth at the bottom to Void at the top. */
    private static final double[][] STONE_Y = { { 1190, 1560 }, { 850, 1190 }, { 575, 850 }, { 290, 575 }, { 0, 290 } };
    /** Each element's glow: Earth ochre, Water blue, Fire orange-red, Wind pale green, Void pale lavender. */
    private static final Color[] GLOW = { Color.web("#d9a441"), Color.web("#3a8dde"), Color.web("#ff5a1f"), Color.web("#9fe39a"), Color.web("#cdb6ff") };
    /** How long the gorinto takes to rise. */
    private static final Duration RISE = Duration.millis(700);

    private final Label info = new Label();

    /** onPick gets the chosen element; onCancel runs when you click outside the stones. */
    public GodaiPicker(Consumer<GodaiElement> onPick, Runnable onCancel) {
        setPrefSize(BoardView.W, BoardView.H);

        // A light veil over the whole table: a click on it (outside the stones) cancels.
        Rectangle outside = new Rectangle(BoardView.W, BoardView.H, Color.rgb(0, 0, 0, 0.35));
        outside.setOnMousePressed(event -> {
            close();
            onCancel.run();
        });
        getChildren().add(outside);

        // The gorinto in the middle of the table, cut into its five stones (and its plinth, which can't be clicked).
        double k = SHOWN_H / PICTURE_H;   // picture pixels -> table pixels
        double x = (BoardView.W - PICTURE_W * k) / 2;
        double y = (BoardView.H - SHOWN_H) / 2;
        Pane pagoda = new Pane();
        pagoda.relocate(x, y);
        Image picture = BoardView.image(PICTURE).getImage();
        pagoda.getChildren().add(band(picture, STONE_Y[0][1], PICTURE_H, k));   // the plinth, the incense and the leaves
        GodaiElement[] elements = GodaiElement.values();   // Earth first: the base stone
        for (int i = 0; i < elements.length; i++) {
            pagoda.getChildren().add(stone(picture, elements[i], STONE_Y[i], GLOW[i], onPick, k));
        }
        getChildren().add(pagoda);

        // What the hovered stone does, on the right of the gorinto.
        info.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #f3ede1;"
                + " -fx-background-color: rgba(20,12,8,0.85); -fx-background-radius: 10; -fx-padding: 10 16;");
        info.setWrapText(true);
        info.setMaxWidth(340);
        info.relocate(x + PICTURE_W * k + 20, BoardView.H / 2 - 40);
        info.setMouseTransparent(true);
        showInfo(null);
        getChildren().add(info);

        rise(pagoda, k);
    }

    /** One stone: its band of the picture, which glows in its colour and names its effect on hover, and picks its element on click. */
    private ImageView stone(Image picture, GodaiElement element, double[] from, Color glow, Consumer<GodaiElement> onPick, double k) {
        ImageView stone = band(picture, from[0], from[1], k);
        stone.setPickOnBounds(false);   // only the stone itself reacts, not the empty corners around it
        stone.setOnMouseEntered(event -> {
            stone.setEffect(new DropShadow(28, glow));
            showInfo(element);
        });
        stone.setOnMouseExited(event -> {
            stone.setEffect(null);
            showInfo(null);
        });
        stone.setOnMousePressed(event -> {
            close();
            onPick.accept(element);
        });
        return stone;
    }

    /** The rows `top` to `bottom` of the picture, drawn in place. */
    private static ImageView band(Image picture, double top, double bottom, double k) {
        ImageView band = new ImageView(picture);
        band.setViewport(new Rectangle2D(0, top, PICTURE_W, bottom - top));
        band.setFitWidth(PICTURE_W * k);
        band.setFitHeight((bottom - top) * k);
        band.relocate(0, top * k);
        return band;
    }

    /** The hovered element's name and effect, or the title when no stone is hovered. */
    private void showInfo(GodaiElement element) {
        if (element == null) {
            info.setText("五大  Godai\nChoose an element");
            return;
        }
        info.setText(element.getKanji() + "  " + element.getDisplayName() + "\n" + element.getEffect());
    }

    /** Opening: the gorinto rises out of the table, stone after stone, the base first. */
    private void rise(Pane pagoda, double k) {
        Rectangle shown = new Rectangle(0, PICTURE_H * k, PICTURE_W * k, 0);   // nothing shows at first
        pagoda.setClip(shown);
        pagoda.setMouseTransparent(true);   // no click on a stone until it has risen
        Timeline up = new Timeline(new KeyFrame(RISE,
                new KeyValue(shown.yProperty(), 0, Interpolator.EASE_OUT),
                new KeyValue(shown.heightProperty(), PICTURE_H * k, Interpolator.EASE_OUT)));
        up.setOnFinished(event -> {
            pagoda.setClip(null);
            pagoda.setMouseTransparent(false);
        });
        up.play();
    }

    /** Removes the picker from the table. */
    private void close() {
        ((Pane) getParent()).getChildren().remove(this);
    }
}
