package com.example.peluqueriaapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;

public class AdaptadorCitas extends ArrayAdapter<Cita> {
    private DecimalFormat decimalFormat;

    public AdaptadorCitas(Context context, List<Cita> citas) {
        super(context, R.layout.listview_citas, citas);
        // Inicializar el formato decimal con dos decimales y símbolo de euro
        decimalFormat = new DecimalFormat("#,##0.00€");
        decimalFormat.setDecimalSeparatorAlwaysShown(true);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        Cita cita = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.listview_citas, parent, false);
        }

        TextView lblServicio = convertView.findViewById(R.id.lbl_servicio);
        TextView lblPrecio = convertView.findViewById(R.id.lbl_precio);
        TextView lblFecha = convertView.findViewById(R.id.lbl_fecha);
        TextView lblHora = convertView.findViewById(R.id.lbl_hora);
        TextView lblAnotaciones = convertView.findViewById(R.id.lbl_anotaciones);

        lblServicio.setText(cita.getServicio());
        lblPrecio.setText(decimalFormat.format(cita.getPrecio()));
        lblFecha.setText(cita.getFecha());
        lblHora.setText(cita.getHora());
        lblAnotaciones.setText(cita.getAnotaciones());

        return convertView;
    }
}
