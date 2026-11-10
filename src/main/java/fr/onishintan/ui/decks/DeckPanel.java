package fr.onishintan.ui.decks;

import fr.onishintan.model.Card;
import fr.onishintan.model.Rarity;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Right side of the deck editor: the deck as one line per card, cheapest first (Ki, name, copies). Right click a line to take one copy out. */
class DeckPanel extends ScrollPane {
    private static final String GOLD = "#b08a2e";

    private final VBox lines = new VBox(6);
    private final Consumer<Card> onRemove;

    DeckPanel(Consumer<Card> onRemove) {
        this.onRemove = onRemove;
        lines.setPadding(new Insets(10));
        setContent(lines);
        setPrefSize(410, 750);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setStyle("-fx-background-color: #fbf8f1; -fx-background: #fbf8f1; -fx-border-color: #d9cfbd; -fx-border-radius: 8;");
    }

    /** Redraws the list from the deck's cards: each different card once, with how many copies there are. */
    void show(List<Card> cards) {
        lines.getChildren().clear();
        if (cards.isEmpty()) {
            lines.getChildren().add(DecksView.text("Left click a card to add it.", 18, DecksView.INK));
            return;
        }

        Map<Card, Integer> copies = SavedDeck.copiesByKi(cards);

        for (Card card : copies.keySet()) {
            lines.getChildren().add(line(card, copies.get(card)));
        }
    }

    /** One line: "3   Kappa   x2", with a stripe in the card's class colour; legendary and mythic names in gold. */
    private HBox line(Card card, int count) {
        Label ki = DecksView.text(String.valueOf(card.getKiCost()), 20, DecksView.INK);
        ki.setMinWidth(30);

        String nameColour = DecksView.INK;
        if (card.getRarity() == Rarity.LEGENDARY || card.getRarity() == Rarity.MYTHIC) {
            nameColour = GOLD;
        }
        Label name = DecksView.text(card.getNameRomaji(), 18, nameColour);

        Region space = new Region();   // pushes the copy count to the right edge
        HBox.setHgrow(space, Priority.ALWAYS);
        Label copies = DecksView.text("x" + count, 18, DecksView.INK);

        HBox line = new HBox(10, ki, name, space, copies);
        line.setAlignment(Pos.CENTER_LEFT);
        line.setPadding(new Insets(6, 12, 6, 12));
        line.setPrefWidth(370);
        line.setStyle("-fx-background-color: white; -fx-background-radius: 6; -fx-border-radius: 6; -fx-border-width: 0 0 0 6;"
                + " -fx-border-color: " + DecksView.CLASS_COLOUR.get(card.getHeroClass()) + ";");
        line.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                onRemove.accept(card);
            }
        });
        return line;
    }
}