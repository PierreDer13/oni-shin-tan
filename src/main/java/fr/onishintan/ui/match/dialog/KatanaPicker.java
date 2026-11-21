package fr.onishintan.ui.match.dialog;

import fr.onishintan.model.KatanaTier;
import fr.onishintan.ui.screens.HomeView;

import java.util.function.Consumer;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import fr.onishintan.ui.match.BoardView;

/**
 * The Samurai's hero power (Katana): the weapon cloth appears rolled up, unrolls, and shows one blade per strap,
 * from the wooden Bokken (1 Ki) on the bottom strap to Honjō Masamune (9 Ki) on the top one.
 * Blades you can't afford are dimmed. A click outside the cloth closes it without using the power.
 */
public final class KatanaPicker extends Pane {
    private static final String OPEN = "/images/hud/katana-picker.png";
    private static final String ROLLED = "/images/hud/katana-picker-rolled-up.png";
    /** The open cloth picture's size (887 x 1774) and the height it is drawn at on the table. */
    private static final double OPEN_W = 887, OPEN_H = 1774, SHOWN_H = 620;
    /** The rolled-up picture's size (2172 x 724) and its width in the open picture's pixels. */
    private static final double ROLLED_W = 2172, ROLLED_H = 724, ROLLED_SHOWN_W = 700;
    /** Measured on the open picture (its own pixels): the 5 straps' heights from top to bottom, the middle of the cloth,
     *  a blade's width, and the bottom of the top roll (all that shows before it unrolls). */
    private static final double[] STRAP_Y = { 395, 667, 951, 1232, 1512 };
    private static final double CENTRE_X = 443, BLADE_W = 400, ROLL_BOTTOM = 175;
    /** The two leather loops of each strap (open picture's pixels): drawn again over the blade so it looks held by them. */
    private static final double LOOP_LEFT_X = 262, LOOP_RIGHT_X = 576, LOOP_W = 46, LOOP_H = 104;
    /** How long the cloth takes to unroll. */
    private static final Duration UNROLL = Duration.millis(650);

    /** kiAvailable = your Ki now; onPick gets the chosen blade; onCancel runs when you click outside. */
    public KatanaPicker(int kiAvailable, Consumer<KatanaTier> onPick, Runnable onCancel) {
        setPrefSize(BoardView.W, BoardView.H);

        // A light veil over the whole table: a click on it (outside the cloth) cancels.
        Rectangle outside = new Rectangle(BoardView.W, BoardView.H, Color.rgb(0, 0, 0, 0.35));
        outside.setOnMousePressed(event -> {
            close();
            onCancel.run();
        });
        getChildren().add(outside);

        // The cloth in the middle of the table, then the rolled-up cloth over its top roll.
        double k = SHOWN_H / OPEN_H;   // open picture pixels -> table pixels
        double x = (BoardView.W - OPEN_W * k) / 2;
        double y = (BoardView.H - SHOWN_H) / 2;

        Pane cloth = cloth(kiAvailable, onPick, k);
        cloth.relocate(x, y);
        getChildren().add(cloth);

        ImageView rolled = BoardView.image(ROLLED);
        double rolledW = ROLLED_SHOWN_W * k;
        double rolledH = rolledW * ROLLED_H / ROLLED_W;
        rolled.setFitWidth(rolledW);
        rolled.setFitHeight(rolledH);
        rolled.setMouseTransparent(true);
        rolled.relocate(x + (OPEN_W * k - rolledW) / 2, y + ROLL_BOTTOM * k / 2 - rolledH / 2);
        getChildren().add(rolled);

        unroll(cloth, rolled, k);
    }

    /** The open cloth: its picture, then one blade per strap, the cheapest on the bottom strap and the dearest on the top one. */
    private Pane cloth(int kiAvailable, Consumer<KatanaTier> onPick, double k) {
        Pane cloth = new Pane();
        Image picture = BoardView.image(OPEN).getImage();
        ImageView back = new ImageView(picture);
        back.setFitWidth(OPEN_W * k);
        back.setFitHeight(OPEN_H * k);
        cloth.getChildren().add(back);

        KatanaTier[] tiers = KatanaTier.values();   // cheapest first
        for (int i = 0; i < tiers.length && i < STRAP_Y.length; i++) {
            double strapY = STRAP_Y[STRAP_Y.length - 1 - i];   // tier 0 (Bokken) on the last strap, at the bottom
            boolean affordable = kiAvailable >= tiers[i].getKiCost();
            cloth.getChildren().add(blade(tiers[i], strapY, affordable, onPick, picture, k));
        }
        return cloth;
    }

    /** One blade across its strap, held by the strap's two loops, with its name, Ki and ATK / durability under it. Clickable only if you can afford it. */
    private Group blade(KatanaTier tier, double strapY, boolean affordable, Consumer<KatanaTier> onPick, Image picture, double k) {
        String id = tier.toCard().getId();   // also the image name: bokken, tanto, katana, odachi, honjo-masamune
        ImageView sword = BoardView.image("/images/weapons/" + id + ".png");
        double width = bladeWidth(tier);
        double swordW = width * k;
        double swordH = swordW * sword.getImage().getHeight() / sword.getImage().getWidth();
        sword.setFitWidth(swordW);
        sword.setFitHeight(swordH);
        sword.relocate((CENTRE_X - width / 2) * k, strapY * k - swordH / 2);

        ImageView leftLoop = loop(picture, LOOP_LEFT_X, strapY, k);
        ImageView rightLoop = loop(picture, LOOP_RIGHT_X, strapY, k);

        Label caption = new Label(tier.toCard().getNameRomaji() + "  ·  " + tier.getKiCost() + " Ki  ·  "
                + tier.getAttack() + " / " + tier.getDurability());
        caption.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #2a1a10;");
        caption.setAlignment(Pos.CENTER);
        caption.setPrefWidth(OPEN_W * k);
        caption.relocate(0, (strapY + LOOP_H / 2 + 8) * k);

        Group row = new Group(sword, leftLoop, rightLoop, caption);
        if (affordable) {
            HomeView.clickable(row, false, () -> {
                close();
                onPick.accept(tier);
            });
        } else {
            row.setEffect(new ColorAdjust(0, -0.5, -0.45, 0));   // not enough Ki: darker and greyer, still solid
        }
        return row;
    }


    /**
     * A blade's width on the cloth (open picture's pixels). Bokken and Tantō stay inside the cloth;
     * the long blades reach over its patterned edges, the Ōdachi a little past them.
     */
    private static double bladeWidth(KatanaTier tier) {
        if (tier == KatanaTier.KATANA) {
            return 560;
        }
        if (tier == KatanaTier.HONJO_MASAMUNE) {
            return 620;
        }
        if (tier == KatanaTier.ODACHI) {
            return 660;
        }
        return BLADE_W;
    }

    /** A strap loop cut out of the cloth picture, drawn again over the blade so the blade looks held by it. */
    private static ImageView loop(Image picture, double loopX, double strapY, double k) {
        ImageView loop = new ImageView(picture);
        loop.setViewport(new Rectangle2D(loopX, strapY - LOOP_H / 2, LOOP_W, LOOP_H));
        loop.setFitWidth(LOOP_W * k);
        loop.setFitHeight(LOOP_H * k);
        loop.relocate(loopX * k, (strapY - LOOP_H / 2) * k);
        loop.setMouseTransparent(true);
        return loop;
    }

    /** Opening: the rolled-up cloth pops in, waits, then fades while the cloth unrolls downwards from its top roll. */
    private void unroll(Pane cloth, ImageView rolled, double k) {
        Rectangle shown = new Rectangle(OPEN_W * k, ROLL_BOTTOM * k);   // only the top roll shows at first
        cloth.setClip(shown);
        cloth.setMouseTransparent(true);   // no click on a blade until the cloth is open
        rolled.setOpacity(0);
        rolled.setScaleX(0.6);
        rolled.setScaleY(0.6);

        Timeline appear = new Timeline(new KeyFrame(Duration.millis(220),
                new KeyValue(rolled.opacityProperty(), 1),
                new KeyValue(rolled.scaleXProperty(), 1, Interpolator.EASE_OUT),
                new KeyValue(rolled.scaleYProperty(), 1, Interpolator.EASE_OUT)));
        PauseTransition hold = new PauseTransition(Duration.millis(250));
        Timeline open = new Timeline(new KeyFrame(UNROLL,
                new KeyValue(shown.heightProperty(), OPEN_H * k, Interpolator.EASE_OUT),
                new KeyValue(rolled.opacityProperty(), 0, Interpolator.EASE_IN)));

        SequentialTransition all = new SequentialTransition(appear, hold, open);
        all.setOnFinished(event -> {
            cloth.setMouseTransparent(false);
            getChildren().remove(rolled);
        });
        all.play();
    }

    /** Removes the picker from the table. */
    private void close() {
        ((Pane) getParent()).getChildren().remove(this);
    }
}