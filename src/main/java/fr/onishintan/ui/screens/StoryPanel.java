package fr.onishintan.ui.screens;

import fr.onishintan.ui.app.Sound;
import fr.onishintan.ui.app.Images;
import java.util.List;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.SequentialTransition;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * A story page over the Adventure map, played like a scene: the map goes dark, the title is stamped in with a thud,
 * then the story appears line by line under its pictures. A click while it plays shows the whole page;
 * Continue closes it and runs `then` (or only closes it when `then` is null).
 */
final class StoryPanel extends StackPane {
    /** How long a line takes to fade in, and the pause before each one. */
    private static final double LINE_MS = 900, LINE_GAP_MS = 700;
    /** The picture's height, and the room kept for the lines under it. */
    private static final double ART_H = 300, PAGE_H = 340;
    /** The thud when the title is stamped in. */
    private static final AudioClip THUD = Sound.clip("door-close");

    private final Label heading;
    private final VBox page;
    /** The page's picture (images/story/), crossfading as the story goes. */
    private final ImageView art = new ImageView();
    private final Image[] pictures;
    private final Label go;
    private final String story;
    private final SequentialTransition show;

    StoryPanel(String title, String story, List<String> pictureNames, Runnable then) {
        this.story = story;
        Rectangle veil = new Rectangle(HomeView.W, HomeView.H, Color.rgb(0, 0, 0, 0.88));
        heading = label(title, 48);
        heading.setTextFill(Color.web("#c41e1e"));
        pictures = new Image[pictureNames.size()];
        for (int i = 0; i < pictures.length; i++) {
            pictures[i] = Images.get("/images/story/" + pictureNames.get(i) + ".png");   // null while not painted yet
        }
        art.setFitHeight(ART_H);
        art.setPreserveRatio(true);
        page = new VBox(8);
        page.setAlignment(Pos.TOP_CENTER);
        page.setPrefHeight(PAGE_H);   // room for every line, so nothing moves while they appear
        go = label("進む  CONTINUE", 30);
        go.setVisible(false);   // shown once the page is written
        HomeView.clickable(go, false, () -> {
            ((Pane) getParent()).getChildren().remove(this);
            if (then != null) {
                then.run();
            }
        });
        VBox box = new VBox(30, heading, art, page, go);
        box.setAlignment(Pos.CENTER);
        getChildren().addAll(veil, box);

        show = new SequentialTransition(darken(), stamp(), reveal());
        show.setOnFinished(e -> go.setVisible(true));
        show.play();
        setOnMousePressed(e -> skip());
    }

    /** 1. The map goes dark. */
    private FadeTransition darken() {
        setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(600), this);
        fade.setToValue(1);
        return fade;
    }

    /** 2. The title falls from big to its size, like a seal stamped on the page, and lands with a thud. */
    private Timeline stamp() {
        heading.setScaleX(2.5);
        heading.setScaleY(2.5);
        heading.setOpacity(0);
        Timeline stamp = new Timeline(new KeyFrame(Duration.millis(250),
                new KeyValue(heading.scaleXProperty(), 1, Interpolator.EASE_IN),
                new KeyValue(heading.scaleYProperty(), 1, Interpolator.EASE_IN),
                new KeyValue(heading.opacityProperty(), 1)));
        stamp.setOnFinished(e -> Sound.play(THUD));
        return stamp;
    }

    /** 3. The story appears line by line, each sentence fading in after a pause; the pictures change as it goes. */
    private SequentialTransition reveal() {
        String[] sentences = story.split("(?<=[.!?])\\s+");
        SequentialTransition reveal = new SequentialTransition();
        for (int i = 0; i < sentences.length; i++) {
            Label line = label(sentences[i], 22);
            line.setWrapText(true);
            line.setMaxWidth(950);
            line.setTextAlignment(TextAlignment.CENTER);
            line.setOpacity(0);
            page.getChildren().add(line);
            int picture = i * pictures.length / sentences.length;   // with 2 pictures: picture 1 for the first half, 2 for the rest
            PauseTransition gap = new PauseTransition(Duration.millis(LINE_GAP_MS));
            gap.setOnFinished(e -> showPicture(picture));
            FadeTransition in = new FadeTransition(Duration.millis(LINE_MS), line);
            in.setToValue(1);
            reveal.getChildren().addAll(gap, in);
        }
        return reveal;
    }

    /** Shows picture k of the page, fading it in, if it is painted and not already shown. */
    private void showPicture(int k) {
        if (pictures[k] == null || art.getImage() == pictures[k]) {
            return;
        }
        art.setImage(pictures[k]);
        art.setOpacity(0);
        FadeTransition in = new FadeTransition(Duration.millis(LINE_MS), art);
        in.setToValue(1);
        in.play();
    }

    /** A click while the scene plays: the whole page at once, ready to continue. */
    private void skip() {
        if (go.isVisible()) {
            return;   // already written
        }
        show.stop();
        setOpacity(1);
        heading.setScaleX(1);
        heading.setScaleY(1);
        heading.setOpacity(1);
        for (Node line : page.getChildren()) {
            line.setOpacity(1);
        }
        showPicture(pictures.length - 1);
        go.setVisible(true);
    }

    /** Cream text in the card font with a soft shadow, like the rest of the Adventure map. */
    private static Label label(String s, double size) {
        Label l = new Label(s);
        l.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: " + size + "; -fx-font-weight: bold; -fx-text-fill: #f3ede1;");
        l.setEffect(new DropShadow(6, Color.BLACK));
        return l;
    }
}
