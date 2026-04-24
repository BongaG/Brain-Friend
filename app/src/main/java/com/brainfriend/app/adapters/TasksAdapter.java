package com.brainfriend.app.adapters;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.brainfriend.app.R;
import com.brainfriend.app.models.Task;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TasksAdapter extends RecyclerView.Adapter<TasksAdapter.TaskViewHolder> {

    public interface OnTaskClickListener {
        void onTaskChecked(Task task, boolean isChecked);
    }

    private List<Task> tasks = new ArrayList<>();
    private List<Task> allTasks = new ArrayList<>();
    private final OnTaskClickListener listener;

    public TasksAdapter(List<Task> tasks, OnTaskClickListener listener) {
        this.tasks = new ArrayList<>(tasks);
        this.allTasks = new ArrayList<>(tasks);
        this.listener = listener;
    }

    public void updateTasks(List<Task> newTasks) {
        this.allTasks = new ArrayList<>(newTasks);
        this.tasks = new ArrayList<>(newTasks);
        notifyDataSetChanged();
    }

    public List<Task> getAllTasks() {
        return allTasks;
    }

    public void filterAll() {
        tasks = new ArrayList<>(allTasks);
        notifyDataSetChanged();
    }

    public void filterToday() {
        List<Task> filtered = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String today = sdf.format(new Date());
        for (Task t : allTasks) {
            if (t.getDueDate() != null && sdf.format(t.getDueDate()).equals(today)) {
                filtered.add(t);
            }
        }
        tasks = filtered;
        notifyDataSetChanged();
    }

    public void filterHigh() {
        List<Task> filtered = new ArrayList<>();
        for (Task t : allTasks) {
            if (t.getImportance() == 3) filtered.add(t);
        }
        tasks = filtered;
        notifyDataSetChanged();
    }

    public void filterOverdue() {
        List<Task> filtered = new ArrayList<>();
        Date now = new Date();
        for (Task t : allTasks) {
            if (!t.isCompleted() && t.getDueDate() != null && t.getDueDate().before(now)) {
                filtered.add(t);
            }
        }
        tasks = filtered;
        notifyDataSetChanged();
    }

    public void removeAt(int position) {
        tasks.remove(position);
        notifyItemRemoved(position);
    }

    public Task getTaskAt(int position) {
        return tasks.get(position);
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);

        holder.tvTitle.setText(task.getTitle());
        holder.tvDetails.setText(task.getDetails());
        holder.tvCategory.setText(task.getCategory());

        // Priority
        switch (task.getImportance()) {
            case 3:
                holder.tvPriority.setText("HIGH");
                holder.tvPriority.setTextColor(Color.parseColor("#EF4444"));
                holder.tvPriority.setBackgroundColor(Color.parseColor("#FFF1F2"));
                holder.priorityBar.setBackgroundColor(Color.parseColor("#EF4444"));
                break;
            case 2:
                holder.tvPriority.setText("MED");
                holder.tvPriority.setTextColor(Color.parseColor("#F59E0B"));
                holder.tvPriority.setBackgroundColor(Color.parseColor("#FFFBEB"));
                holder.priorityBar.setBackgroundColor(Color.parseColor("#F59E0B"));
                break;
            default:
                holder.tvPriority.setText("LOW");
                holder.tvPriority.setTextColor(Color.parseColor("#22C55E"));
                holder.tvPriority.setBackgroundColor(Color.parseColor("#F0FDF4"));
                holder.priorityBar.setBackgroundColor(Color.parseColor("#22C55E"));
                break;
        }

        // Due date
        if (task.getDueDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM", Locale.getDefault());
            String timeStr = String.format(Locale.getDefault(), "%02d:%02d",
                    task.getDueHour(), task.getDueMinute());
            holder.tvDue.setText(sdf.format(task.getDueDate()) + " at " + timeStr);
        }

        // Overdue detection
        boolean isOverdue = !task.isCompleted()
                && task.getDueDate() != null
                && task.getDueDate().before(new Date());
        if (holder.tvOverdue != null) {
            holder.tvOverdue.setVisibility(isOverdue ? View.VISIBLE : View.GONE);
        }
        if (isOverdue) {
            holder.tvTitle.setTextColor(Color.parseColor("#EF4444"));
        } else {
            holder.tvTitle.setTextColor(Color.parseColor("#0F172A"));
        }

        // Alert badge
        holder.tvAlert.setVisibility(task.isAlertEnabled() ? View.VISIBLE : View.GONE);

        // Strike through if completed
        if (task.isCompleted()) {
            holder.tvTitle.setPaintFlags(
                    holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTitle.setAlpha(0.5f);
        } else {
            holder.tvTitle.setPaintFlags(
                    holder.tvTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTitle.setAlpha(1f);
        }

        // Checkbox with animation
        holder.cbDone.setOnCheckedChangeListener(null);
        holder.cbDone.setChecked(task.isCompleted());
        holder.cbDone.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) playCompletionAnimation(holder.itemView);
            if (listener != null) listener.onTaskChecked(task, checked);
        });
    }

    private void playCompletionAnimation(View view) {
        // Scale bounce animation on the card
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.05f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.05f, 1f);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY);
        set.setDuration(300);
        set.setInterpolator(new OvershootInterpolator());
        set.start();
    }

    @Override
    public int getItemCount() {
        return tasks != null ? tasks.size() : 0;
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDetails, tvPriority, tvCategory, tvDue, tvAlert, tvOverdue;
        TextView tvSubtaskCount, tvProgressPct;
        CheckBox cbDone;
        View priorityBar;
        ProgressBar pbSubtasks;
        View llProgress;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_task_title);
            tvDetails = itemView.findViewById(R.id.tv_task_details);
            tvPriority = itemView.findViewById(R.id.tv_task_priority);
            tvCategory = itemView.findViewById(R.id.tv_task_category);
            tvDue = itemView.findViewById(R.id.tv_task_due);
            tvAlert = itemView.findViewById(R.id.tv_task_alert);
            tvOverdue = itemView.findViewById(R.id.tv_overdue_badge);
            cbDone = itemView.findViewById(R.id.cb_task_done);
            priorityBar = itemView.findViewById(R.id.priority_bar);
            pbSubtasks = itemView.findViewById(R.id.pb_subtasks);
            tvSubtaskCount = itemView.findViewById(R.id.tv_subtask_count);
            tvProgressPct = itemView.findViewById(R.id.tv_progress_pct);
            llProgress = itemView.findViewById(R.id.ll_progress);
        }
    }
}