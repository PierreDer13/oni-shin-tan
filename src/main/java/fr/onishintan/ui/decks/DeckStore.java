package fr.onishintan.ui.decks;

import fr.onishintan.model.HeroClass;
import fr.onishintan.data.SaveManager;
import fr.onishintan.model.Card;
import fr.onishintan.data.CardRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Every deck the player has: loaded from disk at start-up (Pierre's SaveManager), written again after each change.
 * With no deck saved, it starts with one starter deck per class (CardRepository), which the player can edit or delete.
 */
public final class DeckStore {
    private static final SaveManager SAVES = new SaveManager();
    private static final List<SavedDeck> DECKS = load();

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

    /** A new deck: kept, then every deck is written to disk. */
    public static void add(SavedDeck deck) {
        DECKS.add(deck);
        save();
    }

    /** A deleted deck: dropped, then every deck is written to disk. */
    public static void delete(SavedDeck deck) {
        DECKS.remove(deck);
        save();
    }

    /** Writes every deck to disk (SaveManager's decks file): after each add, delete and editor Save. */
    public static void save() {
        List<SaveManager.DeckData> data = new ArrayList<>();
        for (SavedDeck deck : DECKS) {
            List<String> ids = new ArrayList<>();
            for (Card card : deck.getCards()) {
                ids.add(card.getId());
            }
            data.add(new SaveManager.DeckData(deck.getName(), deck.getHeroClass(), ids));
        }
        SAVES.saveDecks(data);
    }

    /** The decks saved on disk, their cards found again by id among the test cards (an unknown id is skipped). */
    private static List<SavedDeck> load() {
        Map<String, Card> byId = new HashMap<>();
        for (Card card : CardRepository.all()) {
            byId.put(card.getId(), card);
        }
        List<SavedDeck> decks = new ArrayList<>();
        for (SaveManager.DeckData data : SAVES.loadDecks()) {
            List<Card> cards = new ArrayList<>();
            for (String id : data.cardIds()) {
                Card card = byId.get(id);
                if (card != null) {
                    cards.add(card);
                }
            }
            SavedDeck deck = new SavedDeck(data.name(), data.heroClass());
            deck.setCards(cards);
            decks.add(deck);
        }

        if (decks.isEmpty()) {   // first launch, or every deck deleted: one starter deck per class, editable and deletable like any other
            for (HeroClass heroClass : List.of(HeroClass.SAMURAI, HeroClass.NINJA, HeroClass.KANNUSHI, HeroClass.ONI)) {
                SavedDeck starter = new SavedDeck("Starter " + heroClass.getDisplayName(), heroClass);
                starter.setCards(CardRepository.deckCards(heroClass));
                decks.add(starter);
            }
        }
        return decks;
    }
}