/*
Proyecto: Lucía García BeautyBoard
--------------------------------------------------------------------
Autor: David Maestre Díaz
--------------------------------------------------------------------
Versión: 1.0
--------------------------------------------------------------------
Descripción: La clase ViewPager2Adapter es un adaptador personalizado diseñado para ser utilizado con ViewPager2 en Android.
Se utiliza para mostrar un carrusel de imágenes en un ViewPager2.

Funcionalidades:
- Crea una vista para cada imagen en la lista.
- Establece la imagen en la vista correspondiente.
- Configura la escala de la imagen para llenar el espacio y recortar los bordes.
*/

package com.example.peluqueriaapp;

import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ViewPager2Adapter extends RecyclerView.Adapter<ViewPager2Adapter.ViewHolder> {

    private List<Integer> imagenes;

    public ViewPager2Adapter(List<Integer> imagenes) {
        this.imagenes = imagenes;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Crear una vista para cada imagen
        ImageView imageView = new ImageView(parent.getContext());
        imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return new ViewHolder(imageView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // Establecer la imagen en la vista
        holder.imageView.setImageResource(imagenes.get(position));
        // Establecer la escala de la imagen para llenar el espacio y recortar los bordes
        holder.imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
    }

    @Override
    public int getItemCount() {
        return imagenes.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private ImageView imageView;

        public ViewHolder(ImageView imageView) {
            super(imageView);
            this.imageView = imageView;
        }
    }
}