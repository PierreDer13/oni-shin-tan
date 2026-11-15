package fr.onishintan.ui.decks;

import fr.onishintan.model.HeroClass;
import fr.onishintan.ui.screens.HomeView;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/**
 * The Inventory's class row: All, Samurai, Ninja, Kannushi, Oni, Neutral.
 * It writes the class into the CardFilter, then calls onChange (same look as FilterBar).
 */
public class ClassFilterBar extends HBox {
    private static final HeroClass[] CLASSES = { HeroClass.SAMURAI, HeroClass.NINJA, HeroClass.KANNUSHI, HeroClass.ONI, HeroClass.NEUTRAL };

    private final List<Label> buttons = new ArrayList<>();

    public ClassFilterBar(CardFilter filter, Runnable onChange) {
        super(8);
        setAlignment(Pos.CENTER_LEFT);
        getChildren().add(DecksView.text("Class", 18, DecksView.INK));
        addButton("All", null, filter, onChange);
        for (HeroClass heroClass : CLASSES) {
            addButton(heroClass.getDisplayName(), heroClass, filter, onChange);
        }
        FilterBar.pick(buttons, buttons.get(0));   // at the start every class is shown
    }

    /** One class button (null = every class): picks it, filters, then calls onChange. */
    private void addButton(String words, HeroClass heroClass, CardFilter filter, Runnable onChange) {
        Label button = new Label(words);
        HomeView.clickable(button, false, () -> {
            filter.setHeroClass(heroClass);
            FilterBar.pick(buttons, button);
            onChange.run();
        });
        buttons.add(button);
        getChildren().add(button);
    }
}
