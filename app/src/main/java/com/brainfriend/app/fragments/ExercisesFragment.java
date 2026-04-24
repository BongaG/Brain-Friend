package com.brainfriend.app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.brainfriend.app.R;
import com.brainfriend.app.fragments.games.WordGameFragment;

public class ExercisesFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_exercises, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View cardWordGame = view.findViewById(R.id.card_word_game);
        if (cardWordGame != null) {
            cardWordGame.setOnClickListener(v -> {
                WordGameFragment gameFragment = new WordGameFragment();  // now the import works
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, gameFragment)
                        .addToBackStack(null)
                        .commit();
            });
        }
    }
}