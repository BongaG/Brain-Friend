package com.brainfriend.app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.brainfriend.app.R;

public class DashboardFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Safe inflation
        try {
            return inflater.inflate(R.layout.fragment_dashboard, container, false);
        } catch (Exception e) {
            return new View(getContext()); // Returns empty view if XML is broken to avoid crash
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Every lookup is now crash-proofed
        View cvMissed = view.findViewById(R.id.cv_missed_tasks);
        if (cvMissed != null) cvMissed.setVisibility(View.GONE);

        View cvNext = view.findViewById(R.id.cv_next_task);
        if (cvNext != null) cvNext.setVisibility(View.VISIBLE);
    }
}