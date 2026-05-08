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

                for (Task t : allTasks) {
                    if (t.isCompleted()) {
                        completed++;
                    } else if (t.getDueDate() != null
                            && t.getDueDate().before(now)) {
                        overdue++;
                        if (overdueNames.length() > 0)
                            overdueNames.append(", ");
                        overdueNames.append(t.getTitle());
                    } else {
                        if (t.getImportance() == 3) highPending++;
                        else if (t.getImportance() == 2) medPending++;
                        else lowPending++;
                        if (upcomingNames.length() < 100) {
                            if (upcomingNames.length() > 0)
                                upcomingNames.append(", ");
                            upcomingNames.append(t.getTitle());
                        }
                    }
                }

                int focusLevel = total > 0
                        ? (completed * 100 / total) : 0;
                String today = new SimpleDateFormat(
                        "EEEE", Locale.getDefault()).format(now);

                String prompt = "You are Brain Friend, a cognitive "
                        + "support AI. Today is " + today + ".\n"
                        + "Student stats: " + completed + " of "
                        + total + " tasks done (" + focusLevel
                        + "% focus).\n"
                        + (overdue > 0
                        ? "OVERDUE tasks: " + overdueNames + ".\n"
                        : "No overdue tasks.\n")
                        + (upcomingNames.length() > 0
                        ? "Upcoming: " + upcomingNames + ".\n"
                        : "")
                        + "High priority pending: " + highPending + ".\n\n"
                        + "Write 2-3 sentences that:\n"
                        + "1. Comment specifically on their "
                        + focusLevel + "% focus progress\n"
                        + (overdue > 0
                        ? "2. Urgently name the overdue tasks and push to act\n"
                        : "2. Celebrate their clean task status\n")
                        + "3. Motivate them for what is coming next\n"
                        + "Use 1-2 emojis. Sound like a caring friend. "
                        + "Be specific about their numbers and task names.";

                callClaude(prompt, callback);
            } catch (Exception e) {
                Log.e("AI", "getHomeInsight: " + e.getMessage());
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
            String prompt = "Brain Friend cognitive coach.\n"
                    + "Student focus: " + focusLevel + "%\n"
                    + "Pending: " + pendingCount + " tasks\n\n"
                    + "Give ONE science-backed tip for this exact "
                    + focusLevel + "% focus level. "
                    + "Max 2 sentences. 1 emoji. "
                    + "Suggest a concrete action RIGHT NOW. "
                    + "Vary between: breathing, hydration, movement, "
                    + "Pomodoro, task batching, music, cold water, "
                    + "phone-free zone, power nap, stretching.";
            callClaude(prompt, callback);
        }).start();
    }

    public static void getWeeklyReport(int completedLW, int totalLW,
                                       int missedLW, String bestDay,
                                       List<String> missedTitles,
                                       AiCallback callback) {
        new Thread(() -> {
            int pct = totalLW > 0 ? (completedLW * 100 / totalLW) : 0;
            String missed = missedTitles.isEmpty()
                    ? "none" : String.join(", ", missedTitles);
            String prompt = "Brain Friend weekly report.\n"
                    + "Last week: " + completedLW + "/" + totalLW
                    + " tasks done (" + pct + "%).\n"
                    + "Best day: " + bestDay + ".\n"
                    + "Missed tasks: " + missed + ".\n\n"
                    + "Write a 2-3 sentence personal weekly summary.\n"
                    + "1. Comment on the " + pct + "% completion rate\n"
                    + (missedLW > 0
                    ? "2. Name the missed tasks specifically\n"
                    : "2. Celebrate the clean week\n")
                    + "3. Set one specific goal for this week\n"
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
                    : importance == 2 ? "medium priority"
                    : "low priority";
            String prompt = "Write a phone notification for the task "
                    + "\"" + taskTitle + "\" starting in 10 minutes.\n"
                    + "Priority: " + priority + ". Category: "
                    + category + ".\n"
                    + "1 sentence max. 1 emoji. Personal and specific "
                    + "to this task name. Not generic. Unique each time.";
            callClaude(prompt, callback);
        }).start();
    }

    public static void getCompletionMessage(String taskTitle,
                                            int importance,
                                            String category,
                                            AiCallback callback) {
        new Thread(() -> {
            String level = importance == 3 ? "high priority"
                    : importance == 2 ? "medium priority"
                    : "low priority";
            String prompt = "The student just completed the "
                    + level + " task \"" + taskTitle
                    + "\" (" + category + ").\n"
                    + "Write a 1-2 sentence personal congratulation "
                    + "message.\n"
                    + "1 emoji. Mention the task name \"" + taskTitle
                    + "\" specifically.\n"
                    + "Celebrate this specific achievement. "
                    + "Sound genuinely proud. Be unique every time.";
            callClaude(prompt, callback);
        }).start();
    }

    public static void getMissedTaskMessage(String taskTitle,
                                            int importance,
                                            String category,
                                            AiCallback callback) {
        new Thread(() -> {
            String urgency = importance == 3
                    ? "HIGH PRIORITY URGENT"
                    : importance == 2 ? "medium priority"
                    : "low priority";
            String prompt = "The student missed the " + urgency
                    + " task \"" + taskTitle + "\" (" + category
                    + ").\n"
                    + "Write a 1-2 sentence motivating alert.\n"
                    + "1 emoji. Name the task \"" + taskTitle
                    + "\" specifically.\n"
                    + "Acknowledge the miss without being harsh. "
                    + "Urgently push them to reschedule it NOW. "
                    + "Be unique every time.";
            callClaude(prompt, callback);
        }).start();
    }

    public static void getTaskSuggestion(int totalPending,
                                         int highCount,
                                         int workCount,
                                         int schoolCount,
                                         AiCallback callback) {
        new Thread(() -> {
            String prompt = "Student has free time with no tasks due.\n"
                    + "They have " + totalPending + " pending tasks: "
                    + highCount + " high priority, "
                    + workCount + " work, " + schoolCount + " school.\n"
                    + "Give ONE specific proactive suggestion. "
                    + "Max 2 sentences. 1 emoji. "
                    + "Be specific to their task breakdown. Unique.";
            callClaude(prompt, callback);
        }).start();
    }
    // ─── Routine daily reminder message ───
    public static void getRoutineReminderMessage(String routineTitle,
                                                 int importance,
                                                 String category,
                                                 AiCallback callback) {
        new Thread(() -> {
            String priority = importance == 3 ? "HIGH priority"
                    : importance == 2 ? "medium priority" : "low priority";

            String prompt =
                    "You are Brain Friend AI. This is a daily routine reminder.\n"
                            + "Routine item: \"" + routineTitle + "\"\n"
                            + "Priority: " + priority + "\n"
                            + "Category: " + category + "\n\n"
                            + "Write a SHORT motivating daily reminder. Max 1 sentence. "
                            + "1 emoji. Sound encouraging and personal. "
                            + "Remind them this is part of their daily routine for "
                            + "cognitive health. Be different every time.";

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
            body.put("max_tokens", 200);

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
            Log.d("AI", "HTTP code: " + code);

            if (code != 200) {
                BufferedReader errBr = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream()));
                StringBuilder errBody = new StringBuilder();
                String errLine;
                while ((errLine = errBr.readLine()) != null)
                    errBody.append(errLine);
                Log.e("AI", "Error: " + errBody);
                callback.onError("API error " + code);
                return;
            }

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
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