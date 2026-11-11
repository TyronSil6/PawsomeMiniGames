package com.example.pawsomeminigames;

import android.content.Context;
import android.media.MediaPlayer;

/**
 * Background music for the main hub screen. It follows the Settings window (music switch + volume),
 * plays only while the hub is on screen (each game has its own music), and carries on from where
 * it stopped when you come back from a game.
 *
 * The song is res/raw/hub_music.(ogg/mp3). If that file does not exist yet, it falls back to
 * res/raw/sb_music_bg so the hub is never silent by accident.
 */
final class HubMusic {
    private static HubMusic instance;

    static synchronized HubMusic get(Context c) {
        if (instance == null) instance = new HubMusic(c.getApplicationContext());
        return instance;
    }

    private final Context ctx;
    private MediaPlayer player;
    private boolean wanted = false;   // true while the hub is on screen

    private HubMusic(Context c) { ctx = c; }

    private int findSong() {
        int id = ctx.getResources().getIdentifier("hub_music", "raw", ctx.getPackageName());
        if (id == 0) id = ctx.getResources().getIdentifier("sb_music_bg", "raw", ctx.getPackageName());
        return id;
    }

    private synchronized MediaPlayer ensure() {
        if (player == null) {
            int id = findSong();
            if (id == 0) return null;
            try {
                player = MediaPlayer.create(ctx, id);
                if (player != null) player.setLooping(true);
            } catch (Exception e) {
                player = null;
            }
        }
        return player;
    }

    /** Builds the player ahead of time (the splash screen calls this on a background thread). */
    void warm() { ensure(); }

    /** Call from the hub's onResume. */
    void start() {
        wanted = true;
        apply();
    }

    /** Call from the hub's onPause (leaving for a game, or the app went to the background). */
    void pause() {
        wanted = false;
        apply();
    }

    /** Call after the Settings window changed the music switch or volume. */
    void refresh() { apply(); }

    private void apply() {
        try {
            if (!wanted || !AudioSettings.isMusicOn(ctx)) {
                if (player != null && player.isPlaying()) player.pause();
                return;
            }
            MediaPlayer m = ensure();
            if (m == null) return;
            float v = AudioSettings.getMusicVolume(ctx);
            m.setVolume(v, v);
            if (!m.isPlaying()) m.start();
        } catch (IllegalStateException ignored) {
            // the player was released; nothing to do
        }
    }
}