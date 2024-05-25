/*
Proyecto: Lucía García BeautyBoard
--------------------------------------------------------------------
Autor: David Maestre Díaz
--------------------------------------------------------------------
Versión: 1.0
--------------------------------------------------------------------
Descripción: La clase ServiciosArrayAdapter es una extensión de ArrayAdapter diseñada específicamente para adaptar la visualización de los elementos en un Spinner que contiene una lista de servicios.

Funcionalidades:
- Personaliza la apariencia de los elementos en el Spinner.
- Habilita o deshabilita los elementos correspondientes a las categorías "Servicios de peluquería" y "Servicios de estética".
- Establece el tamaño, la alineación, el color y el estilo del texto.
- Resalta las categorías con un fondo diferente y subrayado.
*/

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
        return !(item != null && (item.equals(getContext().getString(R.string.servicios_de_peluqueria)) || item.equals(getContext().getString(R.string.servicios_de_estetica))));
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
