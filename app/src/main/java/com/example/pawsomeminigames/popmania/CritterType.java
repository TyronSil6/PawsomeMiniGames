
package com.example.pawsomeminigames.popmania;

import com.example.pawsomeminigames.R;

import java.util.Random;

public enum CritterType {

    // Normal characters
    SLOTH(R.drawable.pm_char_sloth, 10, 20),
    CAPYBARA(R.drawable.pm_char_capybara, 10, 20),
    FOX(R.drawable.pm_char_fox, 10, 20),
    RACCOON(R.drawable.pm_char_raccoon, 10, 20),

    // Special characters
    STAR(R.drawable.pm_char_star, 0, 8),
    GOLDEN_RACCOON(R.drawable.pm_char_golden_raccoon, 50, 5),

    // Special Items
    TIMER(R.drawable.pm_char_timer, 0, 8),

    // Penalty character
    SNAKE(R.drawable.pm_char_snake, -15, 8);


    // Character properties
    public final int drawableRes;
    public final int points;
    public final int weight;

    // Constructor
    CritterType(int drawableRes, int points, int weight) {
        this.drawableRes = drawableRes;
        this.points = points;
        this.weight = weight;
    }

    // Check if character is Golden Raccoon
    public boolean isGolden() {
        return this == GOLDEN_RACCOON;
    }

    // Check if character is Star
    public boolean isBomb() {
        return this == STAR;
    }

    // Check if character is Snake
    public boolean isSnake() {
        return this == SNAKE;
    }

    // Check if character is Timer
    public boolean isTimer() {
        return this == TIMER;
    }

    // Get additional time bonus
    public int getTimeBonus() {
        return isTimer() ? 5 : 0;
    }

    // Random character selection
    public static CritterType pickRandom(Random random) {

        int total = 0;

        for (CritterType type : values()) {
            total += type.weight;
        }

        int roll = random.nextInt(total);

        for (CritterType type : values()) {
            roll -= type.weight;

            if (roll < 0) {
                return type;
            }
        }

        return SLOTH;
    }
}
