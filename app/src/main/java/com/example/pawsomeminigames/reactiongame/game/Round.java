package com.example.pawsomeminigames.reactiongame.game;

import java.util.Collections;
import java.util.List;

public final class Round {
    public final int roundNumber;
    public final int tier;
    public final AnimalType cueAnimal;
    public final TileColor cueColor;
    public final long timeLimitMs;

    public final float shuffleAtFraction;
    private final Tile[] tiles;

    Round(int roundNumber, int tier, AnimalType cueAnimal, TileColor cueColor, List<Tile> tileList,
          long timeLimitMs, float shuffleAtFraction) {
        if (tileList.size() != GameManager.TILE_COUNT) {
            throw new IllegalArgumentException("A round needs exactly " + GameManager.TILE_COUNT + " tiles");
        }
        this.roundNumber = roundNumber;
        this.tier = tier;
        this.cueAnimal = cueAnimal;
        this.cueColor = cueColor;
        this.tiles = tileList.toArray(new Tile[0]);
        this.timeLimitMs = timeLimitMs;
        this.shuffleAtFraction = shuffleAtFraction;
        if (countCorrect() != 1) {
            throw new IllegalStateException("Round must have exactly one correct tile, found " + countCorrect());
        }
    }

    public Tile getTile(int index) {
        return tiles[index];
    }

    public int size() {
        return tiles.length;
    }

    public int countCorrect() {
        int n = 0;
        for (Tile t : tiles) {
            if (t.matches(cueAnimal, cueColor)) n++;
        }
        return n;
    }

    public int getCorrectIndex() {
        for (int i = 0; i < tiles.length; i++) {
            if (tiles[i].matches(cueAnimal, cueColor)) return i;
        }
        throw new IllegalStateException("No correct tile");
    }

    public boolean willShuffle() {
        return shuffleAtFraction > 0f;
    }

    void shuffleTiles(java.util.Random rng) {
        Tile[] before = tiles.clone();
        for (int attempt = 0; attempt < 20; attempt++) {
            List<Tile> list = new java.util.ArrayList<>();
            Collections.addAll(list, before);
            Collections.shuffle(list, rng);
            boolean allMoved = true;
            for (int i = 0; i < before.length; i++) {
                if (list.get(i) == before[i]) {
                    allMoved = false;
                    break;
                }
            }
            if (allMoved) {
                for (int i = 0; i < tiles.length; i++) tiles[i] = list.get(i);
                return;
            }
        }

        for (int i = 0; i < tiles.length; i++) tiles[i] = before[(i + 1) % before.length];
    }
}
