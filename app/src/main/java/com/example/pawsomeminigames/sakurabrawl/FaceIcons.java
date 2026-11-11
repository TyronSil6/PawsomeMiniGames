package com.example.pawsomeminigames.sakurabrawl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * The face pictures of the animals (res/drawable/face_capybara.png, face_raccoon.png,
 * face_fox.png, face_sloth.png). If a picture is missing, UiKit simply draws the old
 * Canvas face instead, so nothing crashes.
 */
public final class FaceIcons {

    /** File names without extension, in the same order as the ids in Species. */
    private static final String[] NAMES = {"face_capybara", "face_raccoon", "face_fox", "face_sloth"};

    private static final Bitmap[] BITMAPS = new Bitmap[Species.COUNT];
    private static boolean loaded = false;

    private FaceIcons() {
    }

    /** Call once at the start of the app (MainActivity.onCreate). */
    public static void load(Context context) {
        if (loaded) return;
        Context app = context.getApplicationContext();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;   // keep the picture's real size
        for (int i = 0; i < Species.COUNT; i++) {
            int resId = app.getResources().getIdentifier(NAMES[i], "drawable", app.getPackageName());
            if (resId != 0) {
                BITMAPS[i] = BitmapFactory.decodeResource(app.getResources(), resId, options);
            }
        }
        loaded = true;
    }

    /** The picture for an animal, or null if it is missing. */
    public static Bitmap get(int species) {
        return BITMAPS[Species.safe(species)];
    }
}
