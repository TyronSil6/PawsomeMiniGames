package com.example.pawsomeminigames.reactiongame.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class RoundFactory {
    private RoundFactory() {
    }

    public static int tierForScore(int score) {
        if (score < 3) return 0;
        if (score < 8) return 1;
        if (score < 14) return 2;
        if (score < 22) return 3;
        return 4;
    }

    public static long timeLimitMs(int score, int roundNumber) {
        long ms = Math.max(1500L, 5000L - 110L * score);

        if (roundNumber <= 1) {
            ms += 1000L;
        }

        return ms;
    }

    private static List<AnimalType> poolForTier(int tier) {
        List<AnimalType> pool = new ArrayList<>();

        for (AnimalType animal : AnimalType.values()) {
            if (animal.minTier <= tier) {
                pool.add(animal);
            }
        }

        return pool;
    }

    private static <T> T pick(List<T> list, Random rng) {
        return list.get(rng.nextInt(list.size()));
    }

    public static Round create(Random rng, int score, int roundNumber) {
        final int tier = tierForScore(score);
        final List<AnimalType> pool = poolForTier(tier);

        final List<TileColor> allColors = new ArrayList<>();
        Collections.addAll(allColors, TileColor.values());

        final AnimalType cueAnimal = pick(pool, rng);
        final TileColor cueColor = pick(allColors, rng);

        List<Tile> tiles = new ArrayList<>();

        tiles.add(new Tile(
                cueAnimal,
                cueColor,
                TileRole.CORRECT
        ));

        boolean animalTwin = tier >= 1;
        boolean colorTwin = tier >= 3 ||
                (tier == 2 && rng.nextBoolean());

        List<TileColor> otherColors = new ArrayList<>(allColors);
        otherColors.remove(cueColor);
        Collections.shuffle(otherColors, rng);

        if (animalTwin) {
            TileColor twinColor = otherColors.remove(0);

            tiles.add(new Tile(
                    cueAnimal,
                    twinColor,
                    TileRole.ANIMAL_TWIN
            ));
        }

        List<AnimalType> others = new ArrayList<>(pool);
        others.remove(cueAnimal);
        Collections.shuffle(others, rng);

        if (colorTwin) {
            AnimalType twinAnimal = others.remove(0);

            tiles.add(new Tile(
                    twinAnimal,
                    cueColor,
                    TileRole.COLOR_TWIN
            ));
        }

        while (tiles.size() < GameManager.TILE_COUNT) {
            AnimalType animal = others.remove(0);
            TileColor color;

            if (tier <= 1 && !otherColors.isEmpty()) {
                color = otherColors.remove(0);
            } else {
                color = pick(allColors, rng);
            }

            tiles.add(new Tile(
                    animal,
                    color,
                    TileRole.FILLER
            ));
        }

        Collections.shuffle(tiles, rng);

        float shuffleFraction = 0f;

        if (tier == 3 && rng.nextFloat() < 0.30f) {
            shuffleFraction = 0.55f;
        }

        if (tier >= 4 && rng.nextFloat() < 0.50f) {
            shuffleFraction = 0.55f;
        }

        return new Round(
                roundNumber,
                tier,
                cueAnimal,
                cueColor,
                tiles,
                timeLimitMs(score, roundNumber),
                shuffleFraction
        );
    }
}
