package fr.onishintan.ui.cards;

import fr.onishintan.model.Card;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * A deck as Ki sections, cheapest first, one under the other: the Ki ("3 気"), the deck's cards of that cost
 * as CardChips (PER_ROW per row, each different card once with its copies), then a faint line.
 * Hovering a chip opens its big copy (CardPreview); clicking a chip calls onClick.
 */
public class DeckListView extends VBox {
    private static final int PER_ROW = 4;
    private static final double GAP = 6;
    private static final String FONT = "-fx-font-family: 'Yu Mincho'; -fx-font-weight: bold;";

    private final double width;
    private final String headerColour;
    private final CardPreview preview;
    private final Consumer<Card> onClick;

    /**
     * @param width        the list's width; the chips share it, PER_ROW per row
     * @param headerColour colour of the Ki titles and lines (ink on paper, cream on a dark frame)
     * @param preview      the hover effect (the chip grows, the big card opens)
     * @param onClick      what a click on a chip does (the deck editor takes a copy out); card -> {} for nothing
     */
    public DeckListView(double width, String headerColour, CardPreview preview, Consumer<Card> onClick) {
        super(6);
        this.width = width;
        this.headerColour = headerColour;
        this.preview = preview;
        this.onClick = onClick;
        setPrefWidth(width);
    }

    /** Shows these cards in Ki sections. */
    public void show(List<Card> cards) {
        getChildren().clear();
        Map<Integer, Map<Card, Integer>> byKi = groupByKi(cards);
        boolean firstSection = true;
        for (Map.Entry<Integer, Map<Card, Integer>> section : byKi.entrySet()) {
            if (!firstSection) {
                getChildren().add(separator());
            }
            firstSection = false;
            getChildren().add(kiTitle(section.getKey()));
            getChildren().add(chipRows(section.getValue()));
        }
    }

    /** The cards grouped by Ki (cheapest first), inside each Ki sorted by name, with how many copies of each. */
    private static Map<Integer, Map<Card, Integer>> groupByKi(List<Card> cards) {
        List<Card> sorted = new ArrayList<>(cards);
        Comparator<Card> byKi = Comparator.comparingInt(Card::getKiCost);
        Comparator<Card> byName = Comparator.comparing(Card::getNameRomaji);
        sorted.sort(byKi.thenComparing(byName));

        Map<Integer, Map<Card, Integer>> sections = new TreeMap<>();   // TreeMap keeps the Ki in order
        for (Card card : sorted) {
            Map<Card, Integer> section = sections.computeIfAbsent(card.getKiCost(), ki -> new LinkedHashMap<>());
            section.merge(card, 1, Integer::sum);   // one more copy
        }
        return sections;
    }

    /** "3 気": the section's Ki. */
    private Label kiTitle(int ki) {
        Label title = new Label(ki + " 気");
        title.setStyle(FONT + " -fx-font-size: 18; -fx-text-fill: " + headerColour + ";");
        return title;
    }

    /** The section's chips, PER_ROW per row. */
    private FlowPane chipRows(Map<Card, Integer> section) {
        double chipW = (width - (PER_ROW - 1) * GAP) / PER_ROW;
        FlowPane rows = new FlowPane(GAP, GAP);
        rows.setPrefWrapLength(width);
        for (Map.Entry<Card, Integer> entry : section.entrySet()) {
            Card card = entry.getKey();
            CardChip chip = new CardChip(card, entry.getValue(), chipW);
            preview.attach(chip, card);
            // A click (left or right button) takes one copy out; the big copy closes, since the list is redrawn.
            chip.setOnMousePressed(event -> {
                preview.hide();
                onClick.accept(card);
            });
            rows.getChildren().add(chip);
        }
        return rows;
    }

    /** The faint line between two Ki sections. */
    private Region separator() {
        Region line = new Region();
        line.setPrefSize(width, 1);
        line.setMaxHeight(1);
        line.setStyle("-fx-background-color: " + headerColour + ";");
        line.setOpacity(0.25);
        return line;
    }
}