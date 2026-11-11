package com.example.pawsomeminigames.sakurabrawl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads Sakura Brawl's big background pictures once and keeps them, so opening the menu or a match
 * is instant. The splash screen calls preload() on a background thread; MenuView and GameView ask
 * for pictures here (and decode them the normal way if nothing was preloaded).
 */
public final class Assets {
    private Assets() {}

    private static final Map<String, Bitmap> CACHE = new HashMap<>();

    public static void preload(Context c) {
        FaceIcons.load(c);
        bitmap(c, "menu_bg");
        bitmap(c, "arena_bg");
    }

    /** A picture from res/drawable by name, or null if it is not there. */
    public static synchronized Bitmap bitmap(Context c, String name) {
        Bitmap b = CACHE.get(name);
        if (b != null && !b.isRecycled()) return b;
        Context app = c.getApplicationContext();
        int id = app.getResources().getIdentifier(name, "drawable", app.getPackageName());
        if (id == 0) return null;
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inScaled = false;   // keep the real pixel size, whatever drawable folder it is in
            b = BitmapFactory.decodeResource(app.getResources(), id, o);
            if (b != null) CACHE.put(name, b);
            return b;
        } catch (Exception | OutOfMemoryError e) {
            return null;
        }
    }
}