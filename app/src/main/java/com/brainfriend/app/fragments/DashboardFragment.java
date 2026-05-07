package com.brainfriend.app.fragments;

import android.animation.ObjectAnimator;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.brainfriend.app.MainActivity;
import com.brainfriend.app.R;
import com.brainfriend.app.adapters.NotificationsAdapter;
import com.brainfriend.app.ai.AiInsightsHelper;
import com.brainfriend.app.models.NotificationItem;
import com.brainfriend.app.models.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DashboardFragment extends Fragment {

    private FirebaseFirestore db;
    private String userId;
    private final List<NotificationItem> notificationItems = new ArrayList<>();
    private View rootView;
    private TextView tvAiInsight;
    private boolean aiLoaded = false;
    private boolean cognitiveLoaded = false;
    private boolean weeklyLoaded = false;
    private boolean missedAlerted = false;
    private final Set<String> alertedMissedIds = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        try {
            rootView = inflater.inflate(R.layout.fragment_dashboard,
                    container, false);
            return rootView;
        } catch (Exception e) {
            return new View(getContext());
        }
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();

        tvAiInsight = view.findViewById(R.id.tv_ai_insight);

        View cvMissed = view.findViewById(R.id.cv_missed_tasks);
        if (cvMissed != null) cvMissed.setVisibility(View.GONE);

        View cvNext = view.findViewById(R.id.cv_next_task);
        if (cvNext != null) cvNext.setVisibility(View.VISIBLE);

        ImageView ivBell = view.findViewById(R.id.iv_notification_bell);
        if (ivBell != null)
            ivBell.setOnClickListener(v -> showSlideInPanel(v));

        loadUserName(view);
        loadDashboardData(view);
    }

    private void loadUserName(View view) {
        if (userId == null) return;
        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded() || doc == null) return;
                    String name = doc.getString("name");
                    TextView tvGreeting =
                            view.findViewById(R.id.tv_greeting);
                    if (tvGreeting != null && name != null) {
                        tvGreeting.setText("Hello, "
                                + name.split(" ")[0] + "!");
                    }
                });
    }

    private void loadDashboardData(View view) {
        if (userId == null) return;

        db.collection("tasks")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snap, err) -> {
                    if (!isAdded() || snap == null) return;

                    List<Task> allTasks = snap.toObjects(Task.class);
                    int total = allTasks.size();
                    int completed = 0;
                    notificationItems.clear();
                    Date now = new Date();

                    // Today range
                    Calendar startOfDay = Calendar.getInstance();
                    startOfDay.set(Calendar.HOUR_OF_DAY, 0);
                    startOfDay.set(Calendar.MINUTE, 0);
                    startOfDay.set(Calendar.SECOND, 0);
                    Calendar endOfDay = Calendar.getInstance();
                    endOfDay.set(Calendar.HOUR_OF_DAY, 23);
                    endOfDay.set(Calendar.MINUTE, 59);
                    endOfDay.set(Calendar.SECOND, 59);

                    int todayTotal = 0, todayDone = 0, pendingCount = 0;

                    for (Task t : allTasks) {
                        if (t.isCompleted()) {
                            completed++;
                            notificationItems.add(new NotificationItem(
                                    "✅ Completed",
                                    "\"" + t.getTitle() + "\" is done",
                                    NotificationItem.TYPE_DONE,
                                    t.getId(), t.getTitle(),
                                    t.getImportance(), t.getCategory()));
                        } else if (t.getDueDate() != null
                                && t.getDueDate().before(now)) {
                            notificationItems.add(new NotificationItem(
                                    "⚠️ Overdue",
                                    "\"" + t.getTitle() + "\" is overdue",
                                    NotificationItem.TYPE_OVERDUE,
                                    t.getId(), t.getTitle(),
                                    t.getImportance(), t.getCategory()));

                            // Fire missed notification ONCE per task
                            if (t.getId() != null
                                    && !alertedMissedIds.contains(
                                    t.getId())) {
                                alertedMissedIds.add(t.getId());
                                fireMissedTaskNotification(t);
                            }
                        } else if (t.getDueDate() != null) {
                            long diff = t.getDueDate().getTime()
                                    - now.getTime();
                            if (diff > 0 && diff <= 15 * 60 * 1000) {
                                notificationItems.add(new NotificationItem(
                                        "🔔 Starting Soon",
                                        "\"" + t.getTitle()
                                                + "\" starts in 15 min",
                                        NotificationItem.TYPE_ALERT,
                                        t.getId(), t.getTitle(),
                                        t.getImportance(), t.getCategory()));
                            }
                        }

                        if (!t.isCompleted()) pendingCount++;

                        if (t.getDueDate() != null) {
                            long due = t.getDueDate().getTime();
                            if (due >= startOfDay.getTimeInMillis()
                                    && due <= endOfDay.getTimeInMillis()) {
                                todayTotal++;
                                if (t.isCompleted()) todayDone++;
                            }
                        }
                    }

                    // Focus level
                    int focusLevel = total > 0
                            ? (completed * 100 / total) : 0;

                    // Update UI
                    TextView tvFocus =
                            view.findViewById(R.id.tv_focus_level);
                    if (tvFocus != null)
                        tvFocus.setText(focusLevel + "%");

                    TextView tvPending =
                            view.findViewById(R.id.tv_pending_count);
                    if (tvPending != null)
                        tvPending.setText(String.valueOf(pendingCount));

                    // Progress bar
                    int progressPct = todayTotal > 0
                            ? (todayDone * 100 / todayTotal) : 0;
                    int remaining = todayTotal - todayDone;

                    ProgressBar pbToday =
                            view.findViewById(R.id.pb_today);
                    TextView tvProgressPct =
                            view.findViewById(R.id.tv_today_progress_pct);
                    TextView tvTasksDone =
                            view.findViewById(R.id.tv_tasks_done);
                    TextView tvTasksRemaining =
                            view.findViewById(R.id.tv_tasks_remaining);
                    TextView tvStreak =
                            view.findViewById(R.id.tv_streak_badge);

                    if (pbToday != null)
                        pbToday.setProgress(progressPct);
                    if (tvProgressPct != null)
                        tvProgressPct.setText(progressPct + "% Complete");
                    if (tvTasksDone != null)
                        tvTasksDone.setText("● " + todayDone + "/"
                                + todayTotal + " Tasks Done");
                    if (tvTasksRemaining != null)
                        tvTasksRemaining.setText(remaining + " Remaining");
                    if (tvStreak != null)
                        tvStreak.setText("🔥 " + todayDone + " Done Today");

                    // Badge dot
                    View badge =
                            view.findViewById(R.id.notification_badge);
                    if (badge != null) {
                        boolean hasUrgent = notificationItems.stream()
                                .anyMatch(n ->
                                        n.getType()
                                                == NotificationItem.TYPE_OVERDUE
                                                || n.getType()
                                                == NotificationItem.TYPE_ALERT);
                        badge.setVisibility(
                                hasUrgent ? View.VISIBLE : View.GONE);
                    }

                    // Up Next
                    TextView tvStatus =
                            view.findViewById(R.id.tv_upcoming_status);
                    TextView tvTitle =
                            view.findViewById(R.id.tv_upcoming_title);
                    TextView tvTime =
                            view.findViewById(R.id.tv_upcoming_time);

                    Task nextTask = null;
                    for (Task t : allTasks) {
                        if (!t.isCompleted() && t.getDueDate() != null
                                && t.getDueDate().after(now)) {
                            if (nextTask == null || t.getDueDate().before(
                                    nextTask.getDueDate())) {
                                nextTask = t;
                            }
                        }
                    }

                    if (nextTask != null) {
                        Task fn = nextTask;
                        if (tvStatus != null)
                            tvStatus.setVisibility(View.VISIBLE);
                        if (tvTitle != null) tvTitle.setText(fn.getTitle());
                        if (tvTime != null) {
                            SimpleDateFormat sdf = new SimpleDateFormat(
                                    "EEE, dd MMM 'at' HH:mm",
                                    Locale.getDefault());
                            tvTime.setText(sdf.format(fn.getDueDate()));
                        }
                    } else {
                        if (tvStatus != null)
                            tvStatus.setVisibility(View.GONE);
                        if (tvTitle != null)
                            tvTitle.setText(
                                    "All clear! Relax or add a new task.");
                        if (tvTime != null) tvTime.setText("");
                    }

                    // View All
                    TextView tvViewAll =
                            view.findViewById(R.id.tv_view_all);
                    if (tvViewAll != null) {
                        tvViewAll.setOnClickListener(v2 -> {
                            if (getActivity() != null) {
                                com.google.android.material.bottomnavigation
                                        .BottomNavigationView nav =
                                        getActivity().findViewById(
                                                R.id.bottom_navigation);
                                if (nav != null)
                                    nav.setSelectedItemId(R.id.nav_tasks);
                            }
                        });
                    }

                    // AI — load once per session only
                    if (!aiLoaded && isAdded()) {
                        aiLoaded = true;
                        loadAiInsight(allTasks, focusLevel, view);
                    }

                    // Cognitive — load once per session
                    if (!cognitiveLoaded && total > 0 && isAdded()) {
                        cognitiveLoaded = true;
                        loadCognitivePerformanceSuggestion(
                                focusLevel, total - completed,
                                completed, total, view);
                    }

                    // Weekly — load once per session
                    if (!weeklyLoaded && isAdded()) {
                        weeklyLoaded = true;
                        checkWeeklyReport(allTasks, view);
                    }
                });
    }

    private void loadAiInsight(List<Task> tasks, int focusLevel,
                               View view) {
        if (tvAiInsight != null) {
            tvAiInsight.setText("🧠 Analysing your tasks...");
            startPulseAnimation(tvAiInsight);
        }

        AiInsightsHelper.getHomeInsight(tasks,
                new AiInsightsHelper.AiCallback() {
                    @Override
                    public void onResult(String insight) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (!isAdded() || tvAiInsight == null) return;
                            tvAiInsight.clearAnimation();
                            tvAiInsight.setAlpha(1f);
                            tvAiInsight.setText(insight);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (!isAdded() || tvAiInsight == null) return;
                            tvAiInsight.clearAnimation();
                            tvAiInsight.setAlpha(1f);
                            tvAiInsight.setText(
                                    "🧠 Stay focused — you've got this!");
                        });
                    }
                });
    }

    private void loadCognitivePerformanceSuggestion(int focusLevel,
                                                    int pendingCount,
                                                    int completed,
                                                    int total,
                                                    View view) {
        View focusTipCard = view.findViewById(R.id.cv_focus_tip);
        TextView tvFocusTip = view.findViewById(R.id.tv_focus_tip);
        if (focusTipCard == null || tvFocusTip == null) return;

        focusTipCard.setVisibility(View.VISIBLE);

        if (focusLevel < 50) {
            ((CardView) focusTipCard).setCardBackgroundColor(
                    android.graphics.Color.parseColor("#FFFBEB"));
            tvFocusTip.setTextColor(
                    android.graphics.Color.parseColor("#92400E"));
        } else {
            ((CardView) focusTipCard).setCardBackgroundColor(
                    android.graphics.Color.parseColor("#F0FDF4"));
            tvFocusTip.setTextColor(
                    android.graphics.Color.parseColor("#166534"));
        }

        tvFocusTip.setText("🧠 Generating cognitive tip...");
        startPulseAnimation(tvFocusTip);

        String level = focusLevel < 30 ? "very low"
                : focusLevel < 50 ? "low"
                : focusLevel < 70 ? "moderate"
                : focusLevel < 90 ? "good" : "excellent";

        String prompt = "Brain Friend cognitive coach.\n"
                + "Focus: " + focusLevel + "% (" + level + ")\n"
                + "Completed: " + completed + "/" + total + "\n"
                + "Pending: " + pendingCount + "\n"
                + "Give ONE cognitive tip. Max 2 sentences. 1 emoji. "
                + "Science-backed. Actionable NOW. Vary each time between: "
                + "breathing, movement, hydration, breaks, task batching, "
                + "music, phone-free time, celebration, journaling, "
                + "stretching, Pomodoro.";

        AiInsightsHelper.callCognitivePrompt(prompt,
                new AiInsightsHelper.AiCallback() {
                    @Override
                    public void onResult(String suggestion) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (!isAdded()) return;
                            tvFocusTip.clearAnimation();
                            tvFocusTip.setAlpha(1f);
                            tvFocusTip.setText(suggestion);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (!isAdded()) return;
                            tvFocusTip.clearAnimation();
                            tvFocusTip.setAlpha(1f);
                            tvFocusTip.setText(
                                    "💧 Drink water and take a deep breath "
                                            + "before your next task.");
                        });
                    }
                });
    }

    private void checkWeeklyReport(List<Task> allTasks, View view) {
        View weeklyCard = view.findViewById(R.id.cv_weekly_report);
        TextView tvWeekly = view.findViewById(R.id.tv_weekly_report);
        if (weeklyCard == null || tvWeekly == null) return;

        weeklyCard.setVisibility(View.VISIBLE);
        tvWeekly.setText("📊 Generating weekly report...");

        Calendar lastWeekStart = Calendar.getInstance();
        lastWeekStart.add(Calendar.DAY_OF_YEAR, -7);
        Date lastWeekDate = lastWeekStart.getTime();
        Date now = new Date();

        int completedLW = 0, totalLW = 0, missedLW = 0;
        List<String> missedTitles = new ArrayList<>();

        for (Task t : allTasks) {
            if (t.getDueDate() != null
                    && t.getDueDate().after(lastWeekDate)
                    && t.getDueDate().before(now)) {
                totalLW++;
                if (t.isCompleted()) completedLW++;
                else {
                    missedLW++;
                    missedTitles.add(t.getTitle());
                }
            }
        }

        AiInsightsHelper.getWeeklyReport(completedLW, totalLW, missedLW,
                "Wednesday", missedTitles,
                new AiInsightsHelper.AiCallback() {
                    @Override
                    public void onResult(String report) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (!isAdded()) return;
                            tvWeekly.setText(report);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (!isAdded()) return;
                            weeklyCard.setVisibility(View.GONE);
                        });
                    }
                });
    }

    private void fireMissedTaskNotification(Task task) {
        AiInsightsHelper.getMissedTaskMessage(
                task.getTitle(), task.getImportance(),
                task.getCategory() != null ? task.getCategory() : "Personal",
                new AiInsightsHelper.AiCallback() {
                    @Override
                    public void onResult(String message) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (!isAdded()) return;
                            showMissedTaskNotification(
                                    task.getTitle(), message, task.getId());
                        });
                    }

                    @Override
                    public void onError(String error) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (!isAdded()) return;
                            showMissedTaskNotification(task.getTitle(),
                                    "⚠️ You missed \"" + task.getTitle()
                                            + "\" — reschedule it now!",
                                    task.getId());
                        });
                    }
                });
    }

    private void showMissedTaskNotification(String taskTitle,
                                            String message,
                                            String taskId) {
        if (getContext() == null) return;

        android.app.NotificationManager manager =
                (android.app.NotificationManager) requireContext()
                        .getSystemService(Context.NOTIFICATION_SERVICE);

        Intent mainIntent = new Intent(requireContext(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                requireContext(), taskId.hashCode(), mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(
                        requireContext(), "task_alerts")
                        .setSmallIcon(R.drawable.ic_nav_task)
                        .setContentTitle("⚠️ Missed — " + taskTitle)
                        .setContentText(message)
                        .setStyle(new NotificationCompat
                                .BigTextStyle().bigText(message))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

        if (manager != null)
            manager.notify(("missed_" + taskId).hashCode(),
                    builder.build());
    }

    private void startPulseAnimation(View view) {
        ObjectAnimator pulse = ObjectAnimator.ofFloat(
                view, "alpha", 1f, 0.4f);
        pulse.setDuration(800);
        pulse.setRepeatCount(ObjectAnimator.INFINITE);
        pulse.setRepeatMode(ObjectAnimator.REVERSE);
        pulse.start();
    }

    private void showSlideInPanel(View anchor) {
        View panelView = LayoutInflater.from(getContext())
                .inflate(R.layout.panel_notifications, null);

        RecyclerView rv = panelView.findViewById(R.id.rv_notifications);
        LinearLayout llEmpty =
                panelView.findViewById(R.id.ll_notif_empty);

        if (notificationItems.isEmpty()) {
            rv.setVisibility(View.GONE);
            if (llEmpty != null) llEmpty.setVisibility(View.VISIBLE);
        } else {
            rv.setVisibility(View.VISIBLE);
            if (llEmpty != null) llEmpty.setVisibility(View.GONE);
            rv.setLayoutManager(new LinearLayoutManager(getContext()));
            rv.setAdapter(new NotificationsAdapter(
                    new ArrayList<>(notificationItems)));
        }

        int widthPx = (int) (300
                * getResources().getDisplayMetrics().density);
        PopupWindow popup = new PopupWindow(panelView, widthPx,
                ViewGroup.LayoutParams.MATCH_PARENT, true);
        popup.setElevation(24f);
        popup.setOutsideTouchable(true);
        popup.setBackgroundDrawable(
                new android.graphics.drawable.ColorDrawable(
                        android.graphics.Color.TRANSPARENT));
        popup.showAtLocation(
                requireActivity().getWindow().getDecorView(),
                Gravity.END | Gravity.TOP, 0, 0);

        ObjectAnimator slideIn = ObjectAnimator.ofFloat(
                panelView, "translationX", widthPx, 0f);
        slideIn.setDuration(300);
        slideIn.setInterpolator(
                new android.view.animation.DecelerateInterpolator());
        slideIn.start();

        ImageView ivClose = panelView.findViewById(R.id.iv_close_panel);
        if (ivClose != null) {
            ivClose.setOnClickListener(v -> {
                ObjectAnimator slideOut = ObjectAnimator.ofFloat(
                        panelView, "translationX", 0f, widthPx);
                slideOut.setDuration(250);
                slideOut.setInterpolator(
                        new android.view.animation
                                .AccelerateInterpolator());
                slideOut.addListener(
                        new android.animation.AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(
                                    android.animation.Animator a) {
                                popup.dismiss();
                            }
                        });
                slideOut.start();
            });
        }
    }
}