package com.example.pawsomeminigames;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/** First screen of the app: the loading picture with a bar that fills, then the hub opens. */
public class SplashActivity extends AppCompatActivity {
    private static final long LOAD_MS = 2400;   // how long the bar takes to fill
    private static final long HOLD_MS = 250;    // short pause on a full bar

    private final Handler handler = new Handler(Looper.getMainLooper());
    private ValueAnimator anim;
    private LoadingView view;
    private boolean left = false;
    private long t0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        view = new LoadingView(this);
        setContentView(view);
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        c.hide(WindowInsetsCompat.Type.systemBars());
        c.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        t0 = SystemClock.uptimeMillis();
        HubSound.get(this);   // get the click sound ready before the hub opens
        Preloader.start(this);   // heavy pictures / music get ready in the background

        anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(LOAD_MS);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.addUpdateListener(a -> view.setProgress((float) a.getAnimatedValue()));
        anim.start();
        handler.postDelayed(this::tryOpenHub, LOAD_MS + HOLD_MS);
    }

    /** Opens the hub once the bar is full AND the background loading is finished (but never waits forever). */
    private void tryOpenHub() {
        boolean tooLong = SystemClock.uptimeMillis() - t0 > LOAD_MS + HOLD_MS + 3000;
        if (Preloader.isDone() || tooLong) openHub();
        else handler.postDelayed(this::tryOpenHub, 80);
    }

    private void openHub() {
        if (left || isFinishing()) return;
        left = true;
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (anim != null) anim.cancel();
        if (view != null) view.stopAnimating();
        super.onDestroy();
    }
}