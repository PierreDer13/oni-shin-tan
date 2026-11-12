package fr.onishintan.ui.decks;

import fr.onishintan.model.Card;
import fr.onishintan.model.Deck;
import fr.onishintan.model.HeroClass;
import fr.onishintan.ui.MockCards;
import fr.onishintan.ui.app.SceneRouter;
import fr.onishintan.ui.app.Screen;
import fr.onishintan.ui.app.ScreenGrid;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Deck Builder, screen 2: edit one deck.
 * Left: the cards you may put in it (CollectionPanel). Right: the deck itself (DeckPanel).
 * Left click a card to add it, right click to take it out. Save keeps the changes, Cancel forgets them.
 */
public class DeckEditorView extends Pane {
    private final SceneRouter router;
    private final SavedDeck deck;
    /** The deck list's class filter, so Save and Cancel go back to the same list. */
    private final HeroClass listClass;
    /** The cards while you edit. They are copied into the deck only when you press Save. */
    private final List<Card> cards;

    private final Pane grid = new Pane();
    private final TextField nameField;
    private final Label countLabel = DecksView.text("", 26, DecksView.INK);
    private final Label messageLabel = DecksView.text("", 18, "#c41e1e");
    private final DeckPanel deckPanel;
    /** What the collection shows: search, Ki and card type. The FilterBar changes it. */
    private final CardFilter filter = new CardFilter();
    /** Every card this deck may hold, in display order (the deck's class first, then Neutral). */
    private final List<Card> allowed;
    private final CollectionPanel collection;

    /** Opens the editor for this deck. listClass = the class filter of the list we came from (null = every deck). */
    public static void open(SceneRouter router, SavedDeck deck, HeroClass listClass) {
        router.register(Screen.DECK_EDITOR, () -> new DeckEditorView(router, deck, listClass));
        router.goTo(Screen.DECK_EDITOR);
    }

    private DeckEditorView(SceneRouter router, SavedDeck deck, HeroClass listClass) {
        this.router = router;
        this.deck = deck;
        this.listClass = listClass;
        this.cards = deck.getCards();   // getCards() gives a copy, so the real deck changes only on Save
        ScreenGrid.fitToWindow(this, grid);

        // Plain washi-paper background, until the painted one exists.
        grid.getChildren().add(new Rectangle(ScreenGrid.W, ScreenGrid.H, Color.web(DecksView.PAPER)));

        // Top bar: the deck name you can type in, the card count, a message line, Cancel and Save.
        nameField = new TextField(deck.getName());
        nameField.setPrefWidth(420);
        nameField.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: 26; -fx-font-weight: bold;"
                + " -fx-background-color: #fbf8f1; -fx-border-color: #d9cfbd; -fx-border-radius: 6; -fx-background-radius: 6;");
        nameField.relocate(30, 30);
        countLabel.relocate(480, 30);
        messageLabel.relocate(480, 72);

        Label cancel = DecksView.button("Cancel", this::cancel);
        cancel.relocate(1330, 36);
        Label save = DecksView.button("Save", this::save);
        save.relocate(1460, 36);
        grid.getChildren().addAll(nameField, countLabel, messageLabel, cancel, save);

               // Left: the filter bar, then every card this deck may hold. Right: the deck.
        allowed = cardsAllowed();
        FilterBar filterBar = new FilterBar(filter, this::showCollection);
        filterBar.relocate(30, 125);
        CardPreview preview = new CardPreview(grid);   // the big copy is drawn on the whole grid, above the deck list too
        collection = new CollectionPanel(this::addCard, this::removeCard, preview);
        collection.relocate(30, 180);
        deckPanel = new DeckPanel(this::removeCard);
        deckPanel.relocate(1160, 130);
        grid.getChildren().addAll(filterBar, collection, deckPanel);
        showCollection();

        refresh();
    }

    /**
     * The cards of the deck's class, then the Neutral ones; each group cheapest first, then by name.
     * Placeholder: every test card from MockCards, until the player's own collection is saved.
     */
    private List<Card> cardsAllowed() {
        List<Card> allowed = new ArrayList<>();
        for (Card card : MockCards.ALL) {
            HeroClass cardClass = card.getHeroClass();
            if (cardClass == deck.getHeroClass() || cardClass == HeroClass.NEUTRAL) {
                allowed.add(card);
            }
        }
        Comparator<Card> classFirst = Comparator.comparing(DeckEditorView::isNeutral);   // false (the deck's class) before true (Neutral)
        Comparator<Card> byKi = Comparator.comparingInt(Card::getKiCost);
        Comparator<Card> byName = Comparator.comparing(Card::getNameRomaji);
        allowed.sort(classFirst.thenComparing(byKi).thenComparing(byName));
        return allowed;
    }


    /** Shows in the collection the allowed cards that pass the filters, keeping their order (the deck's class first, then Neutral). */
    private void showCollection() {
        List<Card> shown = new ArrayList<>();
        for (Card card : allowed) {
            if (filter.matches(card)) {
                shown.add(card);
            }
        }
        collection.show(shown);
    }

    /** True for a Neutral card: used to sort the deck's class cards first. */
    private static boolean isNeutral(Card card) {
        return card.getHeroClass() == HeroClass.NEUTRAL;
    }

    /** Left click in the collection: adds one copy, unless the deck is full or already has the most copies allowed. */
    private void addCard(Card card) {
        if (cards.size() >= Deck.SIZE) {
            messageLabel.setText("The deck is full (" + Deck.SIZE + " cards).");
            return;
        }
        int maxCopies = card.getRarity().getMaxCopies();
        if (copiesOf(card) >= maxCopies) {
            messageLabel.setText("At most " + maxCopies + " copies of " + card.getNameRomaji() + ".");
            return;
        }
        cards.add(card);
        messageLabel.setText("");
        refresh();
    }

    /** Right click (on either side): takes one copy out of the deck, if it has one. */
    private void removeCard(Card card) {
        boolean removed = cards.remove(card);
        if (removed) {
            messageLabel.setText("");
            refresh();
        }
    }

    /** How many copies of this card the deck holds right now. */
    private int copiesOf(Card card) {
        int count = 0;
        for (Card inDeck : cards) {
            if (inDeck.getId().equals(card.getId())) {
                count++;
            }
        }
        return count;
    }

    /** Redraws the deck list and the counter after every change. */
    private void refresh() {
        deckPanel.show(cards);
        countLabel.setText(cards.size() + " / " + Deck.SIZE + " cards");
    }

    /** Save: the new name (unless left empty) and the cards go into the deck, then back to the list. */
    private void save() {
        String name = nameField.getText().trim();
        if (!name.isEmpty()) {
            deck.setName(name);
        }
        deck.setCards(cards);
        DecksView.open(router, listClass);
    }

    /** Cancel: back to the list, the deck stays as it was. */
    private void cancel() {
        DecksView.open(router, listClass);
    }
}