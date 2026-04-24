package com.brainfriend.app.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.brainfriend.app.R;
import com.brainfriend.app.models.NotificationItem;
import java.util.List;

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
        holder.tvBody.setText(item.getBody());

        switch (item.getType()) {
            case NotificationItem.TYPE_OVERDUE:
                holder.dot.setBackgroundColor(Color.parseColor("#EF4444"));
                holder.tvTitle.setTextColor(Color.parseColor("#EF4444"));
                break;
            case NotificationItem.TYPE_ALERT:
                holder.dot.setBackgroundColor(Color.parseColor("#F59E0B"));
                holder.tvTitle.setTextColor(Color.parseColor("#F59E0B"));
                break;
            case NotificationItem.TYPE_DONE:
                holder.dot.setBackgroundColor(Color.parseColor("#22C55E"));
                holder.tvTitle.setTextColor(Color.parseColor("#22C55E"));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvBody;
        View dot;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_notif_title);
            tvBody = itemView.findViewById(R.id.tv_notif_body);
            dot = itemView.findViewById(R.id.notif_dot);
        }
    }
}