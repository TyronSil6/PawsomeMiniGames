package com.example.pawsomeminigames.reactiongame.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class GameManagerTest {
    @Test
    public void everyRoundHasExactlyOneCorrectTileAndNoDuplicates() {
        int[] scores = {0, 2, 3, 7, 8, 13, 14, 21, 22, 60};
        Random seeds = new Random(1);
        for (int score : scores) {
            for (int i = 0; i < 5000; i++) {
                Round r = com.example.pawsomeminigames.reactiongame.game.RoundFactory.create(new Random(seeds.nextLong()), score, i + 1);
                assertEquals(1, r.countCorrect());
                Set<String> unique = new HashSet<>();
                for (int k = 0; k < r.size(); k++) {
                    Tile t = r.getTile(k);
                    unique.add(t.animal + "/" + t.color);
                    assertEquals(t.matches(r.cueAnimal, r.cueColor), t.role == TileRole.CORRECT);
                }
                assertEquals(4, unique.size());
            }
        }
    }

    @Test
    public void decoysAppearFromTierOneAndBothFromTierThree() {
        Random rng = new Random(3);
        for (int i = 0; i < 500; i++) {
            Round warmUp = com.example.pawsomeminigames.reactiongame.game.RoundFactory.create(rng, 0, 2);
            Round tier3 = com.example.pawsomeminigames.reactiongame.game.RoundFactory.create(rng, 14, 20);
            int decoysWarmUp = 0, animalTwins = 0, colorTwins = 0;
            for (int k = 0; k < 4; k++) {
                if (warmUp.getTile(k).role.isDecoy()) decoysWarmUp++;
                if (tier3.getTile(k).role == TileRole.ANIMAL_TWIN) animalTwins++;
                if (tier3.getTile(k).role == TileRole.COLOR_TWIN) colorTwins++;
            }
            assertEquals(0, decoysWarmUp);
            assertEquals(1, animalTwins);
            assertEquals(1, colorTwins);
        }
    }

    @Test
    public void roundTimeShrinksButNeverBelowFloor() {
        assertTrue(com.example.pawsomeminigames.reactiongame.game.RoundFactory.timeLimitMs(0, 2) > com.example.pawsomeminigames.reactiongame.game.RoundFactory.timeLimitMs(10, 12));
        assertEquals(1500L, com.example.pawsomeminigames.reactiongame.game.RoundFactory.timeLimitMs(500, 80));
    }

    @Test
    public void correctTapScoresOnceAndSecondTapIsIgnored() {
        GameManager g = new GameManager(5);
        g.startNewRun();
        Round r = g.beginRound();
        int ci = r.getCorrectIndex();
        assertEquals(GameManager.AnswerResult.CORRECT, g.submitAnswer(ci));
        assertEquals(GameManager.POINTS_PER_CORRECT, g.getScore());
        assertEquals(GameManager.AnswerResult.IGNORED, g.submitAnswer(ci));
        assertEquals(GameManager.POINTS_PER_CORRECT, g.getScore());
    }

    @Test
    public void wrongTapEndsTheRunImmediatelyAndOnlyOnce() {
        GameManager g = new GameManager(9);
        g.startNewRun();
        Round r = g.beginRound();
        int wrong = (r.getCorrectIndex() + 1) % 4;
        assertEquals(GameManager.AnswerResult.WRONG, g.submitAnswer(wrong));
        assertTrue(g.isGameOver());
        assertEquals(GameOverReason.WRONG_TILE, g.getReason());
        assertFalse(g.timeUp());
        assertEquals(GameManager.AnswerResult.IGNORED, g.submitAnswer(r.getCorrectIndex()));
    }

    @Test
    public void timeoutEndsTheRun() {
        GameManager g = new GameManager(2);
        g.startNewRun();
        g.beginRound();
        assertTrue(g.timeUp());
        assertEquals(GameOverReason.TIMEOUT, g.getReason());
        assertFalse(g.timeUp());
    }

    @Test
    public void pauseFreezesInputAndResumeRestoresState() {
        GameManager g = new GameManager(8);
        g.startNewRun();
        Round r = g.beginRound();
        assertTrue(g.pause());
        assertEquals(GameManager.AnswerResult.IGNORED, g.submitAnswer(r.getCorrectIndex()));
        assertFalse(g.timeUp());
        assertEquals(GameState.PLAYING, g.resume());
        assertEquals(GameManager.AnswerResult.CORRECT, g.submitAnswer(r.getCorrectIndex()));
    }

    @Test
    public void shuffleMovesEveryTileAndKeepsOneCorrect() {
        for (int seed = 0; seed < 200; seed++) {
            GameManager g = new GameManager(seed);
            g.startNewRun();
            for (int k = 0; k < 25; k++) {
                Round r = g.beginRound();
                g.submitAnswer(r.getCorrectIndex());
            }
            Round r = g.beginRound();
            Tile[] before = new Tile[4];
            for (int k = 0; k < 4; k++) before[k] = r.getTile(k);
            assertTrue(g.shuffleCurrentRound());
            for (int k = 0; k < 4; k++) assertTrue(r.getTile(k) != before[k]);
            assertEquals(1, r.countCorrect());
        }
    }
}
