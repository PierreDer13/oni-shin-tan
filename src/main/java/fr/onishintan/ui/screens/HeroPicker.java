package fr.onishintan.ui.screens;

import fr.onishintan.model.HeroClass;
import fr.onishintan.ui.app.HeroChoice;
import fr.onishintan.ui.app.Images;
import fr.onishintan.ui.app.ScreenGrid;

import java.util.Map;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/** Laid over the home screen from a class banner: that class's three heroes, with a short history; select one, then Confirm. */
public class HeroPicker extends StackPane {
    /** Width of each hero's column; the portraits are 800 x 501. */
    private static final double CARD_W = 380, PORTRAIT_H = CARD_W * 501 / 800;
    private static final String CHOSEN = "-fx-border-color: #ffd84a; -fx-border-width: 3; -fx-border-radius: 8; -fx-padding: 8;";
    private static final String NOT_CHOSEN = "-fx-border-color: transparent; -fx-border-width: 3; -fx-padding: 8;";
    private final HeroClass heroClass;
    /** The three heroes' columns, and the hero with the gold border (saved only on Confirm). */
    private final HBox heroes = new HBox(30);
    private String selected;
    /** Each class's heroes: portrait id, name, kanji, a short history. */
    private static final Map<HeroClass, String[][]> HEROES = Map.of(
            HeroClass.NINJA, new String[][] {
                { "fujibayashi-yasutake", "Fujibayashi Yasutake", "藤林保武", "Iga ninja of the 17th century. In 1676 he gathered the secrets of the Iga and Kōga schools into the Bansenshūkai, the great book of ninjutsu." },
                { "hattori-hanzo", "Hattori Hanzō", "服部半蔵", "Hattori Hanzō Masanari (1542–1596), of Iga descent, served Tokugawa Ieyasu. In 1582 he led Ieyasu to safety through Iga; a gate of Edo Castle still bears his name." },
                { "jinichi-kawakami", "Jinichi Kawakami", "川上仁一", "Born in 1949 and called the last ninja: head of the Ban family of Kōga ninjutsu, he trained from childhood and now teaches its history." } },
            HeroClass.SAMURAI, new String[][] {
                { "yasuke", "Yasuke", "弥助", "A man from Africa who came to Japan with Jesuit missionaries and served Oda Nobunaga from 1581. He fought beside him at Honnō-ji in 1582." },
                { "tomoe-gozen", "Tomoe Gozen", "巴御前", "A warrior of the late 12th century who fought for Minamoto no Yoshinaka in the Genpei War. The Tale of the Heike praises her bow and her sword." },
                { "miyamoto-musashi", "Miyamoto Musashi", "宮本武蔵", "Swordsman (c. 1584–1645) said to have won all of his more than 60 duels. He founded the two-sword Niten Ichi-ryū and wrote The Book of Five Rings." } },
            HeroClass.KANNUSHI, new String[][] {
                { "kada-no-azumamaro", "Kada no Azumamaro", "荷田春満", "Shinto priest (1669–1736) of the Fushimi Inari shrine in Kyoto. He studied Japan's oldest texts and opened the way for the National Learning school." },
                { "yamazaki-ansai", "Yamazaki Ansai", "山崎闇斎", "Scholar (1619–1682) who joined Confucian teaching with Shinto and founded Suika Shinto, which taught reverence for the gods and the emperor." },
                { "yoshikawa-koretaru", "Yoshikawa Koretaru", "吉川惟足", "Founder of Yoshikawa Shinto (1616–1695). The Tokugawa shogunate named him its official in charge of Shinto matters." } },
            HeroClass.ONI, new String[][] {
                { "shuten-doji", "Shuten-dōji", "酒呑童子", "Chief of the oni of Mount Ōe, a great lover of sake who raided Kyoto. Minamoto no Raikō and his four retainers slew him after making him drunk." },
                { "ibaraki-doji", "Ibaraki-dōji", "茨木童子", "Shuten-dōji's chief retainer. Watanabe no Tsuna cut off his arm at the Rashōmon gate; he came back in disguise to take it back." },
                { "kane-doji", "Kane-dōji", "金童子", "One of the four oni lieutenants who guarded Shuten-dōji's fortress on Mount Ōe, and fell with him to Raikō's band." } });

    public HeroPicker(HeroClass heroClass) {
        this.heroClass = heroClass;
        Rectangle veil = new Rectangle(ScreenGrid.W, ScreenGrid.H, Color.rgb(0, 0, 0, 0.6));
        veil.setOnMousePressed(e -> close());

        heroes.setAlignment(Pos.TOP_CENTER);
        for (String[] hero : HEROES.get(heroClass)) {
            heroes.getChildren().add(hero(heroClass, hero));
        }
        select(HeroChoice.portrait(heroClass));   // the current hero starts with the gold border

        Label title = SettingsPanel.label("英雄  Choose your " + heroClass.getDisplayName() + " hero", 36);
        HBox buttons = new HBox(30, SettingsPanel.button("決定  Confirm", this::confirm), SettingsPanel.button("閉じる  Close", this::close));
        buttons.setAlignment(Pos.CENTER);
        VBox panel = new VBox(24, title, heroes, buttons);
        panel.setAlignment(Pos.CENTER);
        panel.setMaxSize(1340, 760);
        panel.setStyle("-fx-background-color: rgba(20,12,8,0.92); -fx-background-radius: 16; -fx-padding: 30;");
        getChildren().addAll(veil, panel);
        setPrefSize(ScreenGrid.W, ScreenGrid.H);
    }

    /** One hero: portrait, name and kanji, history. A click selects them (gold border); Confirm saves the choice. */
    private VBox hero(HeroClass heroClass, String[] hero) {
        ImageView portrait = new ImageView(Images.get("/images/heroes/" + hero[0] + ".png", CARD_W, PORTRAIT_H));
        Label name = SettingsPanel.label(hero[1] + "  " + hero[2], 22);
        Label history = SettingsPanel.label(hero[3], 15);
        history.setWrapText(true);
        history.setMaxWidth(CARD_W);

        VBox column = new VBox(10, portrait, name, history);
        column.setAlignment(Pos.TOP_CENTER);
        column.setPrefWidth(CARD_W + 22);
        column.setUserData(hero[0]);   // which hero this column shows, for select()
        HomeView.clickable(column, false, () -> select(hero[0]));
        return column;
    }

    /** Moves the gold border to this hero; nothing is saved until Confirm. */
    private void select(String portrait) {
        selected = portrait;
        for (Node column : heroes.getChildren()) {
            if (portrait.equals(column.getUserData())) {
                column.setStyle(CHOSEN);
            } else {
                column.setStyle(NOT_CHOSEN);
            }
        }
    }

    /** Plays with the selected hero from now on, then closes. */
    private void confirm() {
        HeroChoice.choose(heroClass, selected);
        close();
    }

    /** Takes the picker off the home screen. */
    private void close() {
        ((Pane) getParent()).getChildren().remove(this);
    }
}