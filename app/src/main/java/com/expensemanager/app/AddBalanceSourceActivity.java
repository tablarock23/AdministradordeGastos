package com.expensemanager.app;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class AddBalanceSourceActivity extends AppCompatActivity {

    private EditText nameEditText, balanceEditText;
    private Spinner typeSpinner;
    private Button saveBtn, cancelBtn;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_balance_source);

        dbHelper = new DatabaseHelper(this);
        initializeViews();
        setupSpinner();
        setupClickListeners();
    }

    private void initializeViews() {
        nameEditText = findViewById(R.id.nameEditText);
        balanceEditText = findViewById(R.id.balanceEditText);
        typeSpinner = findViewById(R.id.typeSpinner);
        saveBtn = findViewById(R.id.saveBtn);
        cancelBtn = findViewById(R.id.cancelBtn);
    }

    private void setupSpinner() {
        String[] balanceTypes = {"YAPE", "CUENTA BANCARIA", "TARJETA DE CRÉDITO", "EFECTIVO"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, balanceTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);
    }

    private void setupClickListeners() {
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveBalanceSource();
            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void saveBalanceSource() {
        String name = nameEditText.getText().toString().trim();
        String balanceStr = balanceEditText.getText().toString().trim();
        String type = typeSpinner.getSelectedItem().toString();

        if (name.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa un nombre", Toast.LENGTH_SHORT).show();
            return;
        }

        if (balanceStr.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa un saldo inicial", Toast.LENGTH_SHORT).show();
            return;
        }

        double balance;
        try {
            balance = Double.parseDouble(balanceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Por favor ingresa un saldo válido", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_BS_NAME, name);
        values.put(DatabaseHelper.COLUMN_BS_TYPE, type);
        values.put(DatabaseHelper.COLUMN_BS_BALANCE, balance);

        long result = db.insert(DatabaseHelper.TABLE_BALANCE_SOURCES, null, values);

        if (result != -1) {
            Toast.makeText(this, "Fuente de saldo agregada exitosamente", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Error al agregar la fuente de saldo", Toast.LENGTH_SHORT).show();
        }
    }
}