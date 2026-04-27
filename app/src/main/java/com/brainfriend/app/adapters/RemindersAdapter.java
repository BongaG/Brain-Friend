package com.brainfriend.app.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.brainfriend.app.R;
import com.brainfriend.app.reminders.ReminderEntity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RemindersAdapter extends RecyclerView.Adapter<RemindersAdapter.VH> {

    private List<ReminderEntity> reminders;

    public RemindersAdapter(List<ReminderEntity> reminders) {
        this.reminders = reminders;
    }

    public void updateReminders(List<ReminderEntity> newReminders) {
        this.reminders = newReminders;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reminder, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ReminderEntity reminder = reminders.get(position);

        // Format time
        SimpleDateFormat timeFmt = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        SimpleDateFormat dateFmt = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault());
        Date date = new Date(reminder.triggerTimeMs);

        holder.tvTime.setText("⏰ " + timeFmt.format(date));
        holder.tvText.setText(reminder.title);
        holder.tvDate.setText(dateFmt.format(date));

        // Color bar and status based on time
        Date now = new Date();
        if (date.before(now)) {
            // Past — grey
            holder.bar.setBackgroundColor(Color.parseColor("#94A3B8"));
            holder.tvStatus.setText("✅");
            holder.tvTime.setTextColor(Color.parseColor("#94A3B8"));
            holder.tvTime.setBackgroundColor(Color.parseColor("#F1F5F9"));
        } else {
            // Upcoming — blue
            long diff = date.getTime() - now.getTime();
            if (diff <= 60 * 60 * 1000) {
                // Within 1 hour — orange urgent
                holder.bar.setBackgroundColor(Color.parseColor("#F59E0B"));
                holder.tvStatus.setText("⚡");
                holder.tvTime.setTextColor(Color.parseColor("#F59E0B"));
                holder.tvTime.setBackgroundColor(Color.parseColor("#FFFBEB"));
            } else {
                // Normal upcoming — blue
                holder.bar.setBackgroundColor(Color.parseColor("#2563EB"));
                holder.tvStatus.setText("🔔");
                holder.tvTime.setTextColor(Color.parseColor("#2563EB"));
                holder.tvTime.setBackgroundColor(Color.parseColor("#EFF6FF"));
            }
        }
    }

    @Override
    public int getItemCount() {
        return reminders != null ? reminders.size() : 0;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTime, tvText, tvDate, tvStatus;
        View bar;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tv_reminder_time);
            tvText = itemView.findViewById(R.id.tv_reminder_text);
            tvDate = itemView.findViewById(R.id.tv_reminder_date);
            tvStatus = itemView.findViewById(R.id.tv_reminder_status);
            bar = itemView.findViewById(R.id.reminder_bar);
        }
    }
}