package com.brainfriend.app.fragments;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.brainfriend.app.R;
import com.brainfriend.app.adapters.TasksAdapter;
import com.brainfriend.app.models.Task;
import com.brainfriend.app.reminders.TaskAlarmReceiver;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    private int currentTab = 0;
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

        adapter = new TasksAdapter(new ArrayList<>(), this);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        // Long press to edit task
        rv.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener());

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
    }

    private void updateTabStyles() {
        if (tabDay == null || tabWeek == null || tabMonth == null) return;
        tabDay.setTextColor(Color.parseColor("#BFDBFE"));
        tabDay.setBackgroundColor(Color.TRANSPARENT);
        tabWeek.setTextColor(Color.parseColor("#BFDBFE"));
        tabWeek.setBackgroundColor(Color.TRANSPARENT);
        tabMonth.setTextColor(Color.parseColor("#BFDBFE"));
        tabMonth.setBackgroundColor(Color.TRANSPARENT);
        TextView selected = currentTab == 0 ? tabDay
                : currentTab == 1 ? tabWeek : tabMonth;
        selected.setTextColor(Color.WHITE);
        selected.setBackgroundColor(Color.parseColor("#2563EB"));
    }

    private void updateStats() {
        if (allRoutineTasks.isEmpty()) {
            if (tvProgressPct != null) tvProgressPct.setText("0% Complete");
            if (pbRoutine != null) pbRoutine.setProgress(0);
            if (tvDone != null) tvDone.setText("● 0/0 Done");
            if (tvStreak != null) tvStreak.setText("🔥 0 Done");
            return;
        }

        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        String label;

        if (currentTab == 0) {
            start.set(Calendar.HOUR_OF_DAY, 0);
            start.set(Calendar.MINUTE, 0);
            start.set(Calendar.SECOND, 0);
            end.set(Calendar.HOUR_OF_DAY, 23);
            end.set(Calendar.MINUTE, 59);
            end.set(Calendar.SECOND, 59);
            label = "TODAY'S ROUTINE";
        } else if (currentTab == 1) {
            start.set(Calendar.DAY_OF_WEEK, start.getFirstDayOfWeek());
            start.set(Calendar.HOUR_OF_DAY, 0);
            end.set(Calendar.DAY_OF_WEEK,
                    start.getFirstDayOfWeek() + 6);
            end.set(Calendar.HOUR_OF_DAY, 23);
            end.set(Calendar.MINUTE, 59);
            label = "THIS WEEK'S ROUTINE";
        } else {
            start.set(Calendar.DAY_OF_MONTH, 1);
            start.set(Calendar.HOUR_OF_DAY, 0);
            end.set(Calendar.DAY_OF_MONTH,
                    end.getActualMaximum(Calendar.DAY_OF_MONTH));
            end.set(Calendar.HOUR_OF_DAY, 23);
            end.set(Calendar.MINUTE, 59);
            label = "THIS MONTH'S ROUTINE";
        }

        if (tvStatsTitle != null) tvStatsTitle.setText(label);

        int total = allRoutineTasks.size();
        int done = 0;
        for (Task t : allRoutineTasks) {
            if (t.isCompleted()) done++;
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

    private void loadRoutineTasks() {
        if (userId == null) return;
        db.collection("tasks")
                .whereEqualTo("userId", userId)
                .whereEqualTo("recurring", true)
                .addSnapshotListener((val, err) -> {
                    if (!isAdded() || val == null) return;
                    allRoutineTasks = val.toObjects(Task.class);
                    allRoutineTasks.sort((a, b) -> {
                        int ha = a.getDueHour() * 60 + a.getDueMinute();
                        int hb = b.getDueHour() * 60 + b.getDueMinute();
                        return Integer.compare(ha, hb);
                    });
                    adapter.updateTasks(allRoutineTasks);
                    showEmpty(allRoutineTasks.isEmpty());
                    updateStats();

                    // Schedule daily notification for each routine task
                    for (Task t : allRoutineTasks) {
                        if (t.getId() != null) {
                            scheduleDailyNotification(t);
                        }
                    }
                });
    }

    private void showEmpty(boolean empty) {
        if (llEmpty != null)
            llEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (rv != null)
            rv.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    // ─── Edit task dialog ───
    private void showEditTaskDialog(Task task) {
        View dv = LayoutInflater.from(getContext())
                .inflate(android.R.layout.simple_list_item_1, null);

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 16);

        // Title field
        EditText etTitle = new EditText(getContext());
        etTitle.setText(task.getTitle());
        etTitle.setHint("Task title");
        layout.addView(etTitle);

        // Date button
        final Calendar cal = Calendar.getInstance();
        if (task.getDueDate() != null) {
            cal.setTime(task.getDueDate());
        }
        final int[] selectedYear = {cal.get(Calendar.YEAR)};
        final int[] selectedMonth = {cal.get(Calendar.MONTH)};
        final int[] selectedDay = {cal.get(Calendar.DAY_OF_MONTH)};
        final int[] selectedHour = {task.getDueHour()};
        final int[] selectedMinute = {task.getDueMinute()};

        Button btnDate = new Button(getContext());
        btnDate.setText(String.format(Locale.getDefault(),
                "📅 Date: %02d/%02d/%04d",
                selectedDay[0], selectedMonth[0] + 1, selectedYear[0]));
        btnDate.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#EFF6FF")));
        btnDate.setTextColor(
                android.graphics.Color.parseColor("#2563EB"));
        btnDate.setOnClickListener(v ->
                new DatePickerDialog(requireContext(), (dp, y, m, d) -> {
                    selectedYear[0] = y;
                    selectedMonth[0] = m;
                    selectedDay[0] = d;
                    btnDate.setText(String.format(Locale.getDefault(),
                            "📅 Date: %02d/%02d/%04d",
                            d, m + 1, y));
                }, selectedYear[0], selectedMonth[0],
                        selectedDay[0]).show());
        layout.addView(btnDate);

        // Time button
        Button btnTime = new Button(getContext());
        btnTime.setText(String.format(Locale.getDefault(),
                "⏰ Time: %02d:%02d",
                selectedHour[0], selectedMinute[0]));
        btnTime.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#EFF6FF")));
        btnTime.setTextColor(
                android.graphics.Color.parseColor("#2563EB"));
        btnTime.setOnClickListener(v ->
                new TimePickerDialog(requireContext(), (tp, h, min) -> {
                    selectedHour[0] = h;
                    selectedMinute[0] = min;
                    btnTime.setText(String.format(Locale.getDefault(),
                            "⏰ Time: %02d:%02d", h, min));
                }, selectedHour[0], selectedMinute[0], true).show());
        layout.addView(btnTime);

        new AlertDialog.Builder(requireContext())
                .setTitle("✏️ Edit Routine Task")
                .setView(layout)
                .setPositiveButton("Save", (d, w) -> {
                    String newTitle = etTitle.getText().toString().trim();
                    if (newTitle.isEmpty()) {
                        Toast.makeText(getContext(), "Title required",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Calendar newCal = Calendar.getInstance();
                    newCal.set(selectedYear[0], selectedMonth[0],
                            selectedDay[0], selectedHour[0],
                            selectedMinute[0], 0);

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("title", newTitle);
                    updates.put("dueDate", newCal.getTime());
                    updates.put("dueHour", selectedHour[0]);
                    updates.put("dueMinute", selectedMinute[0]);

                    db.collection("tasks").document(task.getId())
                            .update(updates)
                            .addOnSuccessListener(a -> {
                                Toast.makeText(getContext(),
                                        "✅ Routine task updated!",
                                        Toast.LENGTH_SHORT).show();
                                // Reschedule daily notification
                                task.setTitle(newTitle);
                                task.setDueDate(newCal.getTime());
                                task.setDueHour(selectedHour[0]);
                                task.setDueMinute(selectedMinute[0]);
                                scheduleDailyNotification(task);
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(),
                                            "Failed to update",
                                            Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                // Delete from routine only — sets recurring = false
                .setNeutralButton("Remove from Routine", (d, w) ->
                        new AlertDialog.Builder(requireContext())
                                .setTitle("Remove from Routine?")
                                .setMessage("This removes \""
                                        + task.getTitle()
                                        + "\" from your routine but keeps "
                                        + "it in your task list.")
                                .setPositiveButton("Remove", (d2, w2) -> {
                                    db.collection("tasks")
                                            .document(task.getId())
                                            .update("recurring", false)
                                            .addOnSuccessListener(a ->
                                                    Toast.makeText(
                                                                    getContext(),
                                                                    "Removed from routine",
                                                                    Toast.LENGTH_SHORT)
                                                            .show());
                                    cancelDailyNotification(task);
                                })
                                .setNegativeButton("Cancel", null)
                                .show())
                .show();
    }

    // ─── Schedule daily notification ───
    private void scheduleDailyNotification(Task task) {
        try {
            if (getContext() == null) return;
            AlarmManager alarmManager = (AlarmManager)
                    requireContext().getSystemService(
                            Context.ALARM_SERVICE);

            Intent intent = new Intent(requireContext(),
                    TaskAlarmReceiver.class);
            intent.putExtra("task_title", task.getTitle());
            intent.putExtra("task_id", task.getId());
            intent.putExtra("task_importance", task.getImportance());
            intent.putExtra("task_category",
                    task.getCategory() != null
                            ? task.getCategory() : "Personal");
            intent.putExtra("is_routine", true);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    requireContext(),
                    ("routine_" + task.getId()).hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                            | PendingIntent.FLAG_IMMUTABLE);

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, task.getDueHour());
            cal.set(Calendar.MINUTE, task.getDueMinute());
            cal.set(Calendar.SECOND, 0);

            // If time already passed today start tomorrow
            if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }

            if (alarmManager != null) {
                alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        cal.getTimeInMillis(),
                        AlarmManager.INTERVAL_DAY,
                        pendingIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ─── Cancel daily notification ───
    private void cancelDailyNotification(Task task) {
        try {
            if (getContext() == null) return;
            AlarmManager alarmManager = (AlarmManager)
                    requireContext().getSystemService(
                            Context.ALARM_SERVICE);
            Intent intent = new Intent(requireContext(),
                    TaskAlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    requireContext(),
                    ("routine_" + task.getId()).hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                            | PendingIntent.FLAG_IMMUTABLE);
            if (alarmManager != null)
                alarmManager.cancel(pendingIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onTaskChecked(Task task, boolean isChecked) {
        if (task != null && task.getId() != null) {
            db.collection("tasks").document(task.getId())
                    .update("completed", isChecked);
        }
    }
}