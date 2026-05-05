package com.brainfriend.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StatsManager {
    private static final String PREFS_NAME = "brain_stats";
    private static final String KEY_SEQUENCING_LEVEL = "seq_level";
    private static final String KEY_SEQUENCING_SCORE = "seq_score";
    private static final String KEY_MEMORY_LEVEL = "mem_level";
    private static final String KEY_MEMORY_SCORE = "mem_score";
    private static final String KEY_AUDIO_LEVEL = "aud_level";
    private static final String KEY_AUDIO_SCORE = "aud_score";
    private static final String KEY_PLAY_HISTORY = "play_history";

    private SharedPreferences prefs;

    public StatsManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // Record a play session (call each time a game is completed)
    public void recordPlay() {
        long now = System.currentTimeMillis();
        String history = prefs.getString(KEY_PLAY_HISTORY, "");
        history += now + ",";
        prefs.edit().putString(KEY_PLAY_HISTORY, history).apply();
    }

    // Calculate current streak (consecutive days with at least one play)
    public int getStreak() {
        String history = prefs.getString(KEY_PLAY_HISTORY, "");
        if (history.isEmpty()) return 0;
        String[] timestamps = history.split(",");
        List<String> uniqueDays = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        for (String ts : timestamps) {
            if (ts.isEmpty()) continue;
            long time = Long.parseLong(ts);
            String day = sdf.format(new Date(time));
            if (!uniqueDays.contains(day)) uniqueDays.add(day);
        }
        Collections.sort(uniqueDays, Collections.reverseOrder());
        int streak = 0;
        Calendar cal = Calendar.getInstance();
        String today = sdf.format(cal.getTime());
        for (String day : uniqueDays) {
            if (day.equals(today)) {
                streak = 1;
                cal.add(Calendar.DATE, -1);
                today = sdf.format(cal.getTime());
            } else if (day.equals(today)) {
                streak++;
                cal.add(Calendar.DATE, -1);
                today = sdf.format(cal.getTime());
            } else {
                break;
            }
        }
        return streak;
    }

    // Overall completion rate = average of the three game scores
    public int getOverallCompletionRate() {
        int seq = getSequencingScore();
        int mem = getMemoryScore();
        int aud = getAudioScore();
        if (seq == 0 && mem == 0 && aud == 0) return 0;
        return (seq + mem + aud) / 3;
    }

    // Getters & Setters for Sequencing
    public void setSequencingLevel(int level) { prefs.edit().putInt(KEY_SEQUENCING_LEVEL, level).apply(); }
    public int getSequencingLevel() { return prefs.getInt(KEY_SEQUENCING_LEVEL, 0); }
    public void setSequencingScore(int score) { prefs.edit().putInt(KEY_SEQUENCING_SCORE, score).apply(); }
    public int getSequencingScore() { return prefs.getInt(KEY_SEQUENCING_SCORE, 0); }

    // Getters & Setters for Memory Match
    public void setMemoryLevel(int level) { prefs.edit().putInt(KEY_MEMORY_LEVEL, level).apply(); }
    public int getMemoryLevel() { return prefs.getInt(KEY_MEMORY_LEVEL, 0); }
    public void setMemoryScore(int score) { prefs.edit().putInt(KEY_MEMORY_SCORE, score).apply(); }
    public int getMemoryScore() { return prefs.getInt(KEY_MEMORY_SCORE, 0); }

    // Getters & Setters for Auditory Sequence
    public void setAudioLevel(int level) { prefs.edit().putInt(KEY_AUDIO_LEVEL, level).apply(); }
    public int getAudioLevel() { return prefs.getInt(KEY_AUDIO_LEVEL, 0); }
    public void setAudioScore(int score) { prefs.edit().putInt(KEY_AUDIO_SCORE, score).apply(); }
    public int getAudioScore() { return prefs.getInt(KEY_AUDIO_SCORE, 0); }

    // AI insight: best time of day (simple based on last 5 plays)
    public String getBestTimeOfDay() {
        String history = prefs.getString(KEY_PLAY_HISTORY, "");
        if (history.isEmpty()) return "Not enough data";
        String[] timestamps = history.split(",");
        List<Integer> hours = new ArrayList<>();
        for (String ts : timestamps) {
            if (ts.isEmpty()) continue;
            long time = Long.parseLong(ts);
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(time);
            hours.add(cal.get(Calendar.HOUR_OF_DAY));
        }
        // Find most common hour range (simplified)
        int[] hourCounts = new int[24];
        for (int h : hours) hourCounts[h]++;
        int bestHour = 0;
        for (int i = 1; i < 24; i++) {
            if (hourCounts[i] > hourCounts[bestHour]) bestHour = i;
        }
        if (bestHour < 12) return "Morning (" + bestHour + ":00)";
        else if (bestHour < 18) return "Afternoon (" + bestHour + ":00)";
        else return "Evening (" + bestHour + ":00)";
    }
}