package com.example.pawsomeminigames.reactiongame.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import com.example.pawsomeminigames.AudioSettings;
import com.example.pawsomeminigames.ClickSound;
import com.example.pawsomeminigames.R;
import com.example.pawsomeminigames.reactiongame.data.PreferencesManager;

import java.util.EnumMap;
import java.util.Map;

/**
 * Reaction Game's music and sound effects.
 *
 * The music / sound switches and volumes are the hub's (see AudioSettings), and button clicks are the
 * hub's ClickSound, so this game sounds like the rest of the app and the hub's Settings window controls it.
 */
public final class SoundManager {
    /** The hub's Music slider is multiplied by these, so the default level matches the original mix. */
    private static final float MAIN_MUSIC_SCALE = 0.8f;
    private static final float GAME_MUSIC_SCALE = 0.72f;

    public enum Sfx {
        CLICK(0), BUTTON(0), START(R.raw.rg_sfx_game), CORRECT(R.raw.rg_sfx_correct),
        WRONG(R.raw.rg_sfx_wrong), COUNTDOWN(R.raw.rg_sfx_countdown), GAME_OVER(R.raw.rg_sfx_gameover),
        HIGH_SCORE(R.raw.rg_sfx_highscore);

        /** 0 = the hub's standard button click (ClickSound), not a file of this game. */
        final int rawId;

        Sfx(int rawId) {
            this.rawId = rawId;
        }
    }

    public enum Track {
        MAIN(R.raw.rg_bgm_main), GAME(R.raw.rg_bgm_game);

        final int rawId;

        Track(int rawId) {
            this.rawId = rawId;
        }
    }

    private static SoundManager instance;

    public static synchronized SoundManager get() {
        if (instance == null) instance = new SoundManager();
        return instance;
    }

    private Context appContext;
    private PreferencesManager prefs;
    private SoundPool soundPool;
    private final Map<Sfx, Integer> soundIds = new EnumMap<>(Sfx.class);
    private final Map<Sfx, Boolean> loaded = new EnumMap<>(Sfx.class);
    private final Map<Integer, Sfx> idToSfx = new java.util.HashMap<>();

    private MediaPlayer player;
    private Track playerTrack;
    private Track wantedTrack;
    private boolean appInForeground = true;
    private boolean musicSuspended;
    private Vibrator vibrator;

    private SoundManager() {
    }

    public synchronized void init(Context context) {
        if (appContext == null) {
            appContext = context.getApplicationContext();
            prefs = PreferencesManager.get(appContext);
        }
        if (soundPool == null) {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            soundPool = new SoundPool.Builder().setMaxStreams(6).setAudioAttributes(attrs).build();
            soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                @Override
                public void onLoadComplete(SoundPool pool, int sampleId, int status) {
                    Sfx sfx = idToSfx.get(sampleId);
                    if (sfx != null && status == 0) loaded.put(sfx, Boolean.TRUE);
                }
            });
            soundIds.clear();
            loaded.clear();
            idToSfx.clear();
            for (Sfx sfx : Sfx.values()) {
                if (sfx.rawId == 0) continue;   // click sounds come from ClickSound
                int id = soundPool.load(appContext, sfx.rawId, 1);
                soundIds.put(sfx, id);
                idToSfx.put(id, sfx);
            }
        }
        if (vibrator == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vm = (VibratorManager) appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                if (vm != null) vibrator = vm.getDefaultVibrator();
            } else {
                vibrator = (Vibrator) appContext.getSystemService(Context.VIBRATOR_SERVICE);
            }
        }
    }

    public synchronized void playSfx(Sfx sfx) {
        if (appContext == null) return;
        if (sfx.rawId == 0) {
            ClickSound.play(appContext);
            return;
        }
        if (soundPool == null || !AudioSettings.isSfxOn(appContext)) return;
        Integer id = soundIds.get(sfx);
        if (id == null || !Boolean.TRUE.equals(loaded.get(sfx))) return;
        float base = (sfx == Sfx.COUNTDOWN) ? 0.8f : 1f;
        float vol = Math.min(1f, AudioSettings.getSfxVolume(appContext) * 1.15f * base);
        if (vol <= 0f) return;
        soundPool.play(id, vol, vol, 1, 0, 1f);
    }

    @SuppressWarnings("deprecation")
    public void vibrate(long millis) {
        if (prefs == null || !prefs.isVibrationOn() || vibrator == null) return;
        try {
            if (!vibrator.hasVibrator()) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(millis);
            }
        } catch (RuntimeException ignored) {
        }
    }

    /** Called from a screen's onResume: the app is on screen again, play this track. */
    public synchronized void playMusic(Track track, boolean suspended) {
        appInForeground = true;
        wantedTrack = track;
        musicSuspended = suspended;
        refreshMusic();
    }

    public synchronized void setMusicSuspended(boolean suspended) {
        musicSuspended = suspended;
        refreshMusic();
    }

    /** Called from a screen's onPause: leaving for another screen, the hub, or the home screen. */
    public synchronized void onAppBackground() {
        appInForeground = false;
        refreshMusic();
    }

    private void refreshMusic() {
        if (appContext == null) return;
        boolean shouldPlay = wantedTrack != null && appInForeground && !musicSuspended
                && AudioSettings.isMusicOn(appContext);
        if (!shouldPlay) {
            if (player != null) {
                try {
                    if (player.isPlaying()) player.pause();
                } catch (IllegalStateException ignored) {
                    releasePlayer();
                }
            }
            return;
        }
        if (player == null || playerTrack != wantedTrack) {
            releasePlayer();
            try {
                AudioAttributes attrs = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();
                player = MediaPlayer.create(appContext, wantedTrack.rawId, attrs, AudioManager.AUDIO_SESSION_ID_GENERATE);
                if (player == null) return;
                playerTrack = wantedTrack;
                player.setLooping(true);
                player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mp, int what, int extra) {
                        synchronized (SoundManager.this) {
                            releasePlayer();
                        }
                        return true;
                    }
                });
            } catch (RuntimeException e) {
                releasePlayer();
                return;
            }
        }
        try {
            float scale = (wantedTrack == Track.MAIN) ? MAIN_MUSIC_SCALE : GAME_MUSIC_SCALE;
            float vol = AudioSettings.getMusicVolume(appContext) * scale;
            player.setVolume(vol, vol);
            if (!player.isPlaying()) player.start();
        } catch (IllegalStateException e) {
            releasePlayer();
        }
    }

    private void releasePlayer() {
        if (player != null) {
            try {
                player.release();
            } catch (RuntimeException ignored) {
            }
            player = null;
            playerTrack = null;
        }
    }

    /** Frees the music and sounds (called when the player leaves Reaction Game for the hub). */
    public synchronized void release() {
        releasePlayer();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
        soundIds.clear();
        loaded.clear();
        idToSfx.clear();
        wantedTrack = null;
        musicSuspended = false;
    }
}
