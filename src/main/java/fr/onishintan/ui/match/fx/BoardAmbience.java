package fr.onishintan.ui.match.fx;

import fr.onishintan.ui.match.BoardView;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.util.Duration;

/** Life on each battlefield, behind the cards: snow on Mount Fuji, petals in town, rain on the ship, embers in the izakaya and on Orochi's board. */
public final class BoardAmbience {
    /** A new particle (or a few, for the rain) every SPAWN_MS. */
    private static final Duration SPAWN_MS = Duration.millis(150);

    private BoardAmbience() {
        // Only a static helper: nobody creates a BoardAmbience.
    }

    /** A layer of particles for this board, to lay right over its picture; it stays empty for a board without ambience. */
    public static Pane of(String board) {
        Pane layer = new Pane();
        layer.setMouseTransparent(true);   // clicks go through to the board
        Timeline spawner = new Timeline(new KeyFrame(SPAWN_MS, event -> spawn(layer, board)));
        spawner.setCycleCount(Animation.INDEFINITE);
        spawner.play();
        // Once the match leaves the window, the particles stop coming.
        layer.sceneProperty().addListener((property, oldScene, newScene) -> {
            if (oldScene != null && newScene == null) {
                spawner.stop();
            }
        });
        return layer;
    }

    /** One more particle of this board's kind. */
    private static void spawn(Pane layer, String board) {
        if (board.equals("fuji")) {
            Circle flake = new Circle(1.5 + Math.random() * 2.5, Color.rgb(255, 255, 255, 0.8));
            fall(layer, flake, 7000, 40);
        } else if (board.equals("town")) {
            Ellipse petal = new Ellipse(5, 3);
            petal.setFill(Color.web("#f6c1d0", 0.85));
            fall(layer, petal, 8000, 120);
        } else if (board.equals("ship")) {
            for (int i = 0; i < 4; i++) {   // rain is fast, so several drops at once
                Line drop = new Line(0, 0, -4, 18);
                drop.setStroke(Color.rgb(200, 215, 230, 0.35));
                fall(layer, drop, 900, 60);
            }
        } else if (board.equals("izakaya")) {
            rise(layer, new Circle(1.5 + Math.random() * 1.5, Color.web("#ffb347", 0.8)));
        } else if (board.equals("dragon")) {
            rise(layer, new Circle(1.5 + Math.random() * 2, Color.web("#ff4b2b", 0.8)));
        }
    }

    /** A particle falls from above the screen to below it in `ms`, drifting up to `sway` pixels sideways and turning. */
    private static void fall(Pane layer, Node particle, double ms, double sway) {
        particle.relocate(Math.random() * BoardView.W, -20);
        layer.getChildren().add(particle);
        Duration time = Duration.millis(ms * (0.8 + Math.random() * 0.4));   // not all at the same speed
        TranslateTransition down = new TranslateTransition(time, particle);
        down.setByY(BoardView.H + 40);
        down.setByX(-sway + Math.random() * sway * 2);
        RotateTransition turn = new RotateTransition(time, particle);
        turn.setByAngle(360);
        ParallelTransition move = new ParallelTransition(down, turn);
        move.setOnFinished(event -> layer.getChildren().remove(particle));
        move.play();
    }

    /** An ember rises from the lower half of the screen, drifting, and fades out on its way up. */
    private static void rise(Pane layer, Node ember) {
        ember.relocate(Math.random() * BoardView.W, BoardView.H / 2 + Math.random() * BoardView.H / 2);
        layer.getChildren().add(ember);
        Duration time = Duration.millis(4000 + Math.random() * 2000);
        TranslateTransition up = new TranslateTransition(time, ember);
        up.setByY(-300);
        up.setByX(-40 + Math.random() * 80);
        FadeTransition fade = new FadeTransition(time, ember);
        fade.setFromValue(1);
        fade.setToValue(0);
        ParallelTransition move = new ParallelTransition(up, fade);
        move.setOnFinished(event -> layer.getChildren().remove(ember));
        move.play();
    }
}
