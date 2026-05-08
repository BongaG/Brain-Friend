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

public class RoutineAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("routine_title");
        int importance = intent.getIntExtra("routine_importance", 1);
        String category = intent.getStringExtra("routine_category");
        if (title == null) title = "Daily Routine";

        final String routineTitle = title;
        final int notifId = ("routine_" + routineTitle).hashCode();

        Intent mainIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Show loading notification first
        showNotification(context, routineTitle,
                "🧠 Your daily routine reminder is being personalised...",
                pendingIntent, notifId);

        // Get AI message for routine — uses same helper but different prompt
        AiInsightsHelper.getRoutineReminderMessage(routineTitle, importance,
                category != null ? category : "Personal",
                new AiInsightsHelper.AiCallback() {
                    @Override
                    public void onResult(String message) {
                        showNotification(context, routineTitle,
                                message, pendingIntent, notifId);
                    }

                    @Override
                    public void onError(String error) {
                        AiInsightsHelper.callCognitivePrompt(
                                "Write a 1 sentence daily routine reminder for: \""
                                        + routineTitle + "\". 1 emoji. Motivating.",
                                new AiInsightsHelper.AiCallback() {
                                    @Override
                                    public void onResult(String msg) {
                                        showNotification(context, routineTitle,
                                                msg, pendingIntent, notifId);
                                    }
                                    @Override
                                    public void onError(String e) {
                                        showNotification(context, routineTitle,
                                                "🔁 Time for your daily routine!",
                                                pendingIntent, notifId);
                                    }
                                });
                    }
                });
    }

    private void showNotification(Context context, String title,
                                  String message, PendingIntent pendingIntent,
                                  int notifId) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, "task_alerts")
                .setSmallIcon(R.drawable.ic_nav_routine)
                .setContentTitle("🔁 Routine — " + title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(notifId, builder.build());
    }
}