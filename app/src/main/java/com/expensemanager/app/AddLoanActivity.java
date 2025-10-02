package com.expensemanager.app;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Calendar;

public class AddLoanActivity extends AppCompatActivity {

    private EditText loanNameEditText, totalAmountEditText, installmentsEditText,
            startDateEditText, reminderDaysEditText;
    private Button saveBtn, cancelBtn;
    private DatabaseHelper dbHelper;
    private Calendar selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_loan);

        dbHelper = new DatabaseHelper(this);
        selectedDate = Calendar.getInstance();

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        loanNameEditText = findViewById(R.id.loanNameEditText);
        totalAmountEditText = findViewById(R.id.totalAmountEditText);
        installmentsEditText = findViewById(R.id.installmentsEditText);
        startDateEditText = findViewById(R.id.startDateEditText);
        reminderDaysEditText = findViewById(R.id.reminderDaysEditText);
        saveBtn = findViewById(R.id.saveBtn);
        cancelBtn = findViewById(R.id.cancelBtn);

        // Valor por defecto para recordatorio
        reminderDaysEditText.setText("3");
    }

    private void setupClickListeners() {
        startDateEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
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

    private void saveLoan() {
        String loanName = loanNameEditText.getText().toString().trim();
        String totalAmountStr = totalAmountEditText.getText().toString().trim();
        String installmentsStr = installmentsEditText.getText().toString().trim();
        String startDateStr = startDateEditText.getText().toString().trim();
        String reminderDaysStr = reminderDaysEditText.getText().toString().trim();

        if (loanName.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa el nombre del préstamo", Toast.LENGTH_SHORT).show();
            return;
        }

        if (totalAmountStr.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa el monto total", Toast.LENGTH_SHORT).show();
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

        double totalAmount;
        int installments;
        int reminderDays;
        try {
            totalAmount = Double.parseDouble(totalAmountStr);
            installments = Integer.parseInt(installmentsStr);
            reminderDays = Integer.parseInt(reminderDaysStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Por favor ingresa valores válidos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (installments <= 0) {
            Toast.makeText(this, "El número de cuotas debe ser mayor a 0", Toast.LENGTH_SHORT).show();
            return;
        }

        double installmentAmount = totalAmount / installments;

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Insertar préstamo
        ContentValues loanValues = new ContentValues();
        loanValues.put(DatabaseHelper.COLUMN_LOAN_NAME, loanName);
        loanValues.put(DatabaseHelper.COLUMN_LOAN_TOTAL_AMOUNT, totalAmount);
        loanValues.put(DatabaseHelper.COLUMN_LOAN_INSTALLMENTS, installments);
        loanValues.put(DatabaseHelper.COLUMN_LOAN_INSTALLMENT_AMOUNT, installmentAmount);
        loanValues.put(DatabaseHelper.COLUMN_LOAN_START_DATE, selectedDate.getTimeInMillis());

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

                long installmentId = db.insert(DatabaseHelper.TABLE_LOAN_INSTALLMENTS, null, installmentValues);

                // Programar notificación para cada cuota
                if (installmentId != -1) {
                    String notificationTitle = loanName + " - Cuota " + i;
                    NotificationScheduler.scheduleNotification(this, (int)installmentId,
                            notificationTitle, installmentAmount, installmentDate.getTimeInMillis(),
                            reminderDays, "loan");
                }

                // Avanzar al siguiente mes
                installmentDate.add(Calendar.MONTH, 1);
            }

            Toast.makeText(this, "Préstamo agregado exitosamente", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Error al agregar el préstamo", Toast.LENGTH_SHORT).show();
        }
    }
}