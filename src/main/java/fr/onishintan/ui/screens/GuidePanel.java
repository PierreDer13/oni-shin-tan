package fr.onishintan.ui.screens;

import fr.onishintan.ui.app.Images;
import fr.onishintan.ui.app.ScreenGrid;
import fr.onishintan.ui.cards.KeywordGuide;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

/** The game guide, laid over the current screen: every keyword, status and spell family with its logo and its rule. */
public class GuidePanel extends StackPane {
    /** Size of each logo; a painted one is read from /images/logos/<name>-logo.png. */
    private static final double LOGO = 48;

    public GuidePanel() {
        Rectangle veil = new Rectangle(ScreenGrid.W, ScreenGrid.H, Color.rgb(0, 0, 0, 0.6));
        veil.setOnMousePressed(e -> close());

        // Two columns of entries, filled left to right.
        GridPane entries = new GridPane();
        entries.setHgap(40);
        entries.setVgap(14);
        for (int i = 0; i < KeywordGuide.ALL.size(); i++) {
            entries.add(entry(KeywordGuide.ALL.get(i)), i % 2, i / 2);
        }

        VBox panel = new VBox(24, SettingsPanel.label("案内  Game guide", 40), entries, SettingsPanel.button("閉じる  Close", this::close));
        panel.setAlignment(Pos.CENTER);
        panel.setMaxSize(1300, 820);
        panel.setStyle("-fx-background-color: rgba(20,12,8,0.92); -fx-background-radius: 16; -fx-padding: 30;");
        getChildren().addAll(veil, panel);
        setPrefSize(ScreenGrid.W, ScreenGrid.H);
    }

    /** One entry: its logo, then its name in bold and its rule under it. */
    private static HBox entry(KeywordGuide.Entry entry) {
        Label rule = SettingsPanel.label(entry.text(), 15);
        rule.setWrapText(true);
        rule.setMaxWidth(470);
        VBox words = new VBox(2, SettingsPanel.label(entry.name(), 20), rule);
        HBox row = new HBox(14, logo(entry), words);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /** The painted logo if images/logos/<name>-logo.png exists, otherwise the kanji in a red circle. */
    private static Node logo(KeywordGuide.Entry entry) {
        Image painted = Images.get("/images/logos/" + entry.name().toLowerCase() + "-logo.png", LOGO, LOGO);
        if (painted != null) {
            return new ImageView(painted);
        }
        Circle circle = new Circle(LOGO / 2, Color.web("#b3372a"));
        Label kanji = SettingsPanel.label(entry.kanji(), 16);
        return new StackPane(circle, kanji);
    }

    /** Takes the guide off the screen it was laid over. */
    private void close() {
        ((Pane) getParent()).getChildren().remove(this);
    }
}