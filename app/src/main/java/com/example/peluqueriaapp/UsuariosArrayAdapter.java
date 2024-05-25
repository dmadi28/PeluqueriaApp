/*
Proyecto: Lucía García BeautyBoard
--------------------------------------------------------------------
Autor: David Maestre Díaz
--------------------------------------------------------------------
Versión: 1.0
--------------------------------------------------------------------
Descripción: La clase UsuariosArrayAdapter es una extensión de ArrayAdapter diseñada específicamente para adaptar la visualización de los elementos en un Spinner que contiene una lista de usuarios.

Funcionalidades:
- Personaliza la apariencia de los elementos en el Spinner.
- Muestra el nombre del usuario en el elemento principal del Spinner en caso de que lo tenga definido y su correo electrónico en los elementos desplegables.
*/

package com.example.peluqueriaapp;

import java.util.List;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class UsuariosArrayAdapter extends ArrayAdapter<Usuario> {

    public UsuariosArrayAdapter(@NonNull Context context, int resource, @NonNull List<Usuario> usuarios) {
        super(context, resource, usuarios);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_spinner_item, parent, false);
        }

        TextView textView = convertView.findViewById(android.R.id.text1);
        Usuario usuario = getItem(position);
        if (usuario != null) {
            String nombre = usuario.getNombre();
            if (nombre == null || nombre.isEmpty()) {
                nombre = usuario.getEmail();
            }
            textView.setText(nombre);
        }

        return convertView;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
        }

        TextView textView = convertView.findViewById(android.R.id.text1);
        Usuario usuario = getItem(position);
        if (usuario != null) {
            textView.setText(usuario.getEmail());
        }

        return convertView;
    }
}
