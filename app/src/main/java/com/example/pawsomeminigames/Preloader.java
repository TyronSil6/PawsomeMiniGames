package com.example.pawsomeminigames;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.example.pawsomeminigames.crittertoss.MenuActivity;
import com.example.pawsomeminigames.sakurabrawl.Assets;

/**
 * Gets the heavy stuff ready on a background thread while the loading screen is showing
 * (big pictures, the game sprites, the music players), so opening a game later does not
 * freeze the screen for a moment.
 */
public final class Preloader {
    private Preloader() {}

    private static boolean started = false;
    private static volatile boolean done = false;
    private static volatile Bitmap iconSakura, iconCritter, iconPop, iconReaction;

    /** Starts the background work once. Safe to call again. */
    public static synchronized void start(Context c) {
        if (started) return;
        started = true;
        final Context app = c.getApplicationContext();
        new Thread(() -> {
            try {
                // hub card pictures (1254 px is far bigger than the cards, so load them at half size,
                // and cut off the empty see-through border so every card picture is the same size)
                iconSakura = trimTransparent(decodeHalf(app, R.drawable.icon_sakura_brawl));
                iconCritter = trimTransparent(decodeHalf(app, R.drawable.icon_critter_toss));
                iconPop = trimTransparent(decodeHalf(app, R.drawable.icon_pop_mania));
                iconReaction = trimTransparent(decodeHalf(app, R.drawable.icon_reaction_game));
                // Sakura Brawl: face pictures + menu/arena backgrounds
                HubMusic.get(app).warm();   // hub music player
                Assets.preload(app);
                // Critter Toss: all critter sprites + background + music player
                MenuActivity.preload(app);
            } catch (Throwable ignored) {
                // anything missing is simply loaded later, the old way
            } finally {
                done = true;
            }
        }, "pawsome-preload").start();
    }

    public static boolean isDone() { return done; }

    /** The ready-made hub card picture, or null (then the caller loads it normally). */
    public static Bitmap icon(int resId) {
        if (resId == R.drawable.icon_sakura_brawl) return iconSakura;
        if (resId == R.drawable.icon_critter_toss) return iconCritter;
        if (resId == R.drawable.icon_pop_mania) return iconPop;
        if (resId == R.drawable.icon_reaction_game) return iconReaction;
        return null;
    }

    /** Same as icon(), but loads + trims the picture itself when the splash has not done it yet. */
    public static Bitmap loadTrimmed(Context c, int resId) {
        try {
            Bitmap b = decodeHalf(c, resId);
            return b == null ? null : trimTransparent(b);
        } catch (Throwable t) {
            return null;
        }
    }

    /** Cuts away the fully see-through border around a picture so the artwork fills the whole bitmap. */
    private static Bitmap trimTransparent(Bitmap src) {
        if (src == null || !src.hasAlpha()) return src;
        int w = src.getWidth(), h = src.getHeight();
        int[] px = new int[w * h];
        src.getPixels(px, 0, w, 0, 0, w, h);
        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if ((px[y * w + x] >>> 24) > 20) {   // not (almost) transparent
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }
        if (maxX < minX || maxY < minY) return src;                   // nothing visible: leave it alone
        if (minX == 0 && minY == 0 && maxX == w - 1 && maxY == h - 1) return src;
        return Bitmap.createBitmap(src, minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private static Bitmap decodeHalf(Context c, int res) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        o.inSampleSize = 2;
        return BitmapFactory.decodeResource(c.getResources(), res, o);
    }
}