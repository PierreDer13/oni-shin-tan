package fr.onishintan.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.onishintan.model.Card;
import fr.onishintan.model.CurseCard;
import fr.onishintan.model.CurseZone;
import fr.onishintan.model.Deck;
import fr.onishintan.model.Hero;
import fr.onishintan.model.HeroClass;
import fr.onishintan.model.Keyword;
import fr.onishintan.model.Minion;
import fr.onishintan.model.MinionCard;
import fr.onishintan.model.Player;
import fr.onishintan.model.Rarity;
import fr.onishintan.model.RiteCard;
import fr.onishintan.model.ShrineCard;
import fr.onishintan.model.SpellCard;
import fr.onishintan.model.TrapCard;
import fr.onishintan.model.TrapTrigger;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Every card of the game, read once from /data/cards.json (one object per card; its "type" says which kind of card it is).
 * all() is every card, for the engine and the AI decks; collectible() is what the player owns and can put in a deck:
 * not the hidden cards (Yamata no Orochi's dragon), and a reward card (the kami, Amaterasu) only once it has been claimed.
 */
public final class CardRepository {
    private static final String FILE = "/data/cards.json";

    /** One card as written in cards.json; the fields its type does not use stay at their default (0 or null). */
    public record CardData(String id, String type, String name, String kanji, HeroClass heroClass, int ki, Rarity rarity, String text,
                    boolean collectible, boolean reward, int attack, int health, Set<Keyword> keywords, TrapTrigger trigger,
                    int turns, CurseZone zone, int copies, int removalKi, int ancestors, Set<Keyword> nowKeywords,
                    int bonusAttack, int bonusHealth, Set<Keyword> bonusKeywords) {}

    /** Each card's data by id, in the file's order (filled by load()). */
    private static final Map<String, CardData> DATA = new LinkedHashMap<>();
    /** Each card by id. */
    private static final Map<String, Card> BY_ID = new LinkedHashMap<>();
    private static final List<Card> ALL = load();

    private CardRepository() {
        // Only static methods: nobody creates a CardRepository.
    }

    /** Every card, hidden and reward cards included: the engine's catalog and the AI decks. */
    public static List<Card> all() {
        return ALL;
    }

    /** The card with this id, or null if there is none. */
    public static Card byId(String id) {
        return BY_ID.get(id);
    }

    /** The cards the player owns (the Inventory and the deck editor): every collectible card, a reward card only once claimed. */
    public static List<Card> collectible() {
        SaveManager saves = new SaveManager();
        List<Card> owned = new ArrayList<>();
        for (Card card : ALL) {
            CardData data = DATA.get(card.getId());
            if (!data.collectible()) {
                continue;   // never shown (Yamata no Orochi's dragon)
            }
            if (data.reward() && !saves.isUnlocked(card.getId())) {
                continue;   // a kami or Amaterasu not claimed yet
            }
            owned.add(card);
        }
        return owned;
    }

    /** A starter deck of exactly Deck.SIZE cards: every card of the class, then Neutral minions (no hidden or reward card). */
    public static List<Card> deckCards(HeroClass heroClass) {
        List<Card> deck = new ArrayList<>();
        for (Card card : ALL) {
            if (starter(card) && card.getHeroClass() == heroClass) {
                deck.add(card);
            }
        }
        for (Card card : ALL) {
            if (deck.size() < Deck.SIZE && starter(card) && card instanceof MinionCard && card.getHeroClass() == HeroClass.NEUTRAL) {
                deck.add(card);
            }
        }
        return deck;
    }

    /** A test player with deckCards() as the deck; the board holds n of them. */
    public static Player player(String name, HeroClass heroClass, int minions) {
        List<Card> deck = deckCards(heroClass);
        Player p = new Player(name, new Hero(heroClass), new Deck(deck));
        for (int i = 0; i < minions; i++) {
            p.getSide().place(new Minion((MinionCard) deck.get(i % deck.size())), i);
        }
        return p;
    }

    /** True if a starter deck may hold this card: collectible and not a reward. */
    private static boolean starter(Card card) {
        CardData data = DATA.get(card.getId());
        return data.collectible() && !data.reward();
    }

    /** Reads cards.json and builds every card. */
    private static List<Card> load() {
        try (InputStream in = CardRepository.class.getResourceAsStream(FILE)) {
            if (in == null) {
                throw new IllegalStateException(FILE + " is missing from the resources");
            }
            List<CardData> cards = new ObjectMapper().readValue(in, new TypeReference<List<CardData>>() {});
            List<Card> all = new ArrayList<>();
            for (CardData data : cards) {
                Card card = build(data);
                DATA.put(data.id(), data);
                BY_ID.put(data.id(), card);
                all.add(card);
            }
            return List.copyOf(all);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + FILE, e);
        }
    }

    /** One card from its data, as the engine's own class for its type. */
    private static Card build(CardData d) {
        return switch (d.type()) {
            case "MINION" -> new MinionCard(d.id(), d.name(), d.kanji(), "", d.ki(), d.rarity(), d.heroClass(), d.text(), "",
                    d.attack(), d.health(), orEmpty(d.keywords()));
            case "TRAP" -> new TrapCard(d.id(), d.name(), d.kanji(), "", d.ki(), d.rarity(), d.heroClass(), d.text(), "", d.trigger());
            case "SHRINE" -> new ShrineCard(d.id(), d.name(), d.kanji(), "", d.ki(), d.rarity(), d.heroClass(), d.text(), "", d.turns());
            case "CURSE" -> new CurseCard(d.id(), d.name(), d.kanji(), "", d.ki(), d.rarity(), d.heroClass(), d.text(), "",
                    d.zone(), d.copies(), d.removalKi());
            case "RITE" -> new RiteCard(d.id(), d.name(), d.kanji(), "", d.ki(), d.rarity(), d.heroClass(), d.text(), "",
                    d.ancestors(), orEmpty(d.nowKeywords()), d.bonusAttack(), d.bonusHealth(), orEmpty(d.bonusKeywords()));
            default -> new SpellCard(d.id(), d.name(), d.kanji(), "", d.ki(), d.rarity(), d.heroClass(), d.text(), "");
        };
    }

    /** The keywords as written, or none when the field is missing. */
    private static Set<Keyword> orEmpty(Set<Keyword> keywords) {
        if (keywords == null) {
            return Set.of();
        }
        return keywords;
    }
}
