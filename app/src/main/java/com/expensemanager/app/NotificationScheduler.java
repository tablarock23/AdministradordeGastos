package com.expensemanager.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class NotificationScheduler {

    public static void scheduleNotification(Context context, int notificationId, String title,
                                            double amount, long dueDate, int reminderDays, String type) {

        // Calcular la fecha de recordatorio
        long reminderTime = dueDate - (reminderDays * 24 * 60 * 60 * 1000L);

        // Solo programar si la fecha de recordatorio es en el futuro
        if (reminderTime > System.currentTimeMillis()) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            Intent intent = new Intent(context, NotificationReceiver.class);
            intent.putExtra("notification_id", notificationId);
            intent.putExtra("title", title);
            intent.putExtra("amount", amount);
            intent.putExtra("type", type);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    notificationId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            if (alarmManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
                }
            }
        }
    }

    public static void cancelNotification(Context context, int notificationId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }
}