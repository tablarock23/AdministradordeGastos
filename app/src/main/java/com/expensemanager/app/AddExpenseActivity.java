package com.expensemanager.app;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Calendar;

public class AddExpenseActivity extends AppCompatActivity {

    private AutoCompleteTextView nameEditText;
    private EditText amountEditText, dueDateEditText, reminderDaysEditText;
    private Spinner typeSpinner;
    private TextView reminderLabel, dueDateLabel;
    private Button saveBtn, cancelBtn;
    private DatabaseHelper dbHelper;
    private Calendar selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        dbHelper = new DatabaseHelper(this);
        selectedDate = Calendar.getInstance();

        initializeViews();
        setupSpinner();
        setupAutoComplete();
        setupClickListeners();
    }

    private void initializeViews() {
        nameEditText = findViewById(R.id.nameEditText);
        amountEditText = findViewById(R.id.amountEditText);
        dueDateEditText = findViewById(R.id.dueDateEditText);
        reminderDaysEditText = findViewById(R.id.reminderDaysEditText);
        typeSpinner = findViewById(R.id.typeSpinner);
        reminderLabel = findViewById(R.id.reminderLabel);
        dueDateLabel = findViewById(R.id.dueDateLabel);
        saveBtn = findViewById(R.id.saveBtn);
        cancelBtn = findViewById(R.id.cancelBtn);

        // Valor por defecto para recordatorio
        reminderDaysEditText.setText("3");
    }

    private void setupSpinner() {
        String[] expenseTypes = {"SUSCRIPCIÓN", "OTROS"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, expenseTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);

        // Listener para mostrar/ocultar campos según el tipo
        typeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedType = parent.getItemAtPosition(position).toString();
                if (selectedType.equals("OTROS")) {
                    // Ocultar fecha de vencimiento y recordatorio para gastos únicos
                    dueDateEditText.setVisibility(View.GONE);
                    dueDateLabel.setVisibility(View.GONE);
                    reminderDaysEditText.setVisibility(View.GONE);
                    reminderLabel.setVisibility(View.GONE);
                } else {
                    // Mostrar para suscripciones
                    dueDateEditText.setVisibility(View.VISIBLE);
                    dueDateLabel.setVisibility(View.VISIBLE);
                    reminderDaysEditText.setVisibility(View.VISIBLE);
                    reminderLabel.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    private void setupAutoComplete() {
        // Sugerencias para nombres de servicios
        String[] suggestions = {
                "Netflix", "Disney+", "Disney Plus", "Amazon Prime Video", "HBO Max",
                "Paramount+", "Paramount Plus", "Apple TV+", "Spotify", "Apple Music",
                "YouTube Premium", "YouTube Music", "Deezer", "Tidal",
                "Amazon Prime", "Mercado Libre", "iCloud", "Google One",
                "Dropbox", "Microsoft 365", "Adobe Creative Cloud",
                "ChatGPT Plus", "ChatGPT", "GitHub Copilot", "Canva Pro",
                "PlayStation Plus", "Xbox Game Pass", "Nintendo Switch Online",
                "Crunchyroll", "Salida", "Fiesta", "Restaurante", "Compras"
        };

        ArrayAdapter<String> autoCompleteAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                suggestions
        );
        nameEditText.setAdapter(autoCompleteAdapter);
        nameEditText.setThreshold(1); // Muestra sugerencias después de 1 letra
    }

    private void setupClickListeners() {
        dueDateEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveExpense();
            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        selectedDate.set(Calendar.YEAR, year);
                        selectedDate.set(Calendar.MONTH, month);
                        selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                        dueDateEditText.setText(dayOfMonth + "/" + (month + 1) + "/" + year);
                    }
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void saveExpense() {
        String name = nameEditText.getText().toString().trim();
        String amountStr = amountEditText.getText().toString().trim();
        String type = typeSpinner.getSelectedItem().toString();

        if (name.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa el nombre del gasto", Toast.LENGTH_SHORT).show();
            return;
        }

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa el monto", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Por favor ingresa un monto válido", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_RE_NAME, name);
        values.put(DatabaseHelper.COLUMN_RE_AMOUNT, amount);
        values.put(DatabaseHelper.COLUMN_RE_TYPE, type);

        // Si es SUSCRIPCIÓN, guardar fecha y programar notificación
        if (type.equals("SUSCRIPCIÓN")) {
            String dueDateStr = dueDateEditText.getText().toString().trim();
            String reminderDaysStr = reminderDaysEditText.getText().toString().trim();

            if (dueDateStr.isEmpty()) {
                Toast.makeText(this, "Por favor selecciona la fecha de vencimiento", Toast.LENGTH_SHORT).show();
                return;
            }

            int reminderDays;
            try {
                reminderDays = Integer.parseInt(reminderDaysStr);
            } catch (NumberFormatException e) {
                reminderDays = 3; // Valor por defecto
            }

            values.put(DatabaseHelper.COLUMN_RE_DUE_DATE, selectedDate.getTimeInMillis());
            values.put(DatabaseHelper.COLUMN_RE_REMINDER_DAYS, reminderDays);
            values.put(DatabaseHelper.COLUMN_RE_IS_ACTIVE, 1);

            long result = db.insert(DatabaseHelper.TABLE_RECURRING_EXPENSES, null, values);

            if (result != -1) {
                // Programar notificación solo para suscripciones
                NotificationScheduler.scheduleNotification(this, (int)result, name, amount,
                        selectedDate.getTimeInMillis(), reminderDays, "expense");

                Toast.makeText(this, "Suscripción agregada con recordatorio", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Error al agregar la suscripción", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Si es OTROS (gasto único), guardar sin fecha ni notificación
            values.put(DatabaseHelper.COLUMN_RE_DUE_DATE, System.currentTimeMillis());
            values.put(DatabaseHelper.COLUMN_RE_REMINDER_DAYS, 0);
            values.put(DatabaseHelper.COLUMN_RE_IS_ACTIVE, 0); // No activo = no se muestra en próximos pagos

            long result = db.insert(DatabaseHelper.TABLE_RECURRING_EXPENSES, null, values);

            if (result != -1) {
                Toast.makeText(this, "Gasto único agregado (sin recordatorio)", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Error al agregar el gasto", Toast.LENGTH_SHORT).show();
            }
        }
    }
}