package com.expensemanager.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.List;

public class ExpenseAdapter extends ArrayAdapter<String> {

    private Context context;
    private List<String> expenses;

    public ExpenseAdapter(Context context, List<String> expenses) {
        super(context, R.layout.expense_list_item, expenses);
        this.context = context;
        this.expenses = expenses;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.expense_list_item, parent, false);
        }

        TextView expenseTextView = convertView.findViewById(R.id.expenseTextView);
        expenseTextView.setText(expenses.get(position));

        return convertView;
    }
}