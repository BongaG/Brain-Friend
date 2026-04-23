package com.brainfriend.app.reminders;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;



public class ReminderScheduler {


    public static void schedule(Context context, long reminderId,
                                 String title, long triggerTimeMs) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = buildPendingIntent(context, reminderId, title);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, triggerTimeMs, pi);
        }
    }



    public static void cancel(Context context, long reminderId) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = buildPendingIntent(context, reminderId, "");
        am.cancel(pi);
    }

    private static PendingIntent buildPendingIntent(Context context,
                                                     long reminderId, String title) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId);
        intent.putExtra(ReminderReceiver.EXTRA_REMINDER_TITLE, title);
        return PendingIntent.getBroadcast(
                context,
                (int) reminderId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}
