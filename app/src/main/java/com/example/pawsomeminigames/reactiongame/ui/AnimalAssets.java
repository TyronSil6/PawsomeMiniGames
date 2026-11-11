package com.example.pawsomeminigames.reactiongame.ui;

import com.example.pawsomeminigames.R;
import com.example.pawsomeminigames.reactiongame.game.AnimalType;
import com.example.pawsomeminigames.reactiongame.game.TileColor;

public final class AnimalAssets {
    private AnimalAssets() {
    }

    public static int idle(AnimalType a) {
        switch (a) {
            case BEAR: return R.drawable.rg_animal_bear_idle;
            case RACCOON: return R.drawable.rg_animal_raccoon_idle;
            case FOX: return R.drawable.rg_animal_fox_idle;
            case SLOTH: return R.drawable.rg_animal_sloth_idle;
            case RABBIT: return R.drawable.rg_animal_rabbit_decoy;
            case FROG: return R.drawable.rg_animal_frog_decoy;
            case HEDGEHOG: return R.drawable.rg_animal_hedgehog_decoy;
            case SQUIRREL: return R.drawable.rg_animal_squirrel_decoy;
            case PENGUIN: return R.drawable.rg_animal_penguin_decoy;
            case SHEEP: return R.drawable.rg_animal_sheep_decoy;
            case DEER: return R.drawable.rg_animal_deer_decoy;
            case PIG: return R.drawable.rg_animal_pig_decoy;
            default: return R.drawable.rg_animal_bear_idle;
        }
    }

    public static int happy(AnimalType a) {
        switch (a) {
            case BEAR: return R.drawable.rg_animal_bear_happy;
            case RACCOON: return R.drawable.rg_animal_raccoon_happy;
            case FOX: return R.drawable.rg_animal_fox_happy;
            case SLOTH: return R.drawable.rg_animal_sloth_happy;
            default: return idle(a);
        }
    }

    public static int sad(AnimalType a) {
        switch (a) {
            case BEAR: return R.drawable.rg_animal_bear_angry;
            case RACCOON: return R.drawable.rg_animal_raccoon_cry;
            case FOX: return R.drawable.rg_animal_fox_cry;
            case SLOTH: return R.drawable.rg_animal_sloth_cry;
            default: return idle(a);
        }
    }

    public static int nameRes(AnimalType a) {
        switch (a) {
            case BEAR: return R.string.rg_animal_bear;
            case RACCOON: return R.string.rg_animal_raccoon;
            case FOX: return R.string.rg_animal_fox;
            case SLOTH: return R.string.rg_animal_sloth;
            case RABBIT: return R.string.rg_animal_rabbit;
            case FROG: return R.string.rg_animal_frog;
            case HEDGEHOG: return R.string.rg_animal_hedgehog;
            case SQUIRREL: return R.string.rg_animal_squirrel;
            case PENGUIN: return R.string.rg_animal_penguin;
            case SHEEP: return R.string.rg_animal_sheep;
            case DEER: return R.string.rg_animal_deer;
            case PIG: return R.string.rg_animal_pig;
            default: return R.string.rg_animal_bear;
        }
    }

    public static int colorNameRes(TileColor c) {
        switch (c) {
            case RED: return R.string.rg_color_red;
            case BLUE: return R.string.rg_color_blue;
            case GREEN: return R.string.rg_color_green;
            default: return R.string.rg_color_yellow;
        }
    }

    public static int tileFrame(TileColor c) {
        switch (c) {
            case RED: return R.drawable.rg_tile_red;
            case BLUE: return R.drawable.rg_tile_blue;
            case GREEN: return R.drawable.rg_tile_green;
            default: return R.drawable.rg_tile_yellow;
        }
    }
}
