package com.example.peluqueriaapp;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String mensaje = intent.getStringExtra("mensaje");
        String canal = intent.getStringExtra("canal");

        mostrarNotificacion(context, mensaje, canal);
    }

    @SuppressLint("MissingPermission")
    private void mostrarNotificacion(Context context, String mensaje, String canal) {
        Intent intent = new Intent(context, LoginActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        // Construir la notificación
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, canal)
                .setSmallIcon(R.drawable.baseline_notifications_24)
                .setContentTitle("Recordatorio de cita")
                .setContentText(mensaje)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Obtener el NotificationManager
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // Mostrar la notificación
        notificationManager.notify(1, builder.build());
    }
}