package fr.onishintan.ui.decks;

import fr.onishintan.ui.screens.HomeView;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

/**
 * The row above the deck editor's collection: a search box, Ki buttons (All, 1 to 6, 7+) and type buttons (All, Minions, Spells).
 * It writes each choice into the CardFilter, then calls onChange.
 */
class FilterBar extends HBox {
    private static final String BUTTON = "-fx-font-family: 'Yu Mincho'; -fx-font-weight: bold; -fx-font-size: 16;"
            + " -fx-padding: 4 10; -fx-background-radius: 8;";
    /** A button that is not picked: ink on light paper. */
    private static final String OFF = BUTTON + " -fx-text-fill: " + DecksView.INK + "; -fx-background-color: #e6dccb;";
    /** The picked button: paper on dark ink, like the other Deck Builder buttons. */
    private static final String ON = BUTTON + " -fx-text-fill: " + DecksView.PAPER + "; -fx-background-color: rgba(20,12,8,0.85);";

    private final CardFilter filter;
    private final Runnable onChange;
    private final List<Label> kiButtons = new ArrayList<>();
    private final List<Label> typeButtons = new ArrayList<>();

    FilterBar(CardFilter filter, Runnable onChange) {
        super(8);
        this.filter = filter;
        this.onChange = onChange;
        setAlignment(Pos.CENTER_LEFT);

        getChildren().add(searchBox());

        getChildren().add(caption("Ki"));
        addKiButton("All", CardFilter.ANY_KI);
        for (int ki = 1; ki < CardFilter.KI_OR_MORE; ki++) {
            addKiButton(String.valueOf(ki), ki);
        }
        addKiButton(CardFilter.KI_OR_MORE + "+", CardFilter.KI_OR_MORE);

        getChildren().add(caption("Type"));
        addTypeButton("All", CardFilter.Type.ALL);
        addTypeButton("Minions", CardFilter.Type.MINIONS);
        addTypeButton("Spells", CardFilter.Type.SPELLS);

        // At the start nothing is filtered: "All" is picked in both rows.
        pick(kiButtons, kiButtons.get(0));
        pick(typeButtons, typeButtons.get(0));
    }

    /** The search box: the collection is filtered again on every key typed. */
    private TextField searchBox() {
        TextField search = new TextField();
        search.setPromptText("Search a name or effect");
        search.setPrefWidth(240);
        search.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: 16;"
                + " -fx-background-color: #fbf8f1; -fx-border-color: #d9cfbd; -fx-border-radius: 6; -fx-background-radius: 6;");
        search.textProperty().addListener((property, oldText, newText) -> {
            filter.setSearch(newText);
            onChange.run();
        });
        return search;
    }

    /** A small label in front of a row of buttons ("Ki", "Type"). */
    private Label caption(String words) {
        Label caption = DecksView.text(words, 18, DecksView.INK);
        caption.setStyle(caption.getStyle() + " -fx-padding: 0 0 0 16;");
        return caption;
    }

    /** One Ki button: shows only the cards of that cost (that cost and more for the last one). */
    private void addKiButton(String words, int ki) {
        Label button = new Label(words);
        HomeView.clickable(button, false, () -> {
            filter.setKi(ki);
            pick(kiButtons, button);
            onChange.run();
        });
        kiButtons.add(button);
        getChildren().add(button);
    }

    /** One type button: All, Minions or Spells. */
    private void addTypeButton(String words, CardFilter.Type type) {
        Label button = new Label(words);
        HomeView.clickable(button, false, () -> {
            filter.setType(type);
            pick(typeButtons, button);
            onChange.run();
        });
        typeButtons.add(button);
        getChildren().add(button);
    }

    /** Shows `picked` as chosen and the other buttons of its row as not chosen. */
    private void pick(List<Label> row, Label picked) {
        for (Label button : row) {
            if (button == picked) {
                button.setStyle(ON);
            } else {
                button.setStyle(OFF);
            }
        }
    }
}