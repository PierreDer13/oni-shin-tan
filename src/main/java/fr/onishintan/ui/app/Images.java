package fr.onishintan.ui.app;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javafx.scene.image.Image;

/**
 * Loads each picture only once and keeps it: every redraw reuses the same decoded Image
 * instead of reading and decoding the PNG again (that made every match redraw slow).
 */
public final class Images {
    private static final Map<String, Image> CACHE = new HashMap<>();

    private Images() {
        // Only a static helper: nobody creates an Images.
    }

    /** The picture at this resource path, at its own size, or null if there is no such file. */
    public static Image get(String path) {
        return get(path, 0, 0);
    }

    /**
     * The picture decoded at width x height (smaller in memory and faster to draw); 0 x 0 keeps its own size.
     * @return the picture, or null if there is no such file
     */
    public static Image get(String path, double width, double height) {
        String key = path + "@" + width + "x" + height;
        if (CACHE.containsKey(key)) {
            return CACHE.get(key);
        }
        Image image = null;
        InputStream in = Images.class.getResourceAsStream(path);
        if (in != null) {
            image = new Image(in, width, height, false, true);
        }
        CACHE.put(key, image);   // a missing file is remembered too, so it isn't searched for again
        return image;
    }
}