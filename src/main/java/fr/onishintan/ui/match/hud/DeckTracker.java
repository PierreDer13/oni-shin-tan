package fr.onishintan.ui.match.hud;

import fr.onishintan.model.Player;
import fr.onishintan.ui.cards.CardPreview;
import fr.onishintan.ui.cards.DeckListView;
import fr.onishintan.ui.screens.HomeView;
import fr.onishintan.ui.app.ScrollTheme;

import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import fr.onishintan.ui.match.BoardView;

/**
 * The deck tracker, on the right side of the match: the cards still in your deck,
 * in Ki sections like the deck builder's deck list (DeckListView).
 * A tab on top opens and closes (minimises) it. Hovering a card shows it bigger (CardPreview).
 */
public class DeckTracker extends Pane {
    private static final double PANEL_W = 330;   // width of the open panel
    private static final double PANEL_H = 600;   // height of the open panel
    private static final double LIST_W = 258;    // width of the card list inside the panel (leaves room for the scroll bar)
    private static final double TOP = 90;        // the panel starts this far from the top of the screen
    private static final String PAPER = "#f3ede1";   // same cream as the deck builder
    private static final String FONT = "-fx-font-family: 'Yu Mincho'; -fx-font-weight: bold;";
    private static final String DARK_BOX = "-fx-background-color: rgba(20,12,8,0.85); -fx-background-radius: 10;";

    private final CardPreview preview;
    private final DeckListView list;
    private final VBox panel = new VBox(10);
    private final Label tab = new Label();
    private final Label title = new Label();
    private int cardsLeft = 0;

    public DeckTracker(CardPreview preview) {
        this.preview = preview;
        setPickOnBounds(false);   // clicks on the empty table still go through this layer

        // The list: the same as the deck builder's. Clicking a card does nothing here.
        list = new DeckListView(LIST_W, PAPER, preview, card -> {});

        // The list scrolls when the deck is long.
        ScrollPane scroll = new ScrollPane(list);
        scroll.setPrefSize(LIST_W + 40, PANEL_H - 60);   // room for the list + the 36 px themed bar
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        ScrollTheme.apply(scroll);   // the paper-scroll bar, like in the Inventory and Adventure screens

        // The title: how many cards are left.
        title.setStyle(FONT + " -fx-font-size: 20; -fx-text-fill: " + PAPER + ";");

        // The panel: title on top, list under it, on a dark background, against the right edge.
        panel.getChildren().addAll(title, scroll);
        panel.setStyle(DARK_BOX + " -fx-padding: 12;");
        panel.setPrefSize(PANEL_W, PANEL_H);
        panel.relocate(BoardView.W - PANEL_W, TOP);
        panel.setVisible(false);   // starts minimised, so it doesn't hide the End Turn button

        // The tab: always on screen, just above the panel. A click opens or closes it.
        tab.setStyle(FONT + " -fx-font-size: 18; -fx-text-fill: " + PAPER + "; " + DARK_BOX + " -fx-padding: 6 12;");
        tab.relocate(BoardView.W - PANEL_W, TOP - 50);
        HomeView.clickable(tab, false, this::toggle);

        getChildren().addAll(panel, tab);
    }

    /** Shows the cards left in this player's deck. Called at every redraw of the board. */
    public void show(Player player) {
        preview.hide();   // the old chips disappear: close a big card that might still be open
        cardsLeft = player.getDeck().size();
        list.show(player.getDeck().getCards());   // DeckListView sorts them, so the deck order stays secret
        title.setText("Cards left: " + cardsLeft);
        updateTab();
    }

    /** Opens the panel if it is closed, closes it if it is open. */
    private void toggle() {
        boolean open = !panel.isVisible();
        panel.setVisible(open);
        updateTab();
    }

    /** The tab's text: the number of cards left, and an arrow showing if a click will open or close. */
    private void updateTab() {
        String arrow;
        if (panel.isVisible()) {
            arrow = "▲";
        } else {
            arrow = "▼";
        }
        tab.setText("山札  DECK (" + cardsLeft + ")  " + arrow);
    }
}
