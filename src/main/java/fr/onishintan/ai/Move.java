package fr.onishintan.ai;

import fr.onishintan.model.Card;
import fr.onishintan.model.Combatant;
import fr.onishintan.model.GodaiElement;
import fr.onishintan.model.KatanaTier;

/**
 * One action the AI chose, for the UI to animate then hand back to AIController.play.
 * PLAY: card on slot (minions, Shrines, Rites; ignored otherwise) with its target(s), null when none.
 * ATTACK: attacker on target. POWER: hero power on target (null for the Kannushi Miko). KATANA: the Samurai blade. GODAI: Takeda Shingen's element on target.
 */
public record Move(Kind kind, Card card, int slot, Combatant target, Combatant second, Combatant attacker, KatanaTier tier, GodaiElement element) {

    public enum Kind { PLAY, ATTACK, POWER, KATANA, GODAI }

    public static Move play(Card card, int slot, Combatant target, Combatant second) {
        return new Move(Kind.PLAY, card, slot, target, second, null, null, null);
    }

    public static Move attack(Combatant attacker, Combatant target) {
        return new Move(Kind.ATTACK, null, -1, target, null, attacker, null, null);
    }

    public static Move power(Combatant target) {
        return new Move(Kind.POWER, null, -1, target, null, null, null, null);
    }

    public static Move katana(KatanaTier tier) {
        return new Move(Kind.KATANA, null, -1, null, null, null, tier, null);
    }

    public static Move godai(GodaiElement element, Combatant target) {
        return new Move(Kind.GODAI, null, -1, target, null, null, null, element);
    }
}
