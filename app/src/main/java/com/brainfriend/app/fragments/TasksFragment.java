package com.brainfriend.app.fragments;

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

public class TasksFragment extends Fragment implements TasksAdapter.OnTaskClickListener {
    private RecyclerView recyclerView;
    private TasksAdapter adapter;
    private FirebaseFirestore db;
    private String userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tasks, container, false);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();

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
        View dv = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_task, null);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dv)
                .create();

        // 1. Find all views from the dialog layout
        TextInputEditText etTitle = dv.findViewById(R.id.et_task_title);
        TextInputEditText etDetails = dv.findViewById(R.id.et_task_details);
        ChipGroup cgImportance = dv.findViewById(R.id.cg_importance);
        ChipGroup cgCategory = dv.findViewById(R.id.cg_category); // Added for Professional Organization
        SwitchMaterial switchRecurring = dv.findViewById(R.id.switch_recurring);
        Button btnConfirm = dv.findViewById(R.id.btn_add_task_confirm);

        // 2. Set up the click listener for the confirm button
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                String title = etTitle != null ? etTitle.getText().toString().trim() : "";
                String details = etDetails != null ? etDetails.getText().toString().trim() : "";
                boolean isRecurring = switchRecurring != null && switchRecurring.isChecked();

                if (title.isEmpty()) {
                    Toast.makeText(getContext(), "Title required", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Importance Logic
                int importance = 1; // Default: Low
                if (cgImportance != null) {
                    int selectedId = cgImportance.getCheckedChipId();
                    if (selectedId == R.id.chip_high) importance = 3;
                    else if (selectedId == R.id.chip_medium) importance = 2;
                }

                // Category Logic (School/Work/Personal)
                String category = "Personal";
                if (cgCategory != null) {
                    int selectedCatId = cgCategory.getCheckedChipId();
                    if (selectedCatId == R.id.chip_school) category = "School";
                    else if (selectedCatId == R.id.chip_work) category = "Work";
                }

                // create the achievement
                Task newTask = new Task(title, details, userId, false, importance, category, isRecurring, Calendar.getInstance().getTime());

                db.collection("tasks").add(newTask).addOnSuccessListener(doc -> {
                    // Update the task with its Firestore ID
                    db.collection("tasks").document(doc.getId()).update("id", doc.getId());
                    dialog.dismiss();
                    Toast.makeText(getContext(), "Task Memory Logged", Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error logging task", Toast.LENGTH_SHORT).show();
                });
            });
        }

        dialog.show();
    }

    private void loadTasks() {
        if (userId == null) return;
        db.collection("tasks")
                .whereEqualTo("userId", userId)
                .orderBy("importance", Query.Direction.DESCENDING) // Smart Sorting: Priority first
                .orderBy("dueDate", Query.Direction.ASCENDING)
                .addSnapshotListener((val, err) -> {
                    if (isAdded() && val != null) {
                        adapter.updateTasks(val.toObjects(Task.class));
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