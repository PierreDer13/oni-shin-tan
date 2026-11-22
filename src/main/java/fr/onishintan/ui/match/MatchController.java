package fr.onishintan.ui.match;

import fr.onishintan.ai.AIController;
import fr.onishintan.ai.Move;
import fr.onishintan.adventure.Upgrade;
import fr.onishintan.engine.GameEngine;
import fr.onishintan.engine.event.GameEvent;
import fr.onishintan.engine.effect.SpellTarget;
import fr.onishintan.model.Card;
import fr.onishintan.model.CurseCard;
import fr.onishintan.model.CurseZone;
import fr.onishintan.model.Combatant;
import fr.onishintan.model.Hero;
import fr.onishintan.model.HeroClass;
import fr.onishintan.model.Keyword;
import fr.onishintan.model.MinionCard;
import fr.onishintan.model.KatanaTier;
import fr.onishintan.model.Oath;
import fr.onishintan.model.OathType;
import fr.onishintan.model.Player;
import fr.onishintan.model.RiteCard;
import fr.onishintan.model.Shrine;
import fr.onishintan.model.SpellCard;
import fr.onishintan.model.TrapCard;
import fr.onishintan.data.CardRepository;
import fr.onishintan.ui.app.HeroChoice;
import javafx.animation.PauseTransition;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;
import fr.onishintan.ui.match.fx.EffectRecorder;
import fr.onishintan.ui.match.hud.ClockTicker;
import fr.onishintan.ui.match.hud.OathView;
import fr.onishintan.ui.match.hud.PlayHistory;

/**
 * Runs one hotseat match. Pierre's GameEngine applies every rule; the BoardView only draws the table.
 * Each click on the board calls one engine method, then the board redraws from the players' new state.
 */
public class MatchController implements CardPlayer {
    private final GameEngine engine;
    /** Notes what an action did (damage, healing, deaths, traps) so the board can show it before redrawing. */
    private final EffectRecorder recorder;
    /** Before a played card's effects: the card landing, or a spell or a Banzai taking effect (GDD timing). */
    private static final double PLACE_PAUSE = BoardView.FLOAT_MS + BoardView.DROP_MS, EFFECT_PAUSE = PLACE_PAUSE + 50, POWER_PAUSE = 500;   // the card lands before its effects; POWER_PAUSE: a shuriken or beam reaches its target
    /** How long effects show before the redraw; at the end of a turn they all fit in 0.75 s. */
    private static final double EFFECT_SHOW = 700, TURN_SHOW = 750;
    /** Under this time left (30 s), the active player's clock turns red and ticks every second. */
    private static final long HURRY_MS = 30_000;
    private final BoardView board;
    /** Plays first and starts at the bottom of the screen. */
    private final Player first;
    private final Player second;
    /** Checks the engine's clock and redraws both times every 200 ms. */
    private final ClockTicker clockTicker = new ClockTicker(this::tickClock);
    /** The last clock timeout (from CLOCK_TIMEOUT): the hero who ran out of time and the damage he took. */
    private Hero timedOutHero;
    private int timeoutDamage;
    /** The target(s) of the card or hero power being played right now, for the play history (the engine's event doesn't carry them). */
    private Combatant aimFirst;
    private Combatant aimSecond;
    /** How many cards the engine drew during endTurn(), so they can fly in after the pass veil. */
    private int cardsDrawnThisTurn;
    /** False until the second player's first turn: then their whole opening hand is dealt, not only the 2 cards drawn. */
    private boolean secondHandDealt;
    /** Adventure: the AI that plays the second player (at the top). Null in a hotseat match. */
    private final AIController ai;
    /** Adventure: the pause between two AI moves, so each one can be seen. */
    private static final Duration AI_STEP = Duration.millis(1500);
    /** Adventure: how long the ENEMY TURN banner shows before the AI's first move. */
    private static final Duration AI_START = Duration.millis(2000);
    /** Adventure: an AI turn never has more moves than this. */
    private static final int AI_MAX_MOVES = 40;
    private int aiMoves;

    /** A hotseat match with test decks from CardRepository (the Adventure uses this); at the end, onEnd gets whether the first player won. */
    public MatchController(String boardName, HeroClass yours, HeroClass theirs, Consumer<Boolean> onEnd) {
        this(boardName, CardRepository.player("Kayne", yours, 0), CardRepository.player(theirs.name(), theirs, 0), onEnd);
    }

    /** A hotseat match between these two players (PvP: the decks they chose); at the end, onEnd gets whether the first player won. */
    public MatchController(String boardName, Player first, Player second, Consumer<Boolean> onEnd) {
        this(boardName, first, second, null, onEnd);
    }

    /**
     * difficulty null: a hotseat match (Oaths, clocks, pass veil, the board flips each turn).
     * Otherwise an Adventure battle: you (first) at the bottom against the AI (second) at the top,
     * with no Oath (so no clock), no pass veil, and the board never flips (GDD, Adventure).
     */
    public MatchController(String boardName, Player first, Player second, AIController.Difficulty difficulty, Consumer<Boolean> onEnd) {
        HeroClass yours = first.getHero().getHeroClass();
        HeroClass theirs = second.getHero().getHeroClass();
        // 1. The two players.
        this.first = first;
        this.second = second;

        // 2. The engine: shuffle and deal the opening hands, then the first player's turn 1 (Ki and a draw).
        engine = new GameEngine(first, second, new Random());
        engine.setCatalog(CardRepository.all());   // the cards that effects create or summon (Sendaiku, Sakana uri, Inari, Oiwa)
        recorder = new EffectRecorder(engine);
        engine.getBus().subscribe(GameEvent.Type.CARD_DRAWN, event -> cardsDrawnThisTurn++);
        engine.startMatch();   // the opening hands; turn 1 begins once both mulligans are done (startTurns)
        ai = difficulty == null ? null : new AIController(engine, second, difficulty, new Random());
        pauseClock();   // the engine starts the first player's clock: it waits until both Oaths are sworn

        // 3. The table, with each action plugged into the engine.
        board = new BoardView(boardName, second, HeroChoice.portrait(theirs), first, HeroChoice.portrait(yours));
        showClocks();
        // The play history (left side): every card played, attack and hero power, with their targets, yours and the AI's.
        PlayHistory history = board.getHistory();
        engine.getBus().subscribe(GameEvent.Type.TURN_STARTED, event -> history.turn(((Player) event.source()).getName()));
        engine.getBus().subscribe(GameEvent.Type.CARD_PLAYED, event -> history.played(((Player) event.source()).getName(), (Card) event.target(), aimFirst, aimSecond));
        engine.getBus().subscribe(GameEvent.Type.ATTACK, event -> history.attacked((Combatant) event.source(), (Combatant) event.target()));
        engine.getBus().subscribe(GameEvent.Type.HERO_POWER_USED, event -> history.heroPower(((Player) event.source()).getName(), aimFirst));
        board.setOnPlay(this);
        board.setOnAttack(this::attack);
        board.setOnPower(this::useHeroPower);
        board.setOnKatana(this::useKatana);
        board.setOnTrade(this::trade);
        board.setOnEndTurn(this::endTurn);
        board.setOnGameOver(() -> onEnd.accept(firstWon()));
        board.setOnSurrender(this::surrender);

        // A trap that goes off is shown to both players. During a play or an end of turn, the replay shows it in its turn instead.
        engine.getBus().subscribe(GameEvent.Type.TRAP_TRIGGERED, event -> {
            if (!recorder.recording()) {
                board.revealTrap((TrapCard) event.target());
            }
        });
        // A trap you set is shown big, then flips and glides to its place by your hero.
        // Adventure: the AI's traps stay hidden (only their face-down back shows by its hero).
        engine.getBus().subscribe(GameEvent.Type.TRAP_SET, event -> {
            if (ai == null || event.source() == first) {
                board.showTrapSet((TrapCard) event.target());
            }
        });
        // A clock timeout: remember who ran out and the damage, for the TIME'S UP animation once checkClock() returns.
        engine.getBus().subscribe(GameEvent.Type.CLOCK_TIMEOUT, event -> {
            timedOutHero = (Hero) event.target();
            timeoutDamage = event.amount();
        });

        // Curses, Rites and Shrines get a banner across the table.
        announceFamilyEvents();

        // Oath rewards, breaks and the end of Oathbreaker get a banner; "Swear again" re-swears the Oath.
        announceOathEvents();
        board.setOnReswear(this::reswear);

        // 4. Hotseat: each player swears an Oath then does the mulligan behind a pass veil; turn 1 then begins.
        //    Adventure: no Oath, the AI does its mulligan at once, then yours; turn 1 then begins.
        if (ai == null) {
            chooseOaths(yours, theirs);
        } else {
            engine.mulligan(second, ai.mulliganChoice());   // the AI sends back its expensive cards at once
            board.mulligan(first, cards -> engine.mulligan(first, cards), () -> {
                startTurns();
                board.dealHand();
            });
        }
    }

    /** The table to show on screen. */
    public BoardView getBoard() {
        return board;
    }

    /** Adventure: the bonuses picked during this run apply to you (engine.setUpgrades); called before the first turn. */
    public void setUpgrades(Set<Upgrade> upgrades) {
        engine.setUpgrades(first, upgrades);
        board.render();   // Inochi's 130 HP shows at once
    }


    /** Banner colours: the glow of each family's kanji. */
    private static final Color CURSE_RED = Color.web("#c41e1e");
    private static final Color PURIFIED = Color.web("#f3ede1");
    private static final Color RITE_CRIMSON = Color.web("#dc143c");
    private static final Color SHRINE_LAVENDER = Color.web("#b7a6dd");

    /** Curses, Rites and Shrines are announced across the table, so both players see what happened. */
    private void announceFamilyEvents() {
        engine.getBus().subscribe(GameEvent.Type.CURSE_GIVEN, event -> {
            CurseCard curse = (CurseCard) event.target();
            String where = "into the enemy's hand";
            if (curse.getZone() == CurseZone.DECK) {
                where = event.amount() + " shuffled into the enemy's deck";
            }
            board.announce("呪", curse.getNameRomaji(), where, CURSE_RED);
        });
        engine.getBus().subscribe(GameEvent.Type.CURSE_TRIGGERED, event -> {
            CurseCard curse = (CurseCard) event.target();
            board.announce("祟", curse.getNameRomaji(), "the curse strikes", CURSE_RED);
        });
        engine.getBus().subscribe(GameEvent.Type.CURSE_REMOVED, event -> {
            CurseCard curse = (CurseCard) event.target();
            board.announce("祓", curse.getNameRomaji(), "purified", PURIFIED);
        });
        engine.getBus().subscribe(GameEvent.Type.RITE_PERFORMED, event -> {
            RiteCard rite = (RiteCard) event.target();
            String rise = rite.getAncestors() + " Ancestors rise";
            if (rite.getAncestors() == 1) {
                rise = "1 Ancestor rises";
            }
            board.announce("儀", rite.getNameRomaji(), rise, RITE_CRIMSON);
        });
        engine.getBus().subscribe(GameEvent.Type.SHRINE_PLACED, event -> {
            Shrine shrine = (Shrine) event.target();
            board.announce("社", shrine.getSource().getNameRomaji(), "a shrine stands", SHRINE_LAVENDER);
        });
    }

    /** Gold for Oath rewards, deep red for a broken Oath. */
    private static final Color OATH_GOLD = Color.web("#ffd84a");
    private static final Color OATH_BROKEN_RED = Color.web("#8f1d1d");

    /** Oath rewards (3, 6, 9 stacks), breaks and the end of Oathbreaker are announced; a reward also stamps the Oath's scroll. */
    private void announceOathEvents() {
        engine.getBus().subscribe(GameEvent.Type.OATH_REWARD, event -> {
            Player player = (Player) event.source();
            Oath oath = (Oath) event.target();
            String name = oath.getType().getDisplayName();
            String reward = OathView.reward(player.getHero().getHeroClass(), name, event.amount());
            board.announce("誓", "Oath of " + name + " · " + event.amount(), reward, OATH_GOLD);
            board.stampOath(player);
        });
        engine.getBus().subscribe(GameEvent.Type.OATH_BROKEN, event -> {
            Player player = (Player) event.source();
            Oath oath = (Oath) event.target();
            HeroClass heroClass = player.getHero().getHeroClass();
            String name = oath.getType().getDisplayName();
            String penalty = OathView.penaltyName(heroClass, name);
            String effect = OathView.penaltyEffect(heroClass, name);
            String subtitle = "Oathbreaker · " + event.amount() + " damage";
            if (!effect.isEmpty()) {
                subtitle = "Oathbreaker · " + effect + " · " + event.amount() + " damage";
            }
            board.announce("破", penalty, subtitle, OATH_BROKEN_RED);
        });
        engine.getBus().subscribe(GameEvent.Type.OATH_RESTORED, event -> {
            Oath oath = (Oath) event.target();
            board.announce("誓", "Oathbreaker is over", "the Oath of " + oath.getType().getDisplayName() + " can be sworn again", OATH_GOLD);
        });
    }

    /** "Swear again" clicked: the engine re-swears the same Oath from 0 stacks, then the scroll is redrawn. */
    private void reswear() {
        engine.reswearOath(engine.active());
        board.render();
    }


    /** The player whose turn it is gives up: the other player wins at once and the clocks stop. */
    private void surrender() {
        // Adventure: you are the only one with a Surrender button, even during the AI's turn.
        Player quitter = engine.active();
        if (ai != null) {
            quitter = first;
        }
        engine.concede(quitter);

        
        clockTicker.stop();
    }

    /** True if the first player won (by a dead hero, a timeout or a surrender); false for a loss or a draw. */
    private boolean firstWon() {
        Player winner = engine.getState().getWinner().orElse(null);
        return winner == first;
    }

    /** Oath choice (GDD): the first player picks, the veil passes to the second, who picks; then the first player's hand is dealt. */
    private void chooseOaths(HeroClass yours, HeroClass theirs) {
        board.chooseOath(yours, firstOath -> {
            swear(first, firstOath);
            board.mulligan(first, firstCards -> engine.mulligan(first, firstCards), () -> {
                board.chooseOath(theirs, secondOath -> {
                    swear(second, secondOath);
                    board.mulligan(second, secondCards -> engine.mulligan(second, secondCards), () -> {
                        board.passTo(first.getName(), () -> {
                            startTurns();
                            resumeClock();          // both Oaths are sworn: the first player's clock starts now
                            clockTicker.start();
                            board.dealHand();
                        });
                    });
                });
                board.passTo(second.getName(), () -> {});
            });
        });
    }

    /** After both mulligans: the first player's turn 1 begins (Ki and a draw), then the table is redrawn. */
    private void startTurns() {
        engine.beginFirstTurn();
        board.render();
    }

    /** The engine records the Oath (it checks the rule at the end of each turn); the board shows its scroll. */
    private void swear(Player player, String oathName) {
        engine.swearOath(player, OathType.fromName(oathName));
        board.swear(player, oathName);
    }

    /** The characters a card from the hand may be aimed at: its Banzai's for a minion, its own for a spell. Empty: no target needed. */
    @Override
    public List<Combatant> targets(Card card) {
        if (card instanceof MinionCard) {
            return engine.banzaiTargets(card);
        }
        return engine.spellTargets(card);
    }

    /** Kettō picks two minions; every other aimed card picks one. */
    @Override
    public int picks(Card card) {
        if (engine.spellTarget(card) == SpellTarget.TWO_MINIONS) {
            return 2;
        }
        return 1;
    }

    /**
     * A card dropped on a slot, with its target(s) once picked: the engine checks the Ki, the slot and the targets,
     * plays it and runs its Banzai. A Curse the enemy gave you is removed instead (it pays its removal Ki).
     */
    @Override
    public boolean play(Card card, int slot, Combatant first, Combatant second) {
        recorder.start();
        boolean removal = card instanceof CurseCard held && engine.active().isHeldCurse(held);
        boolean played;
        if (removal) {
            played = engine.removeCurse((CurseCard) card);   // a Curse the enemy gave you: dropping it pays its removal Ki
        } else {
            aimFirst = first;   // for the play history
            aimSecond = second;
            played = engine.playCard(card, slot, first, second);
            aimFirst = null;
            aimSecond = null;
        }
        List<GameEvent> events = recorder.stop();
        if (!played) {
            board.render();   // refused: the card goes back to the hand at once
            return false;
        }
        if (!removal && !(card instanceof TrapCard)) {
            board.showPlayed(card, slot);   // on the board before its effects show (a Trap has its own animation)
        }
        // Cards the play put in your hand (Kiji, Noppera-bō, Tamashii-gui…) fly in from your deck once the effects have shown.
        int added = 0;
        for (GameEvent event : events) {
            if (event.type() == GameEvent.Type.CARD_ADDED && event.source() == engine.active()) {
                added++;
            }
        }
        int flying = added;
        board.replay(events, pauseBefore(card, events), EFFECT_SHOW, () -> board.dealDrawnCards(flying));
        return true;
    }

    /** How long the table waits before a played card's effects show: 0.3 s for a spell or a Banzai, 0.5 s more when a trap went off. */
    private static double pauseBefore(Card card, List<GameEvent> events) {
        double pause = PLACE_PAUSE;   // the card lands before anything else happens
        boolean spell = card instanceof SpellCard && !(card instanceof TrapCard);
        boolean banzai = card instanceof MinionCard minion && minion.hasKeyword(Keyword.BANZAI);
        if (spell || banzai) {
            pause = EFFECT_PAUSE;
        }
        return pause;
    }

    /** What a hand card costs now: the engine's price for the active player (Yōi, Oath discounts). */
    @Override
    public int cost(Card card) {
        return engine.costOf(engine.active(), card);
    }

    /** True if the engine would accept this card now. */
    @Override
    public boolean canPlay(Card card) {
        return engine.canPlay(card);
    }


    @Override
    public boolean powerUsed() {
        return engine.isHeroPowerUsed();
    }

    @Override
    public int nextFatigue(Player p) {
        return engine.nextFatigue(p);
    }

    /** The engine's bonus on this card's damage or healing number, for the active player. */
    @Override
    public int bonus(Card card) {
        return engine.bonusOf(engine.active(), card);
    }

    /** Who this attacker may hit, by the engine's rule. */
    @Override
    public List<Combatant> attackTargets(Combatant attacker) {
        return engine.legalTargets(attacker);
    }

    /** An attacker and its target: the engine deals the damage both ways and removes the dead. Returns what happened, for BoardView to replay. */
    private List<GameEvent> attack(Combatant attacker, Combatant target) {
        recorder.start();
        engine.attack(attacker, target);
        return recorder.stop();
    }

    /** Hero power with a target (Ninja Shuriken, Oni Jikininki) or without (Kannushi Miko: target is null). True if the engine accepted it. */
    private boolean useHeroPower(Combatant target) {
        recorder.start();
        aimFirst = target;   // for the play history
        boolean used = engine.useHeroPower(target);
        aimFirst = null;
        List<GameEvent> events = recorder.stop();
        if (used) {
            board.replay(events, POWER_PAUSE, EFFECT_SHOW, () -> {});   // the damage, a death, a trap, one step after the other
        }
        return used;
    }

    /** Samurai power: equip the chosen blade. True if the engine accepted it (enough Ki, not used yet this turn). */
    private boolean useKatana(KatanaTier tier) {
        return engine.useKatana(tier);
    }

    /** A hand card dropped on the deck: the engine shuffles it in and draws another (once per turn). True if it was accepted. */
    private boolean trade(Card card) {
        return engine.trade(card);
    }

    /**
     * End-turn switch: the engine runs the end-of-turn effects and checks the Oath (a stack, or a break),
     * then starts the next player's turn with its draw.
     */
    private void endTurn() {
        cardsDrawnThisTurn = 0;
        if (ai == null) {
            board.holdEffects();   // banners and stamps from the end of the turn wait for the pass veil
        }
        recorder.start();
        engine.endTurn();
        // End- and start-of-turn effects show on the table (0.75 s at most) before the turn passes.
        List<GameEvent> events = recorder.stop();
        for (GameEvent e : events) {
            if (e.type() == GameEvent.Type.CARD_PLAYED && e.target() instanceof Card card) {
                board.showAiPlayed(card);   // Yamata no Orochi's dragon, arriving on its own at the start of his turn
            }
        }
        board.replay(events, 0, TURN_SHOW, this::afterTurnEnded);
    }

    /**
     * What the table does once the engine has ended a turn (End turn pressed, or a clock timeout):
     * flip the board to the next player and hide it behind the pass veil. Their clock waits until they click.
     */
    private void afterTurnEnded() {
        // A broken Oath can kill its own hero: then the match is over and there is no pass veil.
        if (engine.getState().isOver()) {
            board.render();
            return;
        }

        // Adventure: no flip and no veil, the AI plays its turn at the top.
        if (ai != null) {
            board.render();
            aiMoves = 0;
            board.announce("敵の番", "ENEMY TURN", second.getName(), CURSE_RED);
            nextAiStep(AI_START);
            return;
        }

        // Flip the board so the next player sits at the bottom, then hide it until they click.
        board.swapSides();
        pauseClock();
        int drawn = cardsDrawnThisTurn;   // kept now: the clock ticks reset the counter while the veil is up
        board.passTo(engine.active().getName(), () -> {
            resumeClock();
            if (!secondHandDealt) {
                // The second player's first turn: their opening hand and the cards they just drew all fly in.
                secondHandDealt = true;
                board.dealHand();
            } else if (drawn > 0) {
                board.dealDrawnCards(drawn);   // the cards they drew fly in from their deck
            }
        });
    }

    /** Adventure: the next AI move comes after a short pause, so you can follow the enemy's turn. */
    private void nextAiStep() {
        nextAiStep(AI_STEP);
    }

    private void nextAiStep(Duration pause) {
        PauseTransition wait = new PauseTransition(pause);
        wait.setOnFinished(event -> aiStep());
        wait.play();
    }

    /** Adventure: the AI plays one move and the board redraws; when it has nothing left to play, its turn ends. */
    private void aiStep() {
        if (engine.getState().isOver()) {
            board.render();
            return;
        }
        Optional<Move> move = Optional.empty();
        if (aiMoves < AI_MAX_MOVES) {
            move = ai.nextMove();
        }
        if (move.isEmpty()) {
            endAiTurn();
            return;
        }
        aiMoves++;
        if (move.get().kind() == Move.Kind.ATTACK) {
            // The attack plays with its animation (lunge, slash, damage), then the next move comes.
            board.aiAttack(move.get().attacker(), move.get().target(), this::nextAiStep);
            return;
        }
        recorder.start();
        aimFirst = move.get().target();   // for the play history
        aimSecond = move.get().second();
        ai.play(move.get());
        aimFirst = null;
        aimSecond = null;
        if (move.get().kind() == Move.Kind.PLAY) {
            board.showAiPlayed(move.get().card());   // its card big on the left and its minion in its slot, before the effects
        }
        List<GameEvent> events = recorder.stop();
        board.replay(events, EFFECT_SHOW, EFFECT_SHOW, this::nextAiStep);   // the card stays readable, then its effects one by one
    }

    /** Adventure: the engine ends the AI's turn and starts yours: the cards you draw fly in, End turn works again. */
    private void endAiTurn() {
        cardsDrawnThisTurn = 0;
        engine.endTurn();
        if (engine.getState().isOver()) {
            board.render();
            return;
        }
        board.yourTurn();
        if (cardsDrawnThisTurn > 0) {
            board.dealDrawnCards(cardsDrawnThisTurn);
        }
    }

    /**
     * Every ClockTicker.TICK: the engine checks the active player's bank (a timeout hits the hero and ends the turn),
     * then both times are redrawn. On a timeout the TIME'S UP animation plays, then the pass veil.
     */
    private void tickClock() {
        if (engine.getState().isOver()) {
            clockTicker.stop();
            return;
        }
        boolean timedOut = engine.checkClock();
        showClocks();
        if (!timedOut) {
            return;
        }
        // Timeout: half HP and +30 s, the turn goes on. The clock waits during the TIME'S UP animation.
        clockTicker.stop();
        pauseClock();
        board.showTimeout(timedOutHero, timeoutDamage, () -> {
            if (engine.getState().isOver()) {
                return;   // the timeout killed the hero: showTimeout already shows the game over
            }
            resumeClock();
            board.render();
            clockTicker.start();
        });
    }

    /** Both chess clocks as m:ss. The active player always sits at the bottom, so the bottom clock is theirs. */
    private void showClocks() {
        if (!engine.clocksEnabled()) {
            board.hideClocks();   // no clock yet (Oaths not sworn) or none at all (Adventure)
            return;
        }
        Player active = engine.active();
        Player waiting = second;
        if (active == second) {
            waiting = first;
        }
        String yours = ClockTicker.format(engine.remainingMs(active));
        String theirs = ClockTicker.format(engine.remainingMs(waiting));
        boolean hurry = engine.remainingMs(active) < HURRY_MS;   // the bottom clock turns red and ticks
        board.showClocks(yours, theirs, true, hurry);
    }

    /** Stops the active player's clock while the table is hidden (Oath choice, pass veil), on the engine's own time. */
    private void pauseClock() {
        engine.pauseClock();
    }

    /** Starts the active player's clock again once they clicked the pass veil. */
    private void resumeClock() {
        engine.resumeClock();
    }
}