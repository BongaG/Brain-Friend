package com.brainfriend.app.fragments;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TasksFragment extends Fragment implements TasksAdapter.OnTaskClickListener {

    private RecyclerView recyclerView;
    private TasksAdapter adapter;
    private FirebaseFirestore db;
    private String userId;
    private View llEmptyState;
    private TextView tvStreak, tvPrioritySummary;

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

        llEmptyState = view.findViewById(R.id.ll_empty_state);
        tvStreak = view.findViewById(R.id.tv_streak);
        tvPrioritySummary = view.findViewById(R.id.tv_priority_summary);

        recyclerView = view.findViewById(R.id.rv_tasks);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new TasksAdapter(new ArrayList<>(), this);
            recyclerView.setAdapter(adapter);
            setupSwipeGestures();
        }

        ChipGroup cgFilter = view.findViewById(R.id.cg_filter);
        if (cgFilter != null) {
            cgFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.isEmpty()) return;
                int id = checkedIds.get(0);
                if (id == R.id.chip_filter_all) adapter.filterAll();
                else if (id == R.id.chip_filter_today) adapter.filterToday();
                else if (id == R.id.chip_filter_high) adapter.filterHigh();
                else if (id == R.id.chip_filter_overdue) adapter.filterOverdue();
                updateEmptyState();
            });
        }

        FloatingActionButton fab = view.findViewById(R.id.fab_add_task);
        if (fab != null) fab.setOnClickListener(v -> showAddTaskDialog());

        loadTasks();
        return view;
    }

    private void setupSwipeGestures() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(@NonNull RecyclerView rv,
                                  @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Task task = adapter.getTaskAt(position);

                if (direction == ItemTouchHelper.RIGHT) {
                    db.collection("tasks").document(task.getId())
                            .update("completed", true);
                    Toast.makeText(getContext(), "✅ Task completed!",
                            Toast.LENGTH_SHORT).show();
                } else {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Delete Task")
                            .setMessage("Delete \"" + task.getTitle() + "\"?")
                            .setPositiveButton("Delete", (d, w) -> {
                                db.collection("tasks").document(task.getId()).delete();
                                Toast.makeText(getContext(), "🗑️ Task deleted",
                                        Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Cancel", (d, w) ->
                                    adapter.notifyItemChanged(position))
                            .show();
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c,
                                    @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState,
                                    boolean isCurrentlyActive) {

                View itemView = viewHolder.itemView;
                Paint paint = new Paint();
                float cornerRadius = 16f;

                if (dX > 0) {
                    paint.setColor(Color.parseColor("#22C55E"));
                    RectF background = new RectF(
                            itemView.getLeft() + 16f,
                            itemView.getTop() + 12f,
                            itemView.getLeft() + dX,
                            itemView.getBottom() - 12f);
                    c.drawRoundRect(background, cornerRadius, cornerRadius, paint);

                    Paint textPaint = new Paint();
                    textPaint.setColor(Color.WHITE);
                    textPaint.setTextSize(40f);
                    textPaint.setAntiAlias(true);
                    c.drawText("✓ Complete",
                            itemView.getLeft() + 40f,
                            itemView.getTop() + (itemView.getHeight() / 2f) + 15f,
                            textPaint);

                } else if (dX < 0) {
                    paint.setColor(Color.parseColor("#EF4444"));
                    RectF background = new RectF(
                            itemView.getRight() + dX,
                            itemView.getTop() + 12f,
                            itemView.getRight() - 16f,
                            itemView.getBottom() - 12f);
                    c.drawRoundRect(background, cornerRadius, cornerRadius, paint);

                    Paint textPaint = new Paint();
                    textPaint.setColor(Color.WHITE);
                    textPaint.setTextSize(40f);
                    textPaint.setAntiAlias(true);
                    textPaint.setTextAlign(Paint.Align.RIGHT);
                    c.drawText("🗑 Delete",
                            itemView.getRight() - 40f,
                            itemView.getTop() + (itemView.getHeight() / 2f) + 15f,
                            textPaint);
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY,
                        actionState, isCurrentlyActive);
            }
        };

        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
    }

    private void updateEmptyState() {
        if (llEmptyState == null || recyclerView == null) return;
        boolean isEmpty = adapter.getItemCount() == 0;
        llEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void updateSummary(List<Task> tasks) {
        int high = 0, med = 0, low = 0, done = 0;
        for (Task t : tasks) {
            if (t.getImportance() == 3) high++;
            else if (t.getImportance() == 2) med++;
            else low++;
            if (t.isCompleted()) done++;
        }
        if (tvPrioritySummary != null) {
            tvPrioritySummary.setText(high + " High · " + med + " Med · " + low + " Low");
        }
        if (tvStreak != null) {
            tvStreak.setText("🔥 " + done + " done today");
        }
    }

    private void showAddTaskDialog() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) requireContext()
                    .getSystemService(Context.ALARM_SERVICE);
            if (am != null && !am.canScheduleExactAlarms()) {
                Intent intent = new Intent(
                        android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
                return;
            }
        }

        View dv;
        try {
            dv = LayoutInflater.from(getContext())
                    .inflate(R.layout.dialog_add_task, null);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error opening dialog",
                    Toast.LENGTH_SHORT).show();
            return;
        }

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

        if (btnPickDate != null) {
            btnPickDate.setOnClickListener(v ->
                    new DatePickerDialog(requireContext(), (dp, year, month, day) -> {
                        selectedYear = year;
                        selectedMonth = month;
                        selectedDay = day;
                        dateSelected = true;
                        btnPickDate.setText(String.format(Locale.getDefault(),
                                "%02d/%02d/%04d", day, month + 1, year));
                    }, selectedYear, selectedMonth, selectedDay).show());
        }

        if (btnPickTime != null) {
            btnPickTime.setOnClickListener(v ->
                    new TimePickerDialog(requireContext(), (tp, hour, minute) -> {
                        selectedHour = hour;
                        selectedMinute = minute;
                        timeSelected = true;
                        btnPickTime.setText(String.format(Locale.getDefault(),
                                "%02d:%02d", hour, minute));
                    }, selectedHour, selectedMinute, true).show());
        }

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                String title = etTitle != null && etTitle.getText() != null
                        ? etTitle.getText().toString().trim() : "";
                String details = etDetails != null && etDetails.getText() != null
                        ? etDetails.getText().toString().trim() : "";

                if (title.isEmpty()) {
                    Toast.makeText(getContext(), "Please enter a title",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!dateSelected) {
                    Toast.makeText(getContext(), "Please select a date",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!timeSelected) {
                    Toast.makeText(getContext(), "Please select a time",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean isRecurring = switchRecurring != null
                        && switchRecurring.isChecked();
                boolean alertEnabled = switchAlert == null
                        || switchAlert.isChecked();

                // ✅ importance and category declared here
                // so they are accessible in scheduleAlert call
                int importance = 1;
                if (cgImportance != null) {
                    int selId = cgImportance.getCheckedChipId();
                    if (selId == R.id.chip_high) importance = 3;
                    else if (selId == R.id.chip_medium) importance = 2;
                }

                String category = "Personal";
                if (cgCategory != null) {
                    int selCatId = cgCategory.getCheckedChipId();
                    if (selCatId == R.id.chip_school) category = "School";
                    else if (selCatId == R.id.chip_work) category = "Work";
                }

                // Make final copies for lambda
                final int finalImportance = importance;
                final String finalCategory = category;

                Calendar dueCal = Calendar.getInstance();
                dueCal.set(selectedYear, selectedMonth, selectedDay,
                        selectedHour, selectedMinute, 0);

                Task newTask = new Task(title, details, userId, false,
                        importance, category, isRecurring,
                        dueCal.getTime(), selectedHour, selectedMinute, alertEnabled);

                db.collection("tasks").add(newTask).addOnSuccessListener(doc -> {
                    String taskId = doc.getId();
                    db.collection("tasks").document(taskId).update("id", taskId);

                    // ✅ Now passes all 5 parameters correctly
                    if (alertEnabled) {
                        scheduleAlert(taskId, title, dueCal,
                                finalImportance, finalCategory);
                    }

                    dialog.dismiss();
                    Toast.makeText(getContext(), "✅ Task added!",
                            Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to save task",
                                Toast.LENGTH_SHORT).show());
            });
        }

        dialog.show();
    }

    // ✅ Correct 5-parameter version
    private void scheduleAlert(String taskId, String title, Calendar dueCal,
                               int importance, String category) {
        try {
            AlarmManager alarmManager = (AlarmManager) requireContext()
                    .getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(requireContext(), TaskAlarmReceiver.class);
            intent.putExtra("task_title", title);
            intent.putExtra("task_id", taskId);
            intent.putExtra("task_importance", importance);
            intent.putExtra("task_category", category);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    requireContext(), taskId.hashCode(), intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            long alertTime = dueCal.getTimeInMillis() - (10 * 60 * 1000);
            if (alertTime > System.currentTimeMillis() && alarmManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP, alertTime, pendingIntent);
                } else {
                    alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP, alertTime, pendingIntent);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "task_alerts", "Task Alerts",
                    NotificationManager.IMPORTANCE_HIGH);
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
                    if (!isAdded() || val == null) return;
                    List<Task> tasks = val.toObjects(Task.class);
                    adapter.updateTasks(tasks);
                    updateEmptyState();
                    updateSummary(tasks);
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