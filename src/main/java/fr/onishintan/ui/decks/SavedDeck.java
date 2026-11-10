package fr.onishintan.ui.decks;

import fr.onishintan.model.Card;
import fr.onishintan.model.Deck;
import fr.onishintan.model.HeroClass;

import java.util.ArrayList;
import java.util.List;

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
}