package com.example.pawsomeminigames.crittertoss;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;

import com.example.pawsomeminigames.AudioSettings;
import com.example.pawsomeminigames.R;

import java.util.Random;

/**
 * Owns Critter Toss's background music + sound effects.
 * The music / sound on-off switches and volumes live in the Pawsome hub's Settings
 * (see AudioSettings); this class just reads them every time it plays something.
 */
public final class SoundManager {

    public static final int THROW = 0, HIT = 1, MISS = 2, CLICK = 3, OVER = 4,
            POWER = 5, COMBO = 6, BOOM = 7;

    private static SoundManager instance;

    public static SoundManager get(Context c) {
        if (instance == null) instance = new SoundManager(c.getApplicationContext());
        return instance;
    }

    private final Context ctx;
    private final SoundPool pool;
    private final int[] ids = new int[8];
    private final Random rng = new Random();
    private MediaPlayer music;

    private SoundManager(Context c) {
        ctx = c;

        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        pool = new SoundPool.Builder().setMaxStreams(6).setAudioAttributes(attrs).build();
        ids[THROW] = pool.load(c, R.raw.ct_sfx_throw, 1);
        ids[HIT] = pool.load(c, R.raw.ct_sfx_hit, 1);
        ids[MISS] = pool.load(c, R.raw.ct_sfx_miss, 1);
        ids[CLICK] = pool.load(c, R.raw.ct_sfx_click, 1);
        ids[OVER] = pool.load(c, R.raw.ct_sfx_over, 1);
        ids[POWER] = pool.load(c, R.raw.ct_sfx_power, 1);
        ids[COMBO] = pool.load(c, R.raw.ct_sfx_combo, 1);
        ids[BOOM] = pool.load(c, R.raw.ct_sfx_boom, 1);
    }

    // ---------- sfx ----------
    public void play(int sfx) {
        if (sfx == CLICK) { com.example.pawsomeminigames.ClickSound.play(ctx); return; }   // same click as the hub + Sakura Brawl
        if (!AudioSettings.isSfxOn(ctx)) return;
        float vol = AudioSettings.getSfxVolume(ctx);
        if (vol <= 0f) return;
        float rate = (sfx == HIT || sfx == MISS) ? .94f + rng.nextFloat() * .14f : 1f;
        pool.play(ids[sfx], vol, vol, 1, 0, rate);
    }

    /** Plays a sound at a chosen pitch (1 = normal). Used to make hits sound higher as the combo grows. */
    public void playPitched(int sfx, float rate) {
        if (!AudioSettings.isSfxOn(ctx)) return;
        float vol = AudioSettings.getSfxVolume(ctx);
        if (vol <= 0f) return;
        pool.play(ids[sfx], vol, vol, 1, 0, Math.max(.5f, Math.min(2f, rate)));
    }

    // ---------- music ----------

    private synchronized MediaPlayer ensureMusic() {
        if (music == null) {
            music = MediaPlayer.create(ctx, R.raw.ct_music_loop);
            if (music != null) music.setLooping(true);
        }
        return music;
    }

    /** Builds the music player ahead of time (the splash screen calls this in the background). */
    public void warmMusic() { ensureMusic(); }

    /** Call from onResume of each activity. Re-reads the hub's music settings every time. */
    public void startMusic() {
        if (!AudioSettings.isMusicOn(ctx)) {
            if (music != null && music.isPlaying()) music.pause();
            return;
        }
        if (ensureMusic() == null) return;
        float v = .45f * AudioSettings.getMusicVolume(ctx);
        music.setVolume(v, v);
        if (!music.isPlaying()) music.start();
    }

    /** Call from onPause of each activity. */
    public void pauseMusic() {
        if (music != null && music.isPlaying()) music.pause();
    }
}