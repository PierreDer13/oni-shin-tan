package fr.onishintan.ui.screens;

import fr.onishintan.ui.app.Sound;
import fr.onishintan.ui.app.Images;
import java.util.List;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * A story page over the Adventure map, played like a scene: the map goes dark, the title is stamped in with a thud,
 * then the story's first line under its picture; each click shows the next line, and the pictures change as it goes;
 * Continue closes it and runs `then` (or only closes it when `then` is null).
 */
final class StoryPanel extends StackPane {
    /** How long a line or a picture takes to fade in. */
    private static final double LINE_MS = 900;
    /** The picture's height, and the room kept for the lines under it. */
    private static final double ART_H = 380, PAGE_H = 300;
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
    /** How many lines are shown so far. */
    private int shown;
    /** The voice files' name (sounds/voice/<voice>-<n>.mp3), or null for a silent page; the line being spoken. */
    private final String voice;
    private MediaPlayer speaking;

    StoryPanel(String title, String story, List<String> pictureNames, Runnable then) {
        this(title, story, pictureNames, null, then);
    }

    /** A page whose lines are also spoken: line n plays sounds/voice/<voice>-<n>.mp3 when it appears (none: silent). */
    StoryPanel(String title, String story, List<String> pictureNames, String voice, Runnable then) {
        this.story = story;
        this.voice = voice;
        Rectangle veil = new Rectangle(HomeView.W, HomeView.H, Color.rgb(0, 0, 0, 0.88));
        heading = label(title, 48);
        heading.setTextFill(Color.web("#c41e1e"));
        pictures = new Image[pictureNames.size()];
        for (int i = 0; i < pictures.length; i++) {
            pictures[i] = Images.get("/images/story/" + pictureNames.get(i) + ".png", 1140, 760);   // twice the size shown, decoded once; null while not painted yet
        }
        art.setFitHeight(ART_H);
        art.setPreserveRatio(true);
        Rectangle corners = new Rectangle(ART_H * 1.5, ART_H);   // the pictures are 3:2 (1536 x 1024)
        corners.setArcWidth(30);
        corners.setArcHeight(30);
        art.setClip(corners);   // slightly rounded corners
        page = new VBox(8);
        page.setAlignment(Pos.TOP_CENTER);
        page.setPrefHeight(PAGE_H);   // room for every line, so nothing moves while they appear
        page.setMinHeight(PAGE_H);
        go = label("進む  CONTINUE", 30);
        go.setVisible(false);   // shown once the page is written
        HomeView.clickable(go, false, () -> {
            hush();
            ((Pane) getParent()).getChildren().remove(this);
            if (then != null) {
                then.run();
            }
        });
        VBox box = new VBox(24, heading, art, page, go);
        box.setAlignment(Pos.CENTER);
        getChildren().addAll(veil, box);

        lines();
        show = new SequentialTransition(darken(), stamp());
        show.setOnFinished(e -> {
            next();   // the first line comes with the title
            setOnMousePressed(click -> next());   // then one more line per click
        });
        show.play();
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

    /** 3. The story's sentences, one line each, hidden until a click shows them. A | in the text starts the next picture. */
    private void lines() {
        String[] parts = story.split("\\s*\\|\\s*");
        for (int k = 0; k < parts.length; k++) {
            for (String sentence : parts[k].split("(?<=[.!?])\\s+")) {
                Label line = label(sentence, 22);
                line.setWrapText(true);
                line.setPrefWidth(950);   // a fixed width, so a long line wraps instead of being cut with "..."
                line.setMaxWidth(950);
                line.setMinHeight(Region.USE_PREF_SIZE);   // never squeezed shorter than its wrapped lines
                line.setTextAlignment(TextAlignment.CENTER);
                line.setOpacity(0);
                line.setUserData(Math.min(k, pictures.length - 1));   // the picture shown with this line
                page.getChildren().add(line);
            }
        }
    }

    /** A click: the next line fades in, with its picture; once every line is shown, Continue appears. */
    private void next() {
        List<Node> lines = page.getChildren();
        if (shown >= lines.size()) {
            return;   // all shown: Continue closes the page
        }
        Object picture = lines.get(shown).getUserData();
        if (shown > 0 && !picture.equals(lines.get(shown - 1).getUserData())) {
            for (int i = 0; i < shown; i++) {
                lines.get(i).setVisible(false);   // a new picture: the lines before it go, the new ones start at the top
                lines.get(i).setManaged(false);
            }
        }
        showPicture((Integer) picture);
        FadeTransition in = new FadeTransition(Duration.millis(LINE_MS), lines.get(shown));
        in.setToValue(1);
        in.play();
        shown++;
        speak(shown);
        if (shown == lines.size()) {
            go.setVisible(true);
        }
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

    /** Speaks line n (counted from 1), stopping the line before; silent when the page or that line has no voice file. */
    private void speak(int n) {
        hush();
        if (voice != null) {
            speaking = Sound.voice(voice + "-" + n);
        }
    }

    /** Stops the line being spoken. */
    private void hush() {
        if (speaking != null) {
            speaking.stop();
            speaking.dispose();
            speaking = null;
        }
    }

    /** Cream text in the card font with a soft shadow, like the rest of the Adventure map. */
    private static Label label(String s, double size) {
        Label l = new Label(s);
        l.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: " + size + "; -fx-font-weight: bold; -fx-text-fill: #f3ede1;");
        l.setEffect(new DropShadow(6, Color.BLACK));
        return l;
    }
}
