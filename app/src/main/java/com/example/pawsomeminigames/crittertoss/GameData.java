package com.example.pawsomeminigames.crittertoss;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/** Difficulty setting + local leaderboard (top 10 per difficulty). */
public final class GameData {
    public static final int EASY = 0, NORMAL = 1, HARD = 2;
    public static final String[] NAMES = {"Easy", "Normal", "Hard"};
    private static final int MAX_ENTRIES = 10;

    public static final class Entry {
        public final int score;
        public final long time;
        Entry(int score, long time) { this.score = score; this.time = time; }
    }

    private GameData() {}

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences("critter", Context.MODE_PRIVATE);
    }

    public static int getDifficulty(Context c) {
        int d = prefs(c).getInt("difficulty", NORMAL);
        return d < EASY || d > HARD ? NORMAL : d;
    }

    public static void setDifficulty(Context c, int d) {
        prefs(c).edit().putInt("difficulty", d).apply();
    }

    public static List<Entry> scores(Context c, int diff) {
        List<Entry> out = new ArrayList<>();
        String raw = prefs(c).getString("scores_" + diff, "");
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

    public static int best(Context c, int diff) {
        List<Entry> l = scores(c, diff);
        return l.isEmpty() ? 0 : l.get(0).score;
    }

    /** Saves a finished round. Returns its rank (0 = new #1), or -1 if it did not make the top 10. */
    public static int addScore(Context c, int diff, int score) {
        if (score <= 0) return -1;
        List<Entry> l = scores(c, diff);
        int pos = l.size();
        for (int i = 0; i < l.size(); i++) {
            if (score > l.get(i).score) { pos = i; break; }   // ties stay below the older score
        }
        if (pos >= MAX_ENTRIES) return -1;
        l.add(pos, new Entry(score, System.currentTimeMillis()));
        while (l.size() > MAX_ENTRIES) l.remove(l.size() - 1);

        StringBuilder sb = new StringBuilder();
        for (Entry e : l) {
            if (sb.length() > 0) sb.append(';');
            sb.append(e.score).append(':').append(e.time);
        }
        prefs(c).edit().putString("scores_" + diff, sb.toString()).apply();
        return pos;
    }
}