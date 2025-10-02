package com.expensemanager.app;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ExpenseManager.db";
    private static final int DATABASE_VERSION = 1;

    // Tabla de fuentes de saldo
    public static final String TABLE_BALANCE_SOURCES = "balance_sources";
    public static final String COLUMN_BS_ID = "id";
    public static final String COLUMN_BS_NAME = "name";
    public static final String COLUMN_BS_TYPE = "type"; // YAPE, CUENTA_BANCARIA, TARJETA_CREDITO, EFECTIVO
    public static final String COLUMN_BS_BALANCE = "balance";

    // Tabla de gastos recurrentes (streaming, etc)
    public static final String TABLE_RECURRING_EXPENSES = "recurring_expenses";
    public static final String COLUMN_RE_ID = "id";
    public static final String COLUMN_RE_NAME = "name";
    public static final String COLUMN_RE_AMOUNT = "amount";
    public static final String COLUMN_RE_TYPE = "type"; // STREAMING, OTROS
    public static final String COLUMN_RE_DUE_DATE = "due_date";
    public static final String COLUMN_RE_REMINDER_DAYS = "reminder_days";
    public static final String COLUMN_RE_IS_ACTIVE = "is_active";

    // Tabla de préstamos
    public static final String TABLE_LOANS = "loans";
    public static final String COLUMN_LOAN_ID = "id";
    public static final String COLUMN_LOAN_NAME = "name";
    public static final String COLUMN_LOAN_TOTAL_AMOUNT = "total_amount";
    public static final String COLUMN_LOAN_INSTALLMENTS = "installments";
    public static final String COLUMN_LOAN_INSTALLMENT_AMOUNT = "installment_amount";
    public static final String COLUMN_LOAN_START_DATE = "start_date";

    // Tabla de cuotas de préstamos
    public static final String TABLE_LOAN_INSTALLMENTS = "loan_installments";
    public static final String COLUMN_LI_ID = "id";
    public static final String COLUMN_LI_LOAN_ID = "loan_id";
    public static final String COLUMN_LI_INSTALLMENT_NUMBER = "installment_number";
    public static final String COLUMN_LI_AMOUNT = "amount";
    public static final String COLUMN_LI_DUE_DATE = "due_date";
    public static final String COLUMN_LI_IS_PAID = "is_paid";
    public static final String COLUMN_LI_REMINDER_DAYS = "reminder_days";

    // Tabla de gastos diversos
    public static final String TABLE_OTHER_EXPENSES = "other_expenses";
    public static final String COLUMN_OE_ID = "id";
    public static final String COLUMN_OE_NAME = "name";
    public static final String COLUMN_OE_AMOUNT = "amount";
    public static final String COLUMN_OE_DATE = "date";
    public static final String COLUMN_OE_CATEGORY = "category";
    public static final String COLUMN_OE_SOURCE_ID = "source_id";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Crear tabla de fuentes de saldo
        String createBalanceSourcesTable = "CREATE TABLE " + TABLE_BALANCE_SOURCES + "("
                + COLUMN_BS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_BS_NAME + " TEXT NOT NULL,"
                + COLUMN_BS_TYPE + " TEXT NOT NULL,"
                + COLUMN_BS_BALANCE + " REAL DEFAULT 0"
                + ")";
        db.execSQL(createBalanceSourcesTable);

        // Crear tabla de gastos recurrentes
        String createRecurringExpensesTable = "CREATE TABLE " + TABLE_RECURRING_EXPENSES + "("
                + COLUMN_RE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_RE_NAME + " TEXT NOT NULL,"
                + COLUMN_RE_AMOUNT + " REAL NOT NULL,"
                + COLUMN_RE_TYPE + " TEXT NOT NULL,"
                + COLUMN_RE_DUE_DATE + " INTEGER NOT NULL,"
                + COLUMN_RE_REMINDER_DAYS + " INTEGER DEFAULT 3,"
                + COLUMN_RE_IS_ACTIVE + " INTEGER DEFAULT 1"
                + ")";
        db.execSQL(createRecurringExpensesTable);

        // Crear tabla de préstamos
        String createLoansTable = "CREATE TABLE " + TABLE_LOANS + "("
                + COLUMN_LOAN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_LOAN_NAME + " TEXT NOT NULL,"
                + COLUMN_LOAN_TOTAL_AMOUNT + " REAL NOT NULL,"
                + COLUMN_LOAN_INSTALLMENTS + " INTEGER NOT NULL,"
                + COLUMN_LOAN_INSTALLMENT_AMOUNT + " REAL NOT NULL,"
                + COLUMN_LOAN_START_DATE + " INTEGER NOT NULL"
                + ")";
        db.execSQL(createLoansTable);

        // Crear tabla de cuotas de préstamos
        String createLoanInstallmentsTable = "CREATE TABLE " + TABLE_LOAN_INSTALLMENTS + "("
                + COLUMN_LI_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_LI_LOAN_ID + " INTEGER NOT NULL,"
                + COLUMN_LI_INSTALLMENT_NUMBER + " INTEGER NOT NULL,"
                + COLUMN_LI_AMOUNT + " REAL NOT NULL,"
                + COLUMN_LI_DUE_DATE + " INTEGER NOT NULL,"
                + COLUMN_LI_IS_PAID + " INTEGER DEFAULT 0,"
                + COLUMN_LI_REMINDER_DAYS + " INTEGER DEFAULT 3,"
                + "FOREIGN KEY(" + COLUMN_LI_LOAN_ID + ") REFERENCES " + TABLE_LOANS + "(" + COLUMN_LOAN_ID + ")"
                + ")";
        db.execSQL(createLoanInstallmentsTable);

        // Crear tabla de gastos diversos
        String createOtherExpensesTable = "CREATE TABLE " + TABLE_OTHER_EXPENSES + "("
                + COLUMN_OE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_OE_NAME + " TEXT NOT NULL,"
                + COLUMN_OE_AMOUNT + " REAL NOT NULL,"
                + COLUMN_OE_DATE + " INTEGER NOT NULL,"
                + COLUMN_OE_CATEGORY + " TEXT,"
                + COLUMN_OE_SOURCE_ID + " INTEGER,"
                + "FOREIGN KEY(" + COLUMN_OE_SOURCE_ID + ") REFERENCES " + TABLE_BALANCE_SOURCES + "(" + COLUMN_BS_ID + ")"
                + ")";
        db.execSQL(createOtherExpensesTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BALANCE_SOURCES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECURRING_EXPENSES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOANS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOAN_INSTALLMENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_OTHER_EXPENSES);
        onCreate(db);
    }
}