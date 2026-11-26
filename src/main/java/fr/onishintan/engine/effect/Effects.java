package fr.onishintan.engine.effect;

/** Ready-made effects. Card effects are built from these (GDD balance model). */
public final class Effects {
    private Effects() {}

    /** Deal n damage to the chosen target (Banzai with a target). */
    public static Effect damageTarget(int n) {
        return ctx -> ctx.engine().dealDamage(ctx.source(), ctx.target(), n);
    }

    /** Deal n damage to the enemy hero (Ashigaru's Seppuku). */
    public static Effect damageEnemyHero(int n) {
        return ctx -> ctx.engine().dealDamage(ctx.source(), ctx.enemy().getHero(), n);
    }

    /** Restore n HP to your own hero. */
    public static Effect healOwnHero(int n) {
        return ctx -> ctx.engine().heal(ctx.owner().getHero(), n);
    }

    /** Draw n cards. */
    public static Effect draw(int n) {
        return ctx -> { for (int i = 0; i < n; i++) ctx.engine().draw(ctx.owner()); };
    }

    /** Gain n armour (Gusoku, Hatamoto). */
    public static Effect gainArmour(int n) {
        return ctx -> ctx.engine().gainArmour(ctx.owner(), n);
    }

    /** The source minion gets +atk / +hp. */
    public static Effect buffSelf(int atk, int hp) {
        return ctx -> { if (ctx.source() != null) ctx.source().buff(atk, hp); };
    }
}