package com.brainfriend.app.ai;

import android.util.Log;
import com.brainfriend.app.BuildConfig;
import com.brainfriend.app.models.Task;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AiInsightsHelper {

    public interface AiCallback {
        void onResult(String insight);
        void onError(String error);
    }

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL = "claude-opus-4-6";

    // ─── 1. Main Home Insights (all 3 cores + smarter insights) ───
    public static void getHomeInsight(List<Task> allTasks, AiCallback callback) {
        new Thread(() -> {
            try {
                // Build task summary
                int total = allTasks.size();
                int completed = 0;
                int overdue = 0;
                int highPending = 0;
                int medPending = 0;
                int lowPending = 0;
                StringBuilder overdueNames = new StringBuilder();
                StringBuilder upcomingNames = new StringBuilder();
                Date now = new Date();

                // Count tasks by hour to find peak time
                int[] hourCounts = new int[24];

                for (Task t : allTasks) {
                    if (t.isCompleted()) {
                        completed++;
                        hourCounts[t.getDueHour()]++;
                    } else if (t.getDueDate() != null && t.getDueDate().before(now)) {
                        overdue++;
                        if (overdueNames.length() > 0) overdueNames.append(", ");
                        overdueNames.append("\"").append(t.getTitle()).append("\"");
                    } else {
                        if (t.getImportance() == 3) highPending++;
                        else if (t.getImportance() == 2) medPending++;
                        else lowPending++;
                        if (upcomingNames.length() < 100) {
                            if (upcomingNames.length() > 0) upcomingNames.append(", ");
                            upcomingNames.append("\"").append(t.getTitle()).append("\"");
                        }
                    }
                }

                // Find peak hour
                int peakHour = 0;
                int peakCount = 0;
                for (int i = 0; i < 24; i++) {
                    if (hourCounts[i] > peakCount) {
                        peakCount = hourCounts[i];
                        peakHour = i;
                    }
                }

                int focusLevel = total > 0 ? (completed * 100 / total) : 0;
                String today = new SimpleDateFormat("EEEE", Locale.getDefault()).format(now);

                String prompt = "You are Brain Friend, a personal AI productivity coach. " +
                        "Analyse this user's task data and give a SHORT, personal, motivating insight.\n\n" +
                        "TODAY IS: " + today + "\n" +
                        "TOTAL TASKS: " + total + "\n" +
                        "COMPLETED: " + completed + " (Focus level: " + focusLevel + "%)\n" +
                        "OVERDUE TASKS (" + overdue + "): " + (overdue > 0 ? overdueNames : "None") + "\n" +
                        "PENDING - High: " + highPending + ", Medium: " + medPending + ", Low: " + lowPending + "\n" +
                        "UPCOMING: " + (upcomingNames.length() > 0 ? upcomingNames : "None") + "\n" +
                        (peakCount > 0 ? "PEAK PRODUCTIVITY HOUR: " + peakHour + ":00\n" : "") +
                        "\nProvide exactly 3 things:\n" +
                        "1. REMINDER: What should the user focus on right now (personalized)\n" +
                        "2. COGNITIVE TIP: A brain performance suggestion based on their data\n" +
                        "3. MISSED ALERT: " + (overdue > 0 ? "Urgent message about overdue tasks" : "Positive reinforcement since nothing is overdue") + "\n\n" +
                        "Format: Write as one flowing paragraph, max 4 sentences. " +
                        "Be specific, use task names. Use 1-2 emojis. Sound like a friend not a robot. " +
                        (focusLevel < 50 ? "Focus is LOW so be extra encouraging." : "Focus is good so be celebratory.") +
                        (overdue > 0 ? " There are OVERDUE tasks so be urgent." : "");

                callClaude(prompt, callback);

            } catch (Exception e) {
                callback.onError("Failed to analyse tasks");
            }
        }).start();
    }

    // ─── 2. Focus Mode Tip (when focus < 50%) ───
    public static void getFocusTip(int focusLevel, int pendingCount, AiCallback callback) {
        new Thread(() -> {
            try {
                String prompt = "You are Brain Friend AI. The user's focus level is " + focusLevel +
                        "% today with " + pendingCount + " pending tasks. " +
                        "Give ONE specific, science-backed cognitive tip to boost their focus. " +
                        "Max 2 sentences. Be specific and actionable. Use 1 emoji. " +
                        "Different tip each time — vary between: breathing, walking, hydration, " +
                        "Pomodoro, music, cold water, task batching, phone-free zones.";

                callClaude(prompt, callback);
            } catch (Exception e) {
                callback.onError("Could not load tip");
            }
        }).start();
    }

    // ─── 3. Weekly Brain Report (Mondays only) ───
    public static void getWeeklyReport(int completedLastWeek, int totalLastWeek,
                                       int missedLastWeek, String bestDay,
                                       List<String> missedTitles, AiCallback callback) {
        new Thread(() -> {
            try {
                int pct = totalLastWeek > 0 ? (completedLastWeek * 100 / totalLastWeek) : 0;
                String missed = String.join(", ", missedTitles);

                String prompt = "You are Brain Friend AI giving a Monday morning weekly brain report. " +
                        "Last week stats:\n" +
                        "- Completed: " + completedLastWeek + "/" + totalLastWeek + " tasks (" + pct + "%)\n" +
                        "- Best day: " + bestDay + "\n" +
                        "- Missed tasks (" + missedLastWeek + "): " + (missed.isEmpty() ? "None" : missed) + "\n\n" +
                        "Write a friendly weekly summary in 3 sentences max. " +
                        "Mention the completion rate, best day, and give one goal for this week. " +
                        "Sound motivating. Use 1-2 emojis.";

                callClaude(prompt, callback);
            } catch (Exception e) {
                callback.onError("Could not load weekly report");
            }
        }).start();
    }

    // ─── 4. Smart Task Reminder Message (for notifications) ───
    public static void getSmartReminderMessage(String taskTitle, int importance,
                                               String category, AiCallback callback) {
        new Thread(() -> {
            try {
                String priorityWord = importance == 3 ? "HIGH priority" :
                        importance == 2 ? "medium priority" : "low priority";
                String tone = importance == 3 ? "urgent and energetic" :
                        importance == 2 ? "calm and encouraging" : "relaxed and friendly";

                String prompt = "Write a SHORT phone notification message for a task starting in 10 minutes. " +
                        "Task: \"" + taskTitle + "\" | Priority: " + priorityWord +
                        " | Category: " + category + "\n" +
                        "Tone: " + tone + ". Max 1 sentence. Include 1 emoji. " +
                        "Sound personal, not robotic. Don't start with 'Hey'.";

                callClaude(prompt, callback);
            } catch (Exception e) {
                callback.onError(taskTitle + " starts in 10 minutes!");
            }
        }).start();
    }

    // ─── 5. AI Task Suggestion (Tasks page when nothing due today) ───
    public static void getTaskSuggestion(int totalPending, int highCount,
                                         int workCount, int schoolCount,
                                         AiCallback callback) {
        new Thread(() -> {
            try {
                String prompt = "You are Brain Friend AI. The user has free time today — " +
                        "no tasks due. Their pending tasks: " + totalPending + " total, " +
                        highCount + " high priority, " + workCount + " work, " +
                        schoolCount + " school.\n" +
                        "Give ONE short suggestion on what to work on proactively. " +
                        "Max 2 sentences. Use 1 emoji. Be specific based on their numbers.";

                callClaude(prompt, callback);
            } catch (Exception e) {
                callback.onError("You have free time — use it wisely!");
            }
        }).start();
    }

    // ─── Core API caller ───
    private static void callClaude(String prompt, AiCallback callback) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("x-api-key", BuildConfig.ANTHROPIC_API_KEY);
            conn.setRequestProperty("anthropic-version", "2023-06-01");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            JSONObject body = new JSONObject();
            body.put("model", MODEL);
            body.put("max_tokens", 300);

            JSONArray messages = new JSONArray();
            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", prompt);
            messages.put(message);
            body.put("messages", messages);

            OutputStream os = conn.getOutputStream();
            os.write(body.toString().getBytes());
            os.close();

            int responseCode = conn.getResponseCode();
            BufferedReader br;
            if (responseCode == 200) {
                br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            } else {
                br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();

            JSONObject jsonResponse = new JSONObject(response.toString());
            String result = jsonResponse
                    .getJSONArray("content")
                    .getJSONObject(0)
                    .getString("text");

            callback.onResult(result);

        } catch (Exception e) {
            Log.e("AiInsightsHelper", "API error: " + e.getMessage());
            callback.onError("AI temporarily unavailable");
        }
    }
}