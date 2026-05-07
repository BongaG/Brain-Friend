package com.brainfriend.app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.brainfriend.app.R;
import com.brainfriend.app.fragments.games.AudioSequenceFragment;
import com.brainfriend.app.fragments.games.MemoryMatchDifficultyFragment;
import com.brainfriend.app.fragments.games.SequencingGameFragment;

public class ExercisesFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_exercises, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Step Sequencing
        Button btnSequencing = view.findViewById(R.id.btn_start_sequencing);
        btnSequencing.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new SequencingGameFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // Memory Match (opens difficulty screen)
        Button btnMemory = view.findViewById(R.id.btn_start_memory);
        btnMemory.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new MemoryMatchDifficultyFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // Auditory Sequence
        Button btnAudio = view.findViewById(R.id.btn_start_audio);
        btnAudio.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new AudioSequenceFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // My Progress
        Button btnProgress = view.findViewById(R.id.btn_progress);
        btnProgress.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new ProgressFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }
}