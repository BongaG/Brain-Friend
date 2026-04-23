package com.brainfriend.app.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.brainfriend.app.R;
import com.brainfriend.app.models.Task;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TasksAdapter extends RecyclerView.Adapter<TasksAdapter.TaskViewHolder> {

    public interface OnTaskClickListener {
        void onTaskChecked(Task task, boolean isChecked);
    }

    private List<Task> tasks;
    private final OnTaskClickListener listener;

    public TasksAdapter(List<Task> tasks, OnTaskClickListener listener) {
        this.tasks = tasks;
        this.listener = listener;
    }

    public void updateTasks(List<Task> newTasks) {
        this.tasks = newTasks;
        notifyDataSetChanged();
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

        // Priority label + color
        switch (task.getImportance()) {
            case 3:
                holder.tvPriority.setText("HIGH");
                holder.tvPriority.setTextColor(Color.parseColor("#EF4444"));
                holder.priorityBar.setBackgroundColor(Color.parseColor("#EF4444"));
                break;
            case 2:
                holder.tvPriority.setText("MED");
                holder.tvPriority.setTextColor(Color.parseColor("#F59E0B"));
                holder.priorityBar.setBackgroundColor(Color.parseColor("#F59E0B"));
                break;
            default:
                holder.tvPriority.setText("LOW");
                holder.tvPriority.setTextColor(Color.parseColor("#22C55E"));
                holder.priorityBar.setBackgroundColor(Color.parseColor("#22C55E"));
                break;
        }

        // Due date + time
        if (task.getDueDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM", Locale.getDefault());
            String dateStr = sdf.format(task.getDueDate());
            String timeStr = String.format(Locale.getDefault(), "%02d:%02d",
                    task.getDueHour(), task.getDueMinute());
            holder.tvDue.setText(dateStr + " at " + timeStr);
        }

        // Alert badge
        holder.tvAlert.setVisibility(task.isAlertEnabled() ? View.VISIBLE : View.GONE);

        // Checkbox
        holder.cbDone.setOnCheckedChangeListener(null);
        holder.cbDone.setChecked(task.isCompleted());
        holder.cbDone.setOnCheckedChangeListener((btn, checked) -> {
            if (listener != null) listener.onTaskChecked(task, checked);
        });

        // Strike through if completed
        holder.tvTitle.setPaintFlags(task.isCompleted()
                ? holder.tvTitle.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                : holder.tvTitle.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
    }

    @Override
    public int getItemCount() { return tasks != null ? tasks.size() : 0; }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDetails, tvPriority, tvCategory, tvDue, tvAlert;
        CheckBox cbDone;
        View priorityBar;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_task_title);
            tvDetails = itemView.findViewById(R.id.tv_task_details);
            tvPriority = itemView.findViewById(R.id.tv_task_priority);
            tvCategory = itemView.findViewById(R.id.tv_task_category);
            tvDue = itemView.findViewById(R.id.tv_task_due);
            tvAlert = itemView.findViewById(R.id.tv_task_alert);
            cbDone = itemView.findViewById(R.id.cb_task_done);
            priorityBar = itemView.findViewById(R.id.priority_bar);
        }
    }
}