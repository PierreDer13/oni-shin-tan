package fr.onishintan.ui.app;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import javafx.geometry.Pos;
import javafx.scene.ImageCursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;

/** Shows one Screen at a time in the main window. Each visit builds the screen fresh. */
public class SceneRouter {
    private final StackPane content = new StackPane();
    private final ShojiTransition doors = new ShojiTransition();
    private final Scene scene = new Scene(new StackPane(content, doors), 1280, 720);
    private final Map<Screen, Supplier<Parent>> screens = new EnumMap<>(Screen.class);
    private final ImageCursor pointer = cursor("pointer"), grab = cursor("grab");
    /** The main theme, for every screen that has no track of its own. */
    private static final String DEFAULT_TRACK = "music";
    /** The background music, and which track each screen plays. */
    private final Music music = new Music();
    private final Map<Screen, String> tracks = new EnumMap<>(Screen.class);

    public SceneRouter(Stage stage) {
        stage.setScene(scene);
        scene.setCursor(pointer);
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> scene.setCursor(grab));
        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> scene.setCursor(pointer));
    }

    /** Tells the router how to build a screen. */
    public void register(Screen s, Supplier<Parent> builder) { screens.put(s, builder); }

    /**
     * Shows a screen: straight away the first time, then behind the shōji doors.
     * The doors start at once; the next screen is built while they are closed, so a slow screen never delays the click.
     * The screen's music starts as it appears (if it is the track already playing, it plays on).
     */
    public void goTo(Screen s) {
        Supplier<Parent> next = screens.getOrDefault(s, () -> placeholder(s));
        String track = tracks.getOrDefault(s, DEFAULT_TRACK);
        if (content.getChildren().isEmpty()) {
            content.getChildren().setAll(next.get());
            music.play(track);
        } else {
            doors.play(() -> {
                content.getChildren().setAll(next.get());
                music.play(track);
            });
        }
    }

    /** The track a screen plays (/sounds/<track>.mp3). Screens without one play DEFAULT_TRACK. */
    public void setMusic(Screen s, String track) {
        tracks.put(s, track);
    }

    /** Stand-in until the real screen exists: its name and a way back home. */
    private Parent placeholder(Screen s) {
        Button home = new Button("Home");
        home.setOnMousePressed(e -> goTo(Screen.HOME));
        VBox box = new VBox(20, new Label(s.name()), home);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    /** A cursor from /images/cursors/, clicking at the pointing fingertip (29, 2 in the 64 x 64 image). */
    private ImageCursor cursor(String name) {
        return new ImageCursor(new Image(getClass().getResourceAsStream("/images/cursors/" + name + ".png")), 29, 2);
    }
}