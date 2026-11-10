package fr.onishintan.ui.decks;

import fr.onishintan.model.Card;
import fr.onishintan.model.Deck;
import fr.onishintan.model.HeroClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

/** One deck the player built: its name, its class and its cards. */
public class SavedDeck {
    private String name;
    private final HeroClass heroClass;
    private final List<Card> cards = new ArrayList<>();

    public SavedDeck(String name, HeroClass heroClass) {
        this.name = name;
        this.heroClass = heroClass;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HeroClass getHeroClass() {
        return heroClass;
    }

    /** A copy of the cards, so nobody changes the deck by accident. Use setCards() to change them. */
    public List<Card> getCards() {
        return new ArrayList<>(cards);
    }

    /** Replaces every card of the deck (the editor calls this when you press Save). */
    public void setCards(List<Card> newCards) {
        cards.clear();
        cards.addAll(newCards);
    }

    /** What is wrong with the deck, using Pierre's rules in Deck.validate(); an empty list means it is legal. */
    public List<String> problems() {
        Deck deck = new Deck(cards);
        return deck.validate(heroClass);
    }

    public boolean isLegal() {
        return problems().isEmpty();
    }

    /**
     * Each different card once, cheapest first, with how many copies of it are in `cards`.
     * A LinkedHashMap keeps the cards in the order they were put in, so in Ki order.
     */
    static Map<Card, Integer> copiesByKi(List<Card> cards) {
        List<Card> sorted = new ArrayList<>(cards);
        sorted.sort(Comparator.comparingInt(Card::getKiCost));
        Map<Card, Integer> copies = new LinkedHashMap<>();
        for (Card card : sorted) {
            int before = copies.getOrDefault(card, 0);
            copies.put(card, before + 1);
        }
        return copies;
    }
}