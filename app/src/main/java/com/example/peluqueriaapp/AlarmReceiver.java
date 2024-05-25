/*
Proyecto: Lucía García BeautyBoard
--------------------------------------------------------------------
Autor: David Maestre Díaz
--------------------------------------------------------------------
Versión: 1.0
--------------------------------------------------------------------
Descripción: La clase AlarmReceiver es un receptor de difusión que recibe las señales de alarma programadas para recordatorios de citas. Cuando se activa, muestra una notificación al usuario con el mensaje de recordatorio.

Funcionalidades:
- Recibe la señal de alarma con el mensaje de recordatorio y el canal de notificación.
- Muestra una notificación al usuario con el mensaje recibido.
- Al hacer clic en la notificación, redirige al usuario a la pantalla de inicio de sesión de la aplicación.
*/

package com.example.peluqueriaapp;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String mensaje = intent.getStringExtra("mensaje");
        String canal = intent.getStringExtra("canal");

        mostrarNotificacion(context.getApplicationContext(), mensaje, canal);
    }

    @SuppressLint("MissingPermission")
    private void mostrarNotificacion(Context context, String mensaje, String canal) {
        Intent intent = new Intent(context, LoginActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        // Construir la notificación
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, canal)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(context.getString(R.string.recordatorio_de_cita))
                .setContentText(mensaje)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Obtener el NotificationManager
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // Mostrar la notificación
        notificationManager.notify(1, builder.build());
    }
}
