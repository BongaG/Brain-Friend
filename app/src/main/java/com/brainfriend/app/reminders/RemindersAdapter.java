package com.brainfriend.app.reminders;

import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.brainfriend.app.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RemindersAdapter extends ListAdapter<ReminderEntity, RemindersAdapter.ViewHolder> {

    public interface OnReminderDoneListener {
        void onDone(ReminderEntity entity);
    }

    private final OnReminderDoneListener listener;
    private static final SimpleDateFormat SDF =
            new SimpleDateFormat("EEE dd MMM, HH:mm", Locale.getDefault());

    public RemindersAdapter(OnReminderDoneListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reminder, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReminderEntity entity = getItem(position);
        holder.tvTitle.setText(entity.title);
        holder.tvTime.setText("⏰ " + SDF.format(new Date(entity.triggerTimeMs)));

        // Strikethrough + gray color for done (inactive) reminders
        if (!entity.isActive) {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTitle.setTextColor(Color.GRAY);
            holder.tvTime.setTextColor(Color.LTGRAY);
        } else {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvTitle.setTextColor(Color.parseColor("#2D2152"));
            holder.tvTime.setTextColor(Color.parseColor("#7B6FB5"));
        }

        // Tap listener – only trigger if reminder is still active
        holder.itemView.setOnClickListener(v -> {
            if (entity.isActive) {
                listener.onDone(entity);
            }
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTime;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_reminder_title);
            tvTime  = itemView.findViewById(R.id.tv_reminder_time);
        }
    }

    private static final DiffUtil.ItemCallback<ReminderEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ReminderEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull ReminderEntity a, @NonNull ReminderEntity b) {
                    return a.id == b.id;
                }
                @Override
                public boolean areContentsTheSame(@NonNull ReminderEntity a, @NonNull ReminderEntity b) {
                    return a.title.equals(b.title) && a.triggerTimeMs == b.triggerTimeMs && a.isActive == b.isActive;
                }
            };
}