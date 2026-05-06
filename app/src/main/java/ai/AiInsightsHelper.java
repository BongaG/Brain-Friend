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

    private static final String API_URL =
            "https://api.anthropic.com/v1/messages";
    private static final String MODEL = "claude-haiku-4-5-20251001";

    public static void getHomeInsight(List<Task> allTasks,
                                      AiCallback callback) {
        new Thread(() -> {
            try {
                int total = allTasks.size();
                int completed = 0, overdue = 0;
                int highPending = 0, medPending = 0, lowPending = 0;
                StringBuilder overdueNames = new StringBuilder();
                StringBuilder upcomingNames = new StringBuilder();
                Date now = new Date();
                int[] hourCounts = new int[24];

                for (Task t : allTasks) {
                    if (t.isCompleted()) {
                        completed++;
                        if (t.getDueHour() >= 0 && t.getDueHour() < 24)
                            hourCounts[t.getDueHour()]++;
                    } else if (t.getDueDate() != null
                            && t.getDueDate().before(now)) {
                        overdue++;
                        if (overdueNames.length() > 0)
                            overdueNames.append(", ");
                        overdueNames.append("\"")
                                .append(t.getTitle()).append("\"");
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

                int peakHour = 0, peakCount = 0;
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

                String prompt = "You are Brain Friend, an AI cognitive "
                        + "support assistant for students.\n"
                        + "TODAY: " + today + "\n"
                        + "TOTAL TASKS: " + total + "\n"
                        + "COMPLETED: " + completed
                        + " (Focus: " + focusLevel + "%)\n"
                        + "OVERDUE (" + overdue + "): "
                        + (overdue > 0 ? overdueNames : "None") + "\n"
                        + "HIGH PENDING: " + highPending + "\n"
                        + "MEDIUM PENDING: " + medPending + "\n"
                        + "LOW PENDING: " + lowPending + "\n"
                        + "UPCOMING: "
                        + (upcomingNames.length() > 0
                        ? upcomingNames : "None") + "\n"
                        + (peakCount > 0
                        ? "PEAK HOUR: " + peakHour + ":00\n" : "")
                        + "\nWrite a short personal motivating insight. "
                        + "Max 3 sentences. 1-2 emojis. "
                        + "Mention task names. Sound like a caring friend. "
                        + (overdue > 0
                        ? "Alert urgently about overdue tasks."
                        : "Give positive reinforcement.");

                callClaude(prompt, callback);
            } catch (Exception e) {
                Log.e("AI", "getHomeInsight error: " + e.getMessage());
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public static void callCognitivePrompt(String prompt,
                                           AiCallback callback) {
        new Thread(() -> callClaude(prompt, callback)).start();
    }

    public static void getFocusTip(int focusLevel, int pendingCount,
                                   AiCallback callback) {
        new Thread(() -> {
            String prompt = "You are Brain Friend cognitive AI.\n"
                    + "Student focus: " + focusLevel + "%\n"
                    + "Pending tasks: " + pendingCount + "\n"
                    + "Give ONE science-backed tip. Max 2 sentences. "
                    + "1 emoji. Actionable RIGHT NOW. "
                    + "Vary between: breathing, hydration, movement, "
                    + "Pomodoro, task batching, music, cold water, "
                    + "phone-free zone, power nap, journaling, stretching.";
            callClaude(prompt, callback);
        }).start();
    }

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
            String prompt = "You are Brain Friend AI weekly reporter.\n"
                    + "Completed: " + completedLastWeek + "/"
                    + totalLastWeek + " (" + pct + "%)\n"
                    + "Best day: " + bestDay + "\n"
                    + "Missed: " + missed + "\n"
                    + "Write a personal weekly summary. Max 3 sentences. "
                    + "1-2 emojis. Motivating and specific.";
            callClaude(prompt, callback);
        }).start();
    }

    public static void getSmartReminderMessage(String taskTitle,
                                               int importance,
                                               String category,
                                               AiCallback callback) {
        new Thread(() -> {
            String priority = importance == 3 ? "HIGH priority"
                    : importance == 2 ? "medium priority" : "low priority";
            String tone = importance == 3 ? "urgent and energetic"
                    : importance == 2 ? "calm and encouraging"
                    : "relaxed and friendly";
            String prompt = "Write a phone notification for a task "
                    + "starting in 10 minutes.\n"
                    + "Task: \"" + taskTitle + "\"\n"
                    + "Priority: " + priority + "\n"
                    + "Category: " + category + "\n"
                    + "Tone: " + tone + "\n"
                    + "Max 1 sentence. 1 emoji. Personal. "
                    + "Do not start with Hey. Be unique each time.";
            callClaude(prompt, callback);
        }).start();
    }

    public static void getCompletionMessage(String taskTitle,
                                            int importance,
                                            String category,
                                            AiCallback callback) {
        new Thread(() -> {
            String prompt = "Brain Friend AI: student completed task.\n"
                    + "Task: \"" + taskTitle + "\"\n"
                    + "Priority: " + (importance == 3 ? "high"
                    : importance == 2 ? "medium" : "low") + "\n"
                    + "Category: " + category + "\n"
                    + "Write SHORT celebration. Max 2 sentences. 1 emoji. "
                    + "Mention task name. Sound proud. Be unique.";
            callClaude(prompt, callback);
        }).start();
    }

    public static void getMissedTaskMessage(String taskTitle,
                                            int importance,
                                            String category,
                                            AiCallback callback) {
        new Thread(() -> {
            String urgency = importance == 3 ? "URGENT HIGH priority"
                    : importance == 2 ? "medium priority" : "low priority";
            String prompt = "Brain Friend AI: student missed a task.\n"
                    + "Task: \"" + taskTitle + "\"\n"
                    + "Priority: " + urgency + "\n"
                    + "Category: " + category + "\n"
                    + "Write SHORT motivating alert. Max 2 sentences. "
                    + "1 emoji. Mention task name. Not harsh. "
                    + "Push to reschedule. Be unique each time.";
            callClaude(prompt, callback);
        }).start();
    }

    public static void getTaskSuggestion(int totalPending, int highCount,
                                         int workCount, int schoolCount,
                                         AiCallback callback) {
        new Thread(() -> {
            String prompt = "Brain Friend AI: student has free time.\n"
                    + "Pending: " + totalPending + " tasks, "
                    + highCount + " high priority, "
                    + workCount + " work, " + schoolCount + " school.\n"
                    + "Give ONE proactive suggestion. Max 2 sentences. "
                    + "1 emoji. Specific. Unique each time.";
            callClaude(prompt, callback);
        }).start();
    }

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
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);

            JSONObject body = new JSONObject();
            body.put("model", MODEL);
            body.put("max_tokens", 150);

            JSONArray messages = new JSONArray();
            JSONObject msg = new JSONObject();
            msg.put("role", "user");
            msg.put("content", prompt);
            messages.put(msg);
            body.put("messages", messages);

            OutputStream os = conn.getOutputStream();
            os.write(body.toString().getBytes("UTF-8"));
            os.close();

            int code = conn.getResponseCode();
            Log.d("AI", "Response code: " + code);

            BufferedReader br;
            if (code == 200) {
                br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
            } else {
                br = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream()));
                StringBuilder errBody = new StringBuilder();
                String errLine;
                while ((errLine = br.readLine()) != null)
                    errBody.append(errLine);
                Log.e("AI", "Error body: " + errBody);
                callback.onError("API error " + code);
                return;
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null)
                response.append(line);
            br.close();

            JSONObject json = new JSONObject(response.toString());
            String result = json.getJSONArray("content")
                    .getJSONObject(0)
                    .getString("text");

            callback.onResult(result.trim());

        } catch (Exception e) {
            Log.e("AI", "callClaude exception: " + e.getMessage());
            callback.onError(e.getMessage());
        }
    }
}