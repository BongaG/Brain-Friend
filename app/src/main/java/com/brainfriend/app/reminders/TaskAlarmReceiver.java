package com.brainfriend.app.reminders;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import com.brainfriend.app.MainActivity;
import com.brainfriend.app.R;
import com.brainfriend.app.ai.AiInsightsHelper;

public class TaskAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("task_title");
        int importance = intent.getIntExtra("task_importance", 1);
        String category = intent.getStringExtra("task_category");
        if (title == null) title = "Upcoming Task";

        final String taskTitle = title;
        final int notifId = ("alert_" + taskTitle).hashCode();

        Intent mainIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, notifId, mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE);

        // Show immediately
        showNotification(context, taskTitle,
                "🧠 Personalising your reminder...",
                pendingIntent, notifId);

        // Get AI message
        AiInsightsHelper.getSmartReminderMessage(taskTitle, importance,
                category != null ? category : "Personal",
                new AiInsightsHelper.AiCallback() {
                    @Override
                    public void onResult(String message) {
                        showNotification(context, taskTitle,
                                message, pendingIntent, notifId);
                    }

                    @Override
                    public void onError(String error) {
                        showNotification(context, taskTitle,
                                "⏰ \"" + taskTitle
                                        + "\" starts in 10 minutes — get ready!",
                                pendingIntent, notifId);
                    }
                });
    }

    private void showNotification(Context context, String title,
                                  String message,
                                  PendingIntent pendingIntent,
                                  int notifId) {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, "task_alerts")
                        .setSmallIcon(R.drawable.ic_nav_task)
                        .setContentTitle("🧠 Brain Friend — " + title)
                        .setContentText(message)
                        .setStyle(new NotificationCompat
                                .BigTextStyle().bigText(message))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null)
            manager.notify(notifId, builder.build());
    }
}