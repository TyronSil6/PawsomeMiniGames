package com.example.pawsomeminigames;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * The one place that stores the music / sound settings for the whole app.
 * The hub's Settings window writes these; Critter Toss and Sakura Brawl only read them.
 * Volumes are 0..1.
 */
public final class AudioSettings {
    private static final String PREFS = "pawsome_audio";

    private AudioSettings() {}

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }

    public static boolean isMusicOn(Context c) { return prefs(c).getBoolean("music_on", true); }
    public static void setMusicOn(Context c, boolean on) { prefs(c).edit().putBoolean("music_on", on).apply(); }

    public static boolean isSfxOn(Context c) { return prefs(c).getBoolean("sfx_on", true); }
    public static void setSfxOn(Context c, boolean on) { prefs(c).edit().putBoolean("sfx_on", on).apply(); }

    public static float getMusicVolume(Context c) { return prefs(c).getFloat("music_vol", 0.7f); }
    public static void setMusicVolume(Context c, float v) { prefs(c).edit().putFloat("music_vol", clamp(v)).apply(); }

    public static float getSfxVolume(Context c) { return prefs(c).getFloat("sfx_vol", 0.8f); }
    public static void setSfxVolume(Context c, float v) { prefs(c).edit().putFloat("sfx_vol", clamp(v)).apply(); }
}
