package com.brainfriend.app.fragments.games;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.brainfriend.app.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class WordGameFragment extends Fragment {

    private TextView tvScrambledWord, tvScore, tvFeedback;
    private EditText etAnswer;
    private Button btnSubmit, btnNextWord;

    private List<String> wordList;
    private String currentWord;
    private int currentScore = 0;
    private Random random = new Random();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_word_game, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvScrambledWord = view.findViewById(R.id.tv_scrambled_word);
        tvScore = view.findViewById(R.id.tv_score);
        tvFeedback = view.findViewById(R.id.tv_feedback);
        etAnswer = view.findViewById(R.id.et_user_answer);
        btnSubmit = view.findViewById(R.id.btn_submit);
        btnNextWord = view.findViewById(R.id.btn_next_word);

        initializeWordList();
        loadNewWord();

        btnSubmit.setOnClickListener(v -> checkAnswer());
        btnNextWord.setOnClickListener(v -> loadNewWord());
    }

    private void initializeWordList() {
        wordList = new ArrayList<>();
        wordList.add("BRAIN");
        wordList.add("MEMORY");
        wordList.add("FOCUS");
        wordList.add("REMINDER");
        wordList.add("COGNITIVE");
        wordList.add("EXERCISE");
        wordList.add("PROGRESS");
        wordList.add("THINKING");
    }

    private void loadNewWord() {
        currentWord = wordList.get(random.nextInt(wordList.size()));
        String scrambled = scrambleWord(currentWord);
        tvScrambledWord.setText(scrambled.toUpperCase());
        etAnswer.setText("");
        tvFeedback.setVisibility(View.GONE);
        btnNextWord.setVisibility(View.GONE);
        btnSubmit.setEnabled(true);
        etAnswer.setEnabled(true);
    }

    private String scrambleWord(String word) {
        List<Character> letters = new ArrayList<>();
        for (char c : word.toCharArray()) {
            letters.add(c);
        }
        Collections.shuffle(letters);
        StringBuilder scrambled = new StringBuilder();
        for (char c : letters) {
            scrambled.append(c);
        }
        if (scrambled.toString().equals(word)) {
            return scrambleWord(word);
        }
        return scrambled.toString();
    }

    private void checkAnswer() {
        String userAnswer = etAnswer.getText().toString().trim().toUpperCase();
        if (userAnswer.isEmpty()) {
            Toast.makeText(getContext(), "Please type your answer", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userAnswer.equals(currentWord.toUpperCase())) {
            currentScore++;
            tvScore.setText("Score: " + currentScore);
            tvFeedback.setText("✅ Correct! Great job!");
            tvFeedback.setVisibility(View.VISIBLE);
            tvFeedback.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            btnSubmit.setEnabled(false);
            etAnswer.setEnabled(false);
            btnNextWord.setVisibility(View.VISIBLE);
        } else {
            tvFeedback.setText("❌ Wrong! The correct word was: " + currentWord);
            tvFeedback.setVisibility(View.VISIBLE);
            tvFeedback.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            btnSubmit.setEnabled(false);
            etAnswer.setEnabled(false);
            btnNextWord.setVisibility(View.VISIBLE);
        }
    }
}