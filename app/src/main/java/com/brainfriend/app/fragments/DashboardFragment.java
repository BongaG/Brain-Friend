package com.brainfriend.app.fragments;

import android.animation.ObjectAnimator;
import android.os.Bundle;
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
import com.brainfriend.app.models.NotificationItem;
import com.brainfriend.app.models.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private FirebaseFirestore db;
    private String userId;
    private final List<NotificationItem> notificationItems = new ArrayList<>();
    private View rootView;

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

        View cvMissed = view.findViewById(R.id.cv_missed_tasks);
        if (cvMissed != null) cvMissed.setVisibility(View.GONE);

        View cvNext = view.findViewById(R.id.cv_next_task);
        if (cvNext != null) cvNext.setVisibility(View.VISIBLE);

        ImageView ivBell = view.findViewById(R.id.iv_notification_bell);
        if (ivBell != null) {
            ivBell.setOnClickListener(v -> showSlideInPanel(v));
        }

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

        // All tasks — for focus level + notifications
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
                            notificationItems.add(new NotificationItem(
                                    "✅ Completed",
                                    "\"" + t.getTitle() + "\" is done",
                                    NotificationItem.TYPE_DONE));
                        } else {
                            if (t.getDueDate() != null && t.getDueDate().before(now)) {
                                notificationItems.add(new NotificationItem(
                                        "⚠️ Overdue",
                                        "\"" + t.getTitle() + "\" is overdue",
                                        NotificationItem.TYPE_OVERDUE));
                            } else if (t.getDueDate() != null) {
                                long diff = t.getDueDate().getTime() - now.getTime();
                                if (diff > 0 && diff <= 15 * 60 * 1000) {
                                    notificationItems.add(new NotificationItem(
                                            "🔔 Starting Soon",
                                            "\"" + t.getTitle() + "\" starts in 15 minutes",
                                            NotificationItem.TYPE_ALERT));
                                }
                            }
                        }
                    }

                    // Focus level
                    int focusLevel = total > 0 ? (completed * 100 / total) : 0;
                    TextView tvFocus = view.findViewById(R.id.tv_focus_level);
                    if (tvFocus != null) tvFocus.setText(focusLevel + "%");

                    // Badge dot
                    View badge = view.findViewById(R.id.notification_badge);
                    if (badge != null) {
                        boolean hasUrgent = notificationItems.stream()
                                .anyMatch(n -> n.getType() == NotificationItem.TYPE_OVERDUE
                                        || n.getType() == NotificationItem.TYPE_ALERT);
                        badge.setVisibility(hasUrgent ? View.VISIBLE : View.GONE);
                    }
                });

        // Pending tasks — for count + Up Next
        db.collection("tasks")
                .whereEqualTo("userId", userId)
                .whereEqualTo("completed", false)
                .orderBy("importance", Query.Direction.DESCENDING)
                .orderBy("dueDate", Query.Direction.ASCENDING)
                .addSnapshotListener((val, err) -> {
                    if (!isAdded() || val == null) return;
                    List<Task> tasks = val.toObjects(Task.class);

                    TextView tvPending = view.findViewById(R.id.tv_pending_count);
                    if (tvPending != null) tvPending.setText(String.valueOf(tasks.size()));

                    TextView tvStatus = view.findViewById(R.id.tv_upcoming_status);
                    TextView tvTitle = view.findViewById(R.id.tv_upcoming_title);
                    TextView tvTime = view.findViewById(R.id.tv_upcoming_time);

                    if (!tasks.isEmpty()) {
                        Task next = tasks.get(0);
                        if (tvStatus != null) tvStatus.setVisibility(View.VISIBLE);
                        if (tvTitle != null) tvTitle.setText(next.getTitle());
                        if (tvTime != null) {
                            SimpleDateFormat sdf = new SimpleDateFormat(
                                    "EEE, dd MMM 'at' HH:mm", Locale.getDefault());
                            tvTime.setText(sdf.format(next.getDueDate()));
                        }
                    } else {
                        if (tvStatus != null) tvStatus.setVisibility(View.GONE);
                        if (tvTitle != null) tvTitle.setText("All clear! Relax or add a new task.");
                        if (tvTime != null) tvTime.setText("");
                    }
                });
    }

    private void showSlideInPanel(View anchor) {
        // Inflate panel
        View panelView = LayoutInflater.from(getContext())
                .inflate(R.layout.panel_notifications, null);

        // Setup RecyclerView inside panel
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

        // Create popup window full screen height, 300dp wide
        int widthPx = (int) (300 * getResources().getDisplayMetrics().density);

        PopupWindow popup = new PopupWindow(
                panelView,
                widthPx,
                ViewGroup.LayoutParams.MATCH_PARENT,
                true);

        popup.setElevation(24f);
        popup.setOutsideTouchable(true);
        popup.setBackgroundDrawable(
                new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

        // Show from right edge
        popup.showAtLocation(requireActivity().getWindow().getDecorView(),
                Gravity.END | Gravity.TOP, 0, 0);

        // Slide in from right animation
        ObjectAnimator slideIn = ObjectAnimator.ofFloat(panelView,
                "translationX", widthPx, 0f);
        slideIn.setDuration(300);
        slideIn.setInterpolator(new android.view.animation.DecelerateInterpolator());
        slideIn.start();

        // Close button
        ImageView ivClose = panelView.findViewById(R.id.iv_close_panel);
        if (ivClose != null) {
            ivClose.setOnClickListener(v -> {
                // Slide out animation before dismissing
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