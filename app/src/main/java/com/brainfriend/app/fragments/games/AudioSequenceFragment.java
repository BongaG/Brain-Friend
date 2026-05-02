package com.brainfriend.app.fragments.games;

import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.brainfriend.app.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AudioSequenceFragment extends Fragment implements TextToSpeech.OnInitListener {

    private TextView tvRoundInfo, tvScore, tvInstruction, tvFeedback;
    private Button btnSound1, btnSound2, btnSound3, btnPlaySequence, btnReset;
    private TextToSpeech tts;
    private List<Integer> currentSequence;
    private List<Integer> userInput;
    private int score = 0;
    private int currentLength = 3;
    private int round = 1;
    private boolean isPlayingSequence = false;
    private boolean isAwaitingUserInput = false;
    private final Handler handler = new Handler();  // ✅ now final

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_audio_sequence, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvRoundInfo = view.findViewById(R.id.tv_round_info);
        tvScore = view.findViewById(R.id.tv_score_audio);
        tvInstruction = view.findViewById(R.id.tv_instruction);
        tvFeedback = view.findViewById(R.id.tv_feedback_audio);
        btnSound1 = view.findViewById(R.id.btn_sound1);
        btnSound2 = view.findViewById(R.id.btn_sound2);
        btnSound3 = view.findViewById(R.id.btn_sound3);
        btnPlaySequence = view.findViewById(R.id.btn_play_sequence);
        btnReset = view.findViewById(R.id.btn_reset_audio);

        tts = new TextToSpeech(getContext(), this);

        btnPlaySequence.setOnClickListener(v -> startNewRound());
        btnReset.setOnClickListener(v -> resetGame());
        btnSound1.setOnClickListener(v -> handleUserInput(0));
        btnSound2.setOnClickListener(v -> handleUserInput(1));
        btnSound3.setOnClickListener(v -> handleUserInput(2));

        resetGame();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.US);
        }
    }

    private void resetGame() {
        score = 0;
        currentLength = 3;
        round = 1;
        updateUI();
        tvFeedback.setVisibility(View.GONE);
        btnReset.setVisibility(View.GONE);
        btnPlaySequence.setVisibility(View.VISIBLE);
        tvInstruction.setText(getString(R.string.instruction_press_play));
        isAwaitingUserInput = false;
        currentSequence = null;
        userInput = null;
    }

    private void startNewRound() {
        if (isPlayingSequence) return;
        currentSequence = new ArrayList<>();
        for (int i = 0; i < currentLength; i++) {
            currentSequence.add((int) (Math.random() * 3));
        }
        userInput = new ArrayList<>();
        playSequence();
    }

    private void playSequence() {
        isPlayingSequence = true;
        btnPlaySequence.setEnabled(false);
        tvInstruction.setText(getString(R.string.instruction_listen));
        playNextItem(0);
    }

    private void playNextItem(final int index) {
        if (index >= currentSequence.size()) {
            isPlayingSequence = false;
            btnPlaySequence.setEnabled(true);
            tvInstruction.setText(getString(R.string.instruction_repeat));
            isAwaitingUserInput = true;
            userInput.clear();
            return;
        }
        final int item = currentSequence.get(index);
        String numberWord;
        switch (item) {
            case 0: numberWord = "one"; break;
            case 1: numberWord = "two"; break;
            default: numberWord = "three"; break;
        }
        tts.speak(numberWord, TextToSpeech.QUEUE_ADD, null, null);
        flashButton(item);
        handler.postDelayed(() -> playNextItem(index + 1), 1200);
    }

    private void flashButton(int item) {
        Button btn = getButtonForItem(item);
        // Use color resource for flash
        int flashColor = getResources().getColor(R.color.orange_flash);
        btn.setBackgroundTintList(getResources().getColorStateList(R.color.orange_flash));
        handler.postDelayed(() -> {
            int originalColor = getOriginalColorForItem(item);
            btn.setBackgroundTintList(getResources().getColorStateList(originalColor));
        }, 300);
    }

    private int getOriginalColorForItem(int item) {
        switch (item) {
            case 0: return R.color.red_button;
            case 1: return R.color.green_button;
            default: return R.color.blue_button;
        }
    }

    private Button getButtonForItem(int item) {
        switch (item) {
            case 0: return btnSound1;
            case 1: return btnSound2;
            default: return btnSound3;
        }
    }

    private void handleUserInput(int item) {
        if (!isAwaitingUserInput) return;
        userInput.add(item);
        flashButton(item);
        if (userInput.size() == currentSequence.size()) {
            isAwaitingUserInput = false;
            boolean correct = true;
            for (int i = 0; i < currentSequence.size(); i++) {
                if (!userInput.get(i).equals(currentSequence.get(i))) {
                    correct = false;
                    break;
                }
            }
            if (correct) {
                score += 10;
                currentLength++;
                round++;
                updateUI();
                tvFeedback.setText(getString(R.string.feedback_correct, currentLength));
                tvFeedback.setVisibility(View.VISIBLE);
                handler.postDelayed(() -> {
                    tvFeedback.setVisibility(View.GONE);
                    startNewRound();
                }, 1500);
            } else {
                tvFeedback.setText(getString(R.string.feedback_wrong, score));
                tvFeedback.setVisibility(View.VISIBLE);
                btnPlaySequence.setEnabled(false);
                btnReset.setVisibility(View.VISIBLE);
                isAwaitingUserInput = false;
            }
        }
    }

    private void updateUI() {
        tvRoundInfo.setText(getString(R.string.round_info, round, currentLength));
        tvScore.setText(getString(R.string.score_label, score));
    }

    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}