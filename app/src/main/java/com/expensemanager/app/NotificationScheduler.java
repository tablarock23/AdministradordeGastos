package com.expensemanager.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import java.util.Calendar;

public class NotificationScheduler {

    private static final String TAG = "NotificationScheduler";
    // CAMBIAR A 1 MINUTO PARA PRUEBAS (normalmente sería 4 * 60 * 60 * 1000L)
    private static final long ONE_HOUR_IN_MILLIS = 60 * 1000L; // 1 MINUTO para pruebas

    /**
     * Programa notificaciones múltiples con intervalo personalizable
     */
    public static void scheduleMultipleNotifications(Context context, int notificationId, String title,
                                                     double amount, long dueDate, int reminderDays,
                                                     String type, int intervalHours) {

        Calendar dueDateCalendar = Calendar.getInstance();
        dueDateCalendar.setTimeInMillis(dueDate);

        Calendar reminderStartDate = (Calendar) dueDateCalendar.clone();
        // CAMBIO PARA PRUEBAS: usar MINUTOS en vez de DÍAS
        reminderStartDate.add(Calendar.MINUTE, -reminderDays); // Ahora son MINUTOS
        reminderStartDate.set(Calendar.SECOND, 0);

        long currentTime = System.currentTimeMillis();
        int notificationCount = 0;

        // Para pruebas: cada "hora" es en realidad 1 minuto
        int notificationsPerPeriod = reminderDays; // Total de notificaciones en el período

        for (int notification = 0; notification < notificationsPerPeriod; notification++) {
            Calendar notificationTime = (Calendar) reminderStartDate.clone();
            // CAMBIO: intervalHours ahora representa MINUTOS
            notificationTime.add(Calendar.MINUTE, notification * intervalHours);

            long triggerTime = notificationTime.getTimeInMillis();

            if (triggerTime > currentTime) {
                int uniqueId = notificationId * 1000 + notification;

                scheduleSingleNotification(context, uniqueId, title, amount, triggerTime, type,
                        notification, notificationsPerPeriod, notificationId);
                notificationCount++;

                Log.d(TAG, String.format("✅ Notificación #%d programada para: %s (en %d minutos)",
                        notificationCount, notificationTime.getTime().toString(),
                        (triggerTime - currentTime) / 60000));
            }
        }

        Calendar sameDayNotification = (Calendar) dueDateCalendar.clone();
        sameDayNotification.set(Calendar.SECOND, 0);

        if (sameDayNotification.getTimeInMillis() > currentTime) {
            int sameDayId = notificationId * 1000 + 999;
            scheduleSingleNotification(context, sameDayId, "¡HOY VENCE! " + title, amount,
                    sameDayNotification.getTimeInMillis(), type, 0, notificationsPerPeriod, notificationId);
            notificationCount++;
        }

        Log.d(TAG, String.format("🔔 MODO PRUEBA: %d notificaciones programadas (cada %d minuto) para: %s",
                notificationCount, intervalHours, title));
    }

    /**
     * Programa una notificación única (método privado auxiliar)
     */
    private static void scheduleSingleNotification(Context context, int notificationId, String title,
                                                   double amount, long triggerTime, String type,
                                                   int daysRemaining, int totalReminderDays, int itemId) {

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("notification_id", notificationId);
        intent.putExtra("title", title);
        intent.putExtra("amount", amount);
        intent.putExtra("type", type);
        intent.putExtra("days_remaining", totalReminderDays - daysRemaining);
        intent.putExtra("item_id", itemId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
        }
    }

    /**
     * Cancela todas las notificaciones relacionadas con un ID base
     */
    public static void cancelAllNotifications(Context context, int baseNotificationId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        for (int i = 0; i < 19; i++) {
            int notificationId = baseNotificationId * 1000 + i;

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

        int sameDayId = baseNotificationId * 1000 + 999;
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                sameDayId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }

        Log.d(TAG, "Todas las notificaciones canceladas para ID base: " + baseNotificationId);
    }

    /**
     * Método SIMPLE para pruebas y suscripciones - programa UNA notificación en el tiempo exacto
     */
    public static void scheduleNotification(Context context, int notificationId, String title,
                                            double amount, long triggerTime, String type,
                                            int daysRemaining, int totalReminderDays) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("notification_id", notificationId);
        intent.putExtra("title", title);
        intent.putExtra("amount", amount);
        intent.putExtra("type", type);
        intent.putExtra("days_remaining", daysRemaining);
        intent.putExtra("item_id", notificationId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
            Log.d(TAG, "✅ Notificación programada para: " + new java.util.Date(triggerTime) + " (en " +
                    (triggerTime - System.currentTimeMillis())/1000 + " segundos)");
        }
    }
}