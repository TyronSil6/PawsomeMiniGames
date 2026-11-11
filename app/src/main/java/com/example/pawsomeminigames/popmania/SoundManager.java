package com.example.pawsomeminigames.popmania;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;

import com.example.pawsomeminigames.AudioSettings;
import com.example.pawsomeminigames.ClickSound;
import com.example.pawsomeminigames.R;

/**
 * Handles Pop Mania's background music and short sound effects.
 *
 * The music / sound switches and volumes are the hub's (see AudioSettings), and the button click is the
 * hub's ClickSound, so Pop Mania sounds like the rest of the app.
 *
 * The music player is shared by every screen (it is static), so the menu music keeps
 * playing smoothly when you move between Home, How to Play and Settings instead of
 * restarting each time. Two tracks exist: a calm menu track and a faster in-game track.
 */
public class SoundManager {

    /** The hub's Music slider is multiplied by this, so the default level matches the original Pop Mania mix. */
    private static final float MUSIC_SCALE = 0.85f;

    private static final int TRACK_NONE = 0;
    private static final int TRACK_MENU = 1;
    private static final int TRACK_GAME = 2;

    // Shared by all screens.
    private static MediaPlayer musicPlayer;
    private static int currentTrack = TRACK_NONE;

    private final SoundPool soundPool;
    private final int popSound, goldenSound, snakeSound, bombSound, gameOverSound, bestSound;
    private final Context appContext;
    private boolean released = false;

    public SoundManager(Context context) {
        appContext = context.getApplicationContext();
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder().setMaxStreams(6)
                .setAudioAttributes(attributes).build();
        popSound = soundPool.load(appContext, R.raw.pm_sfx_pop, 1);
        goldenSound = soundPool.load(appContext, R.raw.pm_sfx_golden, 1);
        snakeSound = soundPool.load(appContext, R.raw.pm_sfx_snake, 1);
        bombSound = soundPool.load(appContext, R.raw.pm_sfx_bomb, 1);
        gameOverSound = soundPool.load(appContext, R.raw.pm_sfx_game_over, 1);
        bestSound = soundPool.load(appContext, R.raw.pm_sfx_best, 1);
    }

    // ------------------------------------------------------------------
    // Sound effects
    // ------------------------------------------------------------------
    public void playPop() { play(popSound, 0.8f); }
    public void playGolden() { play(goldenSound, 0.9f); }
    public void playSnake() { play(snakeSound, 0.85f); }
    public void playBomb() { play(bombSound, 0.95f); }
    public void playGameOver() { play(gameOverSound, 0.8f); }
    public void playBest() { play(bestSound, 0.85f); }

    /** The standard button click used by the whole app (follows the Sound switch + volume). */
    public void playClick() {
        if (released) return;
        ClickSound.play(appContext);
    }

    public boolean isMusicOn() { return AudioSettings.isMusicOn(appContext); }
    public boolean isSfxOn() { return AudioSettings.isSfxOn(appContext); }

    private void play(int sound, float baseVolume) {
        if (released || !isSfxOn()) return;
        float volume = AudioSettings.getSfxVolume(appContext) * baseVolume;
        if (volume <= 0f) return;
        soundPool.play(sound, volume, volume, 1, 0, 1f);
    }

    // ------------------------------------------------------------------
    // Music
    // ------------------------------------------------------------------
    /** Menu music for Home / How to Play: the same song as the game screen (pm_bgm_game). */
    public void startMenuMusic() {
        startTrack(TRACK_MENU, R.raw.pm_bgm_game, false);
    }

    /** Faster track for the game screen. Pass true to start the song from the beginning. */
    public void startGameMusic(boolean fromStart) {
        startTrack(TRACK_GAME, R.raw.pm_bgm_game, fromStart);
    }

    private void startTrack(int track, int resId, boolean fromStart) {
        if (released) return;
        if (!isMusicOn()) {
            pauseMusic();
            return;
        }
        try {
            if (musicPlayer != null && currentTrack != track) {
                releaseMusic();
            }
            if (musicPlayer == null) {
                musicPlayer = MediaPlayer.create(appContext, resId);
                if (musicPlayer == null) return;
                currentTrack = track;
                musicPlayer.setLooping(true);
            } else if (fromStart) {
                musicPlayer.seekTo(0);
            }
            applyVolumes();
            if (!musicPlayer.isPlaying()) musicPlayer.start();
        } catch (RuntimeException e) {
            // A music problem must never crash the game: drop the player, the next call makes a new one
            releaseMusic();
        }
    }

    public void applyVolumes() {
        try {
            if (musicPlayer != null) {
                float volume = AudioSettings.getMusicVolume(appContext) * MUSIC_SCALE;
                musicPlayer.setVolume(volume, volume);
            }
        } catch (RuntimeException e) {
            releaseMusic();
        }
    }

    public void pauseMusic() {
        try {
            if (musicPlayer != null && musicPlayer.isPlaying()) musicPlayer.pause();
        } catch (RuntimeException e) {
            releaseMusic();
        }
    }

    public void stopMusic() {
        pauseMusic();
    }

    /** Frees the music player completely (called when the player leaves Pop Mania). */
    public static void releaseMusic() {
        if (musicPlayer != null) {
            try { musicPlayer.release(); } catch (RuntimeException ignored) { }
            musicPlayer = null;
        }
        currentTrack = TRACK_NONE;
    }

    /** Frees this screen's sound effects. The shared music keeps playing. */
    public void release() {
        if (released) return;
        released = true;
        soundPool.release();
    }
}