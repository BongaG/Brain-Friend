package com.brainfriend.app.adapters;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.brainfriend.app.R;
import com.brainfriend.app.ai.AiInsightsHelper;
import com.brainfriend.app.models.NotificationItem;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.VH> {

    private final List<NotificationItem> items;

    public NotificationsAdapter(List<NotificationItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        NotificationItem item = items.get(position);

        holder.tvTitle.setText(item.getTitle());
        holder.tvBody.setText(item.getAiMessage());

        switch (item.getType()) {
            case NotificationItem.TYPE_OVERDUE:
                holder.dot.setBackgroundColor(Color.parseColor("#EF4444"));
                holder.tvTitle.setTextColor(Color.parseColor("#EF4444"));
                // Show reschedule button
                holder.btnReschedule.setVisibility(View.VISIBLE);
                holder.btnReschedule.setOnClickListener(v ->
                        showReschedulePicker(holder, item));
                break;

            case NotificationItem.TYPE_ALERT:
                holder.dot.setBackgroundColor(Color.parseColor("#F59E0B"));
                holder.tvTitle.setTextColor(Color.parseColor("#F59E0B"));
                holder.btnReschedule.setVisibility(View.GONE);
                break;

            case NotificationItem.TYPE_DONE:
                holder.dot.setBackgroundColor(Color.parseColor("#22C55E"));
                holder.tvTitle.setTextColor(Color.parseColor("#22C55E"));
                holder.btnReschedule.setVisibility(View.GONE);
                break;
        }

        // Load AI message if not loaded yet
        if (!item.isAiLoaded()) {
            item.setAiLoaded(true);
            holder.tvAiLoading.setVisibility(View.VISIBLE);

            if (item.getType() == NotificationItem.TYPE_DONE) {
                AiInsightsHelper.getCompletionMessage(
                        item.getTaskTitle(), item.getImportance(), item.getCategory(),
                        new AiInsightsHelper.AiCallback() {
                            @Override
                            public void onResult(String message) {
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    item.setAiMessage(message);
                                    holder.tvBody.setText(message);
                                    holder.tvAiLoading.setVisibility(View.GONE);
                                });
                            }
                            @Override
                            public void onError(String error) {
                                AiInsightsHelper.callCognitivePrompt(
                                        "Student completed task: \"" + item.getTaskTitle()
                                                + "\". Write a 1 sentence celebration. 1 emoji.",
                                        new AiInsightsHelper.AiCallback() {
                                            @Override
                                            public void onResult(String msg) {
                                                new Handler(Looper.getMainLooper()).post(() -> {
                                                    item.setAiMessage(msg);
                                                    holder.tvBody.setText(msg);
                                                    holder.tvAiLoading.setVisibility(View.GONE);
                                                });
                                            }
                                            @Override
                                            public void onError(String e) {
                                                new Handler(Looper.getMainLooper()).post(() ->
                                                        holder.tvAiLoading.setVisibility(View.GONE));
                                            }
                                        });
                            }
                        });

            } else if (item.getType() == NotificationItem.TYPE_OVERDUE) {
                AiInsightsHelper.getMissedTaskMessage(
                        item.getTaskTitle(), item.getImportance(), item.getCategory(),
                        new AiInsightsHelper.AiCallback() {
                            @Override
                            public void onResult(String message) {
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    item.setAiMessage(message);
                                    holder.tvBody.setText(message);
                                    holder.tvAiLoading.setVisibility(View.GONE);
                                });
                            }
                            @Override
                            public void onError(String error) {
                                AiInsightsHelper.callCognitivePrompt(
                                        "Student missed task: \"" + item.getTaskTitle()
                                                + "\". Write a 1 sentence urgent reminder to reschedule. "
                                                + "1 emoji.",
                                        new AiInsightsHelper.AiCallback() {
                                            @Override
                                            public void onResult(String msg) {
                                                new Handler(Looper.getMainLooper()).post(() -> {
                                                    item.setAiMessage(msg);
                                                    holder.tvBody.setText(msg);
                                                    holder.tvAiLoading.setVisibility(View.GONE);
                                                });
                                            }
                                            @Override
                                            public void onError(String e) {
                                                new Handler(Looper.getMainLooper()).post(() ->
                                                        holder.tvAiLoading.setVisibility(View.GONE));
                                            }
                                        });
                            }
                        });
            } else {
                holder.tvAiLoading.setVisibility(View.GONE);
            }
        }
    }

    private void showReschedulePicker(VH holder, NotificationItem item) {
        Calendar now = Calendar.getInstance();
        new DatePickerDialog(holder.itemView.getContext(),
                (dp, year, month, day) -> {
                    new TimePickerDialog(holder.itemView.getContext(),
                            (tp, hour, minute) -> {
                                Calendar newCal = Calendar.getInstance();
                                newCal.set(year, month, day, hour, minute, 0);

                                // Update task in Firestore
                                if (item.getTaskId() != null) {
                                    FirebaseFirestore.getInstance()
                                            .collection("tasks")
                                            .document(item.getTaskId())
                                            .update(
                                                    "dueDate", newCal.getTime(),
                                                    "dueHour", hour,
                                                    "dueMinute", minute,
                                                    "completed", false
                                            );
                                }

                                // Update button text
                                String newTime = String.format(Locale.getDefault(),
                                        "Rescheduled to %02d/%02d at %02d:%02d",
                                        day, month + 1, hour, minute);
                                holder.btnReschedule.setText("✅ " + newTime);
                                holder.btnReschedule.setBackgroundTintList(
                                        android.content.res.ColorStateList.valueOf(
                                                Color.parseColor("#22C55E")));
                            },
                            now.get(Calendar.HOUR_OF_DAY),
                            now.get(Calendar.MINUTE), true).show();
                },
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)).show();
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvBody, tvAiLoading;
        View dot;
        Button btnReschedule;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_notif_title);
            tvBody = itemView.findViewById(R.id.tv_notif_body);
            tvAiLoading = itemView.findViewById(R.id.tv_ai_loading);
            dot = itemView.findViewById(R.id.notif_dot);
            btnReschedule = itemView.findViewById(R.id.btn_reschedule);
        }
    }
}