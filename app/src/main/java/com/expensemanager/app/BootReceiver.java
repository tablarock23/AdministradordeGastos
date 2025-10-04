package com.expensemanager.app;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.core.app.NotificationCompat;
import java.text.NumberFormat;
import java.util.Locale;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
                "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction())) {

            // Reprogramar todas las notificaciones activas
            rescheduleAllNotifications(context);

            // Verificar pagos vencidos y notificar
            checkOverduePayments(context);
        }
    }

    private void rescheduleAllNotifications(Context context) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        long currentTime = System.currentTimeMillis();

        // Reprogramar cuotas pendientes de préstamos
        Cursor loansCursor = db.rawQuery(
                "SELECT li." + DatabaseHelper.COLUMN_LI_ID + ", l." +
                        DatabaseHelper.COLUMN_LOAN_NAME + ", li." +
                        DatabaseHelper.COLUMN_LI_AMOUNT + ", li." +
                        DatabaseHelper.COLUMN_LI_DUE_DATE + ", li." +
                        DatabaseHelper.COLUMN_LI_INSTALLMENT_NUMBER + ", li." +
                        DatabaseHelper.COLUMN_LI_REMINDER_DAYS + ", li." +
                        DatabaseHelper.COLUMN_LI_NOTIFICATION_INTERVAL_HOURS + " FROM " +
                        DatabaseHelper.TABLE_LOAN_INSTALLMENTS + " li JOIN " +
                        DatabaseHelper.TABLE_LOANS + " l ON li." +
                        DatabaseHelper.COLUMN_LI_LOAN_ID + " = l." +
                        DatabaseHelper.COLUMN_LOAN_ID +
                        " WHERE li." + DatabaseHelper.COLUMN_LI_IS_PAID + " = 0 " +
                        " AND li." + DatabaseHelper.COLUMN_LI_DUE_DATE + " >= ?",
                new String[]{String.valueOf(currentTime)});

        while (loansCursor.moveToNext()) {
            int id = loansCursor.getInt(0);
            String name = loansCursor.getString(1);
            double amount = loansCursor.getDouble(2);
            long dueDate = loansCursor.getLong(3);
            int installmentNumber = loansCursor.getInt(4);
            int reminderDays = loansCursor.getInt(5);
            int intervalHours = loansCursor.getInt(6);

            String title = name + " - Cuota " + installmentNumber;
            NotificationScheduler.scheduleMultipleNotifications(context, id, title, amount,
                    dueDate, reminderDays, "loan", intervalHours);
        }
        loansCursor.close();

        // Reprogramar suscripciones activas
        Cursor subscriptionsCursor = db.rawQuery(
                "SELECT " + DatabaseHelper.COLUMN_RE_ID + ", " +
                        DatabaseHelper.COLUMN_RE_NAME + ", " +
                        DatabaseHelper.COLUMN_RE_AMOUNT + ", " +
                        DatabaseHelper.COLUMN_RE_DUE_DATE + ", " +
                        DatabaseHelper.COLUMN_RE_REMINDER_DAYS + ", " +
                        DatabaseHelper.COLUMN_RE_NOTIFICATION_INTERVAL_HOURS + " FROM " +
                        DatabaseHelper.TABLE_RECURRING_EXPENSES +
                        " WHERE " + DatabaseHelper.COLUMN_RE_IS_ACTIVE + " = 1 " +
                        " AND " + DatabaseHelper.COLUMN_RE_DUE_DATE + " >= ?",
                new String[]{String.valueOf(currentTime)});

        while (subscriptionsCursor.moveToNext()) {
            int id = subscriptionsCursor.getInt(0);
            String name = subscriptionsCursor.getString(1);
            double amount = subscriptionsCursor.getDouble(2);
            long dueDate = subscriptionsCursor.getLong(3);
            int reminderDays = subscriptionsCursor.getInt(4);
            int intervalHours = subscriptionsCursor.getInt(5);

            NotificationScheduler.scheduleMultipleNotifications(context, id, name, amount,
                    dueDate, reminderDays, "expense", intervalHours);
        }
        subscriptionsCursor.close();
        db.close();

        Log.d("BootReceiver", "Notificaciones reprogramadas al encender dispositivo");
    }

    private void checkOverduePayments(Context context) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        long currentTime = System.currentTimeMillis();
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("es", "PE"));

        int overdueCount = 0;
        StringBuilder overdueList = new StringBuilder();

        // Verificar cuotas de préstamos vencidas
        Cursor loansCursor = db.rawQuery(
                "SELECT l." + DatabaseHelper.COLUMN_LOAN_NAME + ", li." +
                        DatabaseHelper.COLUMN_LI_AMOUNT + ", li." +
                        DatabaseHelper.COLUMN_LI_DUE_DATE + ", li." +
                        DatabaseHelper.COLUMN_LI_INSTALLMENT_NUMBER + " FROM " +
                        DatabaseHelper.TABLE_LOAN_INSTALLMENTS + " li JOIN " +
                        DatabaseHelper.TABLE_LOANS + " l ON li." +
                        DatabaseHelper.COLUMN_LI_LOAN_ID + " = l." +
                        DatabaseHelper.COLUMN_LOAN_ID +
                        " WHERE li." + DatabaseHelper.COLUMN_LI_IS_PAID + " = 0 " +
                        " AND li." + DatabaseHelper.COLUMN_LI_DUE_DATE + " < ?",
                new String[]{String.valueOf(currentTime)});

        while (loansCursor.moveToNext()) {
            String name = loansCursor.getString(0);
            double amount = loansCursor.getDouble(1);
            long dueDate = loansCursor.getLong(2);
            int installmentNumber = loansCursor.getInt(3);

            long daysOverdue = (currentTime - dueDate) / (24 * 60 * 60 * 1000);
            overdueCount++;
            overdueList.append("\n💳 ").append(name).append(" (Cuota ").append(installmentNumber)
                    .append(") - ").append(formatter.format(amount))
                    .append(" - Vencida hace ").append(daysOverdue).append(" día").append(daysOverdue > 1 ? "s" : "");
        }
        loansCursor.close();

        // Verificar suscripciones vencidas
        Cursor subscriptionsCursor = db.rawQuery(
                "SELECT " + DatabaseHelper.COLUMN_RE_NAME + ", " +
                        DatabaseHelper.COLUMN_RE_AMOUNT + ", " +
                        DatabaseHelper.COLUMN_RE_DUE_DATE + " FROM " +
                        DatabaseHelper.TABLE_RECURRING_EXPENSES +
                        " WHERE " + DatabaseHelper.COLUMN_RE_IS_ACTIVE + " = 1 " +
                        " AND " + DatabaseHelper.COLUMN_RE_DUE_DATE + " < ?",
                new String[]{String.valueOf(currentTime)});

        while (subscriptionsCursor.moveToNext()) {
            String name = subscriptionsCursor.getString(0);
            double amount = subscriptionsCursor.getDouble(1);
            long dueDate = subscriptionsCursor.getLong(2);

            long daysOverdue = (currentTime - dueDate) / (24 * 60 * 60 * 1000);
            overdueCount++;
            overdueList.append("\n📺 ").append(name).append(" - ").append(formatter.format(amount))
                    .append(" - Vencida hace ").append(daysOverdue).append(" día").append(daysOverdue > 1 ? "s" : "");
        }
        subscriptionsCursor.close();
        db.close();

        // Si hay pagos vencidos, mostrar notificación
        if (overdueCount > 0) {
            showOverdueNotification(context, overdueCount, overdueList.toString());
        }
    }

    private void showOverdueNotification(Context context, int count, String details) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = "⚠️ ATENCIÓN: " + count + " pago" + (count > 1 ? "s" : "") + " vencido" + (count > 1 ? "s" : "");
        String message = "Tienes pagos vencidos que requieren tu atención:" + details;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "EXPENSE_REMINDERS")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText("Toca para ver detalles")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 1000, 500, 1000})
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setCategory(NotificationCompat.CATEGORY_ALARM);

        if (notificationManager != null) {
            notificationManager.notify(99998, builder.build());
        }
    }
}