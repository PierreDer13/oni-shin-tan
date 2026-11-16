package fr.onishintan.ui.screens;

import fr.onishintan.ui.app.Images;

import javafx.beans.property.DoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;

/**
 * A volume slider drawn as the painted scroll (images/frames/volume-bar.png): the red fill (volume-fill.png) grows along
 * its channel and the sakura knob (volume-knob.png) sits at its end. Press or drag anywhere on the scroll to set the volume (0 to 1).
 */
public class VolumeBar extends Pane {
    private static final String BAR = "/images/frames/volume-bar.png";
    private static final String FILL = "/images/frames/volume-fill.png";
    private static final String KNOB = "/images/frames/volume-knob.png";
    /** The bar picture's height for its width (1100 x 288). */
    private static final double PICTURE_RATIO = 288.0 / 1100;
    /** Where the empty channel is in the bar picture, as parts of its width and height. */
    private static final double CHANNEL_LEFT = 0.228, CHANNEL_RIGHT = 0.887, CHANNEL_TOP = 0.426, CHANNEL_BOTTOM = 0.511;
    /** The knob's width as a part of the bar's width. */
    private static final double KNOB_SIZE = 86.0 / 1100;

    private final DoubleProperty volume;
    private final double channelX, channelWidth;
    private final ImageView fill;
    private final ImageView knob;

    /** A scroll `width` wide that shows and changes `volume`; onRelease runs when the mouse is let go (to hear the new level). */
    public VolumeBar(DoubleProperty volume, double width, Runnable onRelease) {
        this.volume = volume;
        double height = width * PICTURE_RATIO;
        channelX = width * CHANNEL_LEFT;
        channelWidth = width * (CHANNEL_RIGHT - CHANNEL_LEFT);
        double channelY = height * CHANNEL_TOP;
        double channelHeight = height * (CHANNEL_BOTTOM - CHANNEL_TOP);

        ImageView bar = new ImageView(Images.get(BAR, width, height));

        // The red fill: the painted red channel stretched to the volume's length, its left end rounded like the channel.
        fill = new ImageView(Images.get(FILL));
        fill.setFitHeight(channelHeight);
        fill.relocate(channelX, channelY);
        Rectangle roundedEnd = new Rectangle(channelWidth, channelHeight);
        roundedEnd.setArcWidth(channelHeight);
        roundedEnd.setArcHeight(channelHeight);
        fill.setClip(roundedEnd);

        // The sakura knob, centred on the channel's height, at the end of the fill.
        Image knobPicture = Images.get(KNOB);
        double knobWidth = width * KNOB_SIZE;
        double knobHeight = knobWidth * knobPicture.getHeight() / knobPicture.getWidth();
        knob = new ImageView(knobPicture);
        knob.setFitWidth(knobWidth);
        knob.setFitHeight(knobHeight);
        knob.setLayoutY(channelY + channelHeight / 2 - knobHeight / 2);

        // The volume in %, written on the paper above the channel, centred on it.
        Label percent = new Label();
        percent.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: " + Math.round(height * 0.13)
                + "; -fx-font-weight: bold; -fx-text-fill: #1f1b17;");
        percent.textProperty().bind(volume.multiply(100).asString("%.0f %%"));
        HBox percentRow = new HBox(percent);
        percentRow.setAlignment(Pos.CENTER);
        percentRow.setPrefWidth(channelWidth);
        percentRow.relocate(channelX, channelY - height * 0.22);
        percentRow.setMouseTransparent(true);

        getChildren().addAll(bar, fill, knob, percentRow);
        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(width, height);

        // Press or drag: the volume follows the mouse; letting go runs onRelease.
        setOnMousePressed(event -> setFromMouse(event.getX()));
        setOnMouseDragged(event -> setFromMouse(event.getX()));
        setOnMouseReleased(event -> onRelease.run());
        volume.addListener((property, oldValue, newValue) -> show());
        show();
    }

    /** The volume at mouse x: 0 at the channel's left end, 1 at its right end. */
    private void setFromMouse(double x) {
        double part = (x - channelX) / channelWidth;
        part = Math.max(0, Math.min(1, part));
        volume.set(part);
    }

    /** Moves the end of the fill and the knob to the current volume. */
    private void show() {
        double length = channelWidth * volume.get();
        fill.setVisible(length > 0);
        fill.setFitWidth(Math.max(1, length));   // fitWidth 0 would mean "natural size", so never 0
        knob.setLayoutX(channelX + length - knob.getFitWidth() / 2);
    }
}