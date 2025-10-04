package com.expensemanager.app;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddLoanActivity extends AppCompatActivity {

    private EditText loanNameEditText, capitalAmountEditText, totalAmountEditText,
            installmentsEditText, startDateEditText, startTimeEditText, customDaysEditText,
            reminderDaysEditText, notificationIntervalEditText;
    private Spinner paymentFrequencySpinner;
    private LinearLayout customDaysLayout;
    private TextView interestInfoTextView, installmentAmountTextView;
    private Button saveBtn, cancelBtn;
    private DatabaseHelper dbHelper;
    private Calendar selectedDate;
    private NumberFormat formatter;
    private String selectedFrequency = "MENSUAL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_loan);

        dbHelper = new DatabaseHelper(this);
        selectedDate = Calendar.getInstance();
        formatter = NumberFormat.getCurrencyInstance(new Locale("es", "PE"));

        initializeViews();
        setupSpinner();
        setupClickListeners();
        setupCalculationListeners();
    }

    private void initializeViews() {
        loanNameEditText = findViewById(R.id.loanNameEditText);
        capitalAmountEditText = findViewById(R.id.capitalAmountEditText);
        totalAmountEditText = findViewById(R.id.totalAmountEditText);
        installmentsEditText = findViewById(R.id.installmentsEditText);
        startDateEditText = findViewById(R.id.startDateEditText);
        startTimeEditText = findViewById(R.id.startTimeEditText);
        reminderDaysEditText = findViewById(R.id.reminderDaysEditText);
        notificationIntervalEditText = findViewById(R.id.notificationIntervalEditText);
        paymentFrequencySpinner = findViewById(R.id.paymentFrequencySpinner);
        customDaysEditText = findViewById(R.id.customDaysEditText);
        customDaysLayout = findViewById(R.id.customDaysLayout);
        interestInfoTextView = findViewById(R.id.interestInfoTextView);
        installmentAmountTextView = findViewById(R.id.installmentAmountTextView);
        saveBtn = findViewById(R.id.saveBtn);
        cancelBtn = findViewById(R.id.cancelBtn);

        // Valores por defecto
        reminderDaysEditText.setText("3"); // 3 días
        notificationIntervalEditText.setText("2"); // cada 2 horas

        // Establecer hora actual por defecto
        Calendar now = Calendar.getInstance();
        startTimeEditText.setText(String.format("%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE)));
    }

    private void setupSpinner() {
        String[] frequencies = {"SEMANAL (cada 7 días)", "QUINCENAL (cada 15 días)",
                "MENSUAL (cada 30 días)", "PERSONALIZADO"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, frequencies);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        paymentFrequencySpinner.setAdapter(adapter);
        paymentFrequencySpinner.setSelection(2); // MENSUAL por defecto

        paymentFrequencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 3) { // PERSONALIZADO
                    selectedFrequency = "PERSONALIZADO";
                    customDaysLayout.setVisibility(View.VISIBLE);
                } else {
                    customDaysLayout.setVisibility(View.GONE);
                    if (position == 0) selectedFrequency = "SEMANAL";
                    else if (position == 1) selectedFrequency = "QUINCENAL";
                    else selectedFrequency = "MENSUAL";
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupCalculationListeners() {
        TextWatcher calculationWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateLoanAmounts();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        capitalAmountEditText.addTextChangedListener(calculationWatcher);
        totalAmountEditText.addTextChangedListener(calculationWatcher);
        installmentsEditText.addTextChangedListener(calculationWatcher);
    }

    private void calculateLoanAmounts() {
        String capitalStr = capitalAmountEditText.getText().toString().trim();
        String totalStr = totalAmountEditText.getText().toString().trim();
        String installmentsStr = installmentsEditText.getText().toString().trim();

        if (capitalStr.isEmpty() || totalStr.isEmpty() || installmentsStr.isEmpty()) {
            interestInfoTextView.setText("Interés: --");
            installmentAmountTextView.setText("Cuota: --");
            return;
        }

        try {
            double capital = Double.parseDouble(capitalStr);
            double total = Double.parseDouble(totalStr);
            int installments = Integer.parseInt(installmentsStr);

            if (installments <= 0) {
                interestInfoTextView.setText("Interés: --");
                installmentAmountTextView.setText("Cuota: --");
                return;
            }

            if (total < capital) {
                interestInfoTextView.setText("❌ El total debe ser mayor al capital");
                installmentAmountTextView.setText("Cuota: --");
                return;
            }

            // Calcular interés automáticamente
            double interestAmount = total - capital;
            double interestRate = ((total - capital) / capital) * 100;
            double installmentAmount = total / installments;

            interestInfoTextView.setText(String.format("💰 Interés: %s (%.2f%%)",
                    formatter.format(interestAmount), interestRate));
            installmentAmountTextView.setText("📊 Cuota: " + formatter.format(installmentAmount));
        } catch (NumberFormatException e) {
            interestInfoTextView.setText("Interés: --");
            installmentAmountTextView.setText("Cuota: --");
        }
    }

    private void setupClickListeners() {
        startDateEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });

        startTimeEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePicker();
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveLoan();
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

                        startDateEditText.setText(dayOfMonth + "/" + (month + 1) + "/" + year);
                    }
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void showTimePicker() {
        android.app.TimePickerDialog timePickerDialog = new android.app.TimePickerDialog(
                this,
                new android.app.TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(android.widget.TimePicker view, int hourOfDay, int minute) {
                        selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        selectedDate.set(Calendar.MINUTE, minute);
                        selectedDate.set(Calendar.SECOND, 0);

                        startTimeEditText.setText(String.format("%02d:%02d", hourOfDay, minute));
                    }
                },
                selectedDate.get(Calendar.HOUR_OF_DAY),
                selectedDate.get(Calendar.MINUTE),
                true // formato 24 horas
        );
        timePickerDialog.show();
    }

    private int getPaymentIntervalDays() {
        switch (selectedFrequency) {
            case "SEMANAL":
                return 7;
            case "QUINCENAL":
                return 15;
            case "MENSUAL":
                return 30;
            case "PERSONALIZADO":
                String customDays = customDaysEditText.getText().toString().trim();
                if (!customDays.isEmpty()) {
                    try {
                        return Integer.parseInt(customDays);
                    } catch (NumberFormatException e) {
                        return 30; // Default
                    }
                }
                return 30;
            default:
                return 30;
        }
    }

    private void saveLoan() {
        String loanName = loanNameEditText.getText().toString().trim();
        String capitalStr = capitalAmountEditText.getText().toString().trim();
        String totalStr = totalAmountEditText.getText().toString().trim();
        String installmentsStr = installmentsEditText.getText().toString().trim();
        String startDateStr = startDateEditText.getText().toString().trim();
        String startTimeStr = startTimeEditText.getText().toString().trim();
        String reminderDaysStr = reminderDaysEditText.getText().toString().trim();

        if (loanName.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa el nombre del préstamo", Toast.LENGTH_SHORT).show();
            return;
        }

        if (capitalStr.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa el monto prestado", Toast.LENGTH_SHORT).show();
            return;
        }

        if (totalStr.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa el total a pagar", Toast.LENGTH_SHORT).show();
            return;
        }

        if (installmentsStr.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa el número de cuotas", Toast.LENGTH_SHORT).show();
            return;
        }

        if (startDateStr.isEmpty()) {
            Toast.makeText(this, "Por favor selecciona la fecha de inicio", Toast.LENGTH_SHORT).show();
            return;
        }

        if (startTimeStr.isEmpty()) {
            Toast.makeText(this, "Por favor selecciona la hora de vencimiento", Toast.LENGTH_SHORT).show();
            return;
        }

        double capital;
        double totalAmount;
        int installments;
        int reminderDays;
        int notificationIntervalHours;
        try {
            capital = Double.parseDouble(capitalStr);
            totalAmount = Double.parseDouble(totalStr);
            installments = Integer.parseInt(installmentsStr);
            reminderDays = Integer.parseInt(reminderDaysStr);
            notificationIntervalHours = Integer.parseInt(notificationIntervalEditText.getText().toString().trim());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Por favor ingresa valores válidos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (totalAmount < capital) {
            Toast.makeText(this, "El total a pagar debe ser mayor o igual al capital", Toast.LENGTH_SHORT).show();
            return;
        }

        if (installments <= 0) {
            Toast.makeText(this, "El número de cuotas debe ser mayor a 0", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedFrequency.equals("PERSONALIZADO")) {
            String customDays = customDaysEditText.getText().toString().trim();
            if (customDays.isEmpty()) {
                Toast.makeText(this, "Por favor ingresa los días personalizados", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Calcular interés automáticamente
        double interestRate = ((totalAmount - capital) / capital) * 100;
        double installmentAmount = totalAmount / installments;
        int paymentIntervalDays = getPaymentIntervalDays();

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Insertar préstamo
        ContentValues loanValues = new ContentValues();
        loanValues.put(DatabaseHelper.COLUMN_LOAN_NAME, loanName);
        loanValues.put(DatabaseHelper.COLUMN_LOAN_CAPITAL, capital);
        loanValues.put(DatabaseHelper.COLUMN_LOAN_INTEREST_RATE, interestRate);
        loanValues.put(DatabaseHelper.COLUMN_LOAN_TOTAL_AMOUNT, totalAmount);
        loanValues.put(DatabaseHelper.COLUMN_LOAN_INSTALLMENTS, installments);
        loanValues.put(DatabaseHelper.COLUMN_LOAN_INSTALLMENT_AMOUNT, installmentAmount);
        loanValues.put(DatabaseHelper.COLUMN_LOAN_START_DATE, selectedDate.getTimeInMillis());
        loanValues.put(DatabaseHelper.COLUMN_LOAN_PAYMENT_FREQUENCY, selectedFrequency);
        loanValues.put(DatabaseHelper.COLUMN_LOAN_PAYMENT_INTERVAL_DAYS, paymentIntervalDays);

        long loanId = db.insert(DatabaseHelper.TABLE_LOANS, null, loanValues);

        if (loanId != -1) {
            // Crear cuotas individuales
            Calendar installmentDate = (Calendar) selectedDate.clone();

            for (int i = 1; i <= installments; i++) {
                ContentValues installmentValues = new ContentValues();
                installmentValues.put(DatabaseHelper.COLUMN_LI_LOAN_ID, loanId);
                installmentValues.put(DatabaseHelper.COLUMN_LI_INSTALLMENT_NUMBER, i);
                installmentValues.put(DatabaseHelper.COLUMN_LI_AMOUNT, installmentAmount);
                installmentValues.put(DatabaseHelper.COLUMN_LI_DUE_DATE, installmentDate.getTimeInMillis());
                installmentValues.put(DatabaseHelper.COLUMN_LI_IS_PAID, 0);
                installmentValues.put(DatabaseHelper.COLUMN_LI_REMINDER_DAYS, reminderDays);
                installmentValues.put(DatabaseHelper.COLUMN_LI_NOTIFICATION_INTERVAL_HOURS, notificationIntervalHours);

                long installmentId = db.insert(DatabaseHelper.TABLE_LOAN_INSTALLMENTS, null, installmentValues);

                // Programar notificaciones múltiples para cada cuota
                if (installmentId != -1) {
                    String notificationTitle = loanName + " - Cuota " + i + "/" + installments;
                    NotificationScheduler.scheduleMultipleNotifications(this, (int)installmentId,
                            notificationTitle, installmentAmount, installmentDate.getTimeInMillis(),
                            reminderDays, "loan", notificationIntervalHours);

                    // Mostrar información de cuándo llegará la primera notificación
                    if (i == 1) {
                        long firstNotificationTime = installmentDate.getTimeInMillis() - (reminderDays * 60 * 1000L);
                        long minutesUntilFirst = (firstNotificationTime - System.currentTimeMillis()) / 60000;

                        String message = String.format("✅ Primera notificación en %d minutos\n⏰ Vencimiento: %s",
                                minutesUntilFirst,
                                startDateStr + " " + startTimeStr);
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    }
                }

                // Avanzar según la frecuencia de pago
                installmentDate.add(Calendar.DAY_OF_YEAR, paymentIntervalDays);
            }

            String frequencyText = selectedFrequency.equals("PERSONALIZADO")
                    ? "cada " + paymentIntervalDays + " días"
                    : selectedFrequency.toLowerCase();

            finish();
        } else {
            Toast.makeText(this, "Error al agregar el préstamo", Toast.LENGTH_SHORT).show();
        }
    }
}