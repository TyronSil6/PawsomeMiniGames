package com.example.pawsomeminigames.sakurabrawl;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.pawsomeminigames.AudioSettings;

/**
 * Saves and loads Sakura Brawl's own settings (difficulty, characters).
 * Music / sound on-off and volumes are read from the Pawsome hub's Settings (AudioSettings).
 */
public class SettingsManager {

    public static final int EASY = 0;
    public static final int NORMAL = 1;
    public static final int HARD = 2;

    private static final String PREFS = "capysumo_settings";
    private final SharedPreferences prefs;
    private final Context appContext;

    public SettingsManager(Context context) {
        appContext = context.getApplicationContext();
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // Audio: owned by the hub's Settings window, read-only here.
    public boolean isMusicOn() { return AudioSettings.isMusicOn(appContext); }
    public boolean isSfxOn() { return AudioSettings.isSfxOn(appContext); }

    /** Volume from 0.0 to 1.0 */
    public float getMusicVolume() { return AudioSettings.getMusicVolume(appContext); }
    public float getSfxVolume() { return AudioSettings.getSfxVolume(appContext); }

    public int getDifficulty() { return prefs.getInt("difficulty", NORMAL); }
    public void setDifficulty(int d) { prefs.edit().putInt("difficulty", d).apply(); }

    /** Which animal the player is (see Species). */
    public int getCharacter() { return Species.safe(prefs.getInt("character", Species.CAPYBARA)); }
    public void setCharacter(int c) { prefs.edit().putInt("character", Species.safe(c)).apply(); }

    /** Which animal player 2 is in 2-player mode. */
    public int getCharacter2() { return Species.safe(prefs.getInt("character2", Species.RACCOON)); }
    public void setCharacter2(int c) { prefs.edit().putInt("character2", Species.safe(c)).apply(); }

}