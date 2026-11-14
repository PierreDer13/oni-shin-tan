package fr.onishintan.ui.decks;

import fr.onishintan.model.Card;
import fr.onishintan.ui.cards.CardPreview;
import fr.onishintan.ui.cards.CardShelves;
import fr.onishintan.ui.cards.StandingCard;
import fr.onishintan.ui.app.ScreenGrid;

import java.util.List;
import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;

/**
 * Left side of the deck editor: the cards you may add, standing in their stands on wooden shelves that scroll.
 * Each class's cards start on a new shelf with the class's name (the deck's class first, then Neutral).
 * The panel fills the whole page: its shelves are the page's background, under the top bar and the deck list.
 * Left click adds a copy, right click removes one; hovering a card grows it, lifts it and opens a big copy beside it (CardPreview).
 */
class CollectionPanel extends ScrollPane {
    private static final int PER_SHELF = 5;
    /** Where the cards stand on each shelf: left of the deck list. */
    private static final double AREA_LEFT = 40, AREA_W = 1100;
    /** The first shelf starts this far down, under the top bar and the filter bar. */
    private static final double TOP_INSET = 80;
    /** How far a hovered card rises. */
    private static final double LIFT = 14;

    private final Consumer<Card> onAdd;
    private final Consumer<Card> onRemove;
    /** The hover effect: the card grows and a big copy opens beside it. */
    private final CardPreview preview;

    CollectionPanel(Consumer<Card> onAdd, Consumer<Card> onRemove, CardPreview preview) {
        this.onAdd = onAdd;
        this.onRemove = onRemove;
        this.preview = preview;
        setPrefSize(ScreenGrid.W, ScreenGrid.H);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setStyle("-fx-background-color: transparent; -fx-background: transparent;");
    }

    /**
     * Shows these cards in the order given, a new shelf group each time the class changes.
     * Scrolls back to the top, since the list has changed.
     */
    void show(List<Card> cards) {
        CardShelves shelves = new CardShelves(ScreenGrid.W, PER_SHELF, AREA_LEFT, AREA_W, TOP_INSET);
        shelves.setMinHeight(ScreenGrid.H);   // the shelves fill the page even with few cards
        if (cards.isEmpty()) {
            Label empty = DecksView.text("No card matches these filters.", 22, DecksView.INK);
            empty.setPadding(new Insets(120, 0, 0, AREA_LEFT));
            shelves.getChildren().add(empty);
        }

        // Cut the list into runs of the same class; each run is one group on the shelves.
        int start = 0;
        for (int i = 1; i <= cards.size(); i++) {
            boolean lastCard = i == cards.size();
            if (lastCard || cards.get(i).getHeroClass() != cards.get(start).getHeroClass()) {
                List<Card> group = cards.subList(start, i);
                String title = group.get(0).getHeroClass().getDisplayName();
                for (StandingCard view : shelves.addGroup(title, group)) {
                    makeClickable(view);
                }
                start = i;
            }
        }
        setContent(shelves);
        setVvalue(0);
    }

    /** Left click adds a copy of the card to the deck, right click takes one out; hovering it shows the big copy. */
    private void makeClickable(StandingCard view) {
        Card card = view.getCard();
        view.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                onAdd.accept(card);
            } else if (event.getButton() == MouseButton.SECONDARY) {
                onRemove.accept(card);
            }
        });
        preview.attach(view.getSleeve(), card, LIFT);   // only the card in its sleeve grows and rises, not the stand
    }
}
