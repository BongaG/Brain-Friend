package com.brainfriend.app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.brainfriend.app.R;
import com.brainfriend.app.adapters.RemindersAdapter;
import com.brainfriend.app.reminders.AddReminderFragment;
import com.brainfriend.app.reminders.AppDatabase;
import com.brainfriend.app.reminders.ReminderEntity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;
import java.util.List;

public class RoutineFragment extends Fragment {

    private RemindersAdapter adapter;
    private View llEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_routine, container, false);




        // Setup RecyclerView
        llEmpty = view.findViewById(R.id.ll_routine_empty);
        RecyclerView rv = view.findViewById(R.id.rv_reminders);
        adapter = new RemindersAdapter(new ArrayList<>());
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        // FAB — open add reminder
        FloatingActionButton fab = view.findViewById(R.id.fab_add_reminder);
        if (fab != null) {
            fab.setOnClickListener(v -> openAddReminder());
        }

        // Load reminders
        loadReminders();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload when coming back from add reminder
        loadReminders();
    }

    private void loadReminders() {
        new Thread(() -> {
            List<ReminderEntity> reminders = AppDatabase
                    .getInstance(requireContext())
                    .reminderDao()
                    .getAllSortedByTime();

            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                adapter.updateReminders(reminders);

                // Show empty state if no reminders
                if (llEmpty != null) {
                    llEmpty.setVisibility(
                            reminders.isEmpty() ? View.VISIBLE : View.GONE);
                }
            });
        }).start();
    }

    private void openAddReminder() {
        AddReminderFragment reminderFragment = new AddReminderFragment();
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, reminderFragment)
                .addToBackStack(null)
                .commit();
    }
}