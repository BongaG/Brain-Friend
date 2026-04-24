package com.brainfriend.app.reminders;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
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

        // Adapter with tap-to-mark-done (only for active reminders)
        adapter = new RemindersAdapter(entity -> {
            // Only mark as done if it's still active
            if (entity.isActive) {
                new Thread(() -> {
                    ReminderScheduler.cancel(requireContext(), entity.id);
                    AppDatabase.getInstance(requireContext()).reminderDao().markDone(entity.id);
                }).start();
            }
        });
        rv.setAdapter(adapter);

        // Swipe-to-delete (works on any reminder)
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                ReminderEntity entity = adapter.getCurrentList().get(position);
                if (entity != null) {
                    new Thread(() -> {
                        ReminderScheduler.cancel(requireContext(), entity.id);
                        AppDatabase.getInstance(requireContext()).reminderDao().deleteById(entity.id);
                    }).start();
                }
            }
        }).attachToRecyclerView(rv);

        // Observe ALL reminders (both active and done)
        AppDatabase.getInstance(requireContext())
                .reminderDao()
                .getAllReminders()
                .observe(getViewLifecycleOwner(), reminders -> adapter.submitList(reminders));
    }
}