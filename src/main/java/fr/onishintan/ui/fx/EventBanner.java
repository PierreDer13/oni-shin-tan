package fr.onishintan.ui.fx;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * A short announcement across the middle of the table (a Curse given, a Rite performed, a Shrine placed...):
 * a dark band with a big glowing kanji, the card's name and what happened. It fades in, stays a moment, fades out.
 * Clicks go through it, so the game never waits for it.
 */
public final class EventBanner {
    /** The match grid (BoardView is 1600 x 900). */
    private static final double W = 1600, H = 900;
    private static final double BAND_H = 170;
    private static final String SERIF = "Yu Mincho";
    private static final Color PAPER = Color.web("#f3ede1");
    private static final Color SOFT = Color.web("#d9cfbd");
    private static final Duration FADE = Duration.millis(250);
    private static final Duration STAY = Duration.millis(1300);

    private EventBanner() {
        // Only a static helper: nobody creates an EventBanner.
    }

    /** Shows the band on `layer`: the kanji glowing in `colour` on the left, the title and subtitle beside it. */
    public static void show(Pane layer, String kanji, String title, String subtitle, Color colour) {
        Rectangle band = new Rectangle(W, BAND_H, Color.rgb(0, 0, 0, 0.7));

        Label kanjiLabel = label(kanji, 110, colour);
        kanjiLabel.setEffect(new DropShadow(25, colour));
        Label titleLabel = label(title, 48, PAPER);
        Label subtitleLabel = label(subtitle, 24, SOFT);
        VBox words = new VBox(4, titleLabel, subtitleLabel);
        words.setAlignment(Pos.CENTER_LEFT);
        HBox line = new HBox(30, kanjiLabel, words);
        line.setAlignment(Pos.CENTER);

        StackPane banner = new StackPane(band, line);
        banner.relocate(0, (H - BAND_H) / 2);
        banner.setMouseTransparent(true);
        banner.setOpacity(0);
        layer.getChildren().add(banner);

        // Fade in, stay, fade out, then take the band away.
        FadeTransition fadeIn = new FadeTransition(FADE, banner);
        fadeIn.setToValue(1);
        PauseTransition stay = new PauseTransition(STAY);
        FadeTransition fadeOut = new FadeTransition(FADE, banner);
        fadeOut.setToValue(0);
        SequentialTransition all = new SequentialTransition(fadeIn, stay, fadeOut);
        all.setOnFinished(event -> layer.getChildren().remove(banner));
        all.play();
    }

    /** A bold Yu Mincho label of this size and colour. */
    private static Label label(String words, double size, Color colour) {
        Label label = new Label(words);
        label.setFont(Font.font(SERIF, FontWeight.BOLD, size));
        label.setTextFill(colour);
        return label;
    }
}