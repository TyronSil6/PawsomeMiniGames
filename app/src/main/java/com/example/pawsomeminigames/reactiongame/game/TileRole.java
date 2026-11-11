package com.example.pawsomeminigames.reactiongame.game;

public enum TileRole {
    CORRECT,

    ANIMAL_TWIN,

    COLOR_TWIN,

    FILLER;

    public boolean isDecoy() {
        return this == ANIMAL_TWIN || this == COLOR_TWIN;
    }
}
