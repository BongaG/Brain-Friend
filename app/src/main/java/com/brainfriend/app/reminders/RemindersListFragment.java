package com.brainfriend.app.reminders;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.brainfriend.app.R;



public class RemindersListFragment extends Fragment {

    private RemindersAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reminders_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rv = view.findViewById(R.id.rv_reminders);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new RemindersAdapter(entity -> {
            // On swipe-to-dismiss or "Done" tap: cancel alarm + mark done in DB
            new Thread(() -> {
                ReminderScheduler.cancel(requireContext(), entity.id);
                AppDatabase.getInstance(requireContext()).reminderDao().markDone(entity.id);
            }).start();
        });
        rv.setAdapter(adapter);

        // Observe LiveData — updates list automatically
        AppDatabase.getInstance(requireContext())
                .reminderDao()
                .getActiveReminders()
                .observe(getViewLifecycleOwner(), reminders -> adapter.submitList(reminders));
    }
}
