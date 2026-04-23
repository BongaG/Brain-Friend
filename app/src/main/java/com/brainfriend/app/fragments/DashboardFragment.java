package com.brainfriend.app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.brainfriend.app.R;
import com.brainfriend.app.models.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private FirebaseFirestore db;
    private String userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        try {
            return inflater.inflate(R.layout.fragment_dashboard, container, false);
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

        loadDashboardData(view);
    }

    private void loadDashboardData(View view) {
        if (userId == null) return;

        db.collection("tasks")
                .whereEqualTo("userId", userId)
                .whereEqualTo("completed", false)
                .orderBy("importance", Query.Direction.DESCENDING)
                .orderBy("dueDate", Query.Direction.ASCENDING)
                .addSnapshotListener((val, err) -> {
                    if (!isAdded() || val == null) return;

                    List<Task> tasks = val.toObjects(Task.class);

                    // Pending count
                    TextView tvPending = view.findViewById(R.id.tv_pending_count);
                    if (tvPending != null) {
                        tvPending.setText(String.valueOf(tasks.size()));
                    }

                    // Up Next — show the highest priority upcoming task
                    TextView tvStatus = view.findViewById(R.id.tv_upcoming_status);
                    TextView tvTitle = view.findViewById(R.id.tv_upcoming_title);
                    TextView tvTime = view.findViewById(R.id.tv_upcoming_time);

                    if (!tasks.isEmpty()) {
                        Task next = tasks.get(0);
                        if (tvStatus != null) tvStatus.setVisibility(View.VISIBLE);
                        if (tvTitle != null) tvTitle.setText(next.getTitle());
                        if (tvTime != null) {
                            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM 'at' HH:mm", Locale.getDefault());
                            tvTime.setText(sdf.format(next.getDueDate()));
                        }
                    } else {
                        if (tvStatus != null) tvStatus.setVisibility(View.GONE);
                        if (tvTitle != null) {
                            tvTitle.setText("All clear! Relax or add a new task.");
                        }
                        if (tvTime != null) tvTime.setText("");
                    }
                });
    }
}