package fr.onishintan.ui.decks;

import fr.onishintan.model.Card;
import fr.onishintan.ui.cards.CardView;

import java.util.List;
import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.control.Label;
import fr.onishintan.model.HeroClass;

/**
 * Left side of the deck editor: the cards you may add, as a scrolling grid.
 * A title row starts each class's cards (the deck's class first, then Neutral). Left click adds a copy, right click removes one;
 * hovering a card grows it and opens a big copy beside it (CardPreview).
 */
class CollectionPanel extends ScrollPane {
    private static final double CARD_W = 160, GAP = 16;
    private static final int PER_ROW = 6;
    /** Width of one full row of cards: a title this wide gets a row of its own. */
    private static final double ROW_W = PER_ROW * CARD_W + (PER_ROW - 1) * GAP;

    private final FlowPane cardGrid = new FlowPane(GAP, GAP);
    private final Consumer<Card> onAdd;
    private final Consumer<Card> onRemove;
    /** The hover effect: the card grows and a big copy opens beside it. */
    private final CardPreview preview;

    CollectionPanel(Consumer<Card> onAdd, Consumer<Card> onRemove, CardPreview preview) {
        this.onAdd = onAdd;
        this.onRemove = onRemove;
        this.preview = preview;
        cardGrid.setPrefWrapLength(ROW_W);
        cardGrid.setPadding(new Insets(10));

        setContent(cardGrid);
        setPrefSize(1100, 690);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setStyle("-fx-background-color: transparent; -fx-background: transparent;");
    }

    /**
     * Shows these cards in the order given, with a title row each time the class changes.
     * Scrolls back to the top, since the list has changed.
     */
    void show(List<Card> cards) {
        cardGrid.getChildren().clear();
        if (cards.isEmpty()) {
            cardGrid.getChildren().add(DecksView.text("No card matches these filters.", 22, DecksView.INK));
        }
        HeroClass lastClass = null;
        for (Card card : cards) {
            if (card.getHeroClass() != lastClass) {
                cardGrid.getChildren().add(classTitle(card.getHeroClass()));
                lastClass = card.getHeroClass();
            }
            cardGrid.getChildren().add(cardView(card));
        }
        setVvalue(0);
    }

    /** A full-width title row with the class's name, underlined. */
    private Label classTitle(HeroClass heroClass) {
        String name = heroClass.name().charAt(0) + heroClass.name().substring(1).toLowerCase();
        Label title = DecksView.text(name, 26, DecksView.INK);
        title.setPrefWidth(ROW_W);
        title.setStyle(title.getStyle() + " -fx-border-color: #d9cfbd; -fx-border-width: 0 0 2 0;");
        return title;
    }

    /** One card of the grid: left click adds a copy to the deck, right click takes one out. */
    private CardView cardView(Card card) {
        CardView view = new CardView(card, CARD_W);
        view.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                onAdd.accept(card);
            } else if (event.getButton() == MouseButton.SECONDARY) {
                onRemove.accept(card);
            }
        });
        preview.attach(view);
        return view;
    }
}