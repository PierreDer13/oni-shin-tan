package fr.onishintan.ui.app;

import javafx.beans.InvalidationListener;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Scale;

/** Every screen is drawn on a fixed 1600 x 900 grid; this keeps that grid as big as the window allows, without stretching it. */
public final class ScreenGrid {
    public static final double W = 1600, H = 900;

    private ScreenGrid() {
        // Only a static helper: nobody creates a ScreenGrid.
    }

    /** Puts `grid` inside `screen` and rescales it every time the window is resized. */
    public static void fitToWindow(Pane screen, Pane grid) {
        Scale scale = new Scale();
        grid.getTransforms().add(scale);
        InvalidationListener fit = change -> {
            double factor = Math.min(screen.getWidth() / W, screen.getHeight() / H);
            scale.setX(factor);
            scale.setY(factor);
        };
        screen.widthProperty().addListener(fit);
        screen.heightProperty().addListener(fit);
        screen.getChildren().add(grid);
    }
}