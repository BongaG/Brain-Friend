package com.brainfriend.app.adapters;

import android.graphics.Color;
import android.graphics.Paint;
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
    private List<Task> tasks;
    private OnTaskClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());

    public interface OnTaskClickListener {
        void onTaskChecked(Task task, boolean isChecked);
    }

    public TasksAdapter(List<Task> tasks, OnTaskClickListener listener) {
        this.tasks = tasks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.bind(task, listener, dateFormat);
    }

    @Override
    public int getItemCount() { return tasks.size(); }

    public void updateTasks(List<Task> newTasks) {
        this.tasks = newTasks;
        notifyDataSetChanged();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDetails, tvTime;
        View importanceIndicator;
        CheckBox cbStatus;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_task_title);
            tvDetails = itemView.findViewById(R.id.tv_task_details);
            tvTime = itemView.findViewById(R.id.tv_task_time);
            importanceIndicator = itemView.findViewById(R.id.importance_indicator);
            cbStatus = itemView.findViewById(R.id.cb_task_status);
        }

        public void bind(Task task, OnTaskClickListener listener, SimpleDateFormat dateFormat) {
            tvTitle.setText(task.getTitle());
            tvDetails.setText(task.getDetails());
            tvDetails.setVisibility(task.getDetails() != null && !task.getDetails().isEmpty() ? View.VISIBLE : View.GONE);
            tvTime.setText(task.getDueDate() != null ? dateFormat.format(task.getDueDate()) : "No date set");

            int indicatorColor = task.getImportance() == 3 ? Color.parseColor("#EF4444") :
                    (task.getImportance() == 2 ? Color.parseColor("#F59E0B") : Color.parseColor("#10B981"));
            importanceIndicator.setBackgroundColor(indicatorColor);

            cbStatus.setOnCheckedChangeListener(null);
            cbStatus.setChecked(task.isCompleted());

            if (task.isCompleted()) {
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                tvTitle.setTextColor(Color.GRAY);
            } else {
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                tvTitle.setTextColor(Color.parseColor("#0F172A"));
            }

            cbStatus.setOnCheckedChangeListener((buttonView, isChecked) -> listener.onTaskChecked(task, isChecked));
        }
    }
}