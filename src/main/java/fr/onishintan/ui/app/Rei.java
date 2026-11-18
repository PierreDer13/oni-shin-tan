package fr.onishintan.ui.app;

import java.util.List;

/**
 * The Rei ranking (earned only in PvP): the 7 stones, and how many points a win or a loss moves.
 * The lower your Rei, the bigger the swing; it shrinks exponentially as you near MAX. A loss costs half of a win.
 */
public final class Rei {
    /** The most Rei a player can have. */
    public static final int MAX = 6000;
    /** Points for a win at 0 Rei and at MAX Rei: in between, the gain falls exponentially. */
    private static final double GAIN_AT_ZERO = 250, GAIN_AT_MAX = 10;

    /** One stone: its picture (images/rei-stones/<id>.png), its names, and the first Rei that reaches it. */
    public record Stone(String id, String name, String kanji, int from) {}

    /** The stones from the lowest to the highest. */
    public static final List<Stone> STONES = List.of(
            new Stone("ishikage", "Ishikage", "石影", 0),
            new Stone("akagane", "Akagane", "赤金", 1000),
            new Stone("hisui", "Hisui", "翡翠", 2000),
            new Stone("kogane", "Kogane", "黄金", 3000),
            new Stone("kurogane", "Kurogane", "黒金", 4000),
            new Stone("ryuseki", "Ryūseki", "龍石", 5000),
            new Stone("tennyu", "Ten'nyū", "天玉", 5500));

    private Rei() {
        // Only static rules: nobody creates a Rei.
    }

    /** Points a PvP win gives at this Rei: 250 at 0, falling exponentially to 10 at MAX. */
    public static int gain(int rei) {
        return (int) Math.round(GAIN_AT_ZERO * Math.pow(GAIN_AT_MAX / GAIN_AT_ZERO, (double) rei / MAX));
    }

    /** Points a PvP loss costs at this Rei: half of a win. */
    public static int loss(int rei) {
        return (int) Math.round(gain(rei) / 2.0);
    }

    /** Rei after a PvP win (never above MAX). */
    public static int afterWin(int rei) {
        return Math.min(MAX, rei + gain(rei));
    }

    /** Rei after a PvP loss (never below 0). */
    public static int afterLoss(int rei) {
        return Math.max(0, rei - loss(rei));
    }

    /** The stone a player with this Rei holds: the last one whose `from` they have reached. */
    public static Stone stoneOf(int rei) {
        Stone held = STONES.get(0);
        for (Stone stone : STONES) {
            if (rei >= stone.from()) {
                held = stone;
            }
        }
        return held;
    }
}