package fr.onishintan.ui.decks;

import fr.onishintan.ui.screens.HomeView;
import fr.onishintan.ui.app.Images;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.image.ImageView;

/**
 * The row above the cards: a search box and the type panels (All, Minions, Spells). The Ki filter is the soroban (SorobanFilter).
 * It writes each choice into the CardFilter, then calls onChange.
 */
public class FilterBar extends HBox {
    private static final String BUTTON = "-fx-font-family: 'Yu Mincho'; -fx-font-weight: bold; -fx-font-size: 16;"
            + " -fx-padding: 4 10; -fx-background-radius: 8;";
    /** A button that is not picked: ink on light paper. */
    static final String OFF = BUTTON + " -fx-text-fill: " + DecksView.INK + "; -fx-background-color: #e6dccb;";
    /** The picked button: paper on dark ink, like the other Deck Builder buttons. */
    static final String ON = BUTTON + " -fx-text-fill: " + DecksView.PAPER + "; -fx-background-color: rgba(20,12,8,0.85);";
    /** How tall the All / Minions / Spells panels are drawn (the pictures are 396 x 132). */
    private static final double PANEL_H = 44;
    /** How see-through a panel that is not picked is. */
    private static final double DIMMED = 0.55;

    private final CardFilter filter;
    private final Runnable onChange;
    private final List<ImageView> typeButtons = new ArrayList<>();

    public FilterBar(CardFilter filter, Runnable onChange) {
        super(8);
        this.filter = filter;
        this.onChange = onChange;
        setAlignment(Pos.CENTER_LEFT);

        getChildren().add(searchBox());

        getChildren().add(caption("Type"));
        addTypeButton("all", CardFilter.Type.ALL);
        addTypeButton("minions", CardFilter.Type.MINIONS);
        addTypeButton("spells", CardFilter.Type.SPELLS);

        // At the start nothing is filtered: "All" is picked.
        pickPanel(typeButtons, typeButtons.get(0));
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
        Label caption = DecksView.text(words, 18, DecksView.PAPER);
        caption.setStyle(caption.getStyle() + " -fx-padding: 0 0 0 16;");
        return caption;
    }

    /** One type panel (images/deck-builder/<name>-panel.png): All, Minions or Spells. */
    private void addTypeButton(String name, CardFilter.Type type) {
        ImageView panel = new ImageView(Images.get("/images/deck-builder/" + name + "-panel.png"));
        panel.setFitHeight(PANEL_H);
        panel.setPreserveRatio(true);
        HomeView.clickable(panel, false, () -> {
            filter.setType(type);
            pickPanel(typeButtons, panel);
            onChange.run();
        });
        typeButtons.add(panel);
        getChildren().add(panel);
    }

    /** Shows `picked` as chosen and the other buttons of its row as not chosen. */
    static void pick(List<Label> row, Label picked) {
        for (Label button : row) {
            if (button == picked) {
                button.setStyle(ON);
            } else {
                button.setStyle(OFF);
            }
        }
    }

    /** Shows `picked` as chosen (full colour) and the other panels of its row dimmed. */
    private static void pickPanel(List<ImageView> row, ImageView picked) {
        for (ImageView panel : row) {
            if (panel == picked) {
                panel.setOpacity(1);
            } else {
                panel.setOpacity(DIMMED);
            }
        }
    }
}