package com.brainfriend.app.fragments.games;

import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.brainfriend.app.R;
import com.brainfriend.app.utils.StatsManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MemoryMatchFragment extends Fragment implements TextToSpeech.OnInitListener {

    private RecyclerView recyclerView;
    private TextView tvScore, tvAttempts, tvTimer, tvFeedback;
    private Button btnReset;
    private Spinner spinnerDifficulty;
    private MemoryAdapter adapter;
    private List<Card> cards;
    private int matchesFound = 0;
    private int attempts = 0;
    private int totalPairs = 4; // default
    private int firstPosition = -1;
    private int secondPosition = -1;
    private boolean isWaiting = false;
    private boolean gameActive = true;

    // Timer
    private int timeRemaining = 90; // seconds
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;

    // Audio
    private TextToSpeech tts;
    private final String[] emojiNames = {"📘", "🎒", "⏰", "🍎", "✏️", "📏"}; // up to 6 pairs
    private final String[] spokenWords = {"book", "backpack", "clock", "apple", "pencil", "ruler"};

    // Stats tracking
    private StatsManager statsManager;
    private boolean gameEndRecorded = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_memory_match, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.rv_memory_grid);
        tvScore = view.findViewById(R.id.tv_score_memory);
        tvAttempts = view.findViewById(R.id.tv_attempts);
        tvTimer = view.findViewById(R.id.tv_timer);
        tvFeedback = view.findViewById(R.id.tv_feedback_memory);
        btnReset = view.findViewById(R.id.btn_reset_memory);
        spinnerDifficulty = view.findViewById(R.id.spinner_difficulty_memory);

        tts = new TextToSpeech(getContext(), this);
        statsManager = new StatsManager(requireContext());

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.memory_difficulty_levels, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDifficulty.setAdapter(adapter);
        spinnerDifficulty.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyDifficulty(position);
                resetGame();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnReset.setOnClickListener(v -> resetGame());

        applyDifficulty(0); // default Easy
        resetGame();
    }

    private void applyDifficulty(int position) {
        switch (position) {
            case 0: // Easy
                totalPairs = 4;
                timeRemaining = 90;
                break;
            case 1: // Medium
                totalPairs = 5;
                timeRemaining = 75;
                break;
            case 2: // Hard
                totalPairs = 6;
                timeRemaining = 60;
                break;
        }
        int columns = 2; // keep 2 columns for all difficulties, rows = totalPairs
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), columns));
        tvTimer.setText("Time: " + timeRemaining + "s");
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.US);
        }
    }

    private void setupGame() {
        // Use emojis and spoken words for the number of pairs
        List<EmojiPair> emojiPairs = new ArrayList<>();
        for (int i = 0; i < totalPairs; i++) {
            emojiPairs.add(new EmojiPair(emojiNames[i], spokenWords[i]));
        }
        cards = new ArrayList<>();
        for (EmojiPair pair : emojiPairs) {
            cards.add(new Card(pair.emoji, pair.spokenWord, pair.pairId));
            cards.add(new Card(pair.emoji, pair.spokenWord, pair.pairId));
        }
        Collections.shuffle(cards);
        adapter = new MemoryAdapter(cards);
        recyclerView.setAdapter(adapter);
        matchesFound = 0;
        attempts = 0;
        firstPosition = -1;
        secondPosition = -1;
        isWaiting = false;
        gameActive = true;
        gameEndRecorded = false;   // reset flag
        updateUI();

        // Start timer
        stopTimer();
        startTimer();
    }

    private void startTimer() {
        if (timerRunnable != null) return;
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (gameActive && timeRemaining > 0) {
                    timeRemaining--;
                    tvTimer.setText("Time: " + timeRemaining + "s");
                    timerHandler.postDelayed(this, 1000);
                    if (timeRemaining == 0) {
                        gameActive = false;
                        tvFeedback.setText("⏰ Time's up! Game over.");
                        tvFeedback.setVisibility(View.VISIBLE);
                        btnReset.setVisibility(View.VISIBLE);
                        // Record stats for timeout (percentage based on matched pairs)
                        if (!gameEndRecorded) {
                            gameEndRecorded = true;
                            recordStats();
                        }
                        adapter.notifyDataSetChanged();
                    }
                }
            }
        };
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void stopTimer() {
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
            timerRunnable = null;
        }
    }

    private void resetGame() {
        stopTimer();
        applyDifficulty(spinnerDifficulty.getSelectedItemPosition());
        setupGame();
        tvFeedback.setVisibility(View.GONE);
        btnReset.setVisibility(View.GONE);
    }

    private void updateUI() {
        tvScore.setText("Matches: " + matchesFound + " / " + totalPairs);
        tvAttempts.setText("Attempts: " + attempts);
        if (matchesFound == totalPairs && !gameEndRecorded) {
            gameActive = false;
            stopTimer();
            tvFeedback.setText("🎉 You won! Great memory!");
            tvFeedback.setVisibility(View.VISIBLE);
            btnReset.setVisibility(View.VISIBLE);
            // Record stats for win
            if (!gameEndRecorded) {
                gameEndRecorded = true;
                recordStats();
            }
        }
    }

    private void recordStats() {
        statsManager.recordPlay();
        int level = spinnerDifficulty.getSelectedItemPosition() + 1; // 1=Easy,2=Medium,3=Hard
        int percentage = (matchesFound * 100) / totalPairs;
        android.util.Log.d("MemoryMatch", "Recording stats - level: " + level + ", score: " + percentage);
        statsManager.recordPlay();
        statsManager.setMemoryLevel(level);
        statsManager.setMemoryScore(percentage);
    }

    private void onCardClick(int position) {
        if (!gameActive || isWaiting) return;
        Card card = cards.get(position);
        if (card.isMatched) return;
        if (firstPosition == -1) {
            // Flip first card
            firstPosition = position;
            card.isFlipped = true;
            adapter.notifyItemChanged(firstPosition);
            // Speak the emoji name
            speak(card.spokenWord);
        } else if (secondPosition == -1 && position != firstPosition) {
            // Flip second card
            secondPosition = position;
            card.isFlipped = true;
            adapter.notifyItemChanged(secondPosition);
            speak(card.spokenWord);
            attempts++;
            updateUI();

            Card card1 = cards.get(firstPosition);
            Card card2 = cards.get(secondPosition);
            if (card1.emoji.equals(card2.emoji)) {
                // Match
                card1.isMatched = true;
                card2.isMatched = true;
                matchesFound++;
                updateUI();
                firstPosition = -1;
                secondPosition = -1;
                // Keep flipped
            } else {
                // No match – flip back after delay
                isWaiting = true;
                new Handler().postDelayed(() -> {
                    if (!card1.isMatched) card1.isFlipped = false;
                    if (!card2.isMatched) card2.isFlipped = false;
                    adapter.notifyItemChanged(firstPosition);
                    adapter.notifyItemChanged(secondPosition);
                    firstPosition = -1;
                    secondPosition = -1;
                    isWaiting = false;
                }, 800);
            }
        }
    }

    private void speak(String word) {
        if (tts != null) {
            tts.speak(word, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    // Helper class for emoji + spoken word
    private static class EmojiPair {
        String emoji;
        String spokenWord;
        int pairId;
        EmojiPair(String emoji, String spokenWord) {
            this.emoji = emoji;
            this.spokenWord = spokenWord;
            this.pairId = emoji.hashCode();
        }
    }

    private static class Card {
        String emoji;
        String spokenWord;
        int pairId;
        boolean isFlipped = false;
        boolean isMatched = false;

        Card(String emoji, String spokenWord, int pairId) {
            this.emoji = emoji;
            this.spokenWord = spokenWord;
            this.pairId = pairId;
        }
    }

    private class MemoryAdapter extends RecyclerView.Adapter<MemoryAdapter.ViewHolder> {
        private List<Card> cards;

        MemoryAdapter(List<Card> cards) {
            this.cards = cards;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_memory_card, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Card card = cards.get(position);
            if (card.isFlipped || card.isMatched) {
                holder.tvFront.setText(card.emoji);
                holder.tvFront.setVisibility(View.VISIBLE);
                holder.tvBack.setVisibility(View.GONE);
            } else {
                holder.tvFront.setVisibility(View.GONE);
                holder.tvBack.setVisibility(View.VISIBLE);
            }
            holder.itemView.setOnClickListener(v -> onCardClick(position));
        }

        @Override
        public int getItemCount() {
            return cards.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvFront, tvBack;
            ViewHolder(View itemView) {
                super(itemView);
                tvFront = itemView.findViewById(R.id.tv_card_front);
                tvBack = itemView.findViewById(R.id.tv_card_back);
            }
        }
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