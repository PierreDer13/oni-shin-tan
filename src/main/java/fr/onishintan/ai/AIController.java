package fr.onishintan.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.onishintan.engine.GameEngine;
import fr.onishintan.engine.SpellTarget;
import fr.onishintan.model.BoardSide;
import fr.onishintan.model.Card;
import fr.onishintan.model.Combatant;
import fr.onishintan.model.CurseCard;
import fr.onishintan.model.Hero;
import fr.onishintan.model.HeroClass;
import fr.onishintan.model.KatanaTier;
import fr.onishintan.model.Keyword;
import fr.onishintan.model.Minion;
import fr.onishintan.model.MinionCard;
import fr.onishintan.model.Player;
import fr.onishintan.model.RiteCard;
import fr.onishintan.model.ShrineCard;
import fr.onishintan.model.Status;
import fr.onishintan.model.BoardSide;

/**
 * The Adventure opponent (GDD, Adventure): plays its turn through GameEngine only, like a player would.
 *
 * Greedy, one move at a time: every legal move gets a score and the best one is played, until none is worth it.
 * Order of a turn: lethal on the hero if it can (HARD), cards from the most expensive it can pay, the hero power
 * with the Ki left, then attacks (good trades first, the hero when no trade is worth more).
 * EASY makes a careless move about one time in three (a random attack, a card on a random target) and
 * sometimes hits the hero instead of trading; NORMAL one time in ten; HARD never does, checks lethal and removes the Curses it holds.
 * When its board is full or its deck is empty, it accepts even trades so that a match always ends.
 *
 * The UI animates each move: nextMove(), animate, play(move), and endTurn when nextMove() is empty.
 * playTurn() does all of that at once (tests, or a match without animations).
 */
public final class AIController {

    public enum Difficulty {
        EASY(0.3), NORMAL(0.1), HARD(0),
        /** Yamata no Orochi only: HARD, plus it goes for the biggest threats first. forBattle() never gives it. */
        EXTREME(0);

        /** How often a random legal move replaces the best one. */
        final double mistakes;

        Difficulty(double mistakes) { this.mistakes = mistakes; }

        /** The Adventure gets harder battle by battle: Town EASY, Ocean NORMAL, Mount Fuji HARD (never EXTREME). */
        public static Difficulty forBattle(int battle) {
            return values()[Math.max(0, Math.min(battle, HARD.ordinal()))];
        }

        /** True for HARD and EXTREME: no mistakes, looks for lethal, removes its Curses. */
        public boolean atLeastHard() { return compareTo(HARD) >= 0; }
    }

    /** Safety net: a turn never has more moves than this. */
    static final int MAX_MOVES = 40;
    private static final Pattern DAMAGE = Pattern.compile("(\\d+) damage");
    /** Words of a card text that aim it at a friendly character (heal, buffs, keywords given). */
    private static final Pattern FRIENDLY = Pattern.compile("(?i)\\b(heal|friendly|give|armour|torii|shinobi)\\b");

    private final GameEngine engine;
    private final Player me;
    private final Difficulty difficulty;
    private final Random rng;
    /** Moves the engine refused this turn: never tried again before the next turn. */
    private final Set<Object> refused = new HashSet<>();
    private int refusedRound = -1;

    public AIController(GameEngine engine, Player me, Difficulty difficulty, Random rng) {
        this.engine = engine;
        this.me = me;
        this.difficulty = difficulty;
        this.rng = rng;
    }

    public Difficulty getDifficulty() { return difficulty; }

    /** True when it is the AI's turn and the match is not over. */
    public boolean isMyTurn() {
        return !engine.getState().isOver() && engine.active() == me;
    }

    /** Plays the whole turn, then ends it. Returns the moves the engine accepted, in order. */
    public List<Move> playTurn() {
        List<Move> done = new ArrayList<>();
        for (int i = 0; i < MAX_MOVES && isMyTurn(); i++) {
            Optional<Move> move = nextMove();
            if (move.isEmpty()) break;
            if (play(move.get())) done.add(move.get());
        }
        if (isMyTurn()) engine.endTurn();
        return done;
    }

    /** Cards above this Ki go back at the mulligan. */
    public static final int MULLIGAN_MAX_KI = 4;

    /** The AI's mulligan: it sends back every opening card costing more than 4 Ki. Call engine.mulligan(me, this). */
    public List<Card> mulliganChoice() {
        List<Card> back = new ArrayList<>();
        for (Card c : me.getHand()) if (engine.costOf(me, c) > MULLIGAN_MAX_KI) back.add(c);
        return back;
    }

    
    /** The move to play now, or empty when the AI should end its turn. */
    public Optional<Move> nextMove() {
        if (!isMyTurn()) return Optional.empty();
        if (engine.getState().getRound() != refusedRound) {
            refused.clear();
            refusedRound = engine.getState().getRound();
        }
        List<Scored> moves = legalMoves();
        if (moves.isEmpty()) return Optional.empty();
        if (rng.nextDouble() < difficulty.mistakes) return mistake();
        return best();
    }

    /** The best-scored move, or empty when none is worth playing. */
    private Optional<Move> best() {
        List<Scored> moves = legalMoves();
        if (moves.isEmpty()) return Optional.empty();
        Scored best = moves.stream().max(Comparator.comparingDouble(Scored::score)).get();
        return best.score > 0 ? Optional.of(best.move) : Optional.empty();
    }

    /** Hands the move to the engine. A refused move is not proposed again this turn. */
    public boolean play(Move m) {
        boolean ok = switch (m.kind()) {
            case PLAY -> m.card() instanceof CurseCard c && me.isHeldCurse(c)
                    ? engine.removeCurse(c)
                    : engine.playCard(m.card(), m.slot(), m.target(), m.second());
            case ATTACK -> engine.attack(m.attacker(), m.target());
            case POWER -> engine.useHeroPower(m.target());
            case KATANA -> engine.useKatana(m.tier());
        };
        if (!ok) refused.add(key(m));
        return ok;
    }

    /**
     * A careless move: a random attack on a random legal target, or a random playable card on a random target.
     * Never an early end of turn: the AI always gets to its attacks. Empty only when nothing at all can be played.
     */
    private Optional<Move> mistake() {
        if (rng.nextBoolean()) {
            List<Combatant> attackers = attackers();
            if (!attackers.isEmpty()) {
                Combatant a = attackers.get(rng.nextInt(attackers.size()));
                List<Combatant> legal = engine.legalTargets(a);
                if (!legal.isEmpty()) return Optional.of(Move.attack(a, legal.get(rng.nextInt(legal.size()))));
            }
        }
        List<Card> playable = me.getHand().stream().filter(engine::canPlay).toList();
        if (playable.isEmpty()) return best();
        Card card = playable.get(rng.nextInt(playable.size()));
        Scored s = cardMove(card);
        if (s == null) return Optional.empty();
        List<Combatant> targets = card instanceof MinionCard ? engine.banzaiTargets(card) : engine.spellTargets(card);
        Move m = s.move;
        if (!targets.isEmpty() && engine.spellTarget(card) != SpellTarget.TWO_MINIONS) {
            m = Move.play(card, m.slot(), targets.get(rng.nextInt(targets.size())), null);   // any target, even its own minion
        }
        return Optional.of(m);
    }

    // ---------- the moves and their scores ----------

    private record Scored(Move move, double score) {}

    /** Every move the engine should accept now, with its score (higher = better, 0 or less = not worth it). */
    List<Scored> legalMoves() {
        List<Scored> out = new ArrayList<>();
        boolean lethal = difficulty.atLeastHard() && lethalOnHero();
        for (Card card : List.copyOf(me.getHand())) {
            Scored s = cardMove(card);
            if (s != null && !lethal) out.add(s);
        }
        Scored power = powerMove();
        if (power != null && !lethal) out.add(power);
        for (Combatant attacker : attackers()) {
            Scored s = attackMove(attacker, lethal);
            if (s != null) out.add(s);
        }
        out.removeIf(s -> refused.contains(key(s.move)));
        return out;
    }

    /** Playing (or, for a held Curse, removing) this card, aimed at its best target. Null if it cannot be played. */
    private Scored cardMove(Card card) {
        if (card instanceof CurseCard c && me.isHeldCurse(c)) {
            if (!difficulty.atLeastHard() || !me.canAfford(c.getRemovalCost())) return null;
            return new Scored(Move.play(card, -1, null, null), 1 + c.getRemovalCost());
        }
        if (!engine.canPlay(card)) return null;
        int slot = -1;
        if (card instanceof MinionCard || card instanceof ShrineCard || card instanceof RiteCard) {
            slot = centreSlot();
            if (slot < 0) return null;
        }
        List<Combatant> targets = card instanceof MinionCard ? engine.banzaiTargets(card) : engine.spellTargets(card);
        Combatant target = null, second = null;
        if (engine.spellTarget(card) == SpellTarget.TWO_MINIONS) {
            List<Combatant> enemies = enemiesFirst(targets);
            if (enemies.size() < 2) return null;
            target = enemies.get(0);
            second = enemies.get(1);
        } else if (!targets.isEmpty()) {
            target = bestTarget(card, targets);
        }
        double score = 10 + engine.costOf(me, card) * 3 + targetBonus(card, target);
        return new Scored(Move.play(card, slot, target, second), score);
    }


    /** Where the AI puts a minion, Shrine or Rite: the free slot nearest the middle (3, then 2, 4, 1, 5, 0, 6); -1 if full. */
    private int centreSlot() {
        int middle = BoardSide.MAX / 2;
        for (int d = 0; d <= middle; d++) {
            if (me.getSide().isFree(middle - d)) return middle - d;
            if (middle + d < BoardSide.MAX && me.getSide().isFree(middle + d)) return middle + d;
        }
        return -1;
    }    

    /** The hero power with the Ki left once the cards are paid: after every card (score below any card). */
    private Scored powerMove() {
        if (engine.isHeroPowerUsed()) return null;
        HeroClass c = me.getHero().getHeroClass();
        if (c == HeroClass.SAMURAI) {
            KatanaTier best = null;
            for (KatanaTier tier : KatanaTier.values()) if (me.canAfford(tier.getKiCost())) best = tier;
            if (best == null || me.getHero().getWeapon() != null) return null;
            return new Scored(Move.katana(best), 5 + best.getAttack() / 10.0);
        }
        if (!me.canAfford(GameEngine.HERO_POWER_COST)) return null;
        if (c == HeroClass.KANNUSHI) {
            return me.getSide().firstFree() < 0 ? null : new Scored(Move.power(null), 5);
        }
        int damage = c == HeroClass.NINJA ? GameEngine.SHURIKEN_DAMAGE : GameEngine.JIKININKI_DRAIN;
        Combatant target = enemy().getHero();
        double best = 0;
        for (Minion m : enemy().getSide().getMinions()) {
            if (m.isHidden() || m.hasTorii()) continue;
            if (m.getHealth() <= damage && value(m) > best) { best = value(m); target = m; }
        }
        if (target instanceof Hero h && h.hasStatus(Status.STEALTH)) return null;
        return new Scored(Move.power(target), 5);
    }

    /** This attacker's best attack: a minion it kills (best if it survives), or the hero. Null if none is worth it. */
    private Scored attackMove(Combatant attacker, boolean lethal) {
        List<Combatant> legal = engine.legalTargets(attacker);
        if (legal.isEmpty()) return null;
        Hero hero = enemy().getHero();
        if (legal.contains(hero) && (lethal || difficulty == Difficulty.EASY && rng.nextBoolean())) {
            return new Scored(Move.attack(attacker, hero), 100 + attacker.getAttack());
        }
        Combatant best = null;
        double bestScore = 0;
        for (Combatant t : legal) {
            double s = t == hero ? attacker.getAttack() * 0.8 : tradeScore(attacker, (Minion) t);
            if (best == null || s > bestScore) { best = t; bestScore = s; }
        }
        if (bestScore <= 0) return null;
        return new Scored(Move.attack(attacker, best), 1 + bestScore / 100.0);
    }

    /** Attacks score below every card and the power, so cards come first; among attacks, the best trade first. */
    private double tradeScore(Combatant attacker, Minion t) {
        boolean kills = !t.hasTorii() && attacker.getAttack() >= t.getHealth();
        boolean dies = !(attacker instanceof Minion a && a.hasTorii()) && t.getAttack() >= attacker.getHealth();
        double mine = attacker instanceof Minion a ? value(a) : 0;
        if (attacker instanceof Hero) dies = false;   // the hero survives the hit (its HP is the risk, not a card)
        double score;
        if (kills && !dies) score = value(t) * 2;
        else if (kills) score = value(t) - mine;
        else if (t.hasTorii()) score = dies ? 2 - mine : 3;   // the hit takes one Torii off: the next one gets through
        else score = dies ? -mine : t.getHealth() < t.getMaxHealth() || attacker.getAttack() > 0 ? 1 : 0;
        if (kills && difficulty == Difficulty.EXTREME) score += t.getAttack() * 2;   // the biggest hitters die first
        return score + pressure();
    }

    /**
     * Pushes the AI to trade when waiting gets it nothing: its board is full (no room for its cards)
     * or its deck is empty. Without it, two full boards of Shirodu and Torii would wait forever.
     */
    private double pressure() {
        double p = 0;
        if (me.getSide().isFull()) p += 6;
        if (me.getDeck().isEmpty()) p += 6;
        return p;
    }

    // ---------- targets ----------

    /** A spell or Banzai target: a friendly one for heals and gifts, else the enemy it kills (best value), else the biggest enemy. */
    private Combatant bestTarget(Card card, List<Combatant> targets) {
        boolean friendly = engine.spellTarget(card) == SpellTarget.FRIENDLY_MINION
                || engine.banzaiTarget(card) == SpellTarget.FRIENDLY_MINION
                || FRIENDLY.matcher(text(card)).find();
        List<Combatant> mine = targets.stream().filter(this::isMine).toList();
        List<Combatant> theirs = targets.stream().filter(t -> !isMine(t)).toList();
        if (friendly && !mine.isEmpty()) {
            boolean heal = text(card).toLowerCase().contains("heal");
            return mine.stream().max(Comparator.comparingDouble(t -> heal ? t.getMaxHealth() - t.getHealth() : value(t))).get();
        }
        List<Combatant> pool = theirs.isEmpty() ? targets : theirs;
        int damage = damageOf(card);
        Optional<Combatant> killed = pool.stream().filter(t -> damage > 0 && t.getHealth() <= damage && !(t instanceof Hero))
                .max(Comparator.comparingDouble(this::value));
        return killed.orElseGet(() -> pool.stream().max(Comparator.comparingDouble(this::value)).get());
    }

    private double targetBonus(Card card, Combatant target) {
        if (target == null) return 0;
        if (isMine(target)) return 1;
        int damage = damageOf(card);
        return damage > 0 && target.getHealth() <= damage ? value(target) : value(target) / 4;
    }

    /** Enemy minions first, biggest first (Kettō on two enemy minions removes one of them). */
    private List<Combatant> enemiesFirst(List<Combatant> targets) {
        return targets.stream().filter(t -> !isMine(t)).sorted(Comparator.comparingDouble(this::value).reversed()).toList();
    }

    /** HARD: the attackers can bring the enemy hero to 0 this turn, and nothing guards it. */
    private boolean lethalOnHero() {
        Hero hero = enemy().getHero();
        int sum = 0;
        for (Combatant a : attackers()) {
            if (!engine.legalTargets(a).contains(hero)) return false;
            sum += a.getAttack();
        }
        return sum > 0 && sum >= hero.getHealth() + hero.getArmour();
    }

    // ---------- helpers ----------

    private List<Combatant> attackers() {
        List<Combatant> out = new ArrayList<>();
        for (Minion m : me.getSide().getMinions()) if (m.canAttack() && m.getAttack() > 0) out.add(m);
        if (me.getHero().canAttack() && me.getHero().getAttack() > 0) out.add(me.getHero());
        out.sort(Comparator.comparingInt(Combatant::getAttack).reversed());
        return out;
    }

    /** How much a character is worth on the board: its stats, plus 2 per keyword. */
    private double value(Combatant c) {
        if (c instanceof Hero) return 30;
        Minion m = (Minion) c;
        return m.getAttack() + m.getHealth() + 2 * m.getKeywords().size() + (m.hasKeyword(Keyword.SHIRODU) ? 2 : 0);
    }

    /** The damage written on the card, with the bonus the engine adds now (Ryu, Havoc); 0 when it deals none. */
    private int damageOf(Card card) {
        Matcher m = DAMAGE.matcher(text(card));
        return m.find() ? Integer.parseInt(m.group(1)) + engine.bonusOf(me, card) : 0;
    }

    private boolean isMine(Combatant c) {
        return c == me.getHero() || me.getSide().getMinions().contains(c);
    }

    private Player enemy() {
        return engine.getState().opponentOf(me);
    }

    private static String text(Card card) {
        return card.getEffectText() == null ? "" : card.getEffectText();
    }

    /** What makes two moves the same for the refused list: the card or the attacker, and the kind. */
    private static Object key(Move m) {
        return List.of(m.kind(), m.card() != null ? m.card() : m.attacker() != null ? m.attacker() : String.valueOf(m.tier()));
    }
}