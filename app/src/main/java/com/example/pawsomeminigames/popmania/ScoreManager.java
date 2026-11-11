package com.example.pawsomeminigames.popmania;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Saves the scores on the device (SharedPreferences): a top-10 list for each mode
 * (EASY / MEDIUM / HARD), like the Critter Toss leaderboard.
 */
public class ScoreManager {

    public static final String[] MODES = {"EASY", "MEDIUM", "HARD"};
    private static final int MAX_ENTRIES = 10;

    private static final String PREFS_NAME = "popmania_prefs";
    private static final String KEY_BEST_SCORE = "best_score";   // old single best score (before the per-mode boards)

    public static final class Entry {
        public final int score;
        public final long time;
        Entry(int score, long time) { this.score = score; this.time = time; }
    }

    private final Context appContext;
    private final SharedPreferences prefs;

    public ScoreManager(Context context) {
        appContext = context.getApplicationContext();
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** The mode chosen on the How to Play screen (EASY / MEDIUM / HARD). */
    public String getCurrentMode() {
        String m = appContext.getSharedPreferences("popmania_settings", Context.MODE_PRIVATE)
                .getString("difficulty", "MEDIUM");
        for (String s : MODES) if (s.equals(m)) return m;
        return "MEDIUM";
    }

    /** Index (0 = easy, 1 = medium, 2 = hard) of the mode chosen on the How to Play screen. */
    public int getCurrentModeIndex() {
        String m = getCurrentMode();
        for (int i = 0; i < MODES.length; i++) if (MODES[i].equals(m)) return i;
        return 1;
    }

    /** Top scores of one mode, best first. */
    public List<Entry> getScores(int modeIndex) {
        List<Entry> out = new ArrayList<>();
        String raw = prefs.getString("scores_" + MODES[modeIndex], "");
        if (raw.isEmpty()) return out;
        for (String part : raw.split(";")) {
            String[] kv = part.split(":");
            if (kv.length != 2) continue;
            try {
                out.add(new Entry(Integer.parseInt(kv[0]), Long.parseLong(kv[1])));
            } catch (NumberFormatException ignored) { }
        }
        return out;
    }

    /** Best score of one mode (0 if none yet). */
    public int getBestScore(int modeIndex) {
        List<Entry> l = getScores(modeIndex);
        return l.isEmpty() ? 0 : l.get(0).score;
    }

    /** Best score of the mode being played right now. */
    public int getBestScore() {
        return getBestScore(getCurrentModeIndex());
    }

    /** Best score over all modes (shown on the home screen). */
    public int getOverallBest() {
        int best = prefs.getInt(KEY_BEST_SCORE, 0);
        for (int i = 0; i < MODES.length; i++) best = Math.max(best, getBestScore(i));
        return best;
    }

    /**
     * Adds a finished round to the board of the current mode (top 10 kept).
     *
     * @return true if this is a new #1 for that mode
     */
    public boolean saveIfBest(int score) {
        if (score <= 0) return false;
        int mode = getCurrentModeIndex();
        List<Entry> l = getScores(mode);
        boolean newBest = l.isEmpty() || score > l.get(0).score;

        int pos = l.size();
        for (int i = 0; i < l.size(); i++) {
            if (score > l.get(i).score) { pos = i; break; }   // ties stay below the older score
        }
        if (pos >= MAX_ENTRIES) return false;
        l.add(pos, new Entry(score, System.currentTimeMillis()));
        while (l.size() > MAX_ENTRIES) l.remove(l.size() - 1);

        StringBuilder sb = new StringBuilder();
        for (Entry e : l) {
            if (sb.length() > 0) sb.append(';');
            sb.append(e.score).append(':').append(e.time);
        }
        prefs.edit().putString("scores_" + MODES[mode], sb.toString()).apply();
        return newBest;
    }
}