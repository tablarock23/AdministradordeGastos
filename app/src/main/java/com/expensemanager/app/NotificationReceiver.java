package com.expensemanager.app;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.core.app.NotificationCompat;
import java.text.NumberFormat;
import java.util.Locale;

public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action != null && action.equals("MARK_AS_PAID")) {
            // Manejar el botón "YA PAGUÉ"
            handleMarkAsPaid(context, intent);
        } else {
            // Mostrar notificación normal
            int notificationId = intent.getIntExtra("notification_id", 0);
            String title = intent.getStringExtra("title");
            double amount = intent.getDoubleExtra("amount", 0);
            String type = intent.getStringExtra("type");
            int daysRemaining = intent.getIntExtra("days_remaining", 0);
            int itemId = intent.getIntExtra("item_id", 0);

            // Verificar si ya fue pagado antes de mostrar notificación
            if (!isAlreadyPaid(context, itemId, type)) {
                showNotification(context, notificationId, title, amount, type, daysRemaining, itemId);
            }
        }
    }

    private boolean isAlreadyPaid(Context context, int itemId, String type) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String table = type.equals("loan") ? DatabaseHelper.TABLE_LOAN_INSTALLMENTS
                : DatabaseHelper.TABLE_RECURRING_EXPENSES;
        String idColumn = type.equals("loan") ? DatabaseHelper.COLUMN_LI_ID
                : DatabaseHelper.COLUMN_RE_ID;
        String isPaidColumn = type.equals("loan") ? DatabaseHelper.COLUMN_LI_IS_PAID
                : DatabaseHelper.COLUMN_RE_IS_ACTIVE;

        Cursor cursor = db.query(table, new String[]{isPaidColumn},
                idColumn + " = ?", new String[]{String.valueOf(itemId)},
                null, null, null);

        boolean isPaid = false;
        if (cursor.moveToFirst()) {
            int status = cursor.getInt(0);
            isPaid = type.equals("loan") ? (status == 1) : (status == 0);
        }
        cursor.close();
        db.close();

        return isPaid;
    }

    private void handleMarkAsPaid(Context context, Intent intent) {
        int itemId = intent.getIntExtra("item_id", 0);
        String type = intent.getStringExtra("type");
        int baseNotificationId = intent.getIntExtra("base_notification_id", 0);

        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();

        if (type.equals("loan")) {
            // Marcar cuota como pagada
            values.put(DatabaseHelper.COLUMN_LI_IS_PAID, 1);
            values.put(DatabaseHelper.COLUMN_LI_PAID_DATE, System.currentTimeMillis());
            db.update(DatabaseHelper.TABLE_LOAN_INSTALLMENTS, values,
                    DatabaseHelper.COLUMN_LI_ID + " = ?", new String[]{String.valueOf(itemId)});
        } else {
            // Desactivar gasto recurrente (marcarlo como pagado este mes)
            values.put(DatabaseHelper.COLUMN_RE_IS_ACTIVE, 0);
            db.update(DatabaseHelper.TABLE_RECURRING_EXPENSES, values,
                    DatabaseHelper.COLUMN_RE_ID + " = ?", new String[]{String.valueOf(itemId)});
        }

        db.close();

        // Cancelar todas las notificaciones futuras de este item
        NotificationScheduler.cancelAllNotifications(context, baseNotificationId);

        // Cancelar la notificación actual
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(baseNotificationId);
        }

        // Mostrar confirmación
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "EXPENSE_REMINDERS")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("✅ Pago registrado")
                .setContentText("El pago ha sido marcado como completado")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true);

        if (notificationManager != null) {
            notificationManager.notify(baseNotificationId + 10000, builder.build());
        }
    }

    private void showNotification(Context context, int notificationId, String title,
                                  double amount, String type, int daysRemaining, int itemId) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("es", "PE"));

        // Mensaje personalizado según días restantes
        String contentText;
        String priorityPrefix = "";
        int priority;

        if (daysRemaining == 0) {
            contentText = "¡VENCE HOY! Monto: " + formatter.format(amount);
            priorityPrefix = "🔴 URGENTE: ";
            priority = NotificationCompat.PRIORITY_MAX;
        } else if (daysRemaining == 1) {
            contentText = "¡Vence mañana! Monto: " + formatter.format(amount);
            priorityPrefix = "⚠️ ";
            priority = NotificationCompat.PRIORITY_HIGH;
        } else {
            contentText = "Vence en " + daysRemaining + " días. Monto: " + formatter.format(amount);
            priorityPrefix = "📅 ";
            priority = NotificationCompat.PRIORITY_HIGH;
        }

        String emoji = type.equals("loan") ? "💳" : "📺";

        // Intent para abrir la app
        Intent activityIntent = new Intent(context, MainActivity.class);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Intent para el botón "YA PAGUÉ"
        Intent paidIntent = new Intent(context, NotificationReceiver.class);
        paidIntent.setAction("MARK_AS_PAID");
        paidIntent.putExtra("item_id", itemId);
        paidIntent.putExtra("type", type);
        paidIntent.putExtra("base_notification_id", notificationId);

        PendingIntent paidPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 5000, // ID diferente para evitar conflictos
                paidIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "EXPENSE_REMINDERS")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(priorityPrefix + emoji + " " + title)
                .setContentText(contentText)
                .setPriority(priority)
                .setAutoCancel(false) // No cancelar automáticamente
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 500, 200, 500})
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(contentText + "\n\n👆 Toca 'YA PAGUÉ' cuando completes el pago"))
                .addAction(android.R.drawable.ic_menu_save, "YA PAGUÉ", paidPendingIntent);

        // Sonido y LED para notificaciones urgentes
        if (daysRemaining <= 1) {
            builder.setDefaults(NotificationCompat.DEFAULT_ALL);
            builder.setCategory(NotificationCompat.CATEGORY_ALARM);
        }

        if (notificationManager != null) {
            notificationManager.notify(notificationId, builder.build());
        }
    }
}