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
    private static final String MODEL = "claude-sonnet-4-20250514";

    // ─── 1. Home Insight ───
    public static void getHomeInsight(List<Task> allTasks, AiCallback callback) {
        new Thread(() -> {
            try {
                int total = allTasks.size();
                int completed = 0;
                int overdue = 0;
                int highPending = 0;
                int medPending = 0;
                int lowPending = 0;
                StringBuilder overdueNames = new StringBuilder();
                StringBuilder upcomingNames = new StringBuilder();
                Date now = new Date();
                int[] hourCounts = new int[24];

                for (Task t : allTasks) {
                    if (t.isCompleted()) {
                        completed++;
                        hourCounts[t.getDueHour()]++;
                    } else if (t.getDueDate() != null
                            && t.getDueDate().before(now)) {
                        overdue++;
                        if (overdueNames.length() > 0)
                            overdueNames.append(", ");
                        overdueNames.append("\"").append(t.getTitle())
                                .append("\"");
                    } else {
                        if (t.getImportance() == 3) highPending++;
                        else if (t.getImportance() == 2) medPending++;
                        else lowPending++;
                        if (upcomingNames.length() < 120) {
                            if (upcomingNames.length() > 0)
                                upcomingNames.append(", ");
                            upcomingNames.append("\"")
                                    .append(t.getTitle()).append("\"");
                        }
                    }
                }

                int peakHour = 0;
                int peakCount = 0;
                for (int i = 0; i < 24; i++) {
                    if (hourCounts[i] > peakCount) {
                        peakCount = hourCounts[i];
                        peakHour = i;
                    }
                }

                int focusLevel = total > 0
                        ? (completed * 100 / total) : 0;
                String today = new SimpleDateFormat(
                        "EEEE", Locale.getDefault()).format(now);

                String prompt =
                        "You are Brain Friend, an AI cognitive support assistant "
                                + "for students with memory and focus challenges.\n\n"
                                + "TODAY: " + today + "\n"
                                + "TOTAL TASKS: " + total + "\n"
                                + "COMPLETED: " + completed
                                + " (Focus: " + focusLevel + "%)\n"
                                + "OVERDUE (" + overdue + "): "
                                + (overdue > 0 ? overdueNames : "None") + "\n"
                                + "HIGH PRIORITY PENDING: " + highPending + "\n"
                                + "MEDIUM PRIORITY PENDING: " + medPending + "\n"
                                + "LOW PRIORITY PENDING: " + lowPending + "\n"
                                + "UPCOMING: " + (upcomingNames.length() > 0
                                ? upcomingNames : "None") + "\n"
                                + (peakCount > 0 ? "PEAK HOUR: " + peakHour
                                + ":00\n" : "")
                                + "\nWrite a short, personal, motivating insight "
                                + "for this student.\n"
                                + "Include:\n"
                                + "1. What to focus on RIGHT NOW\n"
                                + "2. A cognitive performance suggestion\n"
                                + "3. "
                                + (overdue > 0
                                ? "Urgent alert about overdue tasks by name"
                                : "Positive reinforcement")
                                + "\nRules: max 3 sentences, use 1-2 emojis, "
                                + "mention specific task names, sound like a "
                                + "caring friend not a robot, be different every time.";

                callClaude(prompt, callback);

            } catch (Exception e) {
                callClaude(
                        "You are Brain Friend AI. Give a short encouraging "
                                + "cognitive support message for a student. "
                                + "Max 2 sentences. Use 1 emoji.",
                        callback);
            }
        }).start();
    }

    // ─── 2. Cognitive Performance Suggestion ───
    public static void callCognitivePrompt(String prompt,
                                           AiCallback callback) {
        new Thread(() -> callClaude(prompt, callback)).start();
    }

    // ─── 3. Focus Tip ───
    public static void getFocusTip(int focusLevel, int pendingCount,
                                   AiCallback callback) {
        new Thread(() -> {
            String prompt =
                    "You are Brain Friend, a cognitive support AI for "
                            + "students with memory and focus challenges.\n"
                            + "Student focus level: " + focusLevel + "%\n"
                            + "Pending tasks: " + pendingCount + "\n\n"
                            + "Give ONE specific, science-backed cognitive tip. "
                            + "Max 2 sentences. 1 emoji. "
                            + "Be concrete and actionable RIGHT NOW. "
                            + "Never repeat the same tip twice — vary between: "
                            + "breathing exercises, hydration, movement, Pomodoro, "
                            + "task batching, music, cold water, phone-free zones, "
                            + "power nap, journaling, stretching.";
            callClaude(prompt, callback);
        }).start();
    }

    // ─── 4. Weekly Report ───
    public static void getWeeklyReport(int completedLastWeek,
                                       int totalLastWeek,
                                       int missedLastWeek,
                                       String bestDay,
                                       List<String> missedTitles,
                                       AiCallback callback) {
        new Thread(() -> {
            int pct = totalLastWeek > 0
                    ? (completedLastWeek * 100 / totalLastWeek) : 0;
            String missed = missedTitles.isEmpty()
                    ? "None" : String.join(", ", missedTitles);

            String prompt =
                    "You are Brain Friend AI giving a weekly brain report.\n"
                            + "Last week:\n"
                            + "- Completed: " + completedLastWeek + "/"
                            + totalLastWeek + " (" + pct + "%)\n"
                            + "- Best day: " + bestDay + "\n"
                            + "- Missed tasks: " + missed + "\n\n"
                            + "Write a personal weekly summary. Max 3 sentences. "
                            + "Mention the percentage, best day, and one specific "
                            + "goal for this week. Use 1-2 emojis. "
                            + "Be motivating and specific.";
            callClaude(prompt, callback);
        }).start();
    }

    // ─── 5. Smart Reminder Message ───
    public static void getSmartReminderMessage(String taskTitle,
                                               int importance,
                                               String category,
                                               AiCallback callback) {
        new Thread(() -> {
            String priority = importance == 3 ? "HIGH priority"
                    : importance == 2 ? "medium priority"
                    : "low priority";
            String tone = importance == 3
                    ? "urgent and energetic"
                    : importance == 2 ? "calm and encouraging"
                    : "relaxed and friendly";

            String prompt =
                    "Write a short phone notification for a task starting "
                            + "in 10 minutes.\n"
                            + "Task: \"" + taskTitle + "\"\n"
                            + "Priority: " + priority + "\n"
                            + "Category: " + category + "\n"
                            + "Tone: " + tone + "\n"
                            + "Rules: max 1 sentence, 1 emoji, personal not robotic, "
                            + "do not start with Hey, be different every time.";
            callClaude(prompt, callback);
        }).start();
    }

    // ─── 6. Completion Message ───
    public static void getCompletionMessage(String taskTitle,
                                            int importance,
                                            String category,
                                            AiCallback callback) {
        new Thread(() -> {
            String priority = importance == 3 ? "high priority"
                    : importance == 2 ? "medium priority"
                    : "low priority";

            String prompt =
                    "You are Brain Friend AI. The student just completed "
                            + "a task.\n"
                            + "Task: \"" + taskTitle + "\"\n"
                            + "Priority: " + priority + "\n"
                            + "Category: " + category + "\n\n"
                            + "Write a SHORT celebration message. Max 2 sentences. "
                            + "1 emoji. Mention the task name. Sound like a proud "
                            + "friend. Be genuine and specific. "
                            + "Be different every time — vary your tone and words.";
            callClaude(prompt, callback);
        }).start();
    }

    // ─── 7. Missed Task Message ───
    public static void getMissedTaskMessage(String taskTitle,
                                            int importance,
                                            String category,
                                            AiCallback callback) {
        new Thread(() -> {
            String urgency = importance == 3
                    ? "URGENT — this was HIGH priority"
                    : importance == 2 ? "medium priority"
                    : "low priority";

            String prompt =
                    "You are Brain Friend AI. The student missed a task.\n"
                            + "Task: \"" + taskTitle + "\"\n"
                            + "Priority: " + urgency + "\n"
                            + "Category: " + category + "\n\n"
                            + "Write a SHORT motivating alert. Max 2 sentences. "
                            + "1 emoji. Mention the task name. Acknowledge the miss "
                            + "without being harsh. Push them to reschedule NOW. "
                            + "Be specific to the task. "
                            + "Be different every time — never repeat the same message.";
            callClaude(prompt, callback);
        }).start();
    }

    // ─── 8. Task Suggestion (Tasks page) ───
    public static void getTaskSuggestion(int totalPending,
                                         int highCount,
                                         int workCount,
                                         int schoolCount,
                                         AiCallback callback) {
        new Thread(() -> {
            String prompt =
                    "You are Brain Friend AI. The student has free time "
                            + "today with no tasks due.\n"
                            + "Pending tasks: " + totalPending + " total, "
                            + highCount + " high priority, "
                            + workCount + " work, "
                            + schoolCount + " school.\n\n"
                            + "Give ONE proactive suggestion. Max 2 sentences. "
                            + "1 emoji. Be specific about what to tackle. "
                            + "Be different every time.";
            callClaude(prompt, callback);
        }).start();
    }

    // ─── Core Claude API caller ───
    static void callClaude(String prompt, AiCallback callback) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn =
                    (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("x-api-key",
                    BuildConfig.ANTHROPIC_API_KEY);
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
                br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
            } else {
                br = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream()));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();

            JSONObject jsonResponse =
                    new JSONObject(response.toString());
            String result = jsonResponse
                    .getJSONArray("content")
                    .getJSONObject(0)
                    .getString("text");

            callback.onResult(result);

        } catch (Exception e) {
            Log.e("AiInsightsHelper", "API error: " + e.getMessage());
            // Even fallback calls Claude with a simple prompt
            // so it is never hardcoded
            callback.onError("AI unavailable");
        }
    }
}