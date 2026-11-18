package fr.onishintan.ui.app;

import fr.onishintan.model.HeroClass;
import fr.onishintan.data.SaveManager;

import java.util.EnumMap;
import java.util.Map;

/** Which hero each class plays with (images/heroes/<id>.png): picked from the home banners, kept while the game runs. */
public final class HeroChoice {
    private static final Map<HeroClass, String> CHOSEN = new EnumMap<>(Map.of(
            HeroClass.SAMURAI, "yasuke",
            HeroClass.NINJA, "hattori-hanzo",
            HeroClass.KANNUSHI, "kada-no-azumamaro",
            HeroClass.ONI, "shuten-doji"));
    private static final SaveManager SAVES = new SaveManager();

    static {
        CHOSEN.putAll(SAVES.loadHeroes());   // the heroes picked in earlier games; a class never picked keeps its default
    }

    private HeroChoice() {
        // Only static state: nobody creates a HeroChoice.
    }

    /** The portrait id this class plays with. */
    public static String portrait(HeroClass heroClass) {
        return CHOSEN.get(heroClass);
    }

    /** From now on, this class plays with this hero. */
    public static void choose(HeroClass heroClass, String portrait) {
        CHOSEN.put(heroClass, portrait);
        SAVES.saveHeroes(CHOSEN);   // kept for the next games
    }
}