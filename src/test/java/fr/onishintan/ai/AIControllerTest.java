package fr.onishintan.ai;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.junit.jupiter.api.Test;

import fr.onishintan.ai.AIController.Difficulty;
import fr.onishintan.data.AdventureDecks;
import fr.onishintan.engine.GameEngine;
import fr.onishintan.model.*;
import fr.onishintan.data.CardRepository;

/** The AI plays legal turns through the engine, every match ends, HARD beats EASY, and it takes lethal and good trades. */
class AIControllerTest {

    private static final HeroClass[] CLASSES = { HeroClass.SAMURAI, HeroClass.NINJA, HeroClass.KANNUSHI, HeroClass.ONI };
    private static final AdventureDecks ADVENTURE = new AdventureDecks(CardRepository.all());

    /** Plays a whole match AI against AI; the winner, or null for a draw. Fails if it does not end in 300 turns. */
    private static Player match(Player first, Player second, Difficulty d1, Difficulty d2, long seed) {
        Random rng = new Random(seed);
        GameEngine engine = new GameEngine(first, second, rng);
        engine.setCatalog(CardRepository.all());
        engine.setTimeSource(() -> 0L);   // no chess clock timeouts in a test
        AIController a1 = new AIController(engine, first, d1, rng), a2 = new AIController(engine, second, d2, rng);
        engine.startMatch();
        engine.beginFirstTurn();
        for (int turn = 0; turn < 300 && !engine.getState().isOver(); turn++) {
            (engine.active() == first ? a1 : a2).playTurn();
        }
        assertTrue(engine.getState().isOver(), "the match must end");
        return engine.getState().getWinner().orElse(null);
    }

    @Test
    void everyAdventureBattleEnds() {
        for (HeroClass you : CLASSES) {
            for (int battle = 0; battle < AdventureDecks.BATTLES; battle++) {
                match(ADVENTURE.hero("Pierre", you), ADVENTURE.enemy(you, battle), Difficulty.NORMAL,
                        Difficulty.forBattle(battle), you.ordinal() * 10L + battle);
            }
        }
    }

    @Test
    void hardBeatsEasyMostOfTheTime() {
        int hard = 0, games = 60;
        for (int g = 0; g < games; g++) {
            HeroClass c = CLASSES[g % 4];
            Player p = ADVENTURE.hero("Hard", c), q = ADVENTURE.hero("Easy", c);   // same deck: only the level differs
            boolean hardFirst = g % 2 == 0;
            Player winner = hardFirst ? match(p, q, Difficulty.HARD, Difficulty.EASY, g)
                                      : match(q, p, Difficulty.EASY, Difficulty.HARD, g);
            if (winner == p) hard++;
        }
        assertTrue(hard >= games * 0.6, "HARD won " + hard + " of " + games);
    }

    @Test
    void difficultyPerBattle() {
        assertEquals(Difficulty.EASY, Difficulty.forBattle(0));
        assertEquals(Difficulty.NORMAL, Difficulty.forBattle(1));
        assertEquals(Difficulty.HARD, Difficulty.forBattle(2));
        assertEquals(Difficulty.HARD, Difficulty.forBattle(5));
    }

    // ---------- small boards ----------

    private Player me, enemy;
    private GameEngine engine;

    private static MinionCard card(String id, int ki, int atk, int hp, Keyword... keywords) {
        EnumSet<Keyword> set = EnumSet.noneOf(Keyword.class);
        for (Keyword k : keywords) set.add(k);
        return new MinionCard(id, id, "試", "", ki, Rarity.COMMON, HeroClass.NEUTRAL, "", "", atk, hp, set);
    }

    private static Player player(String name, HeroClass cls) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < Deck.SIZE; i++) cards.add(card(name + "-" + i, 9, 1, 1));   // 9 Ki: never affordable early
        return new Player(name, new Hero(cls), new Deck(cards));
    }

    /** A minion of p's that can attack this turn. */
    private static Minion ready(Player p, int slot, MinionCard c) {
        Minion m = new Minion(c);
        p.getSide().place(m, slot);
        m.onTurnStart();
        return m;
    }

    private AIController start(HeroClass mine) {
        me = player("AI", mine);
        enemy = player("Pierre", HeroClass.NINJA);
        engine = new GameEngine(me, enemy, new Random(1));
        engine.setTimeSource(() -> 0L);
        engine.startMatch();
        engine.beginFirstTurn();
        return new AIController(engine, me, Difficulty.HARD, new Random(1));
    }

    @Test
    void nothingWhenItIsNotItsTurn() {
        start(HeroClass.ONI);
        AIController other = new AIController(engine, enemy, Difficulty.HARD, new Random(1));
        assertFalse(other.isMyTurn());
        assertEquals(Optional.empty(), other.nextMove());
    }

    @Test
    void goesForLethal() {
        AIController ai = start(HeroClass.ONI);
        Minion big = ready(me, 0, card("big", 1, 8, 8));
        ready(enemy, 0, card("bait", 1, 1, 1));          // an easy trade it must ignore
        enemy.getHero().takeDamage(enemy.getHero().getHealth() - 5);
        Move m = ai.nextMove().orElseThrow();
        assertEquals(Move.Kind.ATTACK, m.kind());
        assertSame(big, m.attacker());
        assertSame(enemy.getHero(), m.target());
        assertTrue(ai.play(m));
        assertTrue(engine.getState().isOver());
    }

    @Test
    void takesAFreeTradeOnAShirodu() {
        AIController ai = start(HeroClass.ONI);
        Minion mine = ready(me, 0, card("kappa", 1, 5, 7));
        Minion wall = ready(enemy, 0, card("wall", 1, 2, 3, Keyword.SHIRODU));
        Move m = ai.nextMove().orElseThrow();
        assertEquals(Move.Kind.ATTACK, m.kind());
        assertSame(mine, m.attacker());
        assertSame(wall, m.target());
    }

    @Test
    void skipsABadTrade() {
        AIController ai = start(HeroClass.ONI);
        ready(me, 0, card("small", 1, 2, 2));
        ready(enemy, 0, card("wall", 1, 9, 9, Keyword.SHIRODU));   // would kill it and survive
        me.spendKi(me.getKi());                                     // no power either
        assertEquals(Optional.empty(), ai.nextMove());
    }

    @Test
    void playsItsCardsAndEndsItsTurn() {
        AIController ai = start(HeroClass.KANNUSHI);
        MinionCard cheap = card("cheap", 1, 2, 2);
        me.addToHand(cheap);
        List<Move> moves = ai.playTurn();
        assertTrue(moves.stream().anyMatch(m -> m.kind() == Move.Kind.PLAY && m.card() == cheap));
        assertSame(enemy, engine.active(), "the turn was ended");
    }

    @Test
    void samuraiTakesTheBestBladeItCanPay() {
        AIController ai = start(HeroClass.SAMURAI);
        me.spendKi(me.getKi());
        me.gainTemporaryKi(4);
        Move m = ai.nextMove().orElseThrow();
        assertEquals(Move.Kind.KATANA, m.kind());
        assertEquals(KatanaTier.KATANA, m.tier());
    }
}