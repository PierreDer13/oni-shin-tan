package fr.onishintan.ui;

import fr.onishintan.model.Card;
import fr.onishintan.model.HeroClass;
import fr.onishintan.model.Keyword;
import fr.onishintan.model.MinionCard;
import fr.onishintan.model.Rarity;
import java.util.List;
import java.util.Set;
import fr.onishintan.model.Deck;
import fr.onishintan.model.Hero;
import fr.onishintan.model.Minion;
import fr.onishintan.model.Player;

/** Hand-made cards to test the views until cards.json is loaded. Japanese texts are placeholders. */
public final class MockCards {
    public static final List<Card> ALL = List.of(
        new MinionCard("saru", "Saru", "猿", "さる", 1, Rarity.COMMON, HeroClass.NEUTRAL, "", "", 4, 4, Set.of()),
        new MinionCard("genin", "Genin", "下忍", "げにん", 1, Rarity.COMMON, HeroClass.NINJA, "Shinobi", "忍び", 3, 2, Set.of(Keyword.SHINOBI)),
        new MinionCard("kappa", "Kappa", "河童", "かっぱ", 3, Rarity.COMMON, HeroClass.ONI,
            "Banzai: deal 4 damage to the enemy hero", "万歳：敵の英雄に四の傷を与える", 5, 7, Set.of(Keyword.BANZAI)),
        new MinionCard("gravedigger", "Gravedigger", "墓守", "はかもり", 3, Rarity.COMMON, HeroClass.ONI,
            "Passive: when a friendly minion dies, +2 ATK", "常時：味方の手下が死ぬと攻撃＋２", 4, 6, Set.of(Keyword.PASSIVE)),
        new MinionCard("fuma-kotaro", "Fūma Kotarō", "風魔小太郎", "ふうまこたろう", 7, Rarity.LEGENDARY, HeroClass.NINJA,
            "Shinobi. Yogake: each enemy minion attacks another random enemy minion", "忍び。夜駆け：敵の手下が互いに襲い合う", 9, 6, Set.of(Keyword.SHINOBI))
    );

    private MockCards() {}

        /** A test player whose board holds n mock minions (cycling through ALL). */
    public static Player player(String name, HeroClass heroClass, int minions) {
        Player p = new Player(name, new Hero(heroClass), new Deck(ALL));
        for (int i = 0; i < minions; i++) p.getSide().place(new Minion((MinionCard) ALL.get(i % ALL.size())), i);
        return p;
    }
}