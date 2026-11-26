package fr.onishintan.ui.match.hud;

import fr.onishintan.model.Card;
import fr.onishintan.model.Combatant;
import fr.onishintan.model.Hero;
import fr.onishintan.model.Minion;
import fr.onishintan.ui.app.ScrollTheme;
import fr.onishintan.ui.cards.CardPreview;
import fr.onishintan.ui.screens.HomeView;

import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * The play history, on the left side of the match: every card played, attack and hero power, with their targets,
 * newest on top. A tab opens and closes (minimises) it. Hovering a played card shows it bigger (CardPreview).
 */
public class PlayHistory extends Pane {
    private static final double PANEL_W = 330;   // width of the open panel
    private static final double PANEL_H = 560;   // height of the open panel
    private static final double LIST_W = 258;    // width of the list inside the panel (leaves room for the scroll bar)
    private static final double TOP = 140;       // under the Settings button
    private static final String PAPER = "#f3ede1";
    private static final String SOFT = "#aaa293";
    private static final String GOLD = "#d4ab4a";
    private static final String FONT = "-fx-font-family: 'Yu Mincho'; -fx-font-weight: bold;";
    private static final String DARK_BOX = "-fx-background-color: rgba(20,12,8,0.85); -fx-background-radius: 10;";

    private final CardPreview preview;
    private final VBox rows = new VBox(6);
    private final VBox panel = new VBox(10);
    private final Label tab = new Label();

    public PlayHistory(CardPreview preview) {
        this.preview = preview;
        setPickOnBounds(false);   // clicks on the empty table still go through this layer

        // The rows scroll when the history is long.
        ScrollPane scroll = new ScrollPane(rows);
        scroll.setPrefSize(LIST_W + 40, PANEL_H - 24);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        ScrollTheme.apply(scroll);   // the paper-scroll bar

        // The panel: against the left edge, on a dark background.
        panel.getChildren().add(scroll);
        panel.setStyle(DARK_BOX + " -fx-padding: 12;");
        panel.setPrefSize(PANEL_W, PANEL_H);
        panel.relocate(0, TOP);
        panel.setVisible(false);   // starts minimised

        // The tab: always on screen, just above the panel. A click opens or closes it.
        tab.setStyle(FONT + " -fx-font-size: 18; -fx-text-fill: " + PAPER + "; " + DARK_BOX + " -fx-padding: 6 12;");
        tab.relocate(0, TOP - 50);
        HomeView.clickable(tab, false, this::toggle);
        updateTab();

        getChildren().addAll(panel, tab);
    }

    /** A new turn starts: a faint line with whose turn it is. */
    public void turn(String playerName) {
        add(row("— " + playerName + " —", SOFT));
    }

    /** A card played, with its target(s) when it has any; hovering the line shows the card big. */
    public void played(String playerName, Card card, Combatant first, Combatant second) {
        String text = playerName + ":  " + card.getNameRomaji();
        if (first != null) {
            text = text + "  →  " + nameOf(first);
        }
        if (second != null) {
            text = text + "  &  " + nameOf(second);
        }
        Label line = row(text, PAPER);
        preview.attach(line, card);
        add(line);
    }

    /** An attack: who attacked whom. */
    public void attacked(Combatant attacker, Combatant target) {
        add(row("⚔  " + nameOf(attacker) + "  →  " + nameOf(target), PAPER));
    }

    /** A hero power, with its target when it has one (the Miko has none). */
    public void heroPower(String playerName, Combatant target) {
        String text = playerName + ":  hero power";
        if (target != null) {
            text = text + "  →  " + nameOf(target);
        }
        add(row(text, GOLD));
    }

    /** Opens the panel if it is closed, closes it if it is open (its tab, or H in a match). */
    public void toggle() {
        panel.setVisible(!panel.isVisible());
        updateTab();
    }

    /** The tab's text, with an arrow showing if a click will open or close. */
    private void updateTab() {
        String arrow = "▼";
        if (panel.isVisible()) {
            arrow = "▲";
        }
        tab.setText("記録  HISTORY  " + arrow);
    }

    /** Puts a line at the top of the history: the newest first. */
    private void add(Label line) {
        rows.getChildren().add(0, line);
    }

    /** One line of the history, wrapped to the list's width. */
    private static Label row(String text, String colour) {
        Label line = new Label(text);
        line.setWrapText(true);
        line.setMaxWidth(LIST_W);
        line.setStyle(FONT + " -fx-font-size: 15; -fx-text-fill: " + colour + ";");
        return line;
    }

    /** A minion by its card's name, a hero by its class ("Oni hero"). */
    private static String nameOf(Combatant combatant) {
        if (combatant instanceof Minion minion) {
            return minion.getSource().getNameRomaji();
        }
        if (combatant instanceof Hero hero) {
            return hero.getHeroClass().getDisplayName() + " hero";
        }
        return "?";
    }
}
