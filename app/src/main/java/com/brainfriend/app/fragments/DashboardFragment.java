package com.brainfriend.app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.brainfriend.app.R;
import com.brainfriend.app.models.Task;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DashboardFragment extends Fragment {
    private static final String TAG = "DashboardFragment";
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private TextView welcomeText;
    private MaterialCardView upcomingTaskCard;
    private TextView tvUpcomingTitle, tvUpcomingTime;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        welcomeText = view.findViewById(R.id.welcome_text);
        upcomingTaskCard = view.findViewById(R.id.upcoming_task_card);
        tvUpcomingTitle = view.findViewById(R.id.tv_upcoming_task_title);
        tvUpcomingTime = view.findViewById(R.id.tv_upcoming_task_time);

        // Fetch User Name from Firestore
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (isAdded() && documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            if (name != null && !name.isEmpty()) {
                                welcomeText.setText("Welcome back, " + name + "!");
                            } else {
                                String displayName = mAuth.getCurrentUser().getDisplayName();
                                welcomeText.setText("Welcome back, " + (displayName != null ? displayName : "Friend") + "!");
                            }
                        }
                    });
        }

        // Logout Logic
        View logoutBtn = view.findViewById(R.id.btn_logout);
        if (logoutBtn != null) {
            logoutBtn.setOnClickListener(v -> {
                mAuth.signOut();
                try {
                    Class<?> loginClass = Class.forName("com.brainfriend.app.LoginActivity");
                    Intent intent = new Intent(requireContext(), loginClass);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } catch (ClassNotFoundException e) {
                    Toast.makeText(getContext(), "Logged out", Toast.LENGTH_SHORT).show();
                }
            });
        }

        loadDashboardData();
        return view;
    }

    private void loadDashboardData() {
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();

        // Load most important upcoming task
        db.collection("tasks")
                .whereEqualTo("userId", userId)
                .whereEqualTo("completed", false)
                .whereGreaterThanOrEqualTo("dueDate", new Date())
                .orderBy("dueDate", Query.Direction.ASCENDING)
                .limit(1)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading upcoming task", error);
                        return;
                    }
                    if (isAdded() && value != null && !value.isEmpty()) {
                        Task task = value.toObjects(Task.class).get(0);
                        upcomingTaskCard.setVisibility(View.VISIBLE);
                        tvUpcomingTitle.setText(task.getTitle());

                        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
                        tvUpcomingTime.setText(sdf.format(task.getDueDate()));
                    } else if (isAdded()) {
                        upcomingTaskCard.setVisibility(View.VISIBLE);
                        tvUpcomingTitle.setText("No pending tasks");
                        tvUpcomingTime.setText("You're all clear!");
                    }
                });
    }
}