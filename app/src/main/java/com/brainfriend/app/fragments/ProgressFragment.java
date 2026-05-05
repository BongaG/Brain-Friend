package com.brainfriend.app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.brainfriend.app.R;
import com.brainfriend.app.utils.StatsManager;

public class ProgressFragment extends Fragment {

    private StatsManager statsManager;

    // Declare all TextViews as class fields
    private TextView tvStreak, tvCompletion;
    private TextView tvSeqLevel, tvSeqScore;
    private TextView tvMemLevel, tvMemScore;
    private TextView tvAudioLevel, tvAudioScore;
    private TextView tvBestTime, tvCoachSuggestion;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_progress, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        statsManager = new StatsManager(requireContext());

        // Initialize all TextViews from the layout
        tvStreak = view.findViewById(R.id.tv_streak);
        tvCompletion = view.findViewById(R.id.tv_completion_rate);
        tvSeqLevel = view.findViewById(R.id.tv_seq_level);
        tvSeqScore = view.findViewById(R.id.tv_seq_score);
        tvMemLevel = view.findViewById(R.id.tv_mem_level);
        tvMemScore = view.findViewById(R.id.tv_mem_score);
        tvAudioLevel = view.findViewById(R.id.tv_audio_level);
        tvAudioScore = view.findViewById(R.id.tv_audio_score);
        tvBestTime = view.findViewById(R.id.tv_best_time);
        tvCoachSuggestion = view.findViewById(R.id.tv_coach_suggestion);

        // Load initial data
        refreshData();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when returning from a game
        refreshData();
    }

    private void refreshData() {
        // Reload all stats from StatsManager
        tvStreak.setText("🔥 " + statsManager.getStreak() + " day streak");
        tvCompletion.setText(statsManager.getOverallCompletionRate() + "% overall");

        tvSeqLevel.setText("Level " + statsManager.getSequencingLevel());
        tvSeqScore.setText(statsManager.getSequencingScore() + "%");

        tvMemLevel.setText("Level " + statsManager.getMemoryLevel());
        tvMemScore.setText(statsManager.getMemoryScore() + "%");

        tvAudioLevel.setText("Level " + statsManager.getAudioLevel());
        tvAudioScore.setText(statsManager.getAudioScore() + "%");

        tvBestTime.setText("✨ Best time to exercise: " + statsManager.getBestTimeOfDay());

        // Update coach suggestion based on latest scores
        tvCoachSuggestion.setText(generateCoachSuggestion());
    }

    private String generateCoachSuggestion() {
        int seqScore = statsManager.getSequencingScore();
        int memScore = statsManager.getMemoryScore();
        int audioScore = statsManager.getAudioScore();
        int streak = statsManager.getStreak();

        // Priority: lowest performing game first
        if (seqScore < 50 && seqScore > 0) {
            return "📅 Step Sequencing needs practice. Focus on ordering daily tasks correctly to improve your planning skills.";
        }
        if (memScore < 50 && memScore > 0) {
            return "🃏 Memory Match is challenging for you. Try Easy mode and use the audio hints to remember pairs.";
        }
        if (audioScore < 50 && audioScore > 0) {
            return "🎧 Auditory Sequence is tough. Start with length 3 and repeat often – your listening memory will improve.";
        }
        if (seqScore >= 80 && memScore >= 80 && audioScore >= 80) {
            return "🔥 Outstanding performance in all games! Try harder difficulty levels to keep challenging your brain.";
        }
        if (streak >= 5) {
            return "🏆 Amazing streak! Your consistency is building strong cognitive habits. Keep it up!";
        }
        if (seqScore == 0 && memScore == 0 && audioScore == 0) {
            return "💡 Play any game to completion – your first results will appear here and unlock personalised coaching.";
        }
        return "✨ You're making progress. Mix up the games to train different cognitive skills each day.";
    }
}