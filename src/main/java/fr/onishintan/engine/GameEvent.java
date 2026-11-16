package fr.onishintan.engine;

/**
 * Something that happened in the match. The UI listens and animates it.
 * source = who did it, target = who it happened to, amount = damage, heal, Ki...
 */
public record GameEvent(Type type, Object source, Object target, int amount) {

    public enum Type {
        MATCH_STARTED, TURN_STARTED, TURN_ENDED,
        CARD_DRAWN, CARD_BURNED, CARD_PLAYED, CARD_TRADED, SPELL_CAST,
        MINION_SUMMONED, MINION_DIED,
        ATTACK, DAMAGE, HEAL,
        ARMOUR_GAINED, ARMOUR_BROKEN,
        KEYWORD_LOST,               // Torii cut, Shirodu cut, Shinobi revealed
        WEAPON_EQUIPPED, WEAPON_BROKEN,
        HERO_POWER_USED,
        OATH_SWORN,                 // source = Player, target = Oath
        OATH_STACK,                 // amount = stacks now
        OATH_REWARD,                // amount = 3, 6 or 9
        OATH_BROKEN,                // amount = damage taken by the owner's hero
        OATH_RESTORED,              // Oathbreaker over: the player may re-swear
        HAND_REVEALED,              // source = Player whose hand is shown (Revelation), target = List<Card> copy, amount = hand size
        CARD_DISCARDED,             // source = Player, target = the Card thrown away
        TRAPS_STOLEN,               // source = the Player robbed, target = the thief, amount = Traps taken
        TRAP_SET,                   // source = Player, target = the TrapCard (UI: face down unless source is the local player), amount = traps set now        
        TRAP_TRIGGERED,             // source = trap owner, target = the TrapCard, now revealed       
        CURSE_GIVEN,                // source = cursing Player, target = the CurseCard, amount = copies
        CURSE_TRIGGERED,            // source = cursed Player, target = the CurseCard
        CURSE_REMOVED,              // source = Player who paid the removal, target = the CurseCard
        RITE_PERFORMED,             // source = Player, target = the RiteCard (after its Ancestors are summoned)        
        CLOCK_TIMEOUT,              // source = Player, target = his Hero, amount = damage (ceil(HP / 2))       
        SHRINE_PLACED,              // source = Player, target = the Shrine, amount = slot
        SHRINE_EXPIRED,             // source = Player, target = the Shrine (its slot is free again)       
        GAME_OVER                   // target = winning Player, or null for a draw
    }

    public static GameEvent of(Type type) {
        return new GameEvent(type, null, null, 0);
    }

    public static GameEvent of(Type type, Object source, Object target) {
        return new GameEvent(type, source, target, 0);
    }
}