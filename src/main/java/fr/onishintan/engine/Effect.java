package fr.onishintan.engine;

/** What a card does. Effects can be chained with andThen. */
@FunctionalInterface
public interface Effect {
    void apply(EffectContext ctx);

    default Effect andThen(Effect next) {
        return ctx -> { apply(ctx); next.apply(ctx); };
    }
}