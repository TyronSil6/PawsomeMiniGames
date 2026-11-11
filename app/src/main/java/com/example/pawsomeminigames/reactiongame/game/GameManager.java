package com.example.pawsomeminigames.reactiongame.game;

import java.util.Random;

public final class GameManager {
    public static final int TILE_COUNT = 4;

    public static final int POINTS_PER_CORRECT = 1;

    public enum AnswerResult { CORRECT, WRONG, IGNORED }

    private final Random rng;
    private GameState state = GameState.IDLE;
    private GameState stateBeforePause = GameState.IDLE;
    private int score;
    private int roundNumber;
    private Round round;
    private GameOverReason reason = GameOverReason.NONE;
    private Tile lastTappedTile;

    public GameManager(long seed) {
        this.rng = new Random(seed);
    }

    public void startNewRun() {
        state = GameState.IDLE;
        stateBeforePause = GameState.IDLE;
        score = 0;
        roundNumber = 0;
        round = null;
        reason = GameOverReason.NONE;
        lastTappedTile = null;
    }

    public Round beginRound() {
        if (state != GameState.IDLE && state != GameState.RESOLVING) {
            throw new IllegalStateException("Cannot begin a round while " + state);
        }
        roundNumber++;
        round = com.example.pawsomeminigames.reactiongame.game.RoundFactory.create(rng, score, roundNumber);
        state = GameState.PLAYING;
        return round;
    }

    public AnswerResult submitAnswer(int tileIndex) {
        if (state != GameState.PLAYING || round == null) return AnswerResult.IGNORED;
        if (tileIndex < 0 || tileIndex >= round.size()) return AnswerResult.IGNORED;
        Tile tapped = round.getTile(tileIndex);
        lastTappedTile = tapped;
        if (tapped.matches(round.cueAnimal, round.cueColor)) {
            score += POINTS_PER_CORRECT;
            state = GameState.RESOLVING;
            return AnswerResult.CORRECT;
        }

        reason = GameOverReason.WRONG_TILE;
        state = GameState.GAME_OVER;
        return AnswerResult.WRONG;
    }

    public boolean timeUp() {
        if (state != GameState.PLAYING) return false;
        reason = GameOverReason.TIMEOUT;
        lastTappedTile = null;
        state = GameState.GAME_OVER;
        return true;
    }

    public boolean shuffleCurrentRound() {
        if (state != GameState.PLAYING || round == null) return false;
        round.shuffleTiles(rng);
        return true;
    }

    public boolean pause() {
        if (state == GameState.GAME_OVER || state == GameState.PAUSED) return false;
        stateBeforePause = state;
        state = GameState.PAUSED;
        return true;
    }

    public GameState resume() {
        if (state != GameState.PAUSED) return null;
        state = stateBeforePause;
        return state;
    }

    public GameState getState() { return state; }
    public int getScore() { return score; }
    public int getRoundNumber() { return roundNumber; }
    public Round getRound() { return round; }
    public GameOverReason getReason() { return reason; }

    public Tile getLastTappedTile() { return lastTappedTile; }
    public boolean isGameOver() { return state == GameState.GAME_OVER; }
}
