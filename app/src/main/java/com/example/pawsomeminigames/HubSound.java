package com.example.pawsomeminigames;

import android.content.Context;

/** Click-sound entry point for the hub. The real sound lives in ClickSound (shared by every game). */
final class HubSound {
    private static HubSound instance;

    static HubSound get(Context c) {
        if (instance == null) instance = new HubSound(c.getApplicationContext());
        return instance;
    }

    private final Context ctx;

    private HubSound(Context c) {
        ctx = c;
    }

    void click() {
        ClickSound.play(ctx);
    }
}