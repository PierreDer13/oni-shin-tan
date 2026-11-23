package fr.onishintan.engine.event;

/**
 * Something that happened in the match. The UI listens and animates it.
 * source = who did it, target = who it happened to, amount = damage, heal, Ki...
 */
public record GameEvent(Type type, Object source, Object target, int amount) {

    public enum Type {
        MATCH_STARTED, TURN_STARTED, TURN_ENDED,
        MULLIGAN_DONE,              // source = Player, target = List<Card> of the new cards, amount = cards replaced
        CARD_DRAWN, CARD_BURNED, CARD_PLAYED, CARD_TRADED, SPELL_CAST,
        CARD_ADDED,                 // source = Player, target = the Card an effect put in his hand (Koi, Kiji, Noppera-bo, Oath cards)
        MINION_SUMMONED, MINION_DIED,
        MINION_REMOVED,             // source = owner, target = the Minion, gone without dying (Kamikakushi: back into the deck)
        ATTACK, DAMAGE, HEAL,
        ARMOUR_GAINED, ARMOUR_BROKEN,
        KEYWORD_LOST,               // Torii cut, Shirodu cut, Shinobi revealed
        SILENCED,                   // source = owner Player of the minion, target = the Minion silenced by Shike (UI: grey it out)
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
        FATIGUE,                    // source = Player who drew from an empty deck, target = his Hero, amount = damage (1, 2, 3...), then DAMAGE
        EFFECT_TRIGGERED,           // source = the Minion, Shrine, Oath or held CurseCard whose effect goes off now, target = its owner Player;
                                    // published just BEFORE the effect acts (Seppuku, start / end of turn, passives, Shrines, Oath rewards, Gaki)
        CLOCK_TIMEOUT,              // source = Player, target = his Hero, amount = damage (ceil(HP / 2))       
        SHRINE_PLACED,              // source = Player, target = the Shrine, amount = slot
        SHRINE_EXPIRED,             // source = Player, target = the Shrine (its slot is free again)       
        DICE_ROLLED,                // source = Player who rolled (Kunisada Chuji), target = the Minion, amount = the roll (1 to 6)
        CARD_STOLEN,                // source = the thief Player, target = the Card taken from the enemy hand (Izanagi)
        GODAI_GAINED,               // source = Player whose hero power is now Godai (Takeda Shingen)
        CLOCK_CUT,                  // source = Player whose chess clock lost time (Akechi Mitsuhide), amount = the seconds taken
        GAME_OVER                   // target = winning Player, or null for a draw
    }

    public static GameEvent of(Type type) {
        return new GameEvent(type, null, null, 0);
    }

    public static GameEvent of(Type type, Object source, Object target) {
        return new GameEvent(type, source, target, 0);
    }
}