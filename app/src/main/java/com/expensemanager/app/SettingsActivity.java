package com.expensemanager.app;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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

public class SettingsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private ListView balanceSourcesListView;
    private TextView totalExpensesText, totalLoansText;
    private Button backBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        dbHelper = new DatabaseHelper(this);

        initializeViews();
        setupClickListeners();
        updateUI();
    }

    private void initializeViews() {
        balanceSourcesListView = findViewById(R.id.balanceSourcesListView);
        totalExpensesText = findViewById(R.id.totalExpensesText);
        totalLoansText = findViewById(R.id.totalLoansText);
        backBtn = findViewById(R.id.backBtn);
    }

    private void setupClickListeners() {
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void updateUI() {
        updateBalanceSources();
        updateTotalExpenses();
        updateTotalLoans();
    }

    private void updateBalanceSources() {
        List<String> sources = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(DatabaseHelper.TABLE_BALANCE_SOURCES, null, null, null, null, null, null);

        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("es", "PE"));

        while (cursor.moveToNext()) {
            String name = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_BS_NAME));
            String type = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_BS_TYPE));
            double balance = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_BS_BALANCE));

            sources.add(name + " (" + type + ")\n" + formatter.format(balance));
        }
        cursor.close();

        if (sources.isEmpty()) {
            sources.add("No hay fuentes de saldo registradas");
        }

        ExpenseAdapter adapter = new ExpenseAdapter(this, sources);
        balanceSourcesListView.setAdapter(adapter);
    }

    private void updateTotalExpenses() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT SUM(" + DatabaseHelper.COLUMN_RE_AMOUNT + ") as total FROM " +
                        DatabaseHelper.TABLE_RECURRING_EXPENSES +
                        " WHERE " + DatabaseHelper.COLUMN_RE_IS_ACTIVE + " = 1", null);

        double totalExpenses = 0;
        if (cursor.moveToFirst()) {
            totalExpenses = cursor.getDouble(cursor.getColumnIndex("total"));
        }
        cursor.close();

        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("es", "PE"));
        totalExpensesText.setText("Gastos Recurrentes Mensuales: " + formatter.format(totalExpenses));
    }

    private void updateTotalLoans() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT SUM(" + DatabaseHelper.COLUMN_LI_AMOUNT + ") as total FROM " +
                        DatabaseHelper.TABLE_LOAN_INSTALLMENTS +
                        " WHERE " + DatabaseHelper.COLUMN_LI_IS_PAID + " = 0", null);

        double totalLoans = 0;
        if (cursor.moveToFirst()) {
            totalLoans = cursor.getDouble(cursor.getColumnIndex("total"));
        }
        cursor.close();

        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("es", "PE"));
        totalLoansText.setText("Total Pendiente en Préstamos: " + formatter.format(totalLoans));
    }
}