package com.example.pawsomeminigames.reactiongame.game;

public final class Tile {
    public final AnimalType animal;
    public final TileColor color;
    public final TileRole role;

    public Tile(
            AnimalType animal,
            TileColor color,
            TileRole role
    ) {
        if (animal == null || color == null || role == null) {
            throw new IllegalArgumentException(
                    "Tile properties cannot be null"
            );
        }

        this.animal = animal;
        this.color = color;
        this.role = role;
    }

    public boolean matches(
            AnimalType targetAnimal,
            TileColor targetColor
    ) {
        return animal == targetAnimal
                && color == targetColor;
    }

    public AnimalType getAnimal() {
        return animal;
    }

    public TileColor getColor() {
        return color;
    }

    public TileRole getRole() {
        return role;
    }

    public boolean isCorrect() {
        return role == TileRole.CORRECT;
    }

    public boolean isDecoy() {
        return role.isDecoy();
    }
}
