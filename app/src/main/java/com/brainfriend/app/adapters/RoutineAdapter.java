package com.brainfriend.app.adapters;

import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.brainfriend.app.R;
import com.brainfriend.app.models.RoutineItem;
import java.util.List;
import java.util.Locale;

public class RoutineAdapter
        extends RecyclerView.Adapter<RoutineAdapter.VH> {

    public interface RoutineListener {
        void onComplete(RoutineItem item);
        void onEdit(RoutineItem item);
        void onDelete(RoutineItem item);
    }

    private List<RoutineItem> items;
    private final RoutineListener listener;

    public RoutineAdapter(List<RoutineItem> items, RoutineListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void updateItems(List<RoutineItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_routine_task, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        RoutineItem item = items.get(position);

        holder.tvTitle.setText(item.getTitle());
        holder.tvTime.setText(String.format(Locale.getDefault(),
                "%02d:%02d", item.getDueHour(), item.getDueMinute()));

        // Priority bar color
        switch (item.getImportance()) {
            case 3:
                holder.priorityBar.setBackgroundColor(
                        Color.parseColor("#EF4444"));
                break;
            case 2:
                holder.priorityBar.setBackgroundColor(
                        Color.parseColor("#F59E0B"));
                break;
            default:
                holder.priorityBar.setBackgroundColor(
                        Color.parseColor("#22C55E"));
                break;
        }

        // Strike through if done
        holder.cbDone.setOnCheckedChangeListener(null);
        holder.cbDone.setChecked(item.isCompleted());
        if (item.isCompleted()) {
            holder.tvTitle.setPaintFlags(
                    holder.tvTitle.getPaintFlags()
                            | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTitle.setAlpha(0.5f);
        } else {
            holder.tvTitle.setPaintFlags(
                    holder.tvTitle.getPaintFlags()
                            & ~Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTitle.setAlpha(1f);
        }

        holder.cbDone.setOnCheckedChangeListener((btn, checked) -> {
            if (listener != null) listener.onComplete(item);
        });

        holder.ivEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(item);
        });

        holder.ivDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(item);
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTime;
        CheckBox cbDone;
        ImageView ivEdit, ivDelete;
        View priorityBar;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_routine_title);
            tvTime = itemView.findViewById(R.id.tv_routine_time);
            cbDone = itemView.findViewById(R.id.cb_routine_done);
            ivEdit = itemView.findViewById(R.id.iv_routine_edit);
            ivDelete = itemView.findViewById(R.id.iv_routine_delete);
            priorityBar = itemView.findViewById(R.id.routine_priority_bar);
        }
    }
}