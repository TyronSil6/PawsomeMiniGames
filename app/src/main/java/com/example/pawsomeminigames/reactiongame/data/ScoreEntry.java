package com.example.pawsomeminigames.reactiongame.data;

public final class ScoreEntry {
    public final int score;
    public final long timeMillis;

    public ScoreEntry(int score, long timeMillis) {
        this.score = score;
        this.timeMillis = timeMillis;
    }
}
