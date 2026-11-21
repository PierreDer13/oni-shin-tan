package fr.onishintan.engine.turn;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import fr.onishintan.model.Card;
import fr.onishintan.model.Combatant;
import fr.onishintan.model.Minion;
import fr.onishintan.model.MinionCard;
import fr.onishintan.model.Player;

/**
 * What the active player did this turn, read by the Oath rules at the end of it.
 * Reset at TURN_STARTED, except the Justice marks: they last until the marked player's next turn is over.
 */
public class TurnTracker {
    /** Descension: a minion with this base ATK or more is "big". */
    public static final int BIG_ATTACK = 5;

    private int cardsPlayed;
    private int minionsPlayed;
    private int kiSpent;                  // Ki actually paid: cards (Oath discounts included) and hero power
    private int trapsSet;
    private int bigMinionsPlayed;
    private int heroAttacks;              // friendly minions that hit the enemy hero (Honor)
    private int heroSkipped;              // friendly minions that hit a minion while the hero was a legal target (Honor)
    private int heroAttacksPastMinions;   // hits on the enemy hero while he still had minions (Peace)
    private int damageDealt;              // damage taken by enemy characters (Havoc)
    private int cursesGiven;              // Corruption
    private final Set<Combatant> attacked = Collections.newSetFromMap(new IdentityHashMap<>());
    /** Justice: enemy minions that hit a player's hero, by that player. */
    private final Map<Player, Set<Minion>> heroAttackers = new IdentityHashMap<>();

    public void reset() {
        cardsPlayed = 0;
        minionsPlayed = 0;
        kiSpent = 0;
        trapsSet = 0;
        bigMinionsPlayed = 0;
        heroAttacks = 0;
        heroSkipped = 0;
        heroAttacksPastMinions = 0;
        damageDealt = 0;
        cursesGiven = 0;
        attacked.clear();
    }

    public void cardPlayed(Card card, int paid) {
        cardsPlayed++;
        kiSpent += paid;
        if (card instanceof MinionCard mc) {
            minionsPlayed++;
            if (mc.getBaseAttack() >= BIG_ATTACK) bigMinionsPlayed++;
        }
    }

    /** A trap is a card played too, but announced as TRAP_SET to keep it hidden. */
    public void trapSet(Card card, int paid) {
        cardPlayed(card, paid);
        trapsSet++;
    }

    /** Ki paid outside a card (hero power, katana). */
    public void addKiSpent(int paid) { kiSpent += paid; }

    public void attacked(Combatant target) { attacked.add(target); }

    public void minionHitHero(boolean enemyHadMinions) {
        heroAttacks++;
        if (enemyHadMinions) heroAttacksPastMinions++;
    }

    public void minionSkippedHero() { heroSkipped++; }

    public void addDamage(int amount) { damageDealt += amount; }

    public void curseGiven() { cursesGiven++; }

    public void markHeroAttacker(Player defender, Minion attacker) {
        heroAttackers.computeIfAbsent(defender, k -> Collections.newSetFromMap(new IdentityHashMap<>())).add(attacker);
    }

    public void clearHeroAttackers(Player defender) { heroAttackers.remove(defender); }

    public int getCardsPlayed() { return cardsPlayed; }
    public int getMinionsPlayed() { return minionsPlayed; }
    public int getKiSpent() { return kiSpent; }
    public int getTrapsSet() { return trapsSet; }
    public int getBigMinionsPlayed() { return bigMinionsPlayed; }
    public int getHeroAttacks() { return heroAttacks; }
    public int getHeroSkipped() { return heroSkipped; }
    public int getHeroAttacksPastMinions() { return heroAttacksPastMinions; }
    public int getDamageDealt() { return damageDealt; }
    public int getCursesGiven() { return cursesGiven; }
    public boolean hasAttacked(Combatant c) { return attacked.contains(c); }

    /** Enemy minions that hit this player's hero during the enemy's last turn. */
    public Set<Minion> getHeroAttackers(Player defender) {
        return Collections.unmodifiableSet(heroAttackers.getOrDefault(defender, Set.of()));
    }
}