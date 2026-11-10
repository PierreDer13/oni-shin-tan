package fr.onishintan.ui.decks;

import fr.onishintan.model.HeroClass;

import java.util.ArrayList;
import java.util.List;

/**
 * Every deck the player has built, kept in memory while the game runs.
 * Temporary: once Pierre's SaveManager (data package) exists, these methods will call it to save to disk.
 */
public final class DeckStore {
    private static final List<SavedDeck> DECKS = new ArrayList<>();

    private DeckStore() {
        // Only static methods: nobody creates a DeckStore.
    }

    /** All decks, in the order they were created. */
    public static List<SavedDeck> all() {
        return new ArrayList<>(DECKS);
    }

    /** Only the decks of one class (what a class banner shows). */
    public static List<SavedDeck> ofClass(HeroClass heroClass) {
        List<SavedDeck> result = new ArrayList<>();
        for (SavedDeck deck : DECKS) {
            if (deck.getHeroClass() == heroClass) {
                result.add(deck);
            }
        }
        return result;
    }

    public static void add(SavedDeck deck) {
        DECKS.add(deck);
    }

    public static void delete(SavedDeck deck) {
        DECKS.remove(deck);
    }
}