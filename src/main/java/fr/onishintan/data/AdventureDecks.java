package fr.onishintan.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.onishintan.model.Card;
import fr.onishintan.model.Deck;
import fr.onishintan.model.Hero;
import fr.onishintan.model.HeroClass;
import fr.onishintan.model.Player;

/**
 * Adventure data (GDD, Adventure): the given 40-card deck of each class, the three opponents of a run
 * and their decks, battle by battle (1 Town / Machi, 2 Ocean / Taifu, 3 Mount Fuji).
 *
 * An enemy deck = its class core (30 cards) + the battle's 10 neutral cards: small minions in the town,
 * mid-curve minions on the sea, the neutral legendaries on Mount Fuji, so each battle is harder.
 * The cards come from the catalog given to the constructor (MockCards.ALL until cards.json exists).
 * The kami rewards and Amaterasu are never in these decks.
 */
public final class AdventureDecks {

    /** Battles in a run. */
    public static final int BATTLES = 3;

    /** The board of each battle, as BoardView names them: Town, Ocean (the ship), Mount Fuji. */
    private static final String[] BOARDS = { "town", "ship", "fuji" };

    /** Your opponent in battles 1 to 3, per class (GDD, Adventure). */
    private static final Map<HeroClass, HeroClass[]> FOES = new EnumMap<>(Map.of(
            HeroClass.SAMURAI, new HeroClass[] { HeroClass.KANNUSHI, HeroClass.ONI, HeroClass.NINJA },
            HeroClass.NINJA, new HeroClass[] { HeroClass.ONI, HeroClass.KANNUSHI, HeroClass.SAMURAI },
            HeroClass.KANNUSHI, new HeroClass[] { HeroClass.SAMURAI, HeroClass.NINJA, HeroClass.ONI },
            HeroClass.ONI, new HeroClass[] { HeroClass.NINJA, HeroClass.SAMURAI, HeroClass.KANNUSHI }));

    /** The deck the player is given for the run, per class: "id copies" pairs, 40 cards. */
    private static final Map<HeroClass, String> PLAYER_DECKS = new EnumMap<>(Map.of(
            HeroClass.SAMURAI, """
                    ashigaru 3, rite-of-shokan 2, chiburi 1, katana-kaji 2, rite-of-gokito 2, ueta-rikishi 2, yoroi 2,
                    hatamoto 2, ketto 1, rite-of-ujigami-mairi 1, rite-of-daijosai 1, ronin 2, jujutsu-shihan 1, daimyo 1, shogun 1,
                    saru 2, unsui 1, yumihiki 2, tsuru 2, kyabetsu-noka 2, sakana-uri 2, taiko-uchi 2, tora 2, raijin 1""",
            HeroClass.NINJA, """
                    genin 3, komuso 2, kemuri-dama 1, makibishi 2, kawarimi 1, noppera-bo 2, onnagata 2, otoshiana 1,
                    kitsune-no-bakashiya 2, kuji-in 1, tengu 3, shikomi-andon 1, yoi 1, kakuremino 1, jonin 2, fuma-kotaro 1,
                    tanuki 3, yumihiki 2, ikareru-hyakusho 2, saru 2, sakana-uri 2, tora 2, fujin 1""",
            HeroClass.KANNUSHI, """
                    kodama 2, heiwa-no-torii 1, miko 2, meiji-otorii 1, taisha-zukuri 2, goze 2, itsukushima 1,
                    kitsune-no-tsukai 3, harae 2, jushoku 2, kumano-hongu-taisha 1, yamabushi 2, onmyoji 2, kiseki 1,
                    senbon-torii 1, susanoo 1, inari-okami 1,
                    unsui 2, inasaku-noka 2, togeika 2, kame 1, maiko 2, sakana-uri 2, shike 1, yokozuna 1""",
            HeroClass.ONI, """
                    hakahori 2, kappa 3, rokurokubi 2, bakeneko 1, gagoze 1, hinoeuma 1, tatari 1, kamikakushi 1, gaki 1,
                    jorogumo 2, sakasama 1, tamashii-gui 1, oiwa 2, nue 1, shinigami 1, enma 1,
                    saru 2, monogoi 2, ikareru-hyakusho 2, yumihiki 2, tanuki 2, sakana-uri 2, sendaiku 2, taiko-uchi 2, tora 2"""));

    /** The 30 cards every enemy deck of that class starts from (no legendary). */
    private static final Map<HeroClass, String> ENEMY_CORES = new EnumMap<>(Map.of(
            HeroClass.SAMURAI, """
                    ashigaru 3, rite-of-shokan 2, katana-kaji 2, rite-of-gokito 2, ueta-rikishi 2, yoroi 2, hatamoto 3,
                    ketto 1, rite-of-ujigami-mairi 2, rite-of-daijosai 1, ronin 2, jujutsu-shihan 1, daimyo 2,
                    rite-of-mukaebi 1, chiburi 1, tsuru 2, yumihiki 1""",
            HeroClass.NINJA, """
                    genin 3, komuso 2, makibishi 2, kawarimi 2, noppera-bo 2, onnagata 2, otoshiana 2, oyasumi 1,
                    kitsune-no-bakashiya 2, tengu 3, shikomi-andon 1, kakuremino 1, jonin 2, dondengaeshi 1, yoi 1, tanuki 3""",
            HeroClass.KANNUSHI, """
                    kodama 3, miko 2, heiwa-no-torii 1, meiji-otorii 1, taisha-zukuri 2, goze 2, itsukushima 1,
                    kitsune-no-tsukai 3, harae 2, jushoku 2, kumano-hongu-taisha 1, yamabushi 2, onmyoji 2, kiseki 1,
                    senbon-torii 1, kagura 1, unsui 3""",
            HeroClass.ONI, """
                    hakahori 3, kappa 3, rokurokubi 3, bakeneko 1, gagoze 1, hinoeuma 1, tatari 1, gaki 1, gokiburi 1,
                    kamikakushi 1, jorogumo 2, sakasama 1, tamashii-gui 1, oiwa 2, nue 2, shinigami 2, kuchiyose 1,
                    ikareru-hyakusho 3"""));

    /** The 10 neutral cards each battle adds to the enemy core: Town (small), Ocean (mid-curve), Mount Fuji (legendaries). */
    private static final String[] BATTLE_CARDS = {
            "saru 2, monogoi 2, kame 2, kyabetsu-noka 2, togeika 2",
            "sakana-uri 2, sendaiku 2, taiko-uchi 2, tora 2, maiko 2",
            "tora 2, taiko-uchi 2, raijin 1, fujin 1, shike 1, yokozuna 1, ryu 1, tenno 1" };

    private final Map<String, Card> catalog = new HashMap<>();

    /** The cards the decks are built from, found by id (MockCards.ALL for now). */
    public AdventureDecks(Collection<? extends Card> cards) {
        for (Card c : cards) catalog.put(c.getId(), c);
    }

    /** The opponents of a run for this class, battle 1 to 3. */
    public static List<HeroClass> opponents(HeroClass you) {
        return List.of(foes(you));
    }

    /** The opponent's class in battle 0, 1 or 2. */
    public static HeroClass opponent(HeroClass you, int battle) {
        return foes(you)[check(battle)];
    }

    /** The board of battle 0, 1 or 2: "town", "ship" or "fuji". */
    public static String board(int battle) {
        return BOARDS[check(battle)];
    }

    /** The 40 cards the player is given for a run with this class. */
    public List<Card> playerDeck(HeroClass you) {
        return cards(PLAYER_DECKS.get(classOnly(you)));
    }

    /** The 40 cards of the opponent in battle 0, 1 or 2 of a run with this class. */
    public List<Card> enemyDeck(HeroClass you, int battle) {
        List<Card> deck = cards(ENEMY_CORES.get(opponent(you, battle)));
        deck.addAll(cards(BATTLE_CARDS[battle]));
        return deck;
    }

    /** The player of a run, with the class's given deck. */
    public Player hero(String name, HeroClass you) {
        return new Player(name, new Hero(you), new Deck(playerDeck(you)));
    }

    /** The opponent of battle 0, 1 or 2, named after its class (e.g. "Kannushi"), with its deck. */
    public Player enemy(HeroClass you, int battle) {
        HeroClass foe = opponent(you, battle);
        String name = foe.name().charAt(0) + foe.name().substring(1).toLowerCase();
        return new Player(name, new Hero(foe), new Deck(enemyDeck(you, battle)));
    }

    /** "id n, id n, ..." to the cards, each copy the same Card from the catalog. Unknown id: an exception naming it. */
    private List<Card> cards(String list) {
        List<Card> out = new ArrayList<>();
        for (String entry : list.split(",")) {
            String[] parts = entry.trim().split("\\s+");
            Card card = catalog.get(parts[0]);
            if (card == null) throw new IllegalStateException("Adventure deck: no card with id " + parts[0]);
            int copies = Integer.parseInt(parts[1]);
            for (int i = 0; i < copies; i++) out.add(card);
        }
        return out;
    }

    private static HeroClass[] foes(HeroClass you) {
        HeroClass[] foes = FOES.get(classOnly(you));
        return foes;
    }

    private static HeroClass classOnly(HeroClass c) {
        if (c == null || c == HeroClass.NEUTRAL) throw new IllegalArgumentException("Adventure needs a class, not " + c);
        return c;
    }

    private static int check(int battle) {
        if (battle < 0 || battle >= BATTLES) throw new IllegalArgumentException("battle must be 0, 1 or 2, not " + battle);
        return battle;
    }
}