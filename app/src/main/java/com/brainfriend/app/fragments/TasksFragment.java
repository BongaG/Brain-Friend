package com.brainfriend.app.fragments;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.brainfriend.app.R;
import com.brainfriend.app.adapters.TasksAdapter;
import com.brainfriend.app.models.Task;
import com.brainfriend.app.reminders.TaskAlarmReceiver;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TasksFragment extends Fragment implements TasksAdapter.OnTaskClickListener {
    private RecyclerView recyclerView;
    private TasksAdapter adapter;
    private FirebaseFirestore db;
    private String userId;

    // Date/time selection state
    private int selectedYear, selectedMonth, selectedDay;
    private int selectedHour, selectedMinute;
    private boolean dateSelected = false;
    private boolean timeSelected = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view;
        try {
            view = inflater.inflate(R.layout.fragment_tasks, container, false);
        } catch (Exception e) {
            return new View(getContext());
        }

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();

        createNotificationChannel();

        recyclerView = view.findViewById(R.id.rv_tasks);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new TasksAdapter(new ArrayList<>(), this);
            recyclerView.setAdapter(adapter);
        }

        FloatingActionButton fab = view.findViewById(R.id.fab_add_task);
        if (fab != null) {
            fab.setOnClickListener(v -> showAddTaskDialog());
        }

        loadTasks();
        return view;
    }

    private void showAddTaskDialog() {
        View dv;
        try {
            dv = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_task, null);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error opening dialog", Toast.LENGTH_SHORT).show();
            return;
        }

        // Reset selections
        dateSelected = false;
        timeSelected = false;
        Calendar now = Calendar.getInstance();
        selectedYear = now.get(Calendar.YEAR);
        selectedMonth = now.get(Calendar.MONTH);
        selectedDay = now.get(Calendar.DAY_OF_MONTH);
        selectedHour = now.get(Calendar.HOUR_OF_DAY);
        selectedMinute = now.get(Calendar.MINUTE);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dv)
                .create();

        TextInputEditText etTitle = dv.findViewById(R.id.et_task_title);
        TextInputEditText etDetails = dv.findViewById(R.id.et_task_details);
        ChipGroup cgImportance = dv.findViewById(R.id.cg_importance);
        ChipGroup cgCategory = dv.findViewById(R.id.cg_category);
        SwitchMaterial switchRecurring = dv.findViewById(R.id.switch_recurring);
        SwitchMaterial switchAlert = dv.findViewById(R.id.switch_alert);
        Button btnPickDate = dv.findViewById(R.id.btn_pick_date);
        Button btnPickTime = dv.findViewById(R.id.btn_pick_time);
        Button btnConfirm = dv.findViewById(R.id.btn_add_task_confirm);

        // Date picker
        if (btnPickDate != null) {
            btnPickDate.setOnClickListener(v -> {
                new DatePickerDialog(requireContext(), (dp, year, month, day) -> {
                    selectedYear = year;
                    selectedMonth = month;
                    selectedDay = day;
                    dateSelected = true;
                    String label = String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month + 1, year);
                    btnPickDate.setText(label);
                }, selectedYear, selectedMonth, selectedDay).show();
            });
        }

        // Time picker
        if (btnPickTime != null) {
            btnPickTime.setOnClickListener(v -> {
                new TimePickerDialog(requireContext(), (tp, hour, minute) -> {
                    selectedHour = hour;
                    selectedMinute = minute;
                    timeSelected = true;
                    String label = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                    btnPickTime.setText(label);
                }, selectedHour, selectedMinute, true).show();
            });
        }

        // Confirm
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                String title = etTitle != null && etTitle.getText() != null
                        ? etTitle.getText().toString().trim() : "";
                String details = etDetails != null && etDetails.getText() != null
                        ? etDetails.getText().toString().trim() : "";

                if (title.isEmpty()) {
                    Toast.makeText(getContext(), "Please enter a task title", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!dateSelected) {
                    Toast.makeText(getContext(), "Please select a due date", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!timeSelected) {
                    Toast.makeText(getContext(), "Please select a due time", Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean isRecurring = switchRecurring != null && switchRecurring.isChecked();
                boolean alertEnabled = switchAlert == null || switchAlert.isChecked();

                // Priority
                int importance = 1;
                if (cgImportance != null) {
                    int selId = cgImportance.getCheckedChipId();
                    if (selId == R.id.chip_high) importance = 3;
                    else if (selId == R.id.chip_medium) importance = 2;
                }

                // Category
                String category = "Personal";
                if (cgCategory != null) {
                    int selCatId = cgCategory.getCheckedChipId();
                    if (selCatId == R.id.chip_school) category = "School";
                    else if (selCatId == R.id.chip_work) category = "Work";
                }

                // Build due date
                Calendar dueCal = Calendar.getInstance();
                dueCal.set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute, 0);

                Task newTask = new Task(title, details, userId, false, importance,
                        category, isRecurring, dueCal.getTime(), selectedHour, selectedMinute, alertEnabled);

                db.collection("tasks").add(newTask).addOnSuccessListener(doc -> {
                    String taskId = doc.getId();
                    db.collection("tasks").document(taskId).update("id", taskId);

                    if (alertEnabled) {
                        scheduleAlert(taskId, title, dueCal);
                    }

                    dialog.dismiss();
                    Toast.makeText(getContext(), "Task added!", Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to save task", Toast.LENGTH_SHORT).show());
            });
        }

        dialog.show();
    }

    private void scheduleAlert(String taskId, String title, Calendar dueCal) {
        try {
            AlarmManager alarmManager = (AlarmManager) requireContext()
                    .getSystemService(Context.ALARM_SERVICE);

            Intent intent = new Intent(requireContext(), TaskAlarmReceiver.class);
            intent.putExtra("task_title", title);
            intent.putExtra("task_id", taskId);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    requireContext(),
                    taskId.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // 10 minutes before
            long alertTime = dueCal.getTimeInMillis() - (10 * 60 * 1000);

            if (alertTime > System.currentTimeMillis() && alarmManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP, alertTime, pendingIntent);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, alertTime, pendingIntent);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "task_alerts",
                    "Task Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Alerts for upcoming tasks");
            NotificationManager manager = requireContext()
                    .getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void loadTasks() {
        if (userId == null) return;
        db.collection("tasks")
                .whereEqualTo("userId", userId)
                .orderBy("importance", Query.Direction.DESCENDING)
                .orderBy("dueDate", Query.Direction.ASCENDING)
                .addSnapshotListener((val, err) -> {
                    if (isAdded() && val != null) {
                        List<Task> tasks = val.toObjects(Task.class);
                        adapter.updateTasks(tasks);
                    }
                });
    }

    @Override
    public void onTaskChecked(Task task, boolean isChecked) {
        if (task != null && task.getId() != null) {
            db.collection("tasks").document(task.getId())
                    .update("completed", isChecked);
        }
    }
}