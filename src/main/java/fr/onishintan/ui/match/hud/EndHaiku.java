package fr.onishintan.ui.match.hud;

import fr.onishintan.model.HeroClass;

import java.util.List;
import java.util.Map;
import java.util.Random;

import javafx.scene.control.Label;

/** The haiku under the end-of-match seal: one of two per class, for a victory or a defeat. */
public final class EndHaiku {
    private static final Random RANDOM = new Random();

    private static final Map<HeroClass, List<String>> VICTORY = Map.of(
            HeroClass.SAMURAI, List.of(
                    "The blade finds its sheath\ncherry petals settle down\non a quiet field",
                    "One clean, silent cut\nthe wind carries off the dust\nhonour stands alone"),
            HeroClass.NINJA, List.of(
                    "No one saw me come\nno one will see me leave now\nonly smoke remains",
                    "A shadow, a breath\nthe lantern flickers just once\nthe night keeps its word"),
            HeroClass.KANNUSHI, List.of(
                    "The bell rings at dusk\nthe kami have heard our prayer\nthe shrine lamps burn bright",
                    "Pure as morning snow\nthe sacred rope holds firm now\nevil turns to ash"),
            HeroClass.ONI, List.of(
                    "Thunder in the hills\nthe drum of the iron club\nthe village kneels down",
                    "Sake and red flames\nthe mountain laughs with my voice\ntonight the Oni feast"));

    private static final Map<HeroClass, List<String>> DEFEAT = Map.of(
            HeroClass.SAMURAI, List.of(
                    "My sword lies broken\nthe autumn moon does not care\nI will rise at dawn",
                    "Fallen in the rain\neven the proud plum blossom\nreturns to the earth"),
            HeroClass.NINJA, List.of(
                    "Caught by the moonlight\nthe shadow that knew no fear\nfades into the mist",
                    "The trap was my own\neven the quiet spider\nslips on its own thread"),
            HeroClass.KANNUSHI, List.of(
                    "The offering spilled\nthe torii stands in silence\nthe gods look away",
                    "Paper charms in rain\ntheir ink runs down like dark tears\nthe shrine waits for spring"),
            HeroClass.ONI, List.of(
                    "Horns snapped by a child\nthe beans fly at Setsubun\nthe Oni runs home",
                    "Even demons weep\nwhen the spring sun melts the snow\nthat hid their footprints"));

    private EndHaiku() {
        // Only static data: nobody creates an EndHaiku.
    }

    /** A haiku for this class and this outcome, as a soft italic label; empty for a class without haiku. */
    public static Label of(HeroClass heroClass, boolean won) {
        List<String> poems = DEFEAT.getOrDefault(heroClass, List.of(""));
        if (won) {
            poems = VICTORY.getOrDefault(heroClass, List.of(""));
        }
        String poem = poems.get(RANDOM.nextInt(poems.size()));
        Label haiku = new Label(poem);
        haiku.setStyle("-fx-font-family: 'Yu Mincho'; -fx-font-size: 26; -fx-font-style: italic; -fx-text-fill: #f3ede1; -fx-text-alignment: center;");
        return haiku;
    }
}
