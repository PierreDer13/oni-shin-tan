package fr.onishintan.ui.match;

import fr.onishintan.model.Card;
import fr.onishintan.model.Player;
import fr.onishintan.ui.app.Images;
import fr.onishintan.ui.cards.CardView;
import fr.onishintan.ui.screens.HomeView;
import javafx.animation.ScaleTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Mulligan (GDD): the opening hand shown big over the table. Click a card to send it back (it flips face down),
 * click it again to keep it; CONFIRM closes the view and gives the cards to send back (engine.mulligan).
 */
final class MulliganView extends StackPane {
    private static final double CARD_W = 210;
    private static final double CARD_H = CARD_W * 742 / 530;   // card art ratio
    private static final Duration FLIP = Duration.millis(120);

    /** The cards picked to go back, in click order. */
    private final List<Card> chosen = new ArrayList<>();

    MulliganView(Player player, Consumer<List<Card>> onDone) {
        Label title = text("交換  MULLIGAN", 44);
        Label hint = text(player.getName() + ": click the cards to send back, then confirm", 22);

        HBox cards = new HBox(24);
        cards.setAlignment(Pos.CENTER);
        for (Card card : player.getHand()) cards.getChildren().add(slot(card));

        Label confirm = text("決定  CONFIRM", 28);
        confirm.setStyle(confirm.getStyle() + " -fx-background-color: rgba(20,12,8,0.85); -fx-background-radius: 10;"
                + " -fx-padding: 8 22; -fx-border-color: #c9a227; -fx-border-radius: 10;");
        HomeView.clickable(confirm, false, () -> {
            if (getParent() instanceof Pane parent) parent.getChildren().remove(this);
            onDone.accept(List.copyOf(chosen));
        });

        VBox box = new VBox(30, title, hint, cards, confirm);
        box.setAlignment(Pos.CENTER);
        getChildren().addAll(new Rectangle(BoardView.W, BoardView.H, Color.rgb(0, 0, 0, 0.78)), box);
        setPrefSize(BoardView.W, BoardView.H);
    }

    /** One opening card: face up = kept, face down = sent back. Hovering lifts it a little. */
    private StackPane slot(Card card) {
        CardView face = new CardView(card, CARD_W);
        ImageView back = new ImageView(Images.get("/images/frames/card-back.png", CARD_W, CARD_H));
        back.setFitWidth(CARD_W);
        back.setFitHeight(CARD_H);
        back.setVisible(false);
        StackPane slot = new StackPane(face, back);
        slot.setMaxSize(CARD_W, CARD_H);
        slot.setOnMouseEntered(e -> slot.setTranslateY(-14));
        slot.setOnMouseExited(e -> slot.setTranslateY(0));
        slot.setOnMouseClicked(e -> {
            boolean sendBack = !chosen.contains(card);
            if (sendBack) chosen.add(card); else chosen.remove(card);
            flip(slot, face, back, sendBack);
        });
        return slot;
    }

    /** The card turns over: it narrows to nothing, shows its other side, then widens again. */
    private static void flip(Node slot, Node face, Node back, boolean showBack) {
        ScaleTransition close = new ScaleTransition(FLIP, slot);
        close.setToX(0);
        close.setOnFinished(e -> {
            face.setVisible(!showBack);
            back.setVisible(showBack);
            ScaleTransition open = new ScaleTransition(FLIP, slot);
            open.setToX(1);
            open.play();
        });
        close.play();
    }

    private static Label text(String words, double size) {
        Label label = new Label(words);
        label.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: " + size + "; -fx-font-weight: bold; -fx-text-fill: #f3ede1;");
        return label;
    }
}