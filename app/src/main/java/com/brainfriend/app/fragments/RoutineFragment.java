package com.brainfriend.app.fragments;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.brainfriend.app.R;
import com.brainfriend.app.reminders.RoutineAlarmReceiver;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.brainfriend.app.adapters.RoutineAdapter;
import com.brainfriend.app.models.RoutineItem;

public class RoutineFragment extends Fragment {

    private FirebaseFirestore db;
    private String userId;
    private RoutineAdapter adapter;
    private View llEmpty;
    private RecyclerView rv;
    private ProgressBar pbRoutine;
    private TextView tvProgressPct, tvDone, tvStreak, tvStatsTitle;
    private TextView tabDay, tabWeek, tabMonth;
    private int currentTab = 0;
    private List<RoutineItem> allRoutineItems = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_routine, container, false);

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

        adapter = new RoutineAdapter(new ArrayList<>(),
                new RoutineAdapter.RoutineListener() {
                    @Override
                    public void onComplete(RoutineItem item) {
                        // Toggle completed on routines collection
                        db.collection("routines").document(item.getId())
                                .update("completed", !item.isCompleted());
                    }

                    @Override
                    public void onEdit(RoutineItem item) {
                        showEditDialog(item);
                    }

                    @Override
                    public void onDelete(RoutineItem item) {
                        showDeleteDialog(item);
                    }
                });

        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        if (tabDay != null) tabDay.setOnClickListener(v -> switchTab(0));
        if (tabWeek != null) tabWeek.setOnClickListener(v -> switchTab(1));
        if (tabMonth != null) tabMonth.setOnClickListener(v -> switchTab(2));

        loadRoutineItems();
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
        TextView sel = currentTab == 0 ? tabDay : currentTab == 1 ? tabWeek : tabMonth;
        sel.setTextColor(Color.WHITE);
        sel.setBackgroundColor(Color.parseColor("#2563EB"));
    }

    private void updateStats() {
        String label = currentTab == 0 ? "TODAY'S ROUTINE"
                : currentTab == 1 ? "THIS WEEK'S ROUTINE" : "THIS MONTH'S ROUTINE";
        if (tvStatsTitle != null) tvStatsTitle.setText(label);

        if (allRoutineItems.isEmpty()) {
            if (tvProgressPct != null) tvProgressPct.setText("0% Complete");
            if (pbRoutine != null) pbRoutine.setProgress(0);
            if (tvDone != null) tvDone.setText("● 0/0 Done");
            if (tvStreak != null) tvStreak.setText("🔥 0 Done");
            return;
        }

        int total = allRoutineItems.size();
        int done = 0;
        for (RoutineItem item : allRoutineItems) {
            if (item.isCompleted()) done++;
        }

        int pct = total > 0 ? (done * 100 / total) : 0;
        if (pbRoutine != null) pbRoutine.setProgress(pct);
        if (tvProgressPct != null) tvProgressPct.setText(pct + "% Complete");
        if (tvDone != null) tvDone.setText("● " + done + "/" + total + " Done");
        if (tvStreak != null) tvStreak.setText("🔥 " + done + " Done");
    }

    // Reads from "routines" collection — completely separate from "tasks"
    private void loadRoutineItems() {
        if (userId == null) return;
        db.collection("routines")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((val, err) -> {
                    if (!isAdded() || val == null) return;
                    allRoutineItems = val.toObjects(RoutineItem.class);
                    allRoutineItems.sort((a, b) -> {
                        int ha = a.getDueHour() * 60 + a.getDueMinute();
                        int hb = b.getDueHour() * 60 + b.getDueMinute();
                        return Integer.compare(ha, hb);
                    });
                    adapter.updateItems(allRoutineItems);
                    showEmpty(allRoutineItems.isEmpty());
                    updateStats();
                    // Schedule daily notification for each routine item
                    for (RoutineItem item : allRoutineItems) {
                        if (item.getId() != null) scheduleDailyNotification(item);
                    }
                });
    }

    private void showEmpty(boolean empty) {
        if (llEmpty != null) llEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (rv != null) rv.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void showEditDialog(RoutineItem item) {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 16);

        TextView tvLabel = new TextView(getContext());
        tvLabel.setText("Task Name");
        tvLabel.setTextColor(Color.parseColor("#64748B"));
        tvLabel.setTextSize(12f);
        layout.addView(tvLabel);

        EditText etTitle = new EditText(getContext());
        etTitle.setText(item.getTitle());
        layout.addView(etTitle);

        final int[] h = {item.getDueHour()};
        final int[] min = {item.getDueMinute()};

        android.widget.Button btnTime = new android.widget.Button(getContext());
        btnTime.setText(String.format(Locale.getDefault(), "⏰ %02d:%02d", h[0], min[0]));
        btnTime.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                Color.parseColor("#EFF6FF")));
        btnTime.setTextColor(Color.parseColor("#2563EB"));
        btnTime.setOnClickListener(v ->
                new TimePickerDialog(requireContext(), (tp, hour, minute) -> {
                    h[0] = hour; min[0] = minute;
                    btnTime.setText(String.format(Locale.getDefault(),
                            "⏰ %02d:%02d", hour, minute));
                }, h[0], min[0], true).show());
        layout.addView(btnTime);

        new AlertDialog.Builder(requireContext())
                .setTitle("✏️ Edit Routine")
                .setView(layout)
                .setPositiveButton("Save", (dialog, w) -> {
                    String newTitle = etTitle.getText().toString().trim();
                    if (newTitle.isEmpty()) {
                        Toast.makeText(getContext(), "Title cannot be empty",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("title", newTitle);
                    updates.put("dueHour", h[0]);
                    updates.put("dueMinute", min[0]);
                    db.collection("routines").document(item.getId())
                            .update(updates)
                            .addOnSuccessListener(a -> {
                                Toast.makeText(getContext(), "✅ Updated!",
                                        Toast.LENGTH_SHORT).show();
                                item.setTitle(newTitle);
                                item.setDueHour(h[0]);
                                item.setDueMinute(min[0]);
                                scheduleDailyNotification(item);
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Deletes from "routines" collection only — tasks collection untouched
    private void showDeleteDialog(RoutineItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Remove from Routine?")
                .setMessage("\"" + item.getTitle() + "\" will be permanently removed from your routine.")
                .setPositiveButton("Remove", (d, w) -> {
                    db.collection("routines").document(item.getId())
                            .delete()
                            .addOnSuccessListener(a -> {
                                cancelDailyNotification(item);
                                Toast.makeText(getContext(), "Removed from routine",
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Uses RoutineAlarmReceiver — separate from TaskAlarmReceiver
    private void scheduleDailyNotification(RoutineItem item) {
        try {
            if (getContext() == null) return;
            AlarmManager am = (AlarmManager) requireContext()
                    .getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(requireContext(), RoutineAlarmReceiver.class);
            intent.putExtra("routine_title", item.getTitle());
            intent.putExtra("routine_id", item.getId());
            intent.putExtra("routine_importance", item.getImportance());
            intent.putExtra("routine_category",
                    item.getCategory() != null ? item.getCategory() : "Personal");

            PendingIntent pi = PendingIntent.getBroadcast(
                    requireContext(),
                    ("routine_notif_" + item.getId()).hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, item.getDueHour());
            cal.set(Calendar.MINUTE, item.getDueMinute());
            cal.set(Calendar.SECOND, 0);
            if (cal.getTimeInMillis() <= System.currentTimeMillis())
                cal.add(Calendar.DAY_OF_YEAR, 1);

            if (am != null)
                am.setRepeating(AlarmManager.RTC_WAKEUP,
                        cal.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cancelDailyNotification(RoutineItem item) {
        try {
            if (getContext() == null) return;
            AlarmManager am = (AlarmManager) requireContext()
                    .getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(requireContext(), RoutineAlarmReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(
                    requireContext(),
                    ("routine_notif_" + item.getId()).hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            if (am != null) am.cancel(pi);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}