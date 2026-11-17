package fr.onishintan.ui.decks;

import fr.onishintan.ui.screens.HomeView;
import fr.onishintan.ui.app.Images;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * The row above the cards: the search panel and the type panels (All, Minions, Spells). The Ki filter is the soroban (SorobanFilter).
 * It writes each choice into the CardFilter, then calls onChange.
 */
public class FilterBar extends HBox {
    /** The search panel (396 x 132) and how tall it is drawn; the text box sits on its paper. */
    private static final String SEARCH_PANEL = "/images/deck-builder/search-panel.png";
    private static final double SEARCH_H = 60;
    /** How tall the All / Minions / Spells panels are drawn (the pictures are 396 x 132). */
    static final double PANEL_H = 44;
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

        addTypeButton("all", CardFilter.Type.ALL);
        addTypeButton("minions", CardFilter.Type.MINIONS);
        addTypeButton("spells", CardFilter.Type.SPELLS);

        // At the start nothing is filtered: "All" is picked.
        pickPanel(typeButtons, typeButtons.get(0));
    }

    /**
     * The search panel (images/deck-builder/search-panel.png) with a see-through text box on its paper,
     * right of the knot and left of the magnifying glass. The collection is filtered again on every key typed.
     */
    private Pane searchBox() {
        Image picture = Images.get(SEARCH_PANEL);
        double width = SEARCH_H * picture.getWidth() / picture.getHeight();
        ImageView panel = new ImageView(picture);
        panel.setFitWidth(width);
        panel.setFitHeight(SEARCH_H);

        TextField search = new TextField();
        search.setPromptText("Search");
        search.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: 14; -fx-background-color: transparent;"
                + " -fx-font-weight: bold; -fx-alignment: center; -fx-text-fill: white; -fx-prompt-text-fill: white;");
        // Only on the paper, a little above its middle so the text looks centred: 12 % to 78 % of the width.
        search.relocate(width * 0.12, SEARCH_H * 0.22);
        search.setPrefSize(width * 0.66, SEARCH_H * 0.34);
        search.textProperty().addListener((property, oldText, newText) -> {
            filter.setSearch(newText);
            onChange.run();
        });
        return new Pane(panel, search);
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

    /** Shows `picked` as chosen (full colour) and the other panels of its row dimmed. */
    static void pickPanel(List<ImageView> row, ImageView picked) {
        for (ImageView panel : row) {
            if (panel == picked) {
                panel.setOpacity(1);
            } else {
                panel.setOpacity(DIMMED);
            }
        }
    }
}