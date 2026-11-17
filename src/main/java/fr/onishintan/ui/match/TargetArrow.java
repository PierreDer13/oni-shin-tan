package fr.onishintan.ui.match;

import fr.onishintan.ui.app.Images;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;

/**
 * The targeting arrow, from what aims to the mouse, along a curve that bows outwards. It is built from three pictures
 * (images/cursors/): the cloud tail on the source, the arrowhead on the mouse, and dashes between them. Every piece keeps
 * its size: only the gaps between the dashes grow or shrink with the distance.
 */
final class TargetArrow extends Group {
    /** The pictures are saved at twice the size they are drawn, so they stay sharp. */
    private static final double SCALE = 0.5;
    private static final Image TAIL = Images.get("/images/cursors/arrow-tail.png");
    private static final Image DASH = Images.get("/images/cursors/arrow-dash.png");
    private static final Image HEAD = Images.get("/images/cursors/arrow-head.png");
    /** Where the tail's shaft leaves the cloud, and where the head's tip is, in their pictures; the head is drawn 10° downwards. */
    private static final double TAIL_X = 134, TAIL_Y = 17, TIP_X = 143, TIP_Y = 81, HEAD_TILT = 10;
    /** Smallest gap between two dashes, and how far the curve bows, as a share of the arrow's length. */
    private static final double MIN_GAP = 6, BEND = 0.15;
    /** How many points the curve is measured with. */
    private static final int STEPS = 60;

    private final ImageView tail = piece(TAIL);
    private final ImageView head = piece(HEAD);
    private final List<ImageView> dashes = new ArrayList<>();
    /** The curve, measured: its points and the length along it up to each one. */
    private final double[] pointX = new double[STEPS + 1], pointY = new double[STEPS + 1], lengthTo = new double[STEPS + 1];
    /** 1 when the arrow points right, -1 when it points left: the curve and every piece are then mirrored, so they bow and hang outwards. */
    private double side = 1;

    TargetArrow() {
        getChildren().addAll(tail, head);
        setMouseTransparent(true);   // clicks go through to the target under it
        setVisible(false);
    }

    /** Draws the arrow from (fromX, fromY) to (toX, toY). */
    void span(double fromX, double fromY, double toX, double toY) {
        measureCurve(fromX, fromY, toX, toY);
        double total = lengthTo[STEPS];

        place(tail, TAIL_X, TAIL_Y, fromX, fromY, angleAt(1));
        place(head, TIP_X, TIP_Y, toX, toY, angleAt(STEPS) - HEAD_TILT * side);

        // The dashes fill the stretch between the tail and the head, spread with equal gaps.
        double dashLength = DASH.getWidth() * SCALE;
        double start = MIN_GAP;
        double room = total - TIP_X * SCALE - MIN_GAP - start;
        int count = (int) Math.floor((room + MIN_GAP) / (dashLength + MIN_GAP));
        if (count < 0) {
            count = 0;
        }
        double gap = (room - count * dashLength) / (count + 1);
        showDashes(count);
        for (int i = 0; i < count; i++) {
            double along = start + gap * (i + 1) + dashLength * (i + 0.5);
            int at = stepAt(along);
            place(dashes.get(i), DASH.getWidth() / 2, DASH.getHeight() / 2, pointX[at], pointY[at], angleAt(Math.max(1, at)));
        }
    }

    /** The curve from the source to the mouse, bowing outwards; its points and lengths go into pointX, pointY and lengthTo. */
    private void measureCurve(double fromX, double fromY, double toX, double toY) {
        double dx = toX - fromX;
        double dy = toY - fromY;
        double length = Math.max(1, Math.hypot(dx, dy));
        side = 1;
        if (toX < fromX) {
            side = -1;   // to the left, it bows the other way, so it always sags outwards
        }
        double bendX = (fromX + toX) / 2 - dy / length * side * BEND * length;
        double bendY = (fromY + toY) / 2 + dx / length * side * BEND * length;
        for (int i = 0; i <= STEPS; i++) {
            double t = (double) i / STEPS;
            pointX[i] = (1 - t) * (1 - t) * fromX + 2 * (1 - t) * t * bendX + t * t * toX;
            pointY[i] = (1 - t) * (1 - t) * fromY + 2 * (1 - t) * t * bendY + t * t * toY;
            if (i > 0) {
                lengthTo[i] = lengthTo[i - 1] + Math.hypot(pointX[i] - pointX[i - 1], pointY[i] - pointY[i - 1]);
            }
        }
    }

    /** The first point of the curve at least `along` from its start. */
    private int stepAt(double along) {
        int i = 1;
        while (i < STEPS && lengthTo[i] < along) {
            i++;
        }
        return i;
    }

    /** The curve's direction, in degrees, arriving at point i (i from 1). */
    private double angleAt(int i) {
        return Math.toDegrees(Math.atan2(pointY[i] - pointY[i - 1], pointX[i] - pointX[i - 1]));
    }

    /** Puts the picture's point (anchorX, anchorY) on (x, y), turned by `angle` around that point and mirrored when the arrow points left. */
    private void place(ImageView view, double anchorX, double anchorY, double x, double y, double angle) {
        view.relocate(x - anchorX * SCALE, y - anchorY * SCALE);
        view.getTransforms().setAll(new Rotate(angle, anchorX * SCALE, anchorY * SCALE), new Scale(1, side, anchorX * SCALE, anchorY * SCALE));
    }

    /** Makes exactly `count` dashes visible, creating more when needed. */
    private void showDashes(int count) {
        while (dashes.size() < count) {
            ImageView dash = piece(DASH);
            dashes.add(dash);
            getChildren().add(0, dash);   // under the tail and the head
        }
        for (int i = 0; i < dashes.size(); i++) {
            dashes.get(i).setVisible(i < count);
        }
    }

    /** One piece of the arrow, drawn at SCALE. */
    private static ImageView piece(Image picture) {
        ImageView view = new ImageView(picture);
        view.setFitWidth(picture.getWidth() * SCALE);
        view.setFitHeight(picture.getHeight() * SCALE);
        return view;
    }
}
