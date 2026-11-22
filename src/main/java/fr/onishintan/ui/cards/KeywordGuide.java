package fr.onishintan.ui.cards;

import fr.onishintan.model.Card;
import fr.onishintan.model.Keyword;
import fr.onishintan.model.MinionCard;

import java.util.ArrayList;
import java.util.List;

/** The short rule behind each keyword, status and spell family: shown beside big card copies and in the game guide. */
public final class KeywordGuide {
    /** One entry: its name, its kanji (drawn as its logo until a painted one exists) and what it does. */
    public record Entry(String name, String kanji, String text) {}

    /** Every entry, in the guide's order: the 7 keywords, the 4 spell families, Sleep, then the keyboard shortcuts. */
    public static final List<Entry> ALL = List.of(
            new Entry("Banzai", "万歳", "When this minion is played or summoned, its effect happens."),
            new Entry("Seppuku", "切腹", "When this minion dies, its effect happens."),
            new Entry("Passive", "常時", "Its effect works for as long as this minion is on the board."),
            new Entry("Shirodu", "盾", "A wall: enemies must attack it before your other minions and your hero."),
            new Entry("Shinobi", "忍び", "Hidden: it can't be attacked or targeted until it attacks."),
            new Entry("Torii", "鳥居", "Blocks the next damage it would take, then is lost. Several Torii stack."),
            new Entry("Tatakae", "戦え", "Can attack the turn it is played."),
            new Entry("Trap", "罠", "Set face down. It goes off by itself on the enemy's turn when its condition happens."),
            new Entry("Shrine", "社", "Stands in a board slot for a few of your turns; its aura acts on the two minions beside it."),
            new Entry("Ritual", "儀", "Summons 4/4 Ancestors now and makes every later Ancestor stronger."),
            new Entry("Curse", "呪", "Goes into the enemy's hand or deck and hurts them. A Curse in hand is removed by paying its Ki."),
            new Entry("Sleep", "眠り", "Asleep: can't attack on its next turn."),
            new Entry("Shortcuts", "鍵", "In a match: Space ends your turn, D opens the deck tracker, H the play history, Esc the settings, G this guide."));

    private KeywordGuide() {
        // Only static data: nobody creates a KeywordGuide.
    }

    /** The entries that explain this card: its keywords, then its spell family (Trap, Shrine, Ritual, Curse). */
    public static List<Entry> of(Card card) {
        List<Entry> found = new ArrayList<>();
        if (card instanceof MinionCard minion) {
            for (Keyword keyword : minion.getKeywords()) {
                found.add(find(keyword.getDisplayName()));
            }
        }
        CardFamily family = CardFamily.of(card);
        if (family != null) {
            found.add(find(family.getWord()));
        }
        return found;
    }

    /** The entry with this name, whatever its case ("TRAP" finds Trap). */
    private static Entry find(String name) {
        for (Entry entry : ALL) {
            if (entry.name().equalsIgnoreCase(name)) {
                return entry;
            }
        }
        throw new IllegalArgumentException("No guide entry for " + name);
    }
}