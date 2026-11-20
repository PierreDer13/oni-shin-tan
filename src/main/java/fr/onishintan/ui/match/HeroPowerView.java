package fr.onishintan.ui.match;
import fr.onishintan.ui.fx.Anim;

import fr.onishintan.model.HeroClass;
import fr.onishintan.engine.GameEngine;

import java.util.Map;
import java.util.function.IntConsumer;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** A hero power panel: Ki cost in the circle, English name, Japanese name and effect below.
 *  Usable: yellow outline, hover previews the Ki and grows a little, press uses it. Used: dimmed for the turn. */
public class HeroPowerView extends StackPane {
    public static final double SIZE = 120;
    public static final int COST = GameEngine.HERO_POWER_COST;   // Pierre's engine decides the price

    /** Hero powers until the engine has them: the GDD names. */
    private static final Map<HeroClass, String[]> POWER = Map.of(
            HeroClass.NINJA, new String[] { "Shuriken", "手裏剣", "Deal 6 damage to any character" },
            HeroClass.SAMURAI, new String[] { "Katana", "刀", "Equip a weapon: more Ki, stronger blade" },
            HeroClass.KANNUSHI, new String[] { "Miko", "巫女", "Summon a 3/3 Miko" },
            HeroClass.ONI, new String[] { "Jikininki", "食人鬼", "Drain 4 HP to heal your hero" },
            HeroClass.OROCHI, new String[] { "Yatsu-kubi", "八つ首", "8 bites of 1 damage at random" });

    public HeroPowerView(HeroClass hero, String effect, boolean used, boolean usable, IntConsumer previewCost, Runnable onUse) {
        double k = SIZE / 160;
        ImageView frame = BoardView.image("/images/hud/hero-power.png");
        frame.setFitWidth(SIZE);
        frame.setPreserveRatio(true);
        Label cost = new Label(costText(hero));
        cost.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: " + 26 * k + "; -fx-font-weight: bold; -fx-text-fill: #1f1b17;");
        cost.setTranslateY(-SIZE * 0.28);
        String[] info = POWER.get(hero);   // English name, Japanese name, effect
        String effectText = info[2];
        if (effect != null) {
            effectText = effect;   // a power whose strength changes (Yatsu-kubi)
        }
        VBox text = new VBox(1, text(info[0], 15 * k), text(info[1], 12 * k), text(effectText, 9.5 * k));
        text.setAlignment(Pos.CENTER);
        text.setMaxWidth(SIZE * 0.7);
        text.setTranslateY(SIZE * 0.12);
        getChildren().addAll(frame, cost, text);
        if (used) setEffect(new ColorAdjust(0, -0.3, -0.5, 0));
        if (!usable) return;
        setEffect(BoardView.border(8));   // usable: yellow outline like a minion that can attack
        setOnMouseEntered(e -> {
            previewCost.accept(COST);
            setEffect(BoardView.border(14));
            Anim.to(this, Anim.ease(scaleXProperty(), 1.04), Anim.ease(scaleYProperty(), 1.04));
        });
        setOnMouseExited(e -> {
            previewCost.accept(0);
            setEffect(BoardView.border(8));
            Anim.to(this, Anim.ease(scaleXProperty(), 1.0), Anim.ease(scaleYProperty(), 1.0));
        });
        setOnMousePressed(e -> onUse.run());
    }

    /** Dark ink text for the panel, centred and wrapping. */
    private static Label text(String s, double size) {
        Label l = new Label(s);
        l.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: " + size + "; -fx-font-weight: bold; -fx-text-fill: #1f1b17; -fx-text-alignment: center;");
        l.setWrapText(true);
        return l;
    }

    /** The Ki shown in the circle: the Samurai's blades cost 1 to 9, so it shows "1+"; the others pay COST. */
    private static String costText(HeroClass hero) {
        if (hero == HeroClass.SAMURAI) {
            return "1+";
        }
        return String.valueOf(COST);
    }
}
