package fr.onishintan.ui.cards;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Beside a big card copy: one small dark note per keyword or spell family the card has, with what it does. */
final class KeywordNotes extends VBox {
    KeywordNotes(fr.onishintan.model.Card card) {
        super(6);
        for (KeywordGuide.Entry entry : KeywordGuide.of(card)) {
            Label note = new Label(entry.kanji() + "  " + entry.name() + "\n" + entry.text());
            note.setWrapText(true);
            note.setMaxWidth(220);
            note.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #f3ede1;"
                    + " -fx-background-color: rgba(20,12,8,0.9); -fx-background-radius: 6; -fx-padding: 6 8;");
            getChildren().add(note);
        }
        setMouseTransparent(true);   // only words to read: clicks go through
    }
}