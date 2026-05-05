package com.brainfriend.app.fragments.games;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.brainfriend.app.R;
import com.brainfriend.app.utils.StatsManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SequencingGameFragment extends Fragment {

    // ---------- Data structures (using emojis) ----------
    private static class ScenarioStep {
        String title;
        String description;
        String emoji;
        int correctOrder;

        ScenarioStep(String title, String description, String emoji, int correctOrder) {
            this.title = title;
            this.description = description;
            this.emoji = emoji;
            this.correctOrder = correctOrder;
        }
    }

    private static class Scenario {
        String title;
        List<ScenarioStep> steps;

        Scenario(String title, List<ScenarioStep> steps) {
            this.title = title;
            this.steps = steps;
        }
    }

    // ---------- UI components ----------
    private TextView tvScenarioTitle, tvInstruction, tvScore, tvFeedback;
    private Button btnCheckOrder, btnNextScenario;
    private RecyclerView recyclerView;
    private StepAdapter adapter;
    private List<ScenarioStep> currentSteps;
    private int currentScenarioIndex = 0;
    private int totalScore = 0;
    private List<Scenario> scenarios;

    // ---------- Stats tracking ----------
    private StatsManager statsManager;
    private boolean gameEndRecorded = false;

    // ---------- Adapter (inner class) ----------
    private class StepAdapter extends RecyclerView.Adapter<StepAdapter.ViewHolder> {
        private List<ScenarioStep> steps;

        StepAdapter(List<ScenarioStep> steps) {
            this.steps = steps;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_step, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ScenarioStep step = steps.get(position);
            holder.tvText.setText(step.title);
            holder.tvEmoji.setText(step.emoji);
        }

        @Override
        public int getItemCount() {
            return steps.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvEmoji, tvText;
            ImageView ivDragHandle;
            ViewHolder(View itemView) {
                super(itemView);
                tvEmoji = itemView.findViewById(R.id.tv_step_emoji);
                tvText = itemView.findViewById(R.id.tv_step_text);
                ivDragHandle = itemView.findViewById(R.id.iv_drag_handle);
            }
        }
    }

    // ---------- Fragment lifecycle ----------
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sequencing_game, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvScenarioTitle = view.findViewById(R.id.tv_scenario_title);
        tvInstruction = view.findViewById(R.id.tv_instruction);
        tvScore = view.findViewById(R.id.tv_score);
        tvFeedback = view.findViewById(R.id.tv_feedback);
        btnCheckOrder = view.findViewById(R.id.btn_check_order);
        btnNextScenario = view.findViewById(R.id.btn_next_scenario);
        recyclerView = view.findViewById(R.id.rv_steps);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        statsManager = new StatsManager(requireContext());

        setupScenarios();
        loadScenario(currentScenarioIndex);

        btnCheckOrder.setOnClickListener(v -> checkOrder());
        btnNextScenario.setOnClickListener(v -> {
            if (currentScenarioIndex + 1 < scenarios.size()) {
                currentScenarioIndex++;
                loadScenario(currentScenarioIndex);
                btnNextScenario.setVisibility(View.GONE);
                btnCheckOrder.setEnabled(true);
                tvFeedback.setVisibility(View.GONE);
            } else {
                // Game completed – all scenarios done
                tvFeedback.setText("🎉 You completed all scenarios! Final score: " + totalScore);
                tvFeedback.setVisibility(View.VISIBLE);
                btnNextScenario.setEnabled(false);
                btnCheckOrder.setEnabled(false);

                // Record stats only once
                if (!gameEndRecorded) {
                    gameEndRecorded = true;
                    statsManager.recordPlay();
                    // Level = number of scenarios completed (max 5)
                    int level = scenarios.size();
                    // Score percentage = totalScore out of 100
                    int percentage = totalScore;
                    statsManager.setSequencingLevel(level);
                    statsManager.setSequencingScore(percentage);
                }
            }
        });
    }

    // ---------- Data setup with emojis ----------
    private void setupScenarios() {
        scenarios = new ArrayList<>();

        // 1. Morning Routine
        List<ScenarioStep> morning = Arrays.asList(
                new ScenarioStep("Wake up", "Open your eyes", "🌞", 0),
                new ScenarioStep("Brush teeth", "Clean teeth", "🦷", 1),
                new ScenarioStep("Get dressed", "Put on uniform", "👕", 2),
                new ScenarioStep("Eat breakfast", "Have a meal", "🍳", 3),
                new ScenarioStep("Go to school", "Pack bag and leave", "🏫", 4)
        );
        scenarios.add(new Scenario("Morning Routine", morning));

        // 2. Preparing for School
        List<ScenarioStep> prepare = Arrays.asList(
                new ScenarioStep("Pack backpack", "Put books and supplies", "🎒", 0),
                new ScenarioStep("Prepare lunch", "Make a sandwich", "🥪", 1),
                new ScenarioStep("Check schedule", "See today's classes", "📅", 2),
                new ScenarioStep("Leave home", "Head to school", "🚶", 3)
        );
        scenarios.add(new Scenario("Preparing for School", prepare));

        // 3. Writing an Essay
        List<ScenarioStep> essay = Arrays.asList(
                new ScenarioStep("Understand assignment", "Read the prompt", "📖", 0),
                new ScenarioStep("Research", "Find sources", "🔍", 1),
                new ScenarioStep("Create outline", "Organize main points", "📝", 2),
                new ScenarioStep("Write draft", "Get ideas on paper", "✍️", 3),
                new ScenarioStep("Revise and edit", "Check grammar and flow", "🔧", 4),
                new ScenarioStep("Submit", "Turn in the essay", "📤", 5)
        );
        scenarios.add(new Scenario("Writing an Essay", essay));

        // 4. Solving a Math Problem
        List<ScenarioStep> math = Arrays.asList(
                new ScenarioStep("Read the problem", "Understand what is asked", "📖", 0),
                new ScenarioStep("Identify known values", "List what you know", "🔢", 1),
                new ScenarioStep("Choose a strategy", "Decide formula or method", "🧠", 2),
                new ScenarioStep("Solve step by step", "Perform calculations", "🧮", 3),
                new ScenarioStep("Check your answer", "Verify if it makes sense", "✅", 4)
        );
        scenarios.add(new Scenario("Solving a Math Problem", math));

        // 5. Studying for a Test
        List<ScenarioStep> study = Arrays.asList(
                new ScenarioStep("Gather materials", "Notes, textbooks", "📚", 0),
                new ScenarioStep("Make a study plan", "Allocate time", "📋", 1),
                new ScenarioStep("Review notes", "Highlight key points", "🖍️", 2),
                new ScenarioStep("Practice questions", "Test yourself", "📝", 3),
                new ScenarioStep("Get rest before test", "Sleep well", "😴", 4)
        );
        scenarios.add(new Scenario("Studying for a Test", study));
    }

    // ---------- Game logic ----------
    private void loadScenario(int index) {
        Scenario scenario = scenarios.get(index);
        tvScenarioTitle.setText(scenario.title);
        currentSteps = new ArrayList<>(scenario.steps);
        Collections.shuffle(currentSteps);
        adapter = new StepAdapter(currentSteps);
        recyclerView.setAdapter(adapter);

        ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder dragged,
                                  @NonNull RecyclerView.ViewHolder target) {
                int from = dragged.getAdapterPosition();
                int to = target.getAdapterPosition();
                Collections.swap(currentSteps, from, to);
                adapter.notifyItemMoved(from, to);
                return true;
            }
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}
        });
        touchHelper.attachToRecyclerView(recyclerView);

        btnCheckOrder.setEnabled(true);
        btnNextScenario.setVisibility(View.GONE);
        tvFeedback.setVisibility(View.GONE);
    }

    private void checkOrder() {
        Scenario scenario = scenarios.get(currentScenarioIndex);
        List<ScenarioStep> correctOrder = scenario.steps;
        boolean isCorrect = true;
        for (int i = 0; i < currentSteps.size(); i++) {
            if (currentSteps.get(i).correctOrder != i) {
                isCorrect = false;
                break;
            }
        }
        if (isCorrect) {
            int pointsEarned = 100 / scenarios.size();  // each scenario worth 20 points (5 scenarios → 100 total)
            totalScore += pointsEarned;
            tvScore.setText("Score: " + totalScore);
            tvFeedback.setText("✅ Correct! Great sequencing!");
            tvFeedback.setVisibility(View.VISIBLE);
            btnCheckOrder.setEnabled(false);
            btnNextScenario.setVisibility(View.VISIBLE);
        } else {
            tvFeedback.setText("❌ Not quite right. Drag steps to reorder and try again.");
            tvFeedback.setVisibility(View.VISIBLE);
        }
    }
}