package fr.onishintan.model;

/** A spell card: no ATK or HP. Its effects are added by the engine. */
public class SpellCard extends Card {
    public SpellCard(String id, String nameRomaji, String nameKanji, String nameKana,
                     int kiCost, Rarity rarity, HeroClass heroClass,
                     String effectText, String effectTextJp) {
        super(id, nameRomaji, nameKanji, nameKana, kiCost, rarity, heroClass, effectText, effectTextJp);
    }
}