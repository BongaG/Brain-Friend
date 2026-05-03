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
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private FirebaseFirestore db;
    private String userId;
    private final List<NotificationItem> notificationItems = new ArrayList<>();
    private View rootView;
    private TextView tvAiInsight;
    private boolean aiLoaded = false;

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
        if (ivBell != null) ivBell.setOnClickListener(v -> showSlideInPanel(v));

        loadUserName(view);
        loadDashboardData(view);
        checkMissedTasksAndAlert(view);
    }

    // ─── Load user name ───
    private void loadUserName(View view) {
        if (userId == null) return;
        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded() || doc == null) return;
                    String name = doc.getString("name");
                    TextView tvGreeting = view.findViewById(R.id.tv_greeting);
                    if (tvGreeting != null && name != null) {
                        String firstName = name.split(" ")[0];
                        tvGreeting.setText("Hello, " + firstName + "!");
                    }
                });
    }

    // ─── Main dashboard data ───
    private void loadDashboardData(View view) {
        if (userId == null) return;

        // ── All tasks listener — AI insight + focus + notifications ──
        db.collection("tasks")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((allVal, err) -> {
                    if (!isAdded() || allVal == null) return;

                    List<Task> allTasks = allVal.toObjects(Task.class);
                    int total = allTasks.size();
                    int completed = 0;
                    notificationItems.clear();
                    Date now = new Date();

                    for (Task t : allTasks) {
                        if (t.isCompleted()) {
                            completed++;
                            // Completed notification
                            notificationItems.add(new NotificationItem(
                                    "✅ Completed",
                                    "\"" + t.getTitle() + "\" is done",
                                    NotificationItem.TYPE_DONE,
                                    t.getId(),
                                    t.getTitle(),
                                    t.getImportance(),
                                    t.getCategory()));
                        } else if (t.getDueDate() != null
                                && t.getDueDate().before(now)) {
                            // Overdue notification
                            notificationItems.add(new NotificationItem(
                                    "⚠️ Overdue",
                                    "\"" + t.getTitle() + "\" is overdue",
                                    NotificationItem.TYPE_OVERDUE,
                                    t.getId(),
                                    t.getTitle(),
                                    t.getImportance(),
                                    t.getCategory()));
                        } else if (t.getDueDate() != null) {
                            long diff = t.getDueDate().getTime() - now.getTime();
                            if (diff > 0 && diff <= 15 * 60 * 1000) {
                                // Starting soon notification
                                notificationItems.add(new NotificationItem(
                                        "🔔 Starting Soon",
                                        "\"" + t.getTitle()
                                                + "\" starts in 15 minutes",
                                        NotificationItem.TYPE_ALERT,
                                        t.getId(),
                                        t.getTitle(),
                                        t.getImportance(),
                                        t.getCategory()));
                            }
                        }
                    }

                    // Focus level
                    int focusLevel = total > 0
                            ? (completed * 100 / total) : 0;
                    TextView tvFocus = view.findViewById(R.id.tv_focus_level);
                    if (tvFocus != null) tvFocus.setText(focusLevel + "%");

                    // Badge dot
                    View badge = view.findViewById(R.id.notification_badge);
                    if (badge != null) {
                        boolean hasUrgent = notificationItems.stream()
                                .anyMatch(n ->
                                        n.getType() == NotificationItem.TYPE_OVERDUE
                                                || n.getType() == NotificationItem.TYPE_ALERT);
                        badge.setVisibility(
                                hasUrgent ? View.VISIBLE : View.GONE);
                    }

                    // AI home insight — load once per session
                    if (!aiLoaded) {
                        aiLoaded = true;
                        loadAiInsight(allTasks, focusLevel, view);
                    }

                    // Cognitive performance suggestion
                    if (total > 0) {
                        loadCognitivePerformanceSuggestion(
                                focusLevel, total - completed,
                                completed, total, view);
                    }

                    // Weekly brain report
                    checkWeeklyReport(allTasks, view);
                });

        // ── Today's progress + pending count + Up Next ──
        db.collection("tasks")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((val, err) -> {
                    if (!isAdded() || val == null) return;
                    List<Task> allTasks = val.toObjects(Task.class);

                    Calendar startOfDay = Calendar.getInstance();
                    startOfDay.set(Calendar.HOUR_OF_DAY, 0);
                    startOfDay.set(Calendar.MINUTE, 0);
                    startOfDay.set(Calendar.SECOND, 0);

                    Calendar endOfDay = Calendar.getInstance();
                    endOfDay.set(Calendar.HOUR_OF_DAY, 23);
                    endOfDay.set(Calendar.MINUTE, 59);
                    endOfDay.set(Calendar.SECOND, 59);

                    int todayTotal = 0;
                    int todayDone = 0;
                    int pendingCount = 0;
                    Date now = new Date();

                    for (Task t : allTasks) {
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

                    // Pending count
                    TextView tvPending = view.findViewById(R.id.tv_pending_count);
                    if (tvPending != null)
                        tvPending.setText(String.valueOf(pendingCount));

                    // Progress bar
                    int progressPct = todayTotal > 0
                            ? (todayDone * 100 / todayTotal) : 0;
                    int remaining = todayTotal - todayDone;

                    ProgressBar pbToday = view.findViewById(R.id.pb_today);
                    TextView tvProgressPct =
                            view.findViewById(R.id.tv_today_progress_pct);
                    TextView tvTasksDone =
                            view.findViewById(R.id.tv_tasks_done);
                    TextView tvTasksRemaining =
                            view.findViewById(R.id.tv_tasks_remaining);
                    TextView tvStreak =
                            view.findViewById(R.id.tv_streak_badge);

                    if (pbToday != null) pbToday.setProgress(progressPct);
                    if (tvProgressPct != null)
                        tvProgressPct.setText(progressPct + "% Complete");
                    if (tvTasksDone != null)
                        tvTasksDone.setText("● " + todayDone + "/"
                                + todayTotal + " Tasks Done");
                    if (tvTasksRemaining != null)
                        tvTasksRemaining.setText(remaining + " Remaining");
                    if (tvStreak != null)
                        tvStreak.setText("🔥 " + todayDone + " Done Today");

                    // Up Next — next incomplete upcoming task
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
                            if (nextTask == null
                                    || t.getDueDate().before(
                                    nextTask.getDueDate())) {
                                nextTask = t;
                            }
                        }
                    }

                    if (nextTask != null) {
                        Task finalNext = nextTask;
                        if (tvStatus != null)
                            tvStatus.setVisibility(View.VISIBLE);
                        if (tvTitle != null)
                            tvTitle.setText(finalNext.getTitle());
                        if (tvTime != null) {
                            SimpleDateFormat sdf = new SimpleDateFormat(
                                    "EEE, dd MMM 'at' HH:mm",
                                    Locale.getDefault());
                            tvTime.setText(sdf.format(
                                    finalNext.getDueDate()));
                        }
                    } else {
                        if (tvStatus != null)
                            tvStatus.setVisibility(View.GONE);
                        if (tvTitle != null)
                            tvTitle.setText(
                                    "All clear! Relax or add a new task.");
                        if (tvTime != null) tvTime.setText("");
                    }

                    // View All → Tasks tab
                    TextView tvViewAll = view.findViewById(R.id.tv_view_all);
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
                });
    }

    // ─── AI Home Insight ───
    private void loadAiInsight(List<Task> tasks, int focusLevel, View view) {
        if (tvAiInsight != null) {
            tvAiInsight.setText("🧠 Analysing your tasks...");
            startPulseAnimation(tvAiInsight);
        }

        AiInsightsHelper.getHomeInsight(tasks,
                new AiInsightsHelper.AiCallback() {
                    @Override
                    public void onResult(String insight) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (!isAdded()) return;
                            if (tvAiInsight != null) {
                                tvAiInsight.clearAnimation();
                                tvAiInsight.setAlpha(1f);
                                tvAiInsight.setText(insight);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (!isAdded()) return;
                            if (tvAiInsight != null) {
                                tvAiInsight.setText(
                                        "Keep going! Your brain is "
                                                + "working hard today 🧠");
                            }
                        });
                    }
                });
    }

    // ─── AI Cognitive Performance Suggestion ───
    private void loadCognitivePerformanceSuggestion(int focusLevel,
                                                    int pendingCount,
                                                    int completed,
                                                    int total,
                                                    View view) {
        View focusTipCard = view.findViewById(R.id.cv_focus_tip);
        TextView tvFocusTip = view.findViewById(R.id.tv_focus_tip);
        if (focusTipCard == null || tvFocusTip == null) return;

        focusTipCard.setVisibility(View.VISIBLE);
        tvFocusTip.setText("🧠 Analysing your cognitive performance...");

        // Card color based on focus level
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

        String level = focusLevel < 30 ? "very low"
                : focusLevel < 50 ? "low"
                : focusLevel < 70 ? "moderate"
                : focusLevel < 90 ? "good" : "excellent";

        String prompt = "You are Brain Friend, a cognitive support AI for "
                + "students with memory and focus challenges.\n\n"
                + "User's current cognitive performance data:\n"
                + "- Focus level: " + focusLevel + "% (" + level + ")\n"
                + "- Tasks completed today: " + completed
                + " out of " + total + "\n"
                + "- Tasks still pending: " + pendingCount + "\n\n"
                + "Give ONE specific, science-backed cognitive performance "
                + "suggestion tailored to this exact focus level and task "
                + "completion rate.\n"
                + "Rules:\n"
                + "- Max 2 sentences\n"
                + "- Be specific to their numbers\n"
                + "- Suggest a concrete action they can take RIGHT NOW\n"
                + "- Use 1 relevant emoji\n"
                + "- Sound like a supportive cognitive coach\n"
                + "- Vary between: breathing, breaks, task batching, "
                + "hydration, movement, focus techniques, celebration";

        AiInsightsHelper.callCognitivePrompt(prompt,
                new AiInsightsHelper.AiCallback() {
                    @Override
                    public void onResult(String suggestion) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (!isAdded()) return;
                            tvFocusTip.setText(suggestion);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (!isAdded()) return;
                            if (focusLevel >= 80) {
                                tvFocusTip.setText("🔥 " + focusLevel
                                        + "% focus — outstanding! Your brain "
                                        + "is performing at its peak today.");
                            } else if (focusLevel >= 50) {
                                tvFocusTip.setText("💪 " + focusLevel
                                        + "% focus — solid progress! Complete "
                                        + pendingCount
                                        + " more tasks to push higher.");
                            } else {
                                tvFocusTip.setText("💡 Focus at " + focusLevel
                                        + "% — try a 5 min walk to reset "
                                        + "your brain before your next task.");
                            }
                        });
                    }
                });
    }

    // ─── Weekly Brain Report ───
    private void checkWeeklyReport(List<Task> allTasks, View view) {
        View weeklyCard = view.findViewById(R.id.cv_weekly_report);
        TextView tvWeekly = view.findViewById(R.id.tv_weekly_report);
        if (weeklyCard == null || tvWeekly == null) return;

        weeklyCard.setVisibility(View.VISIBLE);
        tvWeekly.setText("📊 Generating your weekly report...");

        Calendar lastWeekStart = Calendar.getInstance();
        lastWeekStart.add(Calendar.DAY_OF_YEAR, -7);
        Date lastWeekDate = lastWeekStart.getTime();

        int completedLastWeek = 0;
        int totalLastWeek = 0;
        int missedLastWeek = 0;
        List<String> missedTitles = new ArrayList<>();
        Date now = new Date();

        for (Task t : allTasks) {
            if (t.getDueDate() != null
                    && t.getDueDate().after(lastWeekDate)
                    && t.getDueDate().before(now)) {
                totalLastWeek++;
                if (t.isCompleted()) completedLastWeek++;
                else {
                    missedLastWeek++;
                    missedTitles.add(t.getTitle());
                }
            }
        }

        AiInsightsHelper.getWeeklyReport(
                completedLastWeek, totalLastWeek,
                missedLastWeek, "Wednesday",
                missedTitles,
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

    // ─── Check and alert missed tasks ───
    private void checkMissedTasksAndAlert(View view) {
        if (userId == null) return;
        db.collection("tasks")
                .whereEqualTo("userId", userId)
                .whereEqualTo("completed", false)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded() || snap == null) return;
                    List<Task> tasks = snap.toObjects(Task.class);
                    Date now = new Date();
                    for (Task t : tasks) {
                        if (t.getDueDate() != null
                                && t.getDueDate().before(now)
                                && t.getId() != null) {
                            fireMissedTaskNotification(t);
                        }
                    }
                });
    }

    private void fireMissedTaskNotification(Task task) {
        AiInsightsHelper.getMissedTaskMessage(
                task.getTitle(),
                task.getImportance(),
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
                            showMissedTaskNotification(
                                    task.getTitle(),
                                    "⚠️ \"" + task.getTitle()
                                            + "\" was missed — reschedule it "
                                            + "now to stay on track!",
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
                        .setContentTitle("⚠️ Missed Task — " + taskTitle)
                        .setContentText(message)
                        .setStyle(new NotificationCompat
                                .BigTextStyle().bigText(message))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

        if (manager != null) {
            manager.notify(("missed_" + taskId).hashCode(),
                    builder.build());
        }
    }

    // ─── Pulse animation ───
    private void startPulseAnimation(View view) {
        ObjectAnimator pulse = ObjectAnimator.ofFloat(
                view, "alpha", 1f, 0.4f);
        pulse.setDuration(800);
        pulse.setRepeatCount(ObjectAnimator.INFINITE);
        pulse.setRepeatMode(ObjectAnimator.REVERSE);
        pulse.start();
    }

    // ─── Notification slide panel ───
    private void showSlideInPanel(View anchor) {
        View panelView = LayoutInflater.from(getContext())
                .inflate(R.layout.panel_notifications, null);

        RecyclerView rv = panelView.findViewById(R.id.rv_notifications);
        LinearLayout llEmpty = panelView.findViewById(R.id.ll_notif_empty);

        if (notificationItems.isEmpty()) {
            rv.setVisibility(View.GONE);
            if (llEmpty != null) llEmpty.setVisibility(View.VISIBLE);
        } else {
            rv.setVisibility(View.VISIBLE);
            if (llEmpty != null) llEmpty.setVisibility(View.GONE);
            rv.setLayoutManager(new LinearLayoutManager(getContext()));
            rv.setAdapter(new NotificationsAdapter(notificationItems));
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
                        new android.view.animation.AccelerateInterpolator());
                slideOut.addListener(
                        new android.animation.AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(
                                    android.animation.Animator animation) {
                                popup.dismiss();
                            }
                        });
                slideOut.start();
            });
        }
    }
}