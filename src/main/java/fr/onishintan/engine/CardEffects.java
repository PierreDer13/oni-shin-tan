package fr.onishintan.engine;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Card id -> its effects by trigger. Filled by CardFactory (data-card-repository), by hand in tests. */
public class CardEffects {
    private final Map<String, Map<Trigger, Effect>> byCard = new HashMap<>();

    public CardEffects register(String cardId, Trigger trigger, Effect effect) {
        byCard.computeIfAbsent(cardId, id -> new EnumMap<>(Trigger.class)).put(trigger, effect);
        return this;
    }

    public Optional<Effect> get(String cardId, Trigger trigger) {
        Map<Trigger, Effect> effects = byCard.get(cardId);
        return effects == null ? Optional.empty() : Optional.ofNullable(effects.get(trigger));
    }
}