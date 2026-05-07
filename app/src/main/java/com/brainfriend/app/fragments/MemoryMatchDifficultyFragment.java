package com.brainfriend.app.fragments.games;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.fragment.app.Fragment;
import com.brainfriend.app.R;

public class MemoryMatchDifficultyFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_memory_difficulty, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button btnEasy = view.findViewById(R.id.btn_easy);
        Button btnMedium = view.findViewById(R.id.btn_medium);
        Button btnHard = view.findViewById(R.id.btn_hard);

        btnEasy.setOnClickListener(v -> startGame(0));
        btnMedium.setOnClickListener(v -> startGame(1));
        btnHard.setOnClickListener(v -> startGame(2));
    }

    private void startGame(int difficulty) {
        Bundle args = new Bundle();
        args.putInt("difficulty", difficulty);
        MemoryMatchFragment fragment = new MemoryMatchFragment();
        fragment.setArguments(args);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}