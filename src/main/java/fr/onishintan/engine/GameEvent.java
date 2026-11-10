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
        GAME_OVER                   // target = winning Player, or null for a draw
    }

    public static GameEvent of(Type type) {
        return new GameEvent(type, null, null, 0);
    }

    public static GameEvent of(Type type, Object source, Object target) {
        return new GameEvent(type, source, target, 0);
    }
}