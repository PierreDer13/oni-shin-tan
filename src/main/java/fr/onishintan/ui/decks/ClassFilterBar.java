package fr.onishintan.ui.decks;

import fr.onishintan.model.HeroClass;
import fr.onishintan.ui.app.Images;
import fr.onishintan.ui.screens.HomeView;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

/**
 * The Inventory's class row: All, Samurai, Ninja, Kannushi, Oni, Neutral.
 * It writes the class into the CardFilter, then calls onChange (same look as FilterBar).
 */
public class ClassFilterBar extends HBox {
    private static final HeroClass[] CLASSES = { HeroClass.SAMURAI, HeroClass.NINJA, HeroClass.KANNUSHI, HeroClass.ONI, HeroClass.NEUTRAL };

    private final List<ImageView> buttons = new ArrayList<>();

    public ClassFilterBar(CardFilter filter, Runnable onChange) {
        super(8);
        setAlignment(Pos.CENTER_LEFT);
        getChildren().add(DecksView.text("Class", 18, DecksView.PAPER));
        addButton(null, filter, onChange);
        for (HeroClass heroClass : CLASSES) {
            addButton(heroClass, filter, onChange);
        }
        FilterBar.pickPanel(buttons, buttons.get(0));   // at the start every class is shown
    }

        /** One class panel (images/deck-builder/<class>-panel.png; null = every class, the All panel): picks it, filters, then calls onChange. */
    private void addButton(HeroClass heroClass, CardFilter filter, Runnable onChange) {
        String name = "all";
        if (heroClass != null) {
            name = heroClass.name().toLowerCase();
        }
        ImageView panel = new ImageView(Images.get("/images/deck-builder/" + name + "-panel.png"));
        panel.setFitHeight(FilterBar.PANEL_H);
        panel.setPreserveRatio(true);
        HomeView.clickable(panel, false, () -> {
            filter.setHeroClass(heroClass);
            FilterBar.pickPanel(buttons, panel);
            onChange.run();
        });
        buttons.add(panel);
        getChildren().add(panel);
    }
}
