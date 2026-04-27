package com.brainfriend.app.fragments.games;

import android.os.Bundle;
import android.os.Handler;
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
import java.util.List;
import java.util.Random;

public class DualTaskFragment extends Fragment {

    private TextView tvInstruction, tvSequence, tvMathQuestion, tvScore;
    private EditText etMathAnswer, etSequenceRecall;
    private Button btnSubmitMath, btnSubmitSequence, btnRestart;

    private List<Integer> numberSequence;
    private List<MathQuestion> mathQuestions;
    private int currentMathIndex = 0;
    private int mathCorrectCount = 0;
    private boolean sequenceRecalledCorrectly = false;

    private final Random random = new Random();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dual_task, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvInstruction = view.findViewById(R.id.tv_instruction);
        tvSequence = view.findViewById(R.id.tv_sequence);
        tvMathQuestion = view.findViewById(R.id.tv_math_question);
        tvScore = view.findViewById(R.id.tv_score);
        etMathAnswer = view.findViewById(R.id.et_math_answer);
        etSequenceRecall = view.findViewById(R.id.et_sequence_recall);
        btnSubmitMath = view.findViewById(R.id.btn_submit_math);
        btnSubmitSequence = view.findViewById(R.id.btn_submit_sequence);
        btnRestart = view.findViewById(R.id.btn_restart);
        btnSubmitMath.setOnClickListener(this::onMathSubmitClick);
        btnSubmitSequence.setOnClickListener(this::onSequenceSubmitClick);
        btnRestart.setOnClickListener(v -> {
            startNewGame();
            tvScore.setVisibility(View.GONE);
            btnRestart.setVisibility(View.GONE);
        });

        startNewGame();
    }

    private void startNewGame() {
        generateNumberSequence();
        generateMathQuestions();

        // Show sequence for 3 seconds
        tvSequence.setVisibility(View.VISIBLE);
        tvSequence.setText(formatSequence(numberSequence));
        tvInstruction.setText("Memorize the numbers...");

        new Handler().postDelayed(() -> {
            tvSequence.setVisibility(View.GONE);
            startMathRound();
        }, 3000);
    }

    private void generateNumberSequence() {
        numberSequence = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            numberSequence.add(random.nextInt(10) + 1); // 1-9
        }
    }

    private String formatSequence(List<Integer> seq) {
        StringBuilder sb = new StringBuilder();
        for (int num : seq) sb.append(num).append(" ");
        return sb.toString().trim();
    }

    private void generateMathQuestions() {
        mathQuestions = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            int a = random.nextInt(10) + 1;
            int b = random.nextInt(10) + 1;
            int answer = a + b;
            mathQuestions.add(new MathQuestion(a + " + " + b, answer));
        }
    }

    private void startMathRound() {
        currentMathIndex = 0;
        mathCorrectCount = 0;
        showNextMathQuestion();
    }

    private void showNextMathQuestion() {
        if (currentMathIndex < mathQuestions.size()) {
            MathQuestion q = mathQuestions.get(currentMathIndex);
            tvMathQuestion.setVisibility(View.VISIBLE);
            tvMathQuestion.setText(q.question + " = ?");
            etMathAnswer.setVisibility(View.VISIBLE);
            btnSubmitMath.setVisibility(View.VISIBLE);
            etMathAnswer.setText("");
            tvInstruction.setText("Solve the math problem " + (currentMathIndex+1) + "/" + mathQuestions.size());
        } else {

            tvMathQuestion.setVisibility(View.GONE);
            etMathAnswer.setVisibility(View.GONE);
            btnSubmitMath.setVisibility(View.GONE);
            startSequenceRecall();
        }
    }

    private void startSequenceRecall() {
        tvInstruction.setText("What was the number sequence?");
        etSequenceRecall.setVisibility(View.VISIBLE);
        btnSubmitSequence.setVisibility(View.VISIBLE);
    }

    private void finishGame() {
        int totalScore = 0;
        if (sequenceRecalledCorrectly) totalScore += 50;
        totalScore += (mathCorrectCount * 50 / mathQuestions.size());

        tvScore.setText("🎉 Your Score: " + totalScore + " / 100");
        tvScore.setVisibility(View.VISIBLE);
        btnRestart.setVisibility(View.VISIBLE);
        tvInstruction.setText("Game finished. Tap Play Again.");
        etSequenceRecall.setVisibility(View.GONE);
        btnSubmitSequence.setVisibility(View.GONE);
    }

    // Button click handlers (to be set in onViewCreated)
    public void onMathSubmitClick(View v) {
        String answerStr = etMathAnswer.getText().toString().trim();
        if (answerStr.isEmpty()) {
            Toast.makeText(getContext(), "Enter an answer", Toast.LENGTH_SHORT).show();
            return;
        }
        int userAnswer = Integer.parseInt(answerStr);
        if (userAnswer == mathQuestions.get(currentMathIndex).answer) {
            mathCorrectCount++;
        }
        currentMathIndex++;
        showNextMathQuestion();
    }

    public void onSequenceSubmitClick(View v) {
        String userSeq = etSequenceRecall.getText().toString().trim().replace(" ", "");
        StringBuilder actualSeq = new StringBuilder();
        for (int num : numberSequence) actualSeq.append(num);
        sequenceRecalledCorrectly = userSeq.equals(actualSeq.toString());
        if (!sequenceRecalledCorrectly) {
            Toast.makeText(getContext(), "Incorrect sequence! The correct was: " + actualSeq.toString(), Toast.LENGTH_LONG).show();
        }
        finishGame();
    }

    private class MathQuestion {
        String question;
        int answer;
        MathQuestion(String q, int a) { question = q; answer = a; }
    }
}

