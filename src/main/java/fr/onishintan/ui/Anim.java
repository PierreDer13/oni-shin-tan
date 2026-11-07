package fr.onishintan.ui;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.value.WritableValue;
import javafx.scene.Node;
import javafx.util.Duration;

/** Smooth movement for the UI: glide a node's properties to new values. */
final class Anim {
    static final Duration FAST = Duration.millis(100);

    private Anim() {}

    /** Animates the node's properties to their targets, cancelling its previous animation first. */
    static void to(Node n, KeyValue... values) {
        stop(n);
        Timeline t = new Timeline(new KeyFrame(FAST, values));
        n.getProperties().put("anim", t);
        t.play();
    }

    /** Stops whatever animation is moving this node (e.g. when the player grabs it). */
    static void stop(Node n) {
        if (n.getProperties().get("anim") instanceof Timeline t) t.stop();
    }

    /** "Property p should reach value v", slowing down at the end. */
    static <T> KeyValue ease(WritableValue<T> p, T v) { return new KeyValue(p, v, Interpolator.EASE_OUT); }
}