package fr.onishintan.ui.cards;

import javafx.scene.Group;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * Drawn over a Curse the enemy put in your hand, in card coordinates (530 x 742):
 * the card goes dark, and a red seal in the middle says CURSED and shows the Ki it costs to get rid of it.
 */
public class CurseSeal extends Group {
    private static final String SERIF = "Yu Mincho";
    private static final Color SEAL_RED = Color.web("#b3372a");
    private static final Color PAPER = Color.web("#f3ede1");
    /** Middle of the seal and its radius. */
    private static final double SEAL_X = CardView.W / 2, SEAL_Y = CardView.H / 2, SEAL_R = 120;

    /** The dark veil and the seal for a Curse that costs removalCost Ki to remove. */
    public CurseSeal(int removalCost) {
        // A dark veil over the whole card: it can't be played, only removed.
        Rectangle veil = new Rectangle(CardView.W, CardView.H, Color.rgb(0, 0, 0, 0.6));

        // The red seal, with CURSED and the removal cost under it.
        Circle seal = new Circle(SEAL_X, SEAL_Y, SEAL_R, SEAL_RED);
        seal.setStroke(PAPER);
        seal.setStrokeWidth(6);
        seal.setEffect(new DropShadow(30, Color.BLACK));
        Text word = centredText("CURSED", 50, SEAL_Y + 12);
        Text cost = centredText(removalCost + " Ki", 44, SEAL_Y + 85);

        getChildren().addAll(veil, seal, word, cost);
        setMouseTransparent(true);   // clicks and drags still reach the card
    }

    /** A bold paper-coloured line of text, centred on the seal, standing on the line y. */
    private static Text centredText(String words, double size, double y) {
        Text text = new Text(words);
        text.setFont(Font.font(SERIF, FontWeight.BOLD, size));
        text.setFill(PAPER);
        double width = text.getLayoutBounds().getWidth();
        text.setX(SEAL_X - width / 2);
        text.setY(y);
        return text;
    }
}