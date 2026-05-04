package com.brainfriend.app.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.brainfriend.app.R;
import com.brainfriend.app.adapters.TasksAdapter;
import com.brainfriend.app.models.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class RoutineFragment extends Fragment
        implements TasksAdapter.OnTaskClickListener {

    private FirebaseFirestore db;
    private String userId;
    private TasksAdapter adapter;
    private View llEmpty;
    private RecyclerView rv;
    private ProgressBar pbRoutine;
    private TextView tvProgressPct, tvDone, tvStreak,
            tvStatsTitle, tabDay, tabWeek, tabMonth;

    private int currentTab = 0; // 0=day, 1=week, 2=month
    private List<Task> allRoutineTasks = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_routine,
                container, false);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();

        // Views
        llEmpty = view.findViewById(R.id.ll_routine_empty);
        rv = view.findViewById(R.id.rv_routine_tasks);
        pbRoutine = view.findViewById(R.id.pb_routine);
        tvProgressPct = view.findViewById(R.id.tv_routine_progress_pct);
        tvDone = view.findViewById(R.id.tv_routine_done);
        tvStreak = view.findViewById(R.id.tv_routine_streak);
        tvStatsTitle = view.findViewById(R.id.tv_routine_stats_title);
        tabDay = view.findViewById(R.id.tab_day);
        tabWeek = view.findViewById(R.id.tab_week);
        tabMonth = view.findViewById(R.id.tab_month);

        // Setup RecyclerView
        adapter = new TasksAdapter(new ArrayList<>(), this);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        // Tab clicks
        if (tabDay != null) tabDay.setOnClickListener(v -> switchTab(0));
        if (tabWeek != null) tabWeek.setOnClickListener(v -> switchTab(1));
        if (tabMonth != null) tabMonth.setOnClickListener(v -> switchTab(2));

        loadRoutineTasks();
        return view;
    }

    private void switchTab(int tab) {
        currentTab = tab;
        updateTabStyles();
        updateStats();
        filterTasksForTab();
    }

    private void updateTabStyles() {
        if (tabDay == null || tabWeek == null || tabMonth == null) return;

        // Reset all
        tabDay.setTextColor(Color.parseColor("#BFDBFE"));
        tabDay.setBackgroundColor(Color.TRANSPARENT);
        tabWeek.setTextColor(Color.parseColor("#BFDBFE"));
        tabWeek.setBackgroundColor(Color.TRANSPARENT);
        tabMonth.setTextColor(Color.parseColor("#BFDBFE"));
        tabMonth.setBackgroundColor(Color.TRANSPARENT);

        // Highlight selected
        TextView selected = currentTab == 0 ? tabDay :
                currentTab == 1 ? tabWeek : tabMonth;
        selected.setTextColor(Color.WHITE);
        selected.setBackgroundColor(Color.parseColor("#2563EB"));
    }

    private void updateStats() {
        if (allRoutineTasks.isEmpty()) {
            if (tvProgressPct != null) tvProgressPct.setText("0% Complete");
            if (pbRoutine != null) pbRoutine.setProgress(0);
            if (tvDone != null) tvDone.setText("● 0/0 Done");
            if (tvStreak != null) tvStreak.setText("🔥 0 Day Streak");
            return;
        }

        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();

        String label;
        if (currentTab == 0) {
            // Today
            start.set(Calendar.HOUR_OF_DAY, 0);
            start.set(Calendar.MINUTE, 0);
            start.set(Calendar.SECOND, 0);
            end.set(Calendar.HOUR_OF_DAY, 23);
            end.set(Calendar.MINUTE, 59);
            end.set(Calendar.SECOND, 59);
            label = "TODAY'S ROUTINE";
        } else if (currentTab == 1) {
            // This week
            start.set(Calendar.DAY_OF_WEEK,
                    start.getFirstDayOfWeek());
            start.set(Calendar.HOUR_OF_DAY, 0);
            end.set(Calendar.DAY_OF_WEEK,
                    start.getFirstDayOfWeek() + 6);
            end.set(Calendar.HOUR_OF_DAY, 23);
            end.set(Calendar.MINUTE, 59);
            label = "THIS WEEK'S ROUTINE";
        } else {
            // This month
            start.set(Calendar.DAY_OF_MONTH, 1);
            start.set(Calendar.HOUR_OF_DAY, 0);
            end.set(Calendar.DAY_OF_MONTH,
                    end.getActualMaximum(Calendar.DAY_OF_MONTH));
            end.set(Calendar.HOUR_OF_DAY, 23);
            end.set(Calendar.MINUTE, 59);
            label = "THIS MONTH'S ROUTINE";
        }

        if (tvStatsTitle != null) tvStatsTitle.setText(label);

        int total = 0;
        int done = 0;

        for (Task t : allRoutineTasks) {
            if (t.getDueDate() != null) {
                long due = t.getDueDate().getTime();
                if (due >= start.getTimeInMillis()
                        && due <= end.getTimeInMillis()) {
                    total++;
                    if (t.isCompleted()) done++;
                }
            }
        }

        // If today tab show all recurring tasks
        if (currentTab == 0 && total == 0) {
            total = allRoutineTasks.size();
            done = 0;
            for (Task t : allRoutineTasks) {
                if (t.isCompleted()) done++;
            }
        }

        int pct = total > 0 ? (done * 100 / total) : 0;

        if (pbRoutine != null) pbRoutine.setProgress(pct);
        if (tvProgressPct != null)
            tvProgressPct.setText(pct + "% Complete");
        if (tvDone != null)
            tvDone.setText("● " + done + "/" + total + " Done");
        if (tvStreak != null)
            tvStreak.setText("🔥 " + done + " Done");
    }

    private void filterTasksForTab() {
        if (allRoutineTasks.isEmpty()) {
            showEmpty(true);
            return;
        }

        // For routine always show all recurring tasks
        // sorted by time
        adapter.updateTasks(allRoutineTasks);
        showEmpty(allRoutineTasks.isEmpty());
    }

    private void showEmpty(boolean empty) {
        if (llEmpty != null)
            llEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (rv != null)
            rv.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void loadRoutineTasks() {
        if (userId == null) return;

        db.collection("tasks")
                .whereEqualTo("userId", userId)
                .whereEqualTo("recurring", true)
                .addSnapshotListener((val, err) -> {
                    if (!isAdded() || val == null) return;

                    allRoutineTasks = val.toObjects(Task.class);

                    // Sort by time
                    allRoutineTasks.sort((a, b) -> {
                        int hourA = a.getDueHour() * 60 + a.getDueMinute();
                        int hourB = b.getDueHour() * 60 + b.getDueMinute();
                        return Integer.compare(hourA, hourB);
                    });

                    adapter.updateTasks(allRoutineTasks);
                    showEmpty(allRoutineTasks.isEmpty());
                    updateStats();
                });
    }

    @Override
    public void onTaskChecked(Task task, boolean isChecked) {
        if (task != null && task.getId() != null) {
            db.collection("tasks")
                    .document(task.getId())
                    .update("completed", isChecked);
        }
    }
}