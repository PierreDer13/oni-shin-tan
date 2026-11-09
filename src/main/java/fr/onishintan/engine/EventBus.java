package fr.onishintan.engine;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Routes each GameEvent to the rule handlers subscribed to its type (triggers, passives, traps). */
public class EventBus {
    private final Map<GameEvent.Type, List<Consumer<GameEvent>>> handlers =
            new EnumMap<>(GameEvent.Type.class);

    public void subscribe(GameEvent.Type type, Consumer<GameEvent> handler) {
        handlers.computeIfAbsent(type, t -> new ArrayList<>()).add(handler);
    }

    public void unsubscribe(GameEvent.Type type, Consumer<GameEvent> handler) {
        List<Consumer<GameEvent>> list = handlers.get(type);
        if (list != null) list.remove(handler);
    }

    /** Handlers may publish new events themselves (a Seppuku that kills another minion). */
    public void publish(GameEvent event) {
        for (Consumer<GameEvent> h : List.copyOf(handlers.getOrDefault(event.type(), List.of()))) {
            h.accept(event);
        }
    }
}