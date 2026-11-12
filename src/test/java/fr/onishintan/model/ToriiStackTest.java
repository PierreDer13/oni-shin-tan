package fr.onishintan.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

/** Torii stacks: each one blocks a whole hit. */
class ToriiStackTest {

    private static Minion minion(Keyword... kws) {
        EnumSet<Keyword> set = EnumSet.noneOf(Keyword.class);
        for (Keyword k : kws) set.add(k);
        return new Minion(new MinionCard("gate", "m", "試", "てすと", 1, Rarity.COMMON,
                HeroClass.NEUTRAL, "", "", 1, 5, set));
    }

    @Test
    void baseToriiIsOneStack() {
        Minion m = minion(Keyword.TORII);
        assertEquals(1, m.getToriiStacks());
        assertEquals(0, m.takeDamage(10));
        assertFalse(m.hasTorii());
        assertEquals(2, m.takeDamage(2));
    }

    @Test
    void givenToriiStacksOnTopOfBaseTorii() {
        Minion m = minion(Keyword.TORII);
        m.giveKeyword(Keyword.TORII);
        assertEquals(2, m.getToriiStacks());
        m.takeDamage(10);
        m.takeDamage(10);
        assertEquals(5, m.getHealth());
        assertFalse(m.hasTorii());
    }

    @Test
    void toriiCanBeStackedOnAMinionWithoutIt() {
        Minion m = minion();
        m.giveKeyword(Keyword.TORII);
        m.giveKeyword(Keyword.TORII);
        m.giveKeyword(Keyword.TORII);
        assertEquals(3, m.getToriiStacks());
    }
}