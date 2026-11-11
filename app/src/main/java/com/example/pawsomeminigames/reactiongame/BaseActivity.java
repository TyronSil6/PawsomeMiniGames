package com.example.pawsomeminigames.reactiongame;

import com.example.pawsomeminigames.Immersive;
import com.example.pawsomeminigames.R;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.pawsomeminigames.reactiongame.audio.SoundManager;
import com.example.pawsomeminigames.reactiongame.data.PreferencesManager;

public abstract class BaseActivity extends AppCompatActivity {
    protected PreferencesManager prefs;
    protected SoundManager sound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = PreferencesManager.get(this);
        sound = SoundManager.get();
        sound.init(this);
        // Same full-screen look as the rest of Pawsome Mini Games (status + navigation bars hidden)
        Immersive.apply(this);
    }

    protected void setScreen(@LayoutRes int layoutRes) {
        setContentView(layoutRes);
        final View safe = findViewById(R.id.safe_area);
        if (safe == null) return;
        final int pl = safe.getPaddingLeft(), pt = safe.getPaddingTop();
        final int pr = safe.getPaddingRight(), pb = safe.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(safe, new androidx.core.view.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                        | WindowInsetsCompat.Type.displayCutout());
                v.setPadding(pl + bars.left, pt + bars.top, pr + bars.right, pb + bars.bottom);
                return insets;
            }
        });
        ViewCompat.requestApplyInsets(safe);
    }

    protected SoundManager.Track musicTrack() {
        return SoundManager.Track.MAIN;
    }

    protected boolean musicStartsSuspended() {
        return false;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) Immersive.apply(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sound.playMusic(musicTrack(), musicStartsSuspended());
    }

    @Override
    protected void onPause() {
        super.onPause();
        sound.onAppBackground();   // music stops when leaving this screen; the next screen starts it again
    }

    protected void onTap(@IdRes int id, final Runnable action) {
        View v = findViewById(id);
        if (v == null) return;
        com.example.pawsomeminigames.reactiongame.ui.ButtonFx.attach(v);   // same press squish as the other games
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sound.playSfx(SoundManager.Sfx.BUTTON);
                action.run();
            }
        });
    }

    protected void staggerIn(View... views) {
        long delay = 0;
        float d = getResources().getDisplayMetrics().density;
        for (View v : views) {
            if (v == null) continue;
            v.setAlpha(0f);
            v.setTranslationY(24 * d);
            v.animate().alpha(1f).translationY(0f).setStartDelay(delay).setDuration(320).start();
            delay += 70;
        }
    }
}
