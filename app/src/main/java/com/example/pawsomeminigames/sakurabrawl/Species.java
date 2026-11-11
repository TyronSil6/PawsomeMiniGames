package com.example.pawsomeminigames.sakurabrawl;

import java.util.Random;

/** The animals you can play as: names and colors. Drawing is done in Capybara and UiKit. */
public final class Species {

    public static final int CAPYBARA = 0;
    public static final int RACCOON = 1;
    public static final int FOX = 2;
    public static final int SLOTH = 3;
    public static final int COUNT = 4;

    private static final String[] NAMES = {"CAPYBARA", "RACCOON", "FOX", "SLOTH"};

    /** Colors, indexed by species id. */
    public static final int[] BODY = {0xFFB97F45, 0xFF8E8E98, 0xFFE8772E, 0xFFA89070};
    public static final int[] HEAD = {0xFFC98F52, 0xFFA7A7B1, 0xFFF28C3C, 0xFFC7B08C};
    public static final int[] DARK = {0xFF8A5A2B, 0xFF4A4A55, 0xFF8A3E12, 0xFF6B5A40};
    public static final int[] BELLY = {0xFFDDB07A, 0xFFD2D2DA, 0xFFFFF1DC, 0xFFD2BF9A};
    public static final int[] NOSE = {0xFF4A2E18, 0xFF1E1E24, 0xFF1E1410, 0xFF2E1E12};

    private Species() {
    }

    /** Makes sure an id from saved settings is valid. */
    public static int safe(int s) {
        return (s < 0 || s >= COUNT) ? CAPYBARA : s;
    }

    public static String name(int s) {
        return NAMES[safe(s)];
    }

    /** The next animal in the list (wraps around). */
    public static int next(int s) {
        return (safe(s) + 1) % COUNT;
    }

    /** A random animal that is different from the player's. */
    public static int randomOpponent(int player, Random random) {
        int pick = random.nextInt(COUNT - 1);
        return pick >= safe(player) ? pick + 1 : pick;
    }
}