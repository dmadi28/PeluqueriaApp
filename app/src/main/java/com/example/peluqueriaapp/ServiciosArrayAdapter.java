package com.example.peluqueriaapp;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class ServiciosArrayAdapter extends ArrayAdapter<String> {

    public ServiciosArrayAdapter(@NonNull Context context, int resource, @NonNull List<String> objects) {
        super(context, resource, objects);
    }

    @Override
    public boolean isEnabled(int position) {
        String item = getItem(position);
        return !(item != null && (item.equals("Servicios de Peluquería") || item.equals("Servicios de Estética") || (item.equals("Hairdressing Services") || item.equals("Aesthetic Services"))));
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        TextView textView = (TextView) view;

        textView.setTextSize(20);
        textView.setGravity(Gravity.CENTER);

        if (!isEnabled(position)) {
            textView.setTextColor(Color.BLACK);
            textView.setTypeface(null, Typeface.BOLD);
            textView.setPaintFlags(textView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            textView.setBackgroundColor(Color.parseColor("#E6E6FA"));
            textView.setPadding(0, 20, 0, 20);
        } else {
            textView.setTextColor(Color.BLACK);
            textView.setBackgroundColor(Color.WHITE);
            textView.setPadding(0, 0, 0, 0);
        }
        return view;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getView(position, convertView, parent);
    }
}
