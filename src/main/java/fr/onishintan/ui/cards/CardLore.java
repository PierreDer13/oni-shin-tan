package fr.onishintan.ui.cards;

import fr.onishintan.model.Card;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/** A short history note for each card (data/lore.properties: card id = text), read once. */
final class CardLore {
    private static final Properties LORE = load();

    private CardLore() { }

    /** The card's history note, or "" when it has none (an engine token). */
    static String of(Card card) {
        return LORE.getProperty(card.getId(), "");
    }

    /** Reads the notes from the resources; none if the file is missing. */
    private static Properties load() {
        Properties lore = new Properties();
        InputStream in = CardLore.class.getResourceAsStream("/data/lore.properties");
        if (in == null) {
            return lore;
        }
        try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            lore.load(reader);
        } catch (IOException e) {
            System.err.println("lore.properties unreadable: " + e.getMessage());
        }
        return lore;
    }
}
