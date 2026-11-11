package com.example.pawsomeminigames.reactiongame.game;

public enum AnimalType {
    BEAR(0), RACCOON(0), FOX(0), SLOTH(0),
    RABBIT(2), FROG(2),
    HEDGEHOG(3), SQUIRREL(3),
    PENGUIN(4), SHEEP(4), DEER(4), PIG(4);

    public final int minTier;

    AnimalType(int minTier) {
        this.minTier = minTier;
    }

    public boolean isMain() {
        return minTier == 0;
    }
}
