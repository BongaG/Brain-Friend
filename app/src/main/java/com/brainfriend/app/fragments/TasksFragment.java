package com.brainfriend.app.fragments;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
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

// The single most important import
import com.brainfriend.app.R;
import com.brainfriend.app.adapters.TasksAdapter;
import com.brainfriend.app.models.Task;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TasksFragment extends Fragment implements TasksAdapter.OnTaskClickListener {
    private static final String TAG = "TasksFragment";
    private RecyclerView recyclerView;
    private TasksAdapter adapter;
    private FirebaseFirestore db;
    private String userId;
    private Calendar calendar = Calendar.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tasks, container, false);

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

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
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_task, null);

        TextInputEditText etTitle = dialogView.findViewById(R.id.et_task_title);
        TextInputEditText etDetails = dialogView.findViewById(R.id.et_task_details);
        Button btnDate = dialogView.findViewById(R.id.btn_date_picker);
        Button btnTime = dialogView.findViewById(R.id.btn_time_picker);
        ChipGroup cgImportance = dialogView.findViewById(R.id.cg_importance);

        calendar = Calendar.getInstance();

        if (btnDate != null) {
            btnDate.setOnClickListener(v -> {
                new DatePickerDialog(requireContext(), (datePicker, year, month, day) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, day);
                    btnDate.setText(day + "/" + (month + 1) + "/" + year);
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
            });
        }

        if (btnTime != null) {
            btnTime.setOnClickListener(v -> {
                new TimePickerDialog(requireContext(), (timePicker, hour, min) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hour);
                    calendar.set(Calendar.MINUTE, min);
                    btnTime.setText(String.format("%02d:%02d", hour, min));
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
            });
        }

        new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Add Task", (dialog, which) -> {
                    if (etTitle == null) return;
                    String title = etTitle.getText().toString().trim();
                    if (title.isEmpty()) {
                        Toast.makeText(getContext(), "Title is required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int importance = 1;
                    if (cgImportance != null) {
                        int checkedId = cgImportance.getCheckedChipId();
                        if (checkedId == R.id.chip_high) importance = 3;
                        else if (checkedId == R.id.chip_medium) importance = 2;
                    }

                    String details = (etDetails != null) ? etDetails.getText().toString() : "";

                    Task newTask = new Task(title, details, userId, false, importance, calendar.getTime());
                    db.collection("tasks").add(newTask).addOnSuccessListener(doc -> {
                        doc.update("id", doc.getId());
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadTasks() {
        if (userId == null) return;
        db.collection("tasks")
                .whereEqualTo("userId", userId)
                .orderBy("importance", Query.Direction.DESCENDING)
                .orderBy("dueDate", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed.", error);
                        return;
                    }
                    if (isAdded() && value != null) {
                        List<Task> tasks = value.toObjects(Task.class);
                        if (adapter != null) {
                            adapter.updateTasks(tasks);
                        }
                    }
                });
    }

    @Override
    public void onTaskChecked(Task task, boolean isChecked) {
        if (task != null && task.getId() != null) {
            db.collection("tasks").document(task.getId()).update("completed", isChecked);
        }
    }
}