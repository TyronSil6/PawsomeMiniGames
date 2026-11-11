package com.example.pawsomeminigames.sakurabrawl;

/** The rules of the match: rounds, timer, winners. No drawing happens here. */
public class GameManager {

    public enum State { COUNTDOWN, PLAYING, ROUND_END, GAME_OVER }

    /** GameView implements this to reset positions and set NPC difficulty. */
    public interface Listener {
        void onNewRound(int round);
    }

    public static final int NONE = 0;
    public static final int PLAYER = 1;
    public static final int NPC = 2;

    /** Rounds in a normal match. A match ends early once someone has WINS_NEEDED wins. */
    public static final int TOTAL_ROUNDS = 7;
    public static final int WINS_NEEDED = 4;
    public static final float COUNTDOWN_TIME = 3.5f;
    public static final float ROUND_TIME = 30f;
    public static final float ROUND_END_TIME = 3f;
    /** The ring starts closing when this many seconds are left. */
    public static final float SHRINK_START = 20f;

    private final Listener listener;

    private State state = State.COUNTDOWN;
    private int round = 1;
    private float timeLeft = ROUND_TIME;
    private float stateTimer = COUNTDOWN_TIME;
    private int roundWinner = NONE;
    private int playerWins = 0;
    private int npcWins = 0;
    private int maxPlayerDeficit = 0;   // biggest gap the player was ever behind by
    private int maxNpcDeficit = 0;

    public GameManager(Listener listener) {
        this.listener = listener;
    }

    /** Starts a brand new match from round 1. */
    public void startGame() {
        round = 1;
        playerWins = 0;
        npcWins = 0;
        maxPlayerDeficit = 0;
        maxNpcDeficit = 0;
        startRound();
    }

    private void startRound() {
        state = State.COUNTDOWN;
        stateTimer = COUNTDOWN_TIME;
        timeLeft = ROUND_TIME;
        roundWinner = NONE;
        if (listener != null) listener.onNewRound(round);
    }

    public void update(float dt) {
        switch (state) {
            case COUNTDOWN:
                stateTimer -= dt;
                if (stateTimer <= 0f) {
                    state = State.PLAYING;
                }
                break;

            case PLAYING:
                timeLeft = Math.max(0f, timeLeft - dt);
                break;

            case ROUND_END:
                stateTimer -= dt;
                if (stateTimer <= 0f) {
                    if (isMatchDecided()) {
                        state = State.GAME_OVER;
                        stateTimer = 0f;
                    } else {
                        round++;
                        startRound();
                    }
                }
                break;

            case GAME_OVER:
                stateTimer += dt; // counts up, used for the restart delay
                break;
        }
    }

    /**
     * Call every frame while playing.
     * edge: 0 = ring center, 1 = ring edge, above 1 = outside the ring.
     */
    public void checkRound(float playerEdge, float npcEdge) {
        if (state != State.PLAYING) return;

        boolean playerOut = playerEdge > 1f;
        boolean npcOut = npcEdge > 1f;

        if (playerOut || npcOut) {
            if (playerOut && npcOut) {
                // Both out: the one further outside loses
                endRound(playerEdge > npcEdge ? NPC : PLAYER);
            } else {
                endRound(playerOut ? NPC : PLAYER);
            }
            return;
        }

        if (timeLeft <= 0f) {
            // Time is up: the one closer to the center has the advantage
            if (Math.abs(playerEdge - npcEdge) < 0.02f) {
                endRound(NONE);
            } else {
                endRound(playerEdge < npcEdge ? PLAYER : NPC);
            }
        }
    }

    private void endRound(int winner) {
        roundWinner = winner;
        if (winner == PLAYER) playerWins++;
        if (winner == NPC) npcWins++;
        maxPlayerDeficit = Math.max(maxPlayerDeficit, npcWins - playerWins);
        maxNpcDeficit = Math.max(maxNpcDeficit, playerWins - npcWins);
        state = State.ROUND_END;
        stateTimer = ROUND_END_TIME;
    }

    /**
     * The match ends when someone reaches WINS_NEEDED, or after the last round if one side
     * leads. If it is still tied after the last round, extra sudden-death rounds are played.
     */
    private boolean isMatchDecided() {
        if (playerWins >= WINS_NEEDED || npcWins >= WINS_NEEDED) return true;
        return round >= TOTAL_ROUNDS && playerWins != npcWins;
    }

    /** True when the next round win would end the match for either side. */
    public boolean isMatchPoint() {
        return state != State.GAME_OVER
                && (playerWins == WINS_NEEDED - 1 || npcWins == WINS_NEEDED - 1);
    }

    /** True if the match winner was once at least 2 rounds behind (only meaningful in GAME_OVER). */
    public boolean isComeback() {
        int w = getMatchWinner();
        return (w == PLAYER && maxPlayerDeficit >= 2) || (w == NPC && maxNpcDeficit >= 2);
    }

    // ---- Getters used by the drawing code ----

    public State getState() { return state; }
    public boolean isPlaying() { return state == State.PLAYING; }
    public int getRound() { return round; }
    public int getRoundWinner() { return roundWinner; }
    public int getPlayerWins() { return playerWins; }
    public int getNpcWins() { return npcWins; }

    public float getTimeLeft() { return timeLeft; }

    /** 0 = ring at full size, 1 = ring fully closed in. Stays put after the round ends. */
    public float getShrinkProgress() {
        float p = (SHRINK_START - timeLeft) / SHRINK_START;
        return Math.max(0f, Math.min(1f, p));
    }

    public int getTimeSeconds() {
        return (int) Math.ceil(timeLeft);
    }

    public String getRoundLabel() {
        if (round > TOTAL_ROUNDS) return "SUDDEN DEATH";
        return round == TOTAL_ROUNDS ? "FINAL ROUND" : "ROUND " + round;
    }

    /** "3", "2", "1" and then "FIGHT!" during the countdown. */
    public String getCountdownText() {
        if (stateTimer > 0.5f) {
            return String.valueOf((int) Math.ceil(stateTimer - 0.5f));
        }
        return "FIGHT!";
    }

    /** Who won the whole match (only meaningful in GAME_OVER). */
    public int getMatchWinner() {
        if (playerWins > npcWins) return PLAYER;
        if (npcWins > playerWins) return NPC;
        return NONE;
    }

    /** Short delay so rapid tapping on the last round doesn't skip the result screen. */
    public boolean canRestart() {
        return state == State.GAME_OVER && stateTimer > 1f;
    }
}