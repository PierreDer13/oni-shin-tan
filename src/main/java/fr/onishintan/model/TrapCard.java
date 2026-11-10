package fr.onishintan.model;

import java.util.Objects;

/** A spell set face down. Hidden from the enemy until its trigger happens. One copy set at a time. */
public class TrapCard extends SpellCard {
    private final TrapTrigger trigger;

    public TrapCard(String id, String nameRomaji, String nameKanji, String nameKana,
                    int kiCost, Rarity rarity, HeroClass heroClass,
                    String effectText, String effectTextJp, TrapTrigger trigger) {
        super(id, nameRomaji, nameKanji, nameKana, kiCost, rarity, heroClass, effectText, effectTextJp);
        this.trigger = Objects.requireNonNull(trigger, "trigger");
    }

    public TrapTrigger getTrigger() { return trigger; }
}