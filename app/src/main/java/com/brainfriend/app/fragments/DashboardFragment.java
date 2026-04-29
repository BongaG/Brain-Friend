package com.brainfriend.app.fragments;

import android.animation.ObjectAnimator;
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
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.brainfriend.app.R;
import com.brainfriend.app.adapters.NotificationsAdapter;
import com.brainfriend.app.ai.AiInsightsHelper;
import com.brainfriend.app.models.NotificationItem;
import com.brainfriend.app.models.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.widget.ProgressBar;

public class DashboardFragment extends Fragment {

    private FirebaseFirestore db;
    private String userId;
    private final List<NotificationItem> notificationItems = new ArrayList<>();
    private View rootView;
    private TextView tvAiInsight;
    private boolean aiLoaded = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        try {
            rootView = inflater.inflate(R.layout.fragment_dashboard, container, false);
            return rootView;
        } catch (Exception e) {
            return new View(getContext());
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
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
    }

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

    private void loadDashboardData(View view) {
        if (userId == null) return;

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
                            // Replace all notificationItems.add() calls with these:

// For completed tasks:
                            // Completed
                            notificationItems.add(new NotificationItem(
                                    "✅ Completed",
                                    "\"" + t.getTitle() + "\" is done",
                                    NotificationItem.TYPE_DONE,
                                    t.getId(),
                                    t.getTitle(),
                                    t.getImportance(),
                                    t.getCategory()));

// Overdue
                            notificationItems.add(new NotificationItem(
                                    "⚠️ Overdue",
                                    "\"" + t.getTitle() + "\" is overdue",
                                    NotificationItem.TYPE_OVERDUE,
                                    t.getId(),
                                    t.getTitle(),
                                    t.getImportance(),
                                    t.getCategory()));

// Starting soon
                            notificationItems.add(new NotificationItem(
                                    "🔔 Starting Soon",
                                    "\"" + t.getTitle() + "\" starts in 15 minutes",
                                    NotificationItem.TYPE_ALERT,
                                    t.getId(),
                                    t.getTitle(),
                                    t.getImportance(),
                                    t.getCategory()));
                            }
                        }

                    // Update notification items}

                    // Focus level
                    int focusLevel = total > 0 ? (completed * 100 / total) : 0;
                    TextView tvFocus = view.findViewById(R.id.tv_focus_level);
                    if (tvFocus != null) tvFocus.setText(focusLevel + "%");

                    // Badge
                    View badge = view.findViewById(R.id.notification_badge);
                    if (badge != null) {
                        boolean hasUrgent = notificationItems.stream()
                                .anyMatch(n -> n.getType() == NotificationItem.TYPE_OVERDUE
                                        || n.getType() == NotificationItem.TYPE_ALERT);
                        badge.setVisibility(hasUrgent ? View.VISIBLE : View.GONE);
                    }

                    // Load AI insight once
                    if (!aiLoaded) {
                        aiLoaded = true;
                        loadAiInsight(allTasks, focusLevel, view);
                    }

                    // Focus tip when below 50%
                    // Focus tip or encouragement based on level
                    if (total > 0) {
                        View focusTipCard = view.findViewById(R.id.cv_focus_tip);
                        TextView tvFocusTip = view.findViewById(R.id.tv_focus_tip);

                        if (focusTipCard != null) focusTipCard.setVisibility(View.VISIBLE);

                        if (focusLevel >= 80) {
                            // High focus — show encouragement
                            if (tvFocusTip != null) {
                                if (focusLevel == 100) {
                                    tvFocusTip.setText("🏆 Perfect focus today! You completed every single task — your brain is firing on all cylinders!");
                                    tvFocusTip.setTextColor(android.graphics.Color.parseColor("#166534"));
                                } else if (focusLevel >= 80) {
                                    tvFocusTip.setText("🔥 " + focusLevel + "% focus — you're crushing it today! Keep this momentum going strong!");
                                    tvFocusTip.setTextColor(android.graphics.Color.parseColor("#166534"));
                                }
                            }
                            // Change card to green
                            if (focusTipCard != null) {
                                ((androidx.cardview.widget.CardView) focusTipCard)
                                        .setCardBackgroundColor(android.graphics.Color.parseColor("#F0FDF4"));
                            }
                        } else if (focusLevel >= 50) {
                            // Medium focus — motivating message
                            if (tvFocusTip != null) {
                                tvFocusTip.setText("💪 " + focusLevel + "% focus — good progress! You're halfway there, keep pushing!");
                                tvFocusTip.setTextColor(android.graphics.Color.parseColor("#92400E"));
                            }
                            if (focusTipCard != null) {
                                ((androidx.cardview.widget.CardView) focusTipCard)
                                        .setCardBackgroundColor(android.graphics.Color.parseColor("#FFFBEB"));
                            }
                        } else {
                            // Low focus — AI tip
                            loadFocusTip(focusLevel, total - completed, view);
                        }
                    }

                    // Weekly report on Mondays
                    checkWeeklyReport(allTasks, view);
                });

        // Pending tasks
        // Pending tasks — for count + Up Next + Today Progress
        db.collection("tasks")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((val, err) -> {
                    if (!isAdded() || val == null) return;
                    List<Task> allTasks = val.toObjects(Task.class);

                    // Get today's date range
                    Calendar startOfDay = Calendar.getInstance();
                    startOfDay.set(Calendar.HOUR_OF_DAY, 0);
                    startOfDay.set(Calendar.MINUTE, 0);
                    startOfDay.set(Calendar.SECOND, 0);
                    Calendar endOfDay = Calendar.getInstance();
                    endOfDay.set(Calendar.HOUR_OF_DAY, 23);
                    endOfDay.set(Calendar.MINUTE, 59);
                    endOfDay.set(Calendar.SECOND, 59);

                    // Filter today's tasks
                    int todayTotal = 0;
                    int todayDone = 0;
                    int pendingCount = 0;
                    Date now = new Date();

                    for (Task t : allTasks) {
                        // Count pending
                        if (!t.isCompleted()) pendingCount++;

                        // Count today's tasks
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
                    if (tvPending != null) tvPending.setText(String.valueOf(pendingCount));

                    // Today's progress
                    int progressPct = todayTotal > 0 ? (todayDone * 100 / todayTotal) : 0;
                    int remaining = todayTotal - todayDone;

                    ProgressBar pbToday = view.findViewById(R.id.pb_today);
                    TextView tvProgressPct = view.findViewById(R.id.tv_today_progress_pct);
                    TextView tvTasksDone = view.findViewById(R.id.tv_tasks_done);
                    TextView tvTasksRemaining = view.findViewById(R.id.tv_tasks_remaining);
                    TextView tvStreak = view.findViewById(R.id.tv_streak_badge);

                    if (pbToday != null) pbToday.setProgress(progressPct);
                    if (tvProgressPct != null) tvProgressPct.setText(progressPct + "% Complete");
                    if (tvTasksDone != null) tvTasksDone.setText("● " + todayDone + "/" + todayTotal + " Tasks Done");
                    if (tvTasksRemaining != null) tvTasksRemaining.setText(remaining + " Remaining");

                    // Streak — count consecutive days with at least 1 completed task
                    if (tvStreak != null) tvStreak.setText("🔥 " + todayDone + " Done Today");

                    // Up Next — show next incomplete task due today
                    TextView tvStatus = view.findViewById(R.id.tv_upcoming_status);
                    TextView tvTitle = view.findViewById(R.id.tv_upcoming_title);
                    TextView tvTime = view.findViewById(R.id.tv_upcoming_time);

                    Task nextTask = null;
                    for (Task t : allTasks) {
                        if (!t.isCompleted() && t.getDueDate() != null
                                && t.getDueDate().after(now)) {
                            if (nextTask == null || t.getDueDate().before(nextTask.getDueDate())) {
                                nextTask = t;
                            }
                        }
                    }

                    if (nextTask != null) {
                        Task finalNext = nextTask;
                        if (tvStatus != null) tvStatus.setVisibility(View.VISIBLE);
                        if (tvTitle != null) tvTitle.setText(finalNext.getTitle());
                        if (tvTime != null) {
                            SimpleDateFormat sdf = new SimpleDateFormat(
                                    "EEE, dd MMM 'at' HH:mm", Locale.getDefault());
                            tvTime.setText(sdf.format(finalNext.getDueDate()));
                        }
                    } else {
                        if (tvStatus != null) tvStatus.setVisibility(View.GONE);
                        if (tvTitle != null) tvTitle.setText("All clear! Relax or add a new task.");
                        if (tvTime != null) tvTime.setText("");
                    }

                    // View All navigates to Tasks tab
                    TextView tvViewAll = view.findViewById(R.id.tv_view_all);
                    if (tvViewAll != null) {
                        tvViewAll.setOnClickListener(v2 -> {
                            if (getActivity() != null) {
                                com.google.android.material.bottomnavigation.BottomNavigationView nav =
                                        getActivity().findViewById(R.id.bottom_navigation);
                                if (nav != null) nav.setSelectedItemId(R.id.nav_tasks);
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

        AiInsightsHelper.getHomeInsight(tasks, new AiInsightsHelper.AiCallback() {
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
                        tvAiInsight.setText("Keep going! Your brain is working hard today 🧠");
                    }
                });
            }
        });
    }

    // ─── Focus Tip ───
    private void loadFocusTip(int focusLevel, int pendingCount, View view) {
        View focusTipCard = view.findViewById(R.id.cv_focus_tip);
        TextView tvFocusTip = view.findViewById(R.id.tv_focus_tip);
        if (focusTipCard == null || tvFocusTip == null) return;

        focusTipCard.setVisibility(View.VISIBLE);
        tvFocusTip.setText("💡 Loading focus tip...");

        AiInsightsHelper.getFocusTip(focusLevel, pendingCount, new AiInsightsHelper.AiCallback() {
            @Override
            public void onResult(String tip) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (!isAdded()) return;
                    tvFocusTip.setText(tip);
                });
            }

            @Override
            public void onError(String error) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (!isAdded()) return;
                    tvFocusTip.setText("💡 Take a 5 min break — your brain needs rest to perform!");
                });
            }
        });
    }

    // ─── Weekly Report (Mondays only) ───
    private void checkWeeklyReport(List<Task> allTasks, View view) {

        View weeklyCard = view.findViewById(R.id.cv_weekly_report);
        TextView tvWeekly = view.findViewById(R.id.tv_weekly_report);
        if (weeklyCard == null || tvWeekly == null) return;

        weeklyCard.setVisibility(View.VISIBLE);
        tvWeekly.setText("📊 Generating your weekly report...");

        // Calculate last week stats
        Calendar lastWeekStart = Calendar.getInstance();
        lastWeekStart.add(Calendar.DAY_OF_YEAR, -7);
        Date lastWeekDate = lastWeekStart.getTime();

        int completedLastWeek = 0;
        int totalLastWeek = 0;
        int missedLastWeek = 0;
        List<String> missedTitles = new ArrayList<>();
        Date now = new Date();

        for (Task t : allTasks) {
            if (t.getDueDate() != null && t.getDueDate().after(lastWeekDate)
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

    // ─── Pulse animation while loading ───
    private void startPulseAnimation(View view) {
        ObjectAnimator pulse = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.4f);
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

        int widthPx = (int) (300 * getResources().getDisplayMetrics().density);
        PopupWindow popup = new PopupWindow(panelView, widthPx,
                ViewGroup.LayoutParams.MATCH_PARENT, true);
        popup.setElevation(24f);
        popup.setOutsideTouchable(true);
        popup.setBackgroundDrawable(
                new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        popup.showAtLocation(requireActivity().getWindow().getDecorView(),
                Gravity.END | Gravity.TOP, 0, 0);

        ObjectAnimator slideIn = ObjectAnimator.ofFloat(panelView, "translationX", widthPx, 0f);
        slideIn.setDuration(300);
        slideIn.setInterpolator(new android.view.animation.DecelerateInterpolator());
        slideIn.start();

        ImageView ivClose = panelView.findViewById(R.id.iv_close_panel);
        if (ivClose != null) {
            ivClose.setOnClickListener(v -> {
                ObjectAnimator slideOut = ObjectAnimator.ofFloat(panelView,
                        "translationX", 0f, widthPx);
                slideOut.setDuration(250);
                slideOut.setInterpolator(new android.view.animation.AccelerateInterpolator());
                slideOut.addListener(new android.animation.AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(android.animation.Animator animation) {
                        popup.dismiss();
                    }
                });
                slideOut.start();
            });
        }
    }
}