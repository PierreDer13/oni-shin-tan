package fr.onishintan.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** A minion card: printed ATK, HP and keywords. */
public class MinionCard extends Card {
    public static final int MAX_BASE_STAT = 20;

    private final int baseAttack;
    private final int baseHealth;
    private final Set<Keyword> keywords;

    public MinionCard(String id, String nameRomaji, String nameKanji, String nameKana,
                      int kiCost, Rarity rarity, HeroClass heroClass,
                      String effectText, String effectTextJp,
                      int baseAttack, int baseHealth, Set<Keyword> keywords) {
        super(id, nameRomaji, nameKanji, nameKana, kiCost, rarity, heroClass, effectText, effectTextJp);
        if (baseAttack < 0 || baseAttack > MAX_BASE_STAT) {
            throw new IllegalArgumentException("Base ATK must be 0-" + MAX_BASE_STAT + ": " + baseAttack);
        }
        if (baseHealth < 1 || baseHealth > MAX_BASE_STAT) {
            throw new IllegalArgumentException("Base HP must be 1-" + MAX_BASE_STAT + ": " + baseHealth);
        }
        this.baseAttack = baseAttack;
        this.baseHealth = baseHealth;
        this.keywords = (keywords == null || keywords.isEmpty())
                ? Collections.unmodifiableSet(EnumSet.noneOf(Keyword.class))
                : Collections.unmodifiableSet(EnumSet.copyOf(keywords));
    }

    public int getBaseAttack() { return baseAttack; }
    public int getBaseHealth() { return baseHealth; }
    public Set<Keyword> getKeywords() { return keywords; }
    public boolean hasKeyword(Keyword k) { return keywords.contains(k); }
}