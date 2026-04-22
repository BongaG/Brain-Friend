package com.brainfriend.app.adapters;

import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.brainfriend.app.R;
import com.brainfriend.app.models.Task;
import java.util.List;

public class TasksAdapter extends RecyclerView.Adapter<TasksAdapter.TaskViewHolder> {
    private List<Task> tasks;
    private OnTaskClickListener listener;

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
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.tvTitle.setText(task.getTitle());
        holder.tvCat.setText(task.getCategory() != null ? task.getCategory() : "General");

        // Importance Styling
        switch (task.getImportance()) {
            case 3: // High
                holder.tvImp.setText("HIGH");
                holder.tvImp.setTextColor(Color.parseColor("#EF4444"));
                holder.cvImp.setCardBackgroundColor(Color.parseColor("#FEE2E2"));
                break;
            case 2: // Medium
                holder.tvImp.setText("MED");
                holder.tvImp.setTextColor(Color.parseColor("#F59E0B"));
                holder.cvImp.setCardBackgroundColor(Color.parseColor("#FEF3C7"));
                break;
            default: // Low
                holder.tvImp.setText("LOW");
                holder.tvImp.setTextColor(Color.parseColor("#10B981"));
                holder.cvImp.setCardBackgroundColor(Color.parseColor("#D1FAE5"));
                break;
        }

        holder.cb.setOnCheckedChangeListener(null);
        holder.cb.setChecked(task.isCompleted());

        if (task.isCompleted()) {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTitle.setAlpha(0.5f);
        } else {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvTitle.setAlpha(1.0f);
        }

        holder.cb.setOnCheckedChangeListener((bv, isChecked) -> listener.onTaskChecked(task, isChecked));
    }

    @Override
    public int getItemCount() { return tasks.size(); }

    public void updateTasks(List<Task> newTasks) {
        this.tasks = newTasks;
        notifyDataSetChanged();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCat, tvImp;
        CardView cvImp;
        CheckBox cb;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_task_title);
            tvCat = itemView.findViewById(R.id.tv_task_category);
            tvImp = itemView.findViewById(R.id.tv_importance_label);
            cvImp = itemView.findViewById(R.id.cv_importance_badge);
            cb = itemView.findViewById(R.id.cb_task_status);
        }
    }
}