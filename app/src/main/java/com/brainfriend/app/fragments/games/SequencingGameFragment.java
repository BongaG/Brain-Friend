package com.brainfriend.app.fragments.games;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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


    private TextView tvScenarioTitle, tvInstruction, tvScore, tvFeedback;
    private Button btnCheckOrder, btnNextScenario, btnHint;
    private RecyclerView recyclerView;
    private StepAdapter adapter;
    private List<ScenarioStep> currentSteps;
    private int currentScenarioIndex = 0;
    private int totalScore = 0;
    private int wrongAttempts = 0;     // track wrong attempts for current scenario
    private List<Scenario> scenarios;
    private StatsManager statsManager;
    private boolean gameEndRecorded = false;


    private class StepAdapter extends RecyclerView.Adapter<StepAdapter.ViewHolder> {
        private List<ScenarioStep> steps;
        StepAdapter(List<ScenarioStep> steps) { this.steps = steps; }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_step, parent, false);
            return new ViewHolder(v);
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ScenarioStep step = steps.get(position);
            holder.tvText.setText(step.title);
            holder.tvEmoji.setText(step.emoji);
        }
        @Override public int getItemCount() { return steps.size(); }
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
        btnHint = view.findViewById(R.id.btn_hint);
        recyclerView = view.findViewById(R.id.rv_steps);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        statsManager = new StatsManager(requireContext());

        setupScenarios();
        loadScenario(currentScenarioIndex);

        btnCheckOrder.setOnClickListener(v -> checkOrder());
        btnNextScenario.setOnClickListener(v -> nextScenario());
        btnHint.setOnClickListener(v -> showHint());

        // Show draggable hint once
        Toast.makeText(getContext(), "Long press any tile to reorder", Toast.LENGTH_SHORT).show();
    }

    private void nextScenario() {
        if (currentScenarioIndex + 1 < scenarios.size()) {
            currentScenarioIndex++;
            loadScenario(currentScenarioIndex);
            btnNextScenario.setVisibility(View.GONE);
            btnCheckOrder.setEnabled(true);
            tvFeedback.setVisibility(View.GONE);
            wrongAttempts = 0;  // reset for new scenario
        } else {
            // Game completed – record stats
            tvFeedback.setText("🎉 You completed all scenarios! Final score: " + totalScore);
            tvFeedback.setVisibility(View.VISIBLE);
            btnNextScenario.setEnabled(false);
            btnCheckOrder.setEnabled(false);
            if (!gameEndRecorded) {
                gameEndRecorded = true;
                statsManager.recordPlay();
                statsManager.setSequencingLevel(scenarios.size());
                statsManager.setSequencingScore(totalScore);
            }
        }
    }

    private void showHint() {
        Scenario scenario = scenarios.get(currentScenarioIndex);
        StringBuilder hint = new StringBuilder("Correct order:\n");
        for (ScenarioStep step : scenario.steps) {
            hint.append(step.emoji).append(" ").append(step.title).append("\n");
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("💡 Hint")
                .setMessage(hint.toString())
                .setPositiveButton("OK", null)
                .show();
    }


    private void setupScenarios() {
        scenarios = new ArrayList<>();

        List<ScenarioStep> morning = Arrays.asList(
                new ScenarioStep("Wake up", "Open your eyes", "🌞", 0),
                new ScenarioStep("Brush teeth", "Clean teeth", "🦷", 1),
                new ScenarioStep("Get dressed", "Put on uniform", "👕", 2),
                new ScenarioStep("Eat breakfast", "Have a meal", "🍳", 3),
                new ScenarioStep("Go to school", "Pack bag and leave", "🏫", 4)
        );
        scenarios.add(new Scenario("Morning Routine", morning));

        List<ScenarioStep> prepare = Arrays.asList(
                new ScenarioStep("Pack backpack", "Put books and supplies", "🎒", 0),
                new ScenarioStep("Prepare lunch", "Make a sandwich", "🥪", 1),
                new ScenarioStep("Check schedule", "See today's classes", "📅", 2),
                new ScenarioStep("Leave home", "Head to school", "🚶", 3)
        );
        scenarios.add(new Scenario("Preparing for School", prepare));

        List<ScenarioStep> essay = Arrays.asList(
                new ScenarioStep("Understand assignment", "Read the prompt", "📖", 0),
                new ScenarioStep("Research", "Find sources", "🔍", 1),
                new ScenarioStep("Create outline", "Organize main points", "📝", 2),
                new ScenarioStep("Write draft", "Get ideas on paper", "✍️", 3),
                new ScenarioStep("Revise and edit", "Check grammar and flow", "🔧", 4),
                new ScenarioStep("Submit", "Turn in the essay", "📤", 5)
        );
        scenarios.add(new Scenario("Writing an Essay", essay));

        List<ScenarioStep> math = Arrays.asList(
                new ScenarioStep("Read problem", "Understand question", "📖", 0),
                new ScenarioStep("Identify known values", "List data", "🔢", 1),
                new ScenarioStep("Choose strategy", "Plan solution", "🧠", 2),
                new ScenarioStep("Solve step by step", "Calculate", "🧮", 3),
                new ScenarioStep("Check answer", "Verify", "✅", 4)
        );
        scenarios.add(new Scenario("Solving a Math Problem", math));

        List<ScenarioStep> study = Arrays.asList(
                new ScenarioStep("Gather materials", "Notes, books", "📚", 0),
                new ScenarioStep("Make study plan", "Schedule time", "📋", 1),
                new ScenarioStep("Review notes", "Highlight key points", "🖍️", 2),
                new ScenarioStep("Practice questions", "Test yourself", "📝", 3),
                new ScenarioStep("Get rest", "Sleep well", "😴", 4)
        );
        scenarios.add(new Scenario("Studying for a Test", study));
    }

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
            // Enable long press drag (default is long press)
        });
        touchHelper.attachToRecyclerView(recyclerView);

        btnCheckOrder.setEnabled(true);
        btnNextScenario.setVisibility(View.GONE);
        tvFeedback.setVisibility(View.GONE);
        wrongAttempts = 0;
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
            int pointsEarned = 100 / scenarios.size(); // each scenario worth 20 points
            totalScore += pointsEarned;
            tvScore.setText("Score: " + totalScore);
            tvFeedback.setText("✅ Correct! Great sequencing!");
            tvFeedback.setVisibility(View.VISIBLE);
            btnCheckOrder.setEnabled(false);
            btnNextScenario.setVisibility(View.VISIBLE);
        } else {
            wrongAttempts++;
            String feedback = "❌ Not quite right. Drag steps to reorder and try again.";
            if (wrongAttempts >= 2) {
                feedback += " Hint: Tap the light bulb for the correct order.";
            }
            tvFeedback.setText(feedback);
            tvFeedback.setVisibility(View.VISIBLE);
            // Keep the check order button enabled so they can try again
        }
    }
}