package com.example.pawsomeminigames.reactiongame.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

public final class PreferencesManager {
    private static final String FILE = "reaction_game_prefs";
    private static final String KEY_VIBRATION = "vibration_on";
    private static final String KEY_BEST = "best_score";
    private static final String KEY_HISTORY = "score_history";

    public static final int MAX_HISTORY = 30;

    private static PreferencesManager instance;
    private final SharedPreferences prefs;

    private PreferencesManager(Context appContext) {
        prefs = appContext.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static synchronized PreferencesManager get(Context context) {
        if (instance == null) {
            instance = new PreferencesManager(context.getApplicationContext());
        }
        return instance;
    }

    public boolean isVibrationOn() { return prefs.getBoolean(KEY_VIBRATION, true); }

    public void setVibrationOn(boolean on) { prefs.edit().putBoolean(KEY_VIBRATION, on).apply(); }

    public int getBestScore() { return prefs.getInt(KEY_BEST, 0); }

    public synchronized boolean addScore(int score) {
        boolean isNewBest = score > 0 && score > getBestScore();
        List<ScoreEntry> list = getHistory();
        list.add(0, new ScoreEntry(score, System.currentTimeMillis()));
        while (list.size() > MAX_HISTORY) list.remove(list.size() - 1);
        SharedPreferences.Editor e = prefs.edit();
        if (isNewBest) e.putInt(KEY_BEST, score);
        e.putString(KEY_HISTORY, serialize(list));
        e.apply();
        return isNewBest;
    }

    public List<ScoreEntry> getHistory() {
        List<ScoreEntry> out = new ArrayList<>();
        String raw = prefs.getString(KEY_HISTORY, "");
        if (raw == null || raw.isEmpty()) return out;
        for (String part : raw.split(";")) {
            String[] kv = part.split(",");
            if (kv.length != 2) continue;
            try {
                out.add(new ScoreEntry(Integer.parseInt(kv[0]), Long.parseLong(kv[1])));
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }

    public void resetScores() {
        prefs.edit().remove(KEY_BEST).remove(KEY_HISTORY).apply();
    }

    private static String serialize(List<ScoreEntry> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(';');
            sb.append(list.get(i).score).append(',').append(list.get(i).timeMillis);
        }
        return sb.toString();
    }
}
