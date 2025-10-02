package com.expensemanager.app;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import java.text.NumberFormat;
import java.util.Locale;

public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int notificationId = intent.getIntExtra("notification_id", 0);
        String title = intent.getStringExtra("title");
        double amount = intent.getDoubleExtra("amount", 0);
        String type = intent.getStringExtra("type");

        showNotification(context, notificationId, title, amount, type);
    }

    private void showNotification(Context context, int notificationId, String title, double amount, String type) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("es", "PE"));
        String contentText = "Próximo pago: " + formatter.format(amount);

        String emoji = type.equals("loan") ? "💳" : "📺";

        Intent activityIntent = new Intent(context, MainActivity.class);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "EXPENSE_REMINDERS")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(emoji + " Recordatorio: " + title)
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 500, 200, 500})
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText));

        if (notificationManager != null) {
            notificationManager.notify(notificationId, builder.build());
        }
    }
}