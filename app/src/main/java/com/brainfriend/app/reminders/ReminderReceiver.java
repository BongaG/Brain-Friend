package com.brainfriend.app.reminders;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.brainfriend.app.R;



public class ReminderReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "brain_friend_reminders";
    public static final String EXTRA_REMINDER_ID = "reminder_id";
    public static final String EXTRA_REMINDER_TITLE = "reminder_title";
    public static final String ACTION_DONE = "com.brainfriend.ACTION_DONE";
    public static final String ACTION_SNOOZE = "com.brainfriend.ACTION_SNOOZE";

    private static final int SNOOZE_MINUTES = 10;

    @Override
    public void onReceive(Context context, Intent intent) {
        long reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1);
        String title = intent.getStringExtra(EXTRA_REMINDER_TITLE);

        if (ACTION_DONE.equals(intent.getAction())) {
            markReminderDone(context, reminderId);
            cancelNotification(context, (int) reminderId);
            return;
        }

        if (ACTION_SNOOZE.equals(intent.getAction())) {
            snoozeReminder(context, reminderId, title);
            cancelNotification(context, (int) reminderId);
            return;
        }

        createNotificationChannel(context);
        showNotification(context, reminderId, title);
    }


    private void showNotification(Context context, long reminderId, String title) {
        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent doneIntent = new Intent(context, ReminderReceiver.class);
        doneIntent.setAction(ACTION_DONE);
        doneIntent.putExtra(EXTRA_REMINDER_ID, reminderId);
        PendingIntent donePi = PendingIntent.getBroadcast(
                context, (int) reminderId * 10,
                doneIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);


        Intent snoozeIntent = new Intent(context, ReminderReceiver.class);
        snoozeIntent.setAction(ACTION_SNOOZE);
        snoozeIntent.putExtra(EXTRA_REMINDER_ID, reminderId);
        snoozeIntent.putExtra(EXTRA_REMINDER_TITLE, title);
        PendingIntent snoozePi = PendingIntent.getBroadcast(
                context, (int) reminderId * 10 + 1,
                snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("🧠 Brain Friend Reminder")
                .setContentText(title)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(title))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .addAction(0, "✅ Done", donePi)
                .addAction(0, "⏰ Snooze 10 min", snoozePi);

        nm.notify((int) reminderId, builder.build());
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Brain Friend reminder alerts");
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            nm.createNotificationChannel(channel);
        }
    }

    private void cancelNotification(Context context, int notifId) {
        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(notifId);
    }



    private void markReminderDone(Context context, long reminderId) {
        new Thread(() -> {
            AppDatabase.getInstance(context).reminderDao().markDone(reminderId);
        }).start();
    }

    private void snoozeReminder(Context context, long reminderId, String title) {
        long newTime = System.currentTimeMillis() + (SNOOZE_MINUTES * 60 * 1000L);
        new Thread(() -> {
            ReminderEntity r = AppDatabase.getInstance(context).reminderDao().getById(reminderId);
            if (r != null) {
                r.triggerTimeMs = newTime;
                AppDatabase.getInstance(context).reminderDao().update(r);
            }
        }).start();
        ReminderScheduler.schedule(context, reminderId, title, newTime);
    }
}
