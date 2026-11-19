package fr.onishintan.ui.screens;

import fr.onishintan.model.HeroClass;

import java.util.Map;

/** The Adventure story, per class: a page read before each battle (Town, Ocean, Mount Fuji) and one after winning it. */
final class StoryLines {
    /** Each battle's title, in the order of AdventureDecks.board(i). */
    static final String[] TITLES = { "町  The Town", "海  The Ocean", "富士  Mount Fuji" };

    /** Per class, the pages before battles 1, 2 and 3 (the foes are those of AdventureDecks); a | starts picture 2. */
    private static final Map<HeroClass, String[]> BEFORE = Map.of(
            HeroClass.SAMURAI, new String[] {
                    "Your lord fell at dawn, betrayed by his own men, and his castle burned before you could draw your sword. "
                            + "Now you are a rōnin, a blade without a master, walking the road of shame. "
                            + "| In the town below the ashes, a Kannushi rings the shrine bell and calls you cursed: "
                            + "the gods, he says, turn their backs on men who outlive their lord. "
                            + "The townsfolk close their doors. Only your sword still listens. Prove the priest wrong.",
                    "The traitor fled east by the sea road, and the sea road belongs to an Oni. "
                            + "Every ship that dares the strait is torn apart in a storm of his making, its sailors dragged beneath the waves. "
                            + "No captain will sail, so you take the helm yourself. "
                            + "| Thunder splits the sky, the deck rolls under your feet, and a horned shadow climbs out of the water.",
                    "Mount Fuji rises white above the clouds. | In the mist on its slopes waits the one you have hunted for so long: "
                            + "the Ninja who opened the castle gates the night your lord died. "
                            + "There will be no witnesses up here, no honour to win, only the truth of the blade. "
                            + "Above the summit, the thunder god Takemikazuchi watches. Draw." },
            HeroClass.NINJA, new String[] {
                    "The clan elders gave you one order: steal the Scroll of the Hidden Moon before the new moon rises. "
                            + "But you are not the only thief in the town tonight. "
                            + "| An Oni wearing the face of a merchant walks the market, and his eyes keep turning toward the scroll's hiding place. "
                            + "The lanterns are not lit yet. Strike first, and strike unseen.",
                    "Rain hammers the deck as you climb the anchor chain in silence. "
                            + "One mistake, one creak of wood, and the whole crew will wake. "
                            + "| The scroll was gone before you reached it, sealed in a box of charms and carried aboard by a Kannushi. "
                            + "The priest knows what it holds and means to burn it on the open sea.",
                    "Every road home ends at the foot of Mount Fuji, and on the last one stands a Samurai of the shōgun. "
                            + "| He has followed your trail for three nights and he does not intend to lose it again. "
                            + "Behind him lies your clan; in your hands, the scroll. "
                            + "Above, the moon itself is hidden, as if Tsukuyomi were holding his breath." },
            HeroClass.KANNUSHI, new String[] {
                    "The great bell of your shrine cracked in the night, and since then the kami have fallen silent. "
                            + "The town is afraid. Children no longer play in the streets, and the rice will not grow. "
                            + "| A Samurai has drawn his sword in front of the torii gate and blames the priests for everything. "
                            + "If blood is spilled on sacred ground, the kami will never return. Calm him, by charm or by force.",
                    "In the confusion, a Ninja slipped into the shrine and stole the Sacred Mirror, the eye through which the kami see the world. "
                            + "He is escaping by sea. | You board the fastest ship in the harbour and chase him through the waves. "
                            + "If the mirror sinks beneath the water, the gods will be blind forever.",
                    "Now you understand: the cracked bell, the stolen mirror, all of it was the work of an Oni. "
                            + "He has climbed Mount Fuji to break the ancient seal on the sacred mountain and free the demons sleeping under it. "
                            + "| The wind howls with their voices. "
                            + "You are the last prayer between the seal and the dark. Hold it." },
            HeroClass.ONI, new String[] {
                    "For three hundred years, men kept your kin locked under paper charms, buried in the mountains. "
                            + "Tonight one charm burned, and you walked free. "
                            + "| But word travels fast, and in the town a Ninja is already hunting you for the reward on your horns. "
                            + "He thinks he is chasing a monster. Show him what a monster truly is.",
                    "The shōgun has sent his finest Samurai to bring back your head, and his ship is crossing the strait at this very moment. "
                            + "| The sea remembers you. The clouds remember you. "
                            + "Raise the storm, turn the waves black, and let the ocean decide which of you deserves to reach the shore.",
                    "On the summit of Mount Fuji kneels the Kannushi who wrote the charms that still bind your kin. "
                            + "His prayers are the last chains. As long as he chants, they cannot wake. "
                            + "| The ground trembles under your feet; deep below, Izanami, queen of the underworld, is listening. "
                            + "Break his prayer." });

    /** Per class, the pages after winning battles 1, 2 and 3. */
    private static final Map<HeroClass, String[]> AFTER = Map.of(
            HeroClass.SAMURAI, new String[] {
                    "The Kannushi lowers his eyes and, before you leave, he whispers a name: the traitor sailed east, by the sea road. "
                            + "For the first time since the fire, you have a direction.",
                    "The storm breaks. The Oni sinks back into the deep and the sun comes out over a calm sea. "
                            + "On the horizon, white and silent, Mount Fuji appears. He is there. You know it.",
                    "The Ninja falls in the snow, and the mountain is quiet again. "
                            + "Lightning strikes the summit, and from the light steps Takemikazuchi, god of thunder and of the sword. "
                            + "A rōnin no more: tonight, you serve a god." },
            HeroClass.NINJA, new String[] {
                    "The Oni's disguise falls away in a cloud of smoke, but the scroll is not on him. "
                            + "A beggar saw a priest board a ship at dawn, carrying a box sealed with charms.",
                    "The box opens under your blade, and the scroll is yours again. "
                            + "You dive from the ship into the black water, but on the shore, the shōgun's men are already waiting.",
                    "The Samurai lies still, and the road home is open. "
                            + "The clouds part, the moon shines again, and in its silver light Tsukuyomi steps down. "
                            + "From this night on, the moon itself will hide you." },
            HeroClass.KANNUSHI, new String[] {
                    "The Samurai sheathes his sword and bows deeply before the torii. "
                            + "But when you return to the sanctuary, the altar is empty: the Sacred Mirror is gone.",
                    "The mirror shines in your hands, saved from the waves. "
                            + "But in its surface you see something else: a horned shadow climbing the slopes of Mount Fuji.",
                    "The seal holds. The Oni is cast back into the dark, and the bell rings clear for the first time in months. "
                            + "Ōkuninushi, kami of the land, appears in the light of the shrine. Your prayers have been heard." },
            HeroClass.ONI, new String[] {
                    "The Ninja runs into the night, and the townsfolk hide behind their doors. "
                            + "But on the harbour, banners rise: the shōgun has sent a Samurai to end you himself.",
                    "The ship is lost beneath the waves, and you stand alone on the beach, the storm still roaring at your back. "
                            + "Far away, a voice is chanting on Mount Fuji. The chains tighten.",
                    "The prayer breaks, and every charm in the land burns at once. "
                            + "The mountain opens, and from the gates of the underworld rises Izanami herself. "
                            + "Your kin are free, and their queen has chosen you." });

    private StoryLines() {
        // Only the texts: nobody creates a StoryLines.
    }

    /** The page read before battle `i` (0, 1 or 2) of this class's run. */
    static String before(HeroClass heroClass, int i) {
        return BEFORE.get(heroClass)[i];
    }

    /** The page read after winning battle `i` (0, 1 or 2) of this class's run. */
    static String after(HeroClass heroClass, int i) {
        return AFTER.get(heroClass)[i];
    }

    /** Picture k (1 to 3) of battle i's story for this class: images/story/<class>-<battle>-<k>, e.g. samurai-1-2. */
    static String picture(HeroClass heroClass, int i, int k) {
        return heroClass.name().toLowerCase() + "-" + (i + 1) + "-" + k;
    }
}
