package fr.onishintan.ui.match;

import fr.onishintan.model.KatanaTier;
import fr.onishintan.ui.screens.HomeView;

import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * The Samurai's hero power (Katana): pick a blade, from the wooden Bokken (1 Ki) to Honjō Masamune (9 Ki).
 * Blades you can't afford are dimmed. A click outside the panel closes it without using the power.
 */
final class KatanaPicker extends Pane {
    private static final double TILE_W = 110;

    /** kiAvailable = your Ki now; onPick gets the chosen blade; onCancel runs when you click outside. */
    KatanaPicker(int kiAvailable, Consumer<KatanaTier> onPick, Runnable onCancel) {
        setPrefSize(BoardView.W, BoardView.H);

        // A light veil over the whole table: a click on it (outside the panel) cancels.
        Rectangle outside = new Rectangle(BoardView.W, BoardView.H, Color.rgb(0, 0, 0, 0.35));
        outside.setOnMousePressed(event -> {
            close();
            onCancel.run();
        });
        getChildren().add(outside);

        // The panel: one tile per blade, cheapest first.
        HBox blades = new HBox(10);
        blades.setPadding(new Insets(14));
        blades.setStyle("-fx-background-color: rgba(20,12,8,0.9); -fx-background-radius: 12;");
        for (KatanaTier tier : KatanaTier.values()) {
            boolean affordable = kiAvailable >= tier.getKiCost();
            blades.getChildren().add(tile(tier, affordable, onPick));
        }
        blades.relocate(660, 430);   // above your hero power
        getChildren().add(blades);
    }

    /** One blade: its picture, name, Ki cost and ATK / durability. Clickable only if you can afford it. */
    private VBox tile(KatanaTier tier, boolean affordable, Consumer<KatanaTier> onPick) {
        String id = tier.toCard().getId();   // also the image name: bokken, tanto, katana, odachi, honjo-masamune
        ImageView blade = BoardView.image("/images/weapons/" + id + ".png");
        blade.setFitWidth(TILE_W - 20);
        blade.setFitHeight(90);
        blade.setPreserveRatio(true);

        Label name = text(tier.toCard().getNameRomaji(), 15);
        Label stats = text(tier.getKiCost() + " Ki    " + tier.getAttack() + " / " + tier.getDurability(), 13);

        VBox tile = new VBox(4, blade, name, stats);
        tile.setAlignment(Pos.CENTER);
        tile.setPrefWidth(TILE_W);
        if (affordable) {
            HomeView.clickable(tile, false, () -> {
                close();
                onPick.accept(tier);
            });
        } else {
            tile.setOpacity(0.35);   // not enough Ki
        }
        return tile;
    }

    /** Light bold text for the dark panel. */
    private static Label text(String words, double size) {
        Label label = new Label(words);
        label.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: " + size + "; -fx-font-weight: bold; -fx-text-fill: #f3ede1;");
        return label;
    }

    /** Removes the picker from the table. */
    private void close() {
        ((Pane) getParent()).getChildren().remove(this);
    }
}