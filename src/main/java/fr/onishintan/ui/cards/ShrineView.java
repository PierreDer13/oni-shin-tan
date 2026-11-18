package fr.onishintan.ui.cards;

import fr.onishintan.model.Shrine;

import javafx.scene.Group;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;

/**
 * A Shrine standing on the board: its card, with the turns it has left in a lavender circle where a minion's HP would be.
 * A Shrine has no ATK or HP and cannot be attacked; the number goes down at the end of each of its owner's turns.
 */
public class ShrineView extends Group {
    /** The HP corner of a card, in card coordinates (530 x 742): its centre and the circle's radius. */
    private static final double TURNS_X = 452, TURNS_Y = 641, TURNS_RADIUS = 46;
    private static final String LAVENDER = "#5e4b8b";
    private static final String CREAM = "#f3ede1";

    private final Shrine shrine;

    /** The Shrine's card `width` wide, with its turns left. */
    public ShrineView(Shrine shrine, double width) {
        this.shrine = shrine;
        double k = width / CardView.W;   // card pixels -> screen pixels

        CardView card = new CardView(shrine.getSource(), width);

        Circle ring = new Circle(TURNS_RADIUS * k, Color.web(LAVENDER));
        ring.setStroke(Color.web(CREAM));
        ring.setStrokeWidth(4 * k);

        Text turns = new Text(String.valueOf(shrine.getTurnsLeft()));
        turns.setFont(Font.font("Yu Mincho", FontWeight.BOLD, 64 * k));
        turns.setFill(Color.web(CREAM));
        turns.setBoundsType(TextBoundsType.VISUAL);   // measured on the digit itself, not the font's tall line box

        StackPane badge = new StackPane(ring, turns);   // centres the digit on the circle
        badge.relocate((TURNS_X - TURNS_RADIUS - 2) * k, (TURNS_Y - TURNS_RADIUS - 2) * k);   // - 2: half the stroke

        getChildren().addAll(card, badge);
    }

    /** The Shrine this view shows. */
    public Shrine getShrine() {
        return shrine;
    }
}