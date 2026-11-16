package fr.onishintan.ui;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Test cards until cards.json is loaded: the first set's minions (stats, rarity and text from the Card Curve artifact,
 *  keywords read from the text), the Ninja Traps and the class spells. ALL holds every one of them; weapons are not here yet. */
public final class MockCards {
    /** id | name | kanji | class | Ki | ATK | HP | rarity (C, L = legendary, M = mythic) | text */
    private static final String CURVE = """
            unsui|Unsui|雲水|NEUTRAL|1|2|3|C|Banzai: heal 6 to a friendly character
            jushoku|Jūshoku|住職|KANNUSHI|4|5|7|C|Passive: end of turn, heal your hero 6
            shike|Shike|師家|NEUTRAL|7|10|12|C|Banzai: remove all effects from an enemy minion
            inasaku-noka|Inasaku nōka|稲作農家|NEUTRAL|1|2|2|C|Seppuku: heal your hero 8
            kyabetsu-noka|Kyabetsu nōka|キャベツ農家|NEUTRAL|2|5|6|C|
            ikareru-hyakusho|Ikareru hyakushō|怒れる百姓|NEUTRAL|2|5|2|C|Banzai: deal 4 damage to a minion
            katana-kaji|Katana kaji|刀鍛冶|SAMURAI|3|5|6|C|Banzai: your weapon gets +3 ATK
            togeika|Tōgeika|陶芸家|NEUTRAL|2|2|7|C|Shirodu
            sendaiku|Sendaiku|船大工|NEUTRAL|4|4|6|C|Seppuku: summon a Sakana uri
            sakana-uri|Sakana uri|魚売り|NEUTRAL|3|5|5|C|Banzai: add a Koi to your hand
            monogoi|Monogoi|物乞い|NEUTRAL|1|2|3|C|Banzai: if your hand is empty, draw 2
            hakahori|Hakahori|墓掘り|ONI|3|4|6|C|Passive: when a friendly minion dies, +2 ATK
            ronin|Ronin|浪人|SAMURAI|5|7|7|C|Passive: while it is your only minion, +6/+6
            goze|Goze|瞽女|KANNUSHI|3|3|6|C|Passive: end of turn, heal all friendly minions 3
            komuso|Komusō|虚無僧|NINJA|2|2|3|C|Shinobi. Passive: end of turn, heal your hero 4
            taiko-uchi|Taiko-uchi|太鼓打ち|NEUTRAL|4|6|6|C|Banzai: other friendly minions +2 ATK
            miko|Miko|巫女|KANNUSHI|1|3|3|C|
            yamabushi|Yamabushi|山伏|KANNUSHI|4|5|6|C|Banzai: heal 12 to any character
            onmyoji|Onmyōji|陰陽師|KANNUSHI|5|8|8|C|Banzai: give a friendly minion Torii
            maiko|Maiko|舞妓|NEUTRAL|2|3|5|C|Banzai: an enemy minion gets −3 ATK
            ueta-rikishi|Ueta rikishi|飢えた力士|SAMURAI|3|4|8|C|Shirodu
            yokozuna|Yokozuna|横綱|NEUTRAL|8|11|16|L|Shirodu. Torii
            ashigaru|Ashigaru|足軽|SAMURAI|2|4|3|C|Tatakae
            hatamoto|Hatamoto|旗本|SAMURAI|4|5|7|C|Shirodu. Passive: end of turn, your hero gains 3 armour
            daimyo|Daimyō|大名|SAMURAI|7|9|9|C|Banzai: equip your hero with a Katana (8/1)
            genin|Genin|下忍|NINJA|1|3|2|C|Shinobi
            jonin|Jōnin|上忍|NINJA|6|8|7|C|Shinobi. Passive: your other Shinobi minions have +3 ATK
            onnagata|Onnagata|女形|NINJA|3|2|6|C|Shinobi. Banzai: set its ATK to an enemy minion's ATK
            yumihiki|Yumihiki|弓引き|NEUTRAL|2|4|3|C|Banzai: deal 4 damage
            jujutsu-shihan|Jujutsu shihan|師範|SAMURAI|6|6|7|C|Seppuku: give your hero Honjō Masamune
            fuma-kotaro|Fūma Kotarō|風魔小太郎|NINJA|7|9|6|L|Shinobi. Yogake: when it reveals itself by attacking, each enemy minion attacks another random enemy minion
            shogun|Shōgun|将軍|SAMURAI|9|12|12|L|Banzai: all friendly minions +4/+4
            tenno|Tennō|天皇|NEUTRAL|10|15|16|L|Passive: your other minions cost 1 less Ki
            kappa|Kappa|河童|ONI|3|5|7|C|Banzai: deal 4 damage to the enemy hero
            tengu|Tengu|天狗|NINJA|4|8|6|C|Shinobi
            noppera-bo|Noppera-bō|のっぺら坊|NINJA|2|4|1|C|Shinobi. Seppuku: return to your hand
            rokurokubi|Rokurokubi|ろくろ首|ONI|3|6|5|C|Can attack a minion behind Shirodu or Torii
            jorogumo|Jorōgumo|絡新婦|ONI|5|6|10|C|Banzai: put an enemy minion to sleep for 1 turn
            oiwa|Oiwa|お岩|ONI|6|9|9|C|Seppuku: shuffle 2 random Curses into the enemy deck
            nue|Nue|鵺|ONI|7|9|9|C|Banzai: deal 6 damage to all other minions
            shinigami|Shinigami|死神|ONI|8|10|11|C|Banzai: destroy a damaged enemy minion
            enma|Enma|閻魔|ONI|10|13|14|L|Banzai: destroy all other minions with 8 ATK or less
            kitsune-no-bakashiya|Kitsune no bakashiya|狐の化かし屋|NINJA|3|3|2|C|Shinobi. Banzai: transform into a copy of a minion
            kitsune-no-tsukai|Kitsune no tsukai|狐の使い|KANNUSHI|3|5|5|C|Torii
            kodama|Kodama|木霊|KANNUSHI|1|1|5|C|Shirodu
            raijin|Raijin|雷神|NEUTRAL|7|8|8|C|Banzai: deal 12 damage split among enemies
            fujin|Fūjin|風神|NEUTRAL|7|10|10|C|Banzai: return an enemy minion to its owner's hand
            ryu|Ryū|龍|NEUTRAL|9|13|13|L|Torii. Passive: +4 to all your damage, healing and armour effects
            inari-okami|Inari Ōkami|稲荷大神|KANNUSHI|8|10|11|L|Passive: end of turn, summon a Kitsune no tsukai
            amaterasu-omikami|Amaterasu Ōmikami|天照大神|NEUTRAL|10|17|17|M|Banzai: restore your hero to full HP
            tsukuyomi|Tsukuyomi|月読|NINJA|8|12|10|L|Banzai: your other minions have Shinobi. Shinobi
            takemikazuchi|Takemikazuchi|建御雷|SAMURAI|8|10|9|L|Tatakae. Banzai: equip your hero with Futsu-no-mitama (10/2)
            okuninushi|Ōkuninushi|大国主|KANNUSHI|8|11|12|L|Banzai: heal all friendly characters for 10
            susanoo|Susanoo|須佐之男|KANNUSHI|7|11|9|L|Tatakae. Banzai: deal 6 damage to an enemy minion
            izanami|Izanami|伊邪那美|ONI|9|11|11|L|Banzai: return your 2 highest-cost dead minions to the board
            saru|Saru|猿|NEUTRAL|1|4|4|C|
            koi|Koi|鯉|NEUTRAL|1|2|2|C|Seppuku: draw a card
            kiji|Kiji|雉|NEUTRAL|1|3|3|C|Banzai: copy a card from your opponent's hand
            kame|Kame|亀|NEUTRAL|2|1|8|C|Shirodu
            tsuru|Tsuru|鶴|NEUTRAL|2|4|3|C|Torii
            tanuki|Tanuki|狸|NEUTRAL|2|5|3|C|Shinobi
            tora|Tora|虎|NEUTRAL|5|10|6|C|Tatakae
            """;

    public static final List<Card> MINIONS = CURVE.lines().map(l -> l.split("\\|", -1)).map(f -> (Card) new MinionCard(f[0], f[1], f[2], "",
            Integer.parseInt(f[4]), f[7].equals("L") ? Rarity.LEGENDARY : f[7].equals("M") ? Rarity.MYTHIC : Rarity.COMMON,
            HeroClass.valueOf(f[3]), f[8], "", Integer.parseInt(f[5]), Integer.parseInt(f[6]), keywords(f[8]))).toList();
    
    /** The 6 Ninja traps (GDD, card families), for testing until cards.json is loaded. */
    public static final List<Card> TRAPS = List.of(
            new TrapCard("kawarimi", "Kawarimi", "変わり身", "", 2, Rarity.COMMON, HeroClass.NINJA,
                    "An enemy attacks a minion: summon a 0/9 tree that takes the hit", "", TrapTrigger.ON_ENEMY_ATTACK_MINION),
            new TrapCard("makibishi", "Makibishi", "撒菱", "", 2, Rarity.COMMON, HeroClass.NINJA,
                    "The enemy summons a minion: deal 6 damage to it", "", TrapTrigger.ON_ENEMY_SUMMON),
            new TrapCard("oyasumi", "Oyasumi", "お休み", "", 3, Rarity.COMMON, HeroClass.NINJA,
                    "The enemy casts a spell: 2 random enemy minions fall asleep for 1 turn", "", TrapTrigger.ON_ENEMY_SPELL),
            new TrapCard("otoshiana", "Otoshiana", "落とし穴", "", 3, Rarity.COMMON, HeroClass.NINJA,
                    "An enemy minion attacks your hero: cancel the attack, it falls asleep", "", TrapTrigger.ON_ENEMY_ATTACK_HERO),
            new TrapCard("shikomi-andon", "Shikomi andon", "仕込み行灯", "", 4, Rarity.COMMON, HeroClass.NINJA,
                    "An enemy minion attacks a minion: deal 14 damage to the attacker first", "", TrapTrigger.ON_ENEMY_ATTACK_MINION),
            new TrapCard("dondengaeshi", "Dondengaeshi", "どんでん返し", "", 6, Rarity.COMMON, HeroClass.NINJA,
                    "The enemy plays a minion: summon a copy of it on your side", "", TrapTrigger.ON_ENEMY_SUMMON));

    /** The 16 plain class spells (GDD, 12 Nov). The family cards are built in familyCards(); Ninja Traps are in TRAPS.
     *  id | name | kanji | class | Ki | text */
    private static final String SPELL_TABLE = """
            kakuremino|Kakuremino|隠れ蓑|NINJA|5|Give all friendly minions Shinobi
            kemuri-dama|Kemuri-dama|煙玉|NINJA|2|Give any minion Shinobi (with Shirodu too, its Shirodu is ignored)
            kuji-in|Kuji-in|九字印|NINJA|3|Refresh your hero power
            yoi|Yōi|用意|NINJA|4|Cards in your hand cost 1 less Ki
            chiburi|Chiburi|血振り|SAMURAI|2|Give your weapon +1 durability
            iaido|Iaidō|居合道|SAMURAI|4|Destroy your weapon; deal its ATK to all minions
            ketto|Kettō|決闘|SAMURAI|4|Choose any 2 minions to duel: a random one wins and survives on half its HP, the other dies
            yoroi|Yoroi|鎧|SAMURAI|3|Give your hero and a friendly minion 6 armour
            harae|Harae|祓|KANNUSHI|4|Heal a character for 15 and draw a card
            kiseki|Kiseki|奇跡|KANNUSHI|6|Heal all friendly minions; a minion already at full HP gets +3/+3 instead
            senbon-torii|Senbon Torii|千本鳥居|KANNUSHI|6|Give all friendly minions Torii
            kagura|Kagura|神楽|KANNUSHI|8|Summon 3 Miko (3/3) with Tatakae
            kuchiyose|Kuchiyose|口寄せ|ONI|7|Return a random dead friendly minion to the board (its Banzai triggers)
            kamikakushi|Kamikakushi|神隠し|ONI|3|Shuffle a minion back into its owner's deck
            sakasama|Sakasama|逆様|ONI|6|Swap the ATK and HP of all enemy minions; their copies in the deck take the new stats
            tamashii-gui|Tamashii-gui|魂喰い|ONI|5|Destroy a minion and a random card in the opponent's hand
            """;

    /** The spells, families included: they show and can be played, but do nothing until the engine has their effects. */
    public static final List<Card> SPELLS = spells();

    /** Every test card (minions, Traps and spells): what the Inventory and the deck editor show. */
    public static final List<Card> ALL = allCards();

    private MockCards() {}

    /** The keywords a card text starts a sentence with ("Shinobi.", "Banzai: …"); "behind Shirodu" doesn't count. */
    private static Set<Keyword> keywords(String text) {
        return Arrays.stream(Keyword.values()).filter(k -> text.matches("(.*\\. )?" + k.getDisplayName() + "([.:].*)?")).collect(Collectors.toSet());
    }

    /**
     * Builds the spells: the 18 family cards first (familyCards), then one plain spell per SPELL_TABLE line.
     * @return the 18 family cards (Shrines, Curses, Rites) and the 16 plain class spells, all common
     */
    private static List<Card> spells() {
        List<Card> spells = new ArrayList<>(familyCards());
        for (String line : SPELL_TABLE.lines().toList()) {
            String[] field = line.split("\\|", -1);
            String id = field[0];
            String name = field[1];
            String kanji = field[2];
            HeroClass heroClass = HeroClass.valueOf(field[3]);
            int ki = Integer.parseInt(field[4]);
            String text = field[5];
            spells.add(new SpellCard(id, name, kanji, "", ki, Rarity.COMMON, heroClass, text, ""));
        }
        return spells;
    }

    /** The 18 class family cards (GDD, 9 Nov) as the engine's own classes, so the engine runs them. */
    private static List<Card> familyCards() {
        List<Card> family = new ArrayList<>();

        // Kannushi Shrines: id, name, kanji, Ki, turns on the board, text
        family.add(shrine("heiwa-no-torii", "Heiwa no Torii", "平和の鳥居", 1, 2, "Shrine, 2 turns. Adjacent minions: +0/+3; heal 3 at the end of your turn"));
        family.add(shrine("meiji-otorii", "Meiji Ōtorii", "明治大鳥居", 2, 2, "Shrine, 2 turns. Adjacent minions: +3 ATK; heal 3 after attacking"));
        family.add(shrine("itsukushima", "Itsukushima", "厳島", 3, 3, "Shrine, 3 turns. Adjacent minions gain Torii at the end of your turn"));
        family.add(shrine("kumano-hongu-taisha", "Kumano Hongū Taisha", "熊野本宮大社", 4, 3, "Shrine, 3 turns. Adjacent minions: +2/+4 and Shirodu"));
        family.add(shrine("fushimi-inari", "Fushimi Inari", "伏見稲荷", 5, 3, "Shrine, 3 turns. On the opponent's turns, adjacent minions cannot die from a single hit"));
        family.add(shrine("motonosumi-shrine", "Motonosumi Jinja", "元乃隅神社", 7, 2, "Shrine, 2 turns. Adjacent minions, and minions played next to it: +3/+3 and Tatakae"));

        // Oni Curses: id, name, kanji, Ki, where it goes, copies, removal Ki, text
        family.add(curse("bakeneko", "Bakeneko", "化け猫", 2, CurseZone.HAND, 1, 2, "Curse in the enemy hand (removal 2 Ki): while held, their minions cost 1 more Ki"));
        family.add(curse("gagoze", "Gagoze", "元興寺", 2, CurseZone.DECK, 3, 0, "Curse, 3 into the enemy deck: when drawn, 4 damage to a random minion of that player"));
        family.add(curse("hinoeuma", "Hinoeuma", "丙午", 3, CurseZone.HAND, 1, 3, "Curse in the enemy hand (removal 3 Ki): end of their turn, 8 damage to their hero"));
        family.add(curse("tatari", "Tatari", "祟り", 3, CurseZone.DECK, 5, 0, "Curse, 5 into the enemy deck: when drawn, 5 damage to that hero"));
        family.add(curse("gaki", "Gaki", "餓鬼", 4, CurseZone.HAND, 1, 5, "Curse in the enemy hand (removal 5 Ki): each turn, they lose 1 playable Ki"));
        family.add(curse("gokiburi", "Gokiburi", "ゴキブリ", 4, CurseZone.HAND, 1, 3, "Curse in the enemy hand (removal 3 Ki): end of their turn, eats the card to its left"));

        // Samurai Rites: id, name, kanji, Ki, Ancestors now, their keywords, lasting +ATK / +HP, lasting keywords, text
        family.add(rite("rite-of-shokan", "Rite of Shōkan", "召喚の儀", 2, 1, Set.of(), 1, 1, Set.of(),
                "Summon a 4/4 Ancestor. Future Ancestors: +1/+1"));
        family.add(rite("rite-of-gokito", "Rite of Gokitō", "御祈祷の儀", 3, 1, Set.of(), 0, 0, Set.of(Keyword.BANZAI),
                "Summon a 4/4 Ancestor. Future Ancestors: Banzai: draw a card (stacks)"));
        family.add(rite("rite-of-ujigami-mairi", "Rite of Ujigami-mairi", "氏神参りの儀", 4, 1, Set.of(Keyword.TORII), 0, 0, Set.of(Keyword.TORII),
                "Summon a 4/4 Ancestor with Torii. Future Ancestors: +1 Torii (stacks)"));
        family.add(rite("rite-of-daijosai", "Rite of Daijōsai", "大嘗祭", 5, 1, Set.of(Keyword.TATAKAE), 2, 2, Set.of(Keyword.TATAKAE),
                "Summon a 4/4 Ancestor with Tatakae. Future Ancestors: Tatakae and +2/+2"));
        family.add(rite("rite-of-sanpasen", "Rite of Sanpasen", "儀式", 6, 2, Set.of(Keyword.SHIRODU), 0, 0, Set.of(Keyword.SHIRODU),
                "Summon 2 Ancestors with Shirodu. Future Ancestors: Shirodu"));
        family.add(rite("rite-of-mukaebi", "Rite of Mukaebi", "迎え火", 7, 3, Set.of(), 2, 2, Set.of(),
                "Summon 3 Ancestors. Future Ancestors: +2/+2"));
        return family;
    }

    /** A Kannushi Shrine that stays `turns` of its owner's turns on the board. */
    private static ShrineCard shrine(String id, String name, String kanji, int ki, int turns, String text) {
        return new ShrineCard(id, name, kanji, "", ki, Rarity.COMMON, HeroClass.KANNUSHI, text, "", turns);
    }

    /** An Oni Curse: into the enemy hand (1 copy, removable for `removalKi`) or shuffled into the enemy deck (`copies`, removal 0). */
    private static CurseCard curse(String id, String name, String kanji, int ki, CurseZone zone, int copies, int removalKi, String text) {
        return new CurseCard(id, name, kanji, "", ki, Rarity.COMMON, HeroClass.ONI, text, "", zone, copies, removalKi);
    }

    /** A Samurai Rite: `ancestors` 4/4 Ancestors now with `nowKeywords`, and a lasting bonus for every later Ancestor. */
    private static RiteCard rite(String id, String name, String kanji, int ki, int ancestors, Set<Keyword> nowKeywords,
                                 int bonusAttack, int bonusHealth, Set<Keyword> bonusKeywords, String text) {
        return new RiteCard(id, name, kanji, "", ki, Rarity.COMMON, HeroClass.SAMURAI, text, "",
                ancestors, nowKeywords, bonusAttack, bonusHealth, bonusKeywords);
    }

    /**
     * Every test card: the minions, then the Ninja Traps, then the spells.
     * @return one list for the Inventory and the deck editor's collection
     */
    private static List<Card> allCards() {
        List<Card> all = new ArrayList<>(MINIONS);
        all.addAll(TRAPS);
        all.addAll(SPELLS);
        return all;
    }

    /** A test deck: the class's minions, Traps and spells plus the neutral minions. Also the PvP starter decks. */
    public static List<Card> deckCards(HeroClass heroClass) {
        List<Card> deck = new ArrayList<>(MINIONS.stream().filter(c -> c.getHeroClass() == heroClass || c.getHeroClass() == HeroClass.NEUTRAL).toList());
        for (Card trap : TRAPS) {
            if (trap.getHeroClass() == heroClass) {
                deck.add(trap);
            }
        }
        for (Card spell : SPELLS) {
            if (spell.getHeroClass() == heroClass) {
                deck.add(spell);
            }
        }
        return deck;
    }

    /** A test player with deckCards() as the deck; the board holds n of them. */
    public static Player player(String name, HeroClass heroClass, int minions) {
        List<Card> deck = deckCards(heroClass);
        Player p = new Player(name, new Hero(heroClass), new Deck(deck));
        for (int i = 0; i < minions; i++) p.getSide().place(new Minion((MinionCard) deck.get(i % deck.size())), i);
        return p;
    }
}
