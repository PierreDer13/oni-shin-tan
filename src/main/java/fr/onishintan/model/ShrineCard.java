package fr.onishintan.model;

/** A Kannushi spell that stays on the board for a few turns and takes a slot. */
public class ShrineCard extends SpellCard {
    private final int duration;    // owner's turns it stays

    public ShrineCard(String id, String nameRomaji, String nameKanji, String nameKana,
                      int kiCost, Rarity rarity, HeroClass heroClass,
                      String effectText, String effectTextJp, int duration) {
        super(id, nameRomaji, nameKanji, nameKana, kiCost, rarity, heroClass, effectText, effectTextJp);
        if (duration < 1) throw new IllegalArgumentException("Shrine duration must be >= 1: " + duration);
        this.duration = duration;
    }

    public int getDuration() { return duration; }
}