package fr.onishintan.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * A Samurai spell: summons 2/2 Ancestor tokens now, and leaves a lasting bonus
 * on every Ancestor its owner summons later in the match.
 * Its BANZAI effect in CardEffects (if any) is the lasting Banzai of future Ancestors.
 */
public class RiteCard extends SpellCard {
    public static final int ANCESTOR_ATK = 2;   // lowered from 4: the Rites' bonuses make them grow, not their base
    public static final int ANCESTOR_HP = 2;

    private final int ancestors;
    private final Set<Keyword> nowKeywords;
    private final int bonusAttack;
    private final int bonusHealth;
    private final Set<Keyword> bonusKeywords;

    public RiteCard(String id, String nameRomaji, String nameKanji, String nameKana,
                    int kiCost, Rarity rarity, HeroClass heroClass,
                    String effectText, String effectTextJp,
                    int ancestors, Set<Keyword> nowKeywords,
                    int bonusAttack, int bonusHealth, Set<Keyword> bonusKeywords) {
        super(id, nameRomaji, nameKanji, nameKana, kiCost, rarity, heroClass, effectText, effectTextJp);
        if (ancestors < 1 || ancestors > BoardSide.MAX)
            throw new IllegalArgumentException("Ancestors must be 1-" + BoardSide.MAX + ": " + ancestors);
        this.ancestors = ancestors;
        this.nowKeywords = copy(nowKeywords);
        this.bonusAttack = bonusAttack;
        this.bonusHealth = bonusHealth;
        this.bonusKeywords = copy(bonusKeywords);
    }

    private static Set<Keyword> copy(Set<Keyword> s) {
        return (s == null || s.isEmpty()) ? EnumSet.noneOf(Keyword.class) : EnumSet.copyOf(s);
    }

    /** The 2/2 Ancestor token every Rite summons. */
    public static MinionCard ancestorCard() {
        return new MinionCard("ancestor", "Ancestor", "祖霊", "それい", 0, Rarity.COMMON,
                HeroClass.SAMURAI, "", "", ANCESTOR_ATK, ANCESTOR_HP, EnumSet.noneOf(Keyword.class));
    }

    public int getAncestors() { return ancestors; }
    public Set<Keyword> getNowKeywords() { return nowKeywords; }
    public int getBonusAttack() { return bonusAttack; }
    public int getBonusHealth() { return bonusHealth; }
    public Set<Keyword> getBonusKeywords() { return bonusKeywords; }
}