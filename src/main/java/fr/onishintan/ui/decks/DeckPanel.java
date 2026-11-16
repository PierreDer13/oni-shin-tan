package fr.onishintan.ui.decks;

import fr.onishintan.model.Card;
import fr.onishintan.ui.cards.CardPreview;
import fr.onishintan.ui.cards.DeckListView;

import java.util.List;
import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

/**
 * Right side of the deck editor, inside the decorated frame: the deck in Ki sections, cheapest first (DeckListView).
 * Hovering a card shows it big; a right click takes one copy out.
 */
class DeckPanel extends ScrollPane {
    /** The panel's width, and the list's width inside it (minus the padding and the scroll bar). */
    private static final double WIDTH = 410, LIST_W = 370;

    private final VBox content = new VBox();
    private final DeckListView list;

    DeckPanel(CardPreview preview, Consumer<Card> onRemove) {
        list = new DeckListView(LIST_W, DecksView.PAPER, preview, onRemove);
        content.setPadding(new Insets(10));
        setContent(content);
        setPrefSize(WIDTH, 545);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setStyle("-fx-background-color: transparent; -fx-background: transparent;");   // the frame's dark inside shows through
    }

    /** Redraws the deck: each different card once, with how many copies there are, in Ki sections. */
    void show(List<Card> cards) {
        if (cards.isEmpty()) {
            content.getChildren().setAll(DecksView.text("Left click a card to add it.", 18, DecksView.PAPER));
            return;
        }
        list.show(cards);
        content.getChildren().setAll(list);
    }
}