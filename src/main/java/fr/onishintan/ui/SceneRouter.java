package fr.onishintan.ui;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import javafx.geometry.Pos;
import javafx.scene.ImageCursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;

/** Shows one Screen at a time in the main window. Each visit builds the screen fresh. */
public class SceneRouter {
    private final Scene scene =new Scene(new VBox(), 1280, 720);
    private final Map<Screen, Supplier<Parent>> screens = new EnumMap<>(Screen.class);
    private final ImageCursor pointer = cursor("pointer"), grab = cursor("grab");

    public SceneRouter(Stage stage) {
        stage.setScene(scene);
        scene.setCursor(pointer);
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> scene.setCursor(grab));
        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> scene.setCursor(pointer));
    }

    /** Tells the router how to build a screen. */
    public void register(Screen s, Supplier<Parent> builder) { screens.put(s, builder); }

    public void goTo(Screen s) {
        scene.setRoot(screens.getOrDefault(s, () -> placeholder(s)).get());
    }

    /** Stand-in until the real screen exists: its name and a way back home. */
    private Parent placeholder(Screen s) {
        Button home = new Button("Home");
        home.setOnAction(e -> goTo(Screen.HOME));
        VBox box = new VBox(20, new Label(s.name()), home);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    /** A cursor from /images/cursors/, clicking at the pointing fingertip (29, 2 in the 64 x 64 image). */
    private ImageCursor cursor(String name) {
        return new ImageCursor(new Image(getClass().getResourceAsStream("/images/cursors/" + name + ".png")), 29, 2);
    }
}