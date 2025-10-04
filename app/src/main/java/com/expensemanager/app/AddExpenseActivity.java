package com.expensemanager.app;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
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
    private EditText amountEditText, dueDateEditText, reminderDaysEditText, notificationIntervalEditText;
    private Spinner typeSpinner;
    private TextView reminderLabel, dueDateLabel, intervalLabel;
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
        notificationIntervalEditText = findViewById(R.id.notificationIntervalEditText);
        typeSpinner = findViewById(R.id.typeSpinner);
        reminderLabel = findViewById(R.id.reminderLabel);
        dueDateLabel = findViewById(R.id.dueDateLabel);
        intervalLabel = findViewById(R.id.intervalLabel);
        saveBtn = findViewById(R.id.saveBtn);
        cancelBtn = findViewById(R.id.cancelBtn);

        reminderDaysEditText.setText("3"); // 3 minutos para pruebas
        notificationIntervalEditText.setText("1"); // cada 1 minuto para pruebas
    }

    private void setupSpinner() {
        String[] expenseTypes = {"SUSCRIPCIÓN", "OTROS"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, expenseTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);

        typeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedType = parent.getItemAtPosition(position).toString();
                if (selectedType.equals("OTROS")) {
                    dueDateEditText.setVisibility(View.GONE);
                    dueDateLabel.setVisibility(View.GONE);
                    reminderDaysEditText.setVisibility(View.GONE);
                    reminderLabel.setVisibility(View.GONE);
                    notificationIntervalEditText.setVisibility(View.GONE);
                    intervalLabel.setVisibility(View.GONE);
                } else {
                    dueDateEditText.setVisibility(View.VISIBLE);
                    dueDateLabel.setVisibility(View.VISIBLE);
                    reminderDaysEditText.setVisibility(View.VISIBLE);
                    reminderLabel.setVisibility(View.VISIBLE);
                    notificationIntervalEditText.setVisibility(View.VISIBLE);
                    intervalLabel.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    private void setupAutoComplete() {
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
        nameEditText.setThreshold(1);
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

        if (type.equals("SUSCRIPCIÓN")) {
            String dueDateStr = dueDateEditText.getText().toString().trim();
            String reminderDaysStr = reminderDaysEditText.getText().toString().trim();
            String intervalStr = notificationIntervalEditText.getText().toString().trim();

            if (dueDateStr.isEmpty()) {
                Toast.makeText(this, "Por favor selecciona la fecha de vencimiento", Toast.LENGTH_SHORT).show();
                return;
            }

            int reminderDays;
            int intervalHours;
            try {
                reminderDays = Integer.parseInt(reminderDaysStr);
                intervalHours = Integer.parseInt(intervalStr);
            } catch (NumberFormatException e) {
                reminderDays = 3;
                intervalHours = 2;
            }

            values.put(DatabaseHelper.COLUMN_RE_DUE_DATE, selectedDate.getTimeInMillis());
            values.put(DatabaseHelper.COLUMN_RE_REMINDER_DAYS, reminderDays);
            values.put(DatabaseHelper.COLUMN_RE_NOTIFICATION_INTERVAL_HOURS, intervalHours);
            values.put(DatabaseHelper.COLUMN_RE_IS_ACTIVE, 1);

            long result = db.insert(DatabaseHelper.TABLE_RECURRING_EXPENSES, null, values);

            if (result != -1) {
                NotificationScheduler.scheduleMultipleNotifications(this, (int)result, name, amount,
                        selectedDate.getTimeInMillis(), reminderDays, "expense", intervalHours);

                Toast.makeText(this, "Suscripción agregada con recordatorio", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Error al agregar la suscripción", Toast.LENGTH_SHORT).show();
            }
        } else {
            values.put(DatabaseHelper.COLUMN_RE_DUE_DATE, System.currentTimeMillis());
            values.put(DatabaseHelper.COLUMN_RE_REMINDER_DAYS, 0);
            values.put(DatabaseHelper.COLUMN_RE_NOTIFICATION_INTERVAL_HOURS, 2);
            values.put(DatabaseHelper.COLUMN_RE_IS_ACTIVE, 0);

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