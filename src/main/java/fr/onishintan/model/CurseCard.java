package fr.onishintan.model;

import java.util.Objects;

/** An Oni spell given to the enemy: into his hand (removable for Ki) or shuffled into his deck. */
public class CurseCard extends SpellCard {
    private final CurseZone zone;
    private final int copies;        // DECK: copies shuffled in; HAND: always 1
    private final int removalCost;   // HAND: Ki the holder pays to get rid of it; DECK: 0

    public CurseCard(String id, String nameRomaji, String nameKanji, String nameKana,
                     int kiCost, Rarity rarity, HeroClass heroClass,
                     String effectText, String effectTextJp,
                     CurseZone zone, int copies, int removalCost) {
        super(id, nameRomaji, nameKanji, nameKana, kiCost, rarity, heroClass, effectText, effectTextJp);
        this.zone = Objects.requireNonNull(zone);
        if (copies < 1 || (zone == CurseZone.HAND && copies != 1))
            throw new IllegalArgumentException("Bad copies for a " + zone + " Curse: " + copies);
        if (removalCost < 0 || removalCost > MAX_KI)
            throw new IllegalArgumentException("Removal cost must be 0-" + MAX_KI + ": " + removalCost);
        this.copies = copies;
        this.removalCost = removalCost;
    }

    public CurseZone getZone() { return zone; }
    public int getCopies() { return copies; }
    public int getRemovalCost() { return removalCost; }
}