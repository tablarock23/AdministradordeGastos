package com.expensemanager.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private TextView totalBalanceText;
    private ListView expensesListView;
    private Button addBalanceBtn, addExpenseBtn, addLoanBtn, settingsBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        createNotificationChannel();

        initializeViews();
        setupClickListeners();
        updateUI();

        // Verificar pagos vencidos al abrir la app
        checkOverduePayments();
    }

    private void initializeViews() {
        totalBalanceText = findViewById(R.id.totalBalanceText);
        expensesListView = findViewById(R.id.expensesListView);
        addBalanceBtn = findViewById(R.id.addBalanceBtn);
        addExpenseBtn = findViewById(R.id.addExpenseBtn);
        addLoanBtn = findViewById(R.id.addLoanBtn);
        settingsBtn = findViewById(R.id.settingsBtn);
    }

    private void setupClickListeners() {
        addBalanceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AddBalanceSourceActivity.class));
            }
        });

        addExpenseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AddExpenseActivity.class));
            }
        });

        addLoanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AddLoanActivity.class));
            }
        });

        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });
    }

    private void updateUI() {
        updateTotalBalance();
        updateExpensesList();
    }

    private void updateTotalBalance() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(" + DatabaseHelper.COLUMN_BS_BALANCE + ") as total FROM " +
                DatabaseHelper.TABLE_BALANCE_SOURCES, null);

        double totalBalance = 0;
        if (cursor.moveToFirst()) {
            totalBalance = cursor.getDouble(cursor.getColumnIndex("total"));
        }
        cursor.close();

        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("es", "PE"));
        totalBalanceText.setText("Saldo Total: " + formatter.format(totalBalance));
    }

    private void updateExpensesList() {
        final List<String> expenses = new ArrayList<>();
        final List<PaymentItem> paymentItems = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        long currentTime = System.currentTimeMillis();

        // Gastos recurrentes próximos a vencer
        Cursor recurringCursor = db.rawQuery(
                "SELECT " + DatabaseHelper.COLUMN_RE_ID + ", " +
                        DatabaseHelper.COLUMN_RE_NAME + ", " +
                        DatabaseHelper.COLUMN_RE_AMOUNT + ", " +
                        DatabaseHelper.COLUMN_RE_DUE_DATE + " FROM " +
                        DatabaseHelper.TABLE_RECURRING_EXPENSES +
                        " WHERE " + DatabaseHelper.COLUMN_RE_IS_ACTIVE + " = 1 " +
                        " ORDER BY " + DatabaseHelper.COLUMN_RE_DUE_DATE + " ASC LIMIT 10", null);

        while (recurringCursor.moveToNext()) {
            int id = recurringCursor.getInt(0);
            String name = recurringCursor.getString(1);
            double amount = recurringCursor.getDouble(2);
            long dueDate = recurringCursor.getLong(3);

            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("es", "PE"));
            String timeInfo = getTimeUntilDue(dueDate, currentTime);

            expenses.add("📺 " + name + " - " + formatter.format(amount) + "\n   " + timeInfo);
            paymentItems.add(new PaymentItem(id, "expense", name, amount, dueDate));
        }
        recurringCursor.close();

        // Cuotas de préstamos próximas
        Cursor loansCursor = db.rawQuery(
                "SELECT li." + DatabaseHelper.COLUMN_LI_ID + ", l." +
                        DatabaseHelper.COLUMN_LOAN_NAME + ", li." +
                        DatabaseHelper.COLUMN_LI_AMOUNT + ", li." +
                        DatabaseHelper.COLUMN_LI_DUE_DATE + ", li." +
                        DatabaseHelper.COLUMN_LI_INSTALLMENT_NUMBER + ", li." +
                        DatabaseHelper.COLUMN_LI_IS_PAID + " FROM " +
                        DatabaseHelper.TABLE_LOAN_INSTALLMENTS + " li JOIN " +
                        DatabaseHelper.TABLE_LOANS + " l ON li." +
                        DatabaseHelper.COLUMN_LI_LOAN_ID + " = l." +
                        DatabaseHelper.COLUMN_LOAN_ID +
                        " WHERE li." + DatabaseHelper.COLUMN_LI_IS_PAID + " = 0 " +
                        " ORDER BY li." + DatabaseHelper.COLUMN_LI_DUE_DATE + " ASC LIMIT 10", null);

        while (loansCursor.moveToNext()) {
            int id = loansCursor.getInt(0);
            String name = loansCursor.getString(1);
            double amount = loansCursor.getDouble(2);
            long dueDate = loansCursor.getLong(3);
            int installmentNumber = loansCursor.getInt(4);

            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("es", "PE"));
            String timeInfo = getTimeUntilDue(dueDate, currentTime);

            expenses.add("💳 " + name + " (Cuota " + installmentNumber + ") - " +
                    formatter.format(amount) + "\n   " + timeInfo);
            paymentItems.add(new PaymentItem(id, "loan", name + " - Cuota " + installmentNumber, amount, dueDate));
        }
        loansCursor.close();

        if (expenses.isEmpty()) {
            expenses.add("No hay gastos programados");
        }

        ExpenseAdapter adapter = new ExpenseAdapter(this, expenses);
        expensesListView.setAdapter(adapter);

        // Configurar clic largo para editar/eliminar
        expensesListView.setOnItemLongClickListener(new android.widget.AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position < paymentItems.size()) {
                    showEditDeleteDialog(paymentItems.get(position));
                }
                return true;
            }
        });
    }

    private void showEditDeleteDialog(final PaymentItem item) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(item.name);
        builder.setMessage("¿Qué deseas hacer?");

        builder.setPositiveButton("Marcar como Pagado", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                markAsPaid(item);
            }
        });

        builder.setNegativeButton("Eliminar", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                confirmDelete(item);
            }
        });

        builder.setNeutralButton("Cancelar", null);
        builder.show();
    }

    private void markAsPaid(PaymentItem item) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        android.content.ContentValues values = new android.content.ContentValues();

        if (item.type.equals("loan")) {
            values.put(DatabaseHelper.COLUMN_LI_IS_PAID, 1);
            values.put(DatabaseHelper.COLUMN_LI_PAID_DATE, System.currentTimeMillis());
            db.update(DatabaseHelper.TABLE_LOAN_INSTALLMENTS, values,
                    DatabaseHelper.COLUMN_LI_ID + " = ?", new String[]{String.valueOf(item.id)});

            NotificationScheduler.cancelAllNotifications(this, item.id);
        } else {
            values.put(DatabaseHelper.COLUMN_RE_IS_ACTIVE, 0);
            db.update(DatabaseHelper.TABLE_RECURRING_EXPENSES, values,
                    DatabaseHelper.COLUMN_RE_ID + " = ?", new String[]{String.valueOf(item.id)});

            NotificationScheduler.cancelAllNotifications(this, item.id);
        }

        db.close();
        Toast.makeText(this, "Marcado como pagado", Toast.LENGTH_SHORT).show();
        updateUI();
    }

    private void confirmDelete(final PaymentItem item) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Confirmar eliminación");
        builder.setMessage("¿Estás seguro de eliminar este pago?\n\n" + item.name);

        builder.setPositiveButton("Eliminar", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                deletePayment(item);
            }
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void deletePayment(PaymentItem item) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        if (item.type.equals("loan")) {
            db.delete(DatabaseHelper.TABLE_LOAN_INSTALLMENTS,
                    DatabaseHelper.COLUMN_LI_ID + " = ?", new String[]{String.valueOf(item.id)});
            NotificationScheduler.cancelAllNotifications(this, item.id);
        } else {
            db.delete(DatabaseHelper.TABLE_RECURRING_EXPENSES,
                    DatabaseHelper.COLUMN_RE_ID + " = ?", new String[]{String.valueOf(item.id)});
            NotificationScheduler.cancelAllNotifications(this, item.id);
        }

        db.close();
        Toast.makeText(this, "Eliminado exitosamente", Toast.LENGTH_SHORT).show();
        updateUI();
    }

    // Clase auxiliar para guardar info de los items
    private static class PaymentItem {
        int id;
        String type;
        String name;
        double amount;
        long dueDate;

        PaymentItem(int id, String type, String name, double amount, long dueDate) {
            this.id = id;
            this.type = type;
            this.name = name;
            this.amount = amount;
            this.dueDate = dueDate;
        }
    }

    private String getTimeUntilDue(long dueDate, long currentTime) {
        long diffMillis = dueDate - currentTime;

        if (diffMillis < 0) {
            // Ya venció
            long overdueDays = Math.abs(diffMillis) / (24 * 60 * 60 * 1000);
            if (overdueDays == 0) {
                return "🔴 VENCIDO HOY - FALTA PAGAR";
            } else if (overdueDays == 1) {
                return "🔴 VENCIDO hace 1 día - FALTA PAGAR";
            } else {
                return "🔴 VENCIDO hace " + overdueDays + " días - FALTA PAGAR";
            }
        }

        long days = diffMillis / (24 * 60 * 60 * 1000);
        long hours = (diffMillis % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
        long minutes = (diffMillis % (60 * 60 * 1000)) / (60 * 1000);

        if (days > 0) {
            java.text.DateFormat dateFormat = java.text.DateFormat.getDateInstance();
            return "Vence en " + days + " día" + (days > 1 ? "s" : "") + " (" +
                    dateFormat.format(new java.util.Date(dueDate)) + ")";
        } else if (hours > 0) {
            return "⚠️ Vence en " + hours + " hora" + (hours > 1 ? "s" : "");
        } else {
            return "🔴 Vence en " + minutes + " minuto" + (minutes > 1 ? "s" : "");
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Recordatorios de Gastos";
            String description = "Notificaciones para recordar pagos pendientes";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("EXPENSE_REMINDERS", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
        checkOverduePayments();
    }

    private void checkOverduePayments() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        long currentTime = System.currentTimeMillis();
        int overdueCount = 0;

        // Contar cuotas vencidas
        Cursor loansCursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_LOAN_INSTALLMENTS +
                        " WHERE " + DatabaseHelper.COLUMN_LI_IS_PAID + " = 0 " +
                        " AND " + DatabaseHelper.COLUMN_LI_DUE_DATE + " < ?",
                new String[]{String.valueOf(currentTime)});
        if (loansCursor.moveToFirst()) {
            overdueCount += loansCursor.getInt(0);
        }
        loansCursor.close();

        // Contar suscripciones vencidas
        Cursor subscriptionsCursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_RECURRING_EXPENSES +
                        " WHERE " + DatabaseHelper.COLUMN_RE_IS_ACTIVE + " = 1 " +
                        " AND " + DatabaseHelper.COLUMN_RE_DUE_DATE + " < ?",
                new String[]{String.valueOf(currentTime)});
        if (subscriptionsCursor.moveToFirst()) {
            overdueCount += subscriptionsCursor.getInt(0);
        }
        subscriptionsCursor.close();
        db.close();

        // Mostrar alerta si hay pagos vencidos
        if (overdueCount > 0) {
            String message = "Tienes " + overdueCount + " pago" + (overdueCount > 1 ? "s" : "") +
                    " vencido" + (overdueCount > 1 ? "s" : "") + " sin pagar";
            Toast.makeText(this, "⚠️ " + message, Toast.LENGTH_LONG).show();
        }
    }
}