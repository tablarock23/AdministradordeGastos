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
        List<String> expenses = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Gastos recurrentes próximos a vencer
        Cursor recurringCursor = db.rawQuery(
                "SELECT " + DatabaseHelper.COLUMN_RE_NAME + ", " +
                        DatabaseHelper.COLUMN_RE_AMOUNT + ", " +
                        DatabaseHelper.COLUMN_RE_DUE_DATE + " FROM " +
                        DatabaseHelper.TABLE_RECURRING_EXPENSES +
                        " WHERE " + DatabaseHelper.COLUMN_RE_IS_ACTIVE + " = 1 " +
                        " ORDER BY " + DatabaseHelper.COLUMN_RE_DUE_DATE + " ASC LIMIT 5", null);

        while (recurringCursor.moveToNext()) {
            String name = recurringCursor.getString(0);
            double amount = recurringCursor.getDouble(1);
            long dueDate = recurringCursor.getLong(2);

            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("es", "PE"));
            expenses.add("📺 " + name + " - " + formatter.format(amount) +
                    " (Vence: " + java.text.DateFormat.getDateInstance().format(new java.util.Date(dueDate)) + ")");
        }
        recurringCursor.close();

        // Cuotas de préstamos próximas
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
                        " ORDER BY li." + DatabaseHelper.COLUMN_LI_DUE_DATE + " ASC LIMIT 5", null);

        while (loansCursor.moveToNext()) {
            String name = loansCursor.getString(0);
            double amount = loansCursor.getDouble(1);
            long dueDate = loansCursor.getLong(2);
            int installmentNumber = loansCursor.getInt(3);

            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("es", "PE"));
            expenses.add("💳 " + name + " (Cuota " + installmentNumber + ") - " +
                    formatter.format(amount) + " (Vence: " +
                    java.text.DateFormat.getDateInstance().format(new java.util.Date(dueDate)) + ")");
        }
        loansCursor.close();

        // Si no hay gastos, mostrar mensaje
        if (expenses.isEmpty()) {
            expenses.add("No hay gastos programados");
        }

        ExpenseAdapter adapter = new ExpenseAdapter(this, expenses);
        expensesListView.setAdapter(adapter);
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
    }
}