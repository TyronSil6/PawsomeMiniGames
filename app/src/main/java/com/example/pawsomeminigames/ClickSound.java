package com.example.pawsomeminigames;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.SystemClock;

/**
 * THE button click for the whole app (hub, Critter Toss and Sakura Brawl all play it from here),
 * so every button has exactly the same sound, volume and pitch.
 * It follows the Settings window: sound switch + sound volume (see AudioSettings).
 */
public final class ClickSound {
    /** Fixed on purpose. Change these two numbers and every button in every game changes together. */
    private static final float VOLUME_SCALE = 1f;   // 0..1, multiplied with the Settings sound volume
    private static final float PITCH = 1f;          // 1 = normal pitch

    private static final long MIN_GAP_MS = 40;      // stops machine-gun double clicks

    private static ClickSound instance;

    private static ClickSound get(Context c) {
        if (instance == null) instance = new ClickSound(c.getApplicationContext());
        return instance;
    }

    /** Plays the standard button click. Safe to call from anywhere. */
    public static void play(Context c) {
        get(c).playNow();
    }

    private final Context ctx;
    private final SoundPool pool;
    private final int click;
    private long last;

    private ClickSound(Context c) {
        ctx = c;
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        pool = new SoundPool.Builder().setMaxStreams(3).setAudioAttributes(attrs).build();
        click = pool.load(c, R.raw.ct_sfx_click, 1);
    }

    private void playNow() {
        if (!AudioSettings.isSfxOn(ctx)) return;
        float v = AudioSettings.getSfxVolume(ctx) * VOLUME_SCALE;
        if (v <= 0f) return;
        long now = SystemClock.uptimeMillis();
        if (now - last < MIN_GAP_MS) return;
        last = now;
        pool.play(click, v, v, 1, 0, PITCH);
    }
}