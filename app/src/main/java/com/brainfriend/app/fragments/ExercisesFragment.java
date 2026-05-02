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
import com.brainfriend.app.fragments.games.SequencingGameFragment;
import com.brainfriend.app.fragments.games.MemoryMatchFragment;   // add this import

public class ExercisesFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_exercises, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Sequencing Game button
        Button btnStart = view.findViewById(R.id.btn_start_sequencing);
        btnStart.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new SequencingGameFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // Memory Match button
        Button btnMemory = view.findViewById(R.id.btn_start_memory);
        btnMemory.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new MemoryMatchFragment())
                    .addToBackStack(null)
                    .commit();
        });

        Button btnAudio = view.findViewById(R.id.btn_start_audio);
        btnAudio.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new AudioSequenceFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }
}