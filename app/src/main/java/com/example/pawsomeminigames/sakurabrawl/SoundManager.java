package com.example.pawsomeminigames.sakurabrawl;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

/**
 * All the game audio: short sound effects (SoundPool) and looping background music (MediaPlayer).
 * The music/sound settings come from the Pawsome hub's Settings (via SettingsManager), so the player's choices are respected.
 * If an audio file is missing from res/raw, that sound is simply skipped (no crash).
 */
public class SoundManager {

    // Sound effect ids (pass these to play)
    public static final int CLICK = 0;
    public static final int PUSH = 1;
    public static final int HIT = 2;
    public static final int CHEER = 3;
    public static final int COUNTDOWN = 4;
    public static final int WIN = 5;
    public static final int LOSE = 6;

    /** File names in res/raw (without extension), in the same order as the ids above. */
    private static final String[] SFX_NAMES = {
            "sb_sfx_click", "sb_sfx_push", "sb_sfx_hit", "sb_sfx_cheer",
            "sb_sfx_countdown", "sb_sfx_win", "sb_sfx_lose"
    };
    private static final String MUSIC_NAME = "sb_music_bg2";

    /** Minimum milliseconds between two plays of the same sound (stops machine-gun repeats). */
    private static final long[] MIN_GAP_MS = {40, 90, 140, 600, 0, 800, 800};

    private final Context context;
    private final SettingsManager settings;
    private final SoundPool pool;
    private final int[] soundIds = new int[SFX_NAMES.length];   // 0 = file missing
    private final long[] lastPlayed = new long[SFX_NAMES.length];

    private MediaPlayer music;
    private boolean appActive = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile boolean released = false;

    public SoundManager(Context context) {
        this.context = context.getApplicationContext();
        this.settings = new SettingsManager(context);

        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        pool = new SoundPool.Builder()
                .setMaxStreams(6)
                .setAudioAttributes(attrs)
                .build();

        for (int i = 0; i < SFX_NAMES.length; i++) {
            int resId = findRaw(SFX_NAMES[i]);
            soundIds[i] = resId == 0 ? 0 : pool.load(this.context, resId, 1);
        }

        // MediaPlayer.create decodes the song, which froze the screen for a moment: do it in the background
        final int musicRes = findRaw(MUSIC_NAME);
        if (musicRes != 0) {
            new Thread(() -> {
                MediaPlayer mp = null;
                try {
                    mp = MediaPlayer.create(this.context, musicRes);
                    if (mp != null) mp.setLooping(true);
                } catch (Exception e) {
                    mp = null;
                }
                final MediaPlayer ready = mp;
                mainHandler.post(() -> {
                    if (released) {                       // the screen was closed while it was loading
                        if (ready != null) ready.release();
                        return;
                    }
                    music = ready;
                    refreshMusic();                       // start it if the game is already on screen
                });
            }, "sb-music").start();
        }
    }

    private int findRaw(String name) {
        return context.getResources().getIdentifier(name, "raw", context.getPackageName());
    }

    // ------------------------------------------------------------ effects

    public void play(int sound) {
        play(sound, 1f, 1f);
    }

    /** volumeScale 0..1 is multiplied with the player's sound volume. rate 1 = normal pitch. */
    public void play(int sound, float volumeScale, float rate) {
        if (sound < 0 || sound >= soundIds.length) return;
        if (sound == CLICK) { com.example.pawsomeminigames.ClickSound.play(context); return; }   // same click as the hub + Critter Toss
        if (soundIds[sound] == 0) return;
        if (!settings.isSfxOn()) return;

        long now = SystemClock.uptimeMillis();
        if (now - lastPlayed[sound] < MIN_GAP_MS[sound]) return;
        lastPlayed[sound] = now;

        float v = settings.getSfxVolume() * Math.max(0f, Math.min(1f, volumeScale));
        if (v <= 0.001f) return;
        float r = Math.max(0.5f, Math.min(2f, rate));
        pool.play(soundIds[sound], v, v, 1, 0, r);
    }

    // -------------------------------------------------------------- music

    /** Call from the activity's onResume. */
    public void onAppResume() {
        appActive = true;
        refreshMusic();
    }

    /** Call from the activity's onPause: music must not play in the background. */
    public void onAppPause() {
        appActive = false;
        if (music != null && music.isPlaying()) music.pause();
    }

    /** Re-reads the music settings (on/off and volume) and starts or stops the music. */
    public void refreshMusic() {
        if (music == null) return;
        try {
            if (settings.isMusicOn() && appActive) {
                float v = settings.getMusicVolume();
                music.setVolume(v, v);
                if (!music.isPlaying()) music.start();
            } else if (music.isPlaying()) {
                music.pause();
            }
        } catch (IllegalStateException ignored) {
            // The player was released; nothing to do
        }
    }

    /** Lets the music volume follow the slider while the player is dragging it. */
    public void previewMusicVolume(float v) {
        if (music == null) return;
        try {
            music.setVolume(v, v);
        } catch (IllegalStateException ignored) {
        }
    }

    /** Call once when the app is closing. */
    public void release() {
        released = true;
        pool.release();
        if (music != null) {
            try {
                music.release();
            } catch (Exception ignored) {
            }
            music = null;
        }
    }
}