package com.example.peluqueriaapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ReservarActivity extends AppCompatActivity implements View.OnClickListener {

    DrawerLayout drawerLayout;
    ImageView menu;
    TextView titulo;
    LinearLayout home, citas, info, qr, logout;
    String[] horasDisponibles = {"10:00", "11:00", "12:00", "13:00", "15:00", "16:00", "17:00", "18:00", "19:00"};
    String usuarioActivo = "";
    String servicioSeleccionado = "";

    float precioDeServicio;
    String fechaSeleccionada = "";
    String horaSeleccionada = "";
    Calendar fechaHoraInicio;
    String anotaciones = "";
    Spinner spinnerServicios;
    DatePicker calendario;
    RadioGroup radioGroupHoras;
    EditText etAnotaciones;
    Button buttonSiguiente, buttonReservar;
    ImageButton buttonBack;

    FirebaseManager firebaseManager;
    GoogleSignInClient mGoogleSignInClient;
    final String CHANNEL_ID = "canal";
    long pressedTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reservar);

        firebaseManager = new FirebaseManager();
        mGoogleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN);

        usuarioActivo = firebaseManager.getCurrentUserEmail();

        // Asignar las vistas de los layout a los elementos
        setupViews();
        // Obtener el nombre del usuario y actualizar el título
        firebaseManager.getCurrentUserName(firebaseManager.getCurrentUserEmail(), new FirebaseManager.UserNameCallback() {
            @Override
            public void onUserNameReceived(String userName) {
                // Actualizar título de ventana
                updateTitle(userName);
            }
        });
        // Configurar OnClickListener de los botones
        setupOnClickListeners();
        // Configurar Spinner de servicios
        setupSpinnerServicios();
        // Configurar el calendario de citas
        setupCalendario();
        // Configurar RadioGroup de horas disponibles
        setupRadioGroupHoras();
    }

    private void setupViews() {
        titulo = findViewById(R.id.title);

        drawerLayout = findViewById(R.id.drawerLayout);
        menu = findViewById(R.id.menu);
        home = findViewById(R.id.home);
        citas = findViewById(R.id.citas);
        info = findViewById(R.id.info);
        qr = findViewById(R.id.qr);
        logout = findViewById(R.id.logout);

        spinnerServicios = findViewById(R.id.spinnerServicios);
        calendario = findViewById(R.id.calendarView);
        buttonSiguiente = findViewById(R.id.buttonSiguiente);
        buttonBack = findViewById(R.id.buttonBack);
        buttonReservar = findViewById(R.id.buttonReservar);
        radioGroupHoras = findViewById(R.id.radioGroupHoras);
        etAnotaciones = findViewById(R.id.editTextAnotaciones);
    }

    private void updateTitle(String userName) {
        if (!userName.isEmpty()) {
            titulo.setText("Bienvenido, " + userName);
        } else {
            titulo.setText("Bienvenido, " + firebaseManager.getCurrentUserEmail());
        }
    }

    private void setupOnClickListeners() {
        menu.setOnClickListener(this);
        home.setOnClickListener(this);
        citas.setOnClickListener(this);
        info.setOnClickListener(this);
        qr.setOnClickListener(this);
        logout.setOnClickListener(this);
        buttonSiguiente.setOnClickListener(this);
        buttonBack.setOnClickListener(this);
        buttonReservar.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.menu) {
            openDrawer(drawerLayout);
        } else if (v.getId() == R.id.home) {
            recreate();
        } else if (v.getId() == R.id.citas) {
            redirectActivity(this, ConsultarActivity.class, usuarioActivo);
        } else if (v.getId() == R.id.info) {
            redirectActivity(this, InfoActivity.class, usuarioActivo);
        } else if (v.getId() == R.id.qr) {
            redirectActivity(this, QrActivity.class, usuarioActivo);
        } else if (v.getId() == R.id.logout) {
            firebaseManager.signOut();
            mGoogleSignInClient.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        } else if (v.getId() == R.id.buttonSiguiente) {
            handleButtonSiguiente();
        } else if (v.getId() == R.id.buttonBack) {
            handleButtonBack();
        } else if (v.getId() == R.id.buttonReservar) {
            handleButtonReservar();
        }
    }

    private void handleButtonSiguiente() {
        if (servicioSeleccionado.isEmpty() || precioDeServicio == 0) {
            mostrarToast("Por favor, seleccione un servicio.");
            return;
        }

        int dia = calendario.getDayOfMonth();
        int mes = calendario.getMonth();
        int año = calendario.getYear();

        Calendar fechaElegida = Calendar.getInstance();
        fechaElegida.set(año, mes, dia);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        fechaSeleccionada = dateFormat.format(fechaElegida.getTime());

        firebaseManager.verificarCitasPorFecha(fechaSeleccionada, new FirebaseManager.CitasPorFechaCallback() {
            @Override
            public void onCitasPorFechaVerificadas(List<String> horasReservadas) {
                mostrarHorasDisponibles(horasReservadas, fechaElegida);
            }

            @Override
            public void onError(String errorMessage) {
                Log.e("ReservarActivity", "Error al verificar citas por fecha: " + errorMessage);
            }
        });
    }

    private void handleButtonBack() {
        radioGroupHoras.removeAllViews();
        findViewById(R.id.cvHora).setVisibility(View.GONE);
        findViewById(R.id.textViewServicio).setVisibility(View.VISIBLE);
        findViewById(R.id.spinnerServicios).setVisibility(View.VISIBLE);
        findViewById(R.id.cvCalendario).setVisibility(View.VISIBLE);
    }

    private void handleButtonReservar() {
        if (horaSeleccionada.isEmpty() || fechaSeleccionada.isEmpty()) {
            mostrarToast("Por favor, seleccione una fecha y hora.");
            return;
        }

        // Obtener la fecha y hora seleccionadas
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String fechaHoraInicioString = fechaSeleccionada + " " + horaSeleccionada;
        try {
            Date fechaHoraInicioDate = dateFormat.parse(fechaHoraInicioString);
            fechaHoraInicio = Calendar.getInstance();
            fechaHoraInicio.setTime(fechaHoraInicioDate);
        } catch (ParseException e) {
            Log.e("ReservarActivity", "Error al convertir fecha y hora de inicio: " + e.getMessage());
        }

        anotaciones = etAnotaciones.getText().toString();

        Cita nuevaCita = new Cita(usuarioActivo, servicioSeleccionado, precioDeServicio, fechaSeleccionada, horaSeleccionada, anotaciones);

        firebaseManager.registrarCita(nuevaCita);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            crearCanalNotificacion();
        } else {
            mostrarNotificacionReserva();
        }

        agregarEventoCalendario();
        configurarAlarmas(fechaHoraInicio);

        limpiarCampos();
    }

    private void setupSpinnerServicios() {
        firebaseManager.obtenerServicios(new FirebaseManager.ServiciosCallback() {
            @Override
            public void onServiciosObtenidos(List<String> servicios) {
                if (!servicios.isEmpty()) {
                    ServiciosArrayAdapter adapter = new ServiciosArrayAdapter(ReservarActivity.this,
                            android.R.layout.simple_spinner_item, servicios);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerServicios.setAdapter(adapter);
                } else {
                    Log.e("MainActivity", "No se encontraron servicios.");
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e("MainActivity", "Error al obtener servicios: " + errorMessage);
            }
        });

        spinnerServicios.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = parent.getItemAtPosition(position).toString();
                formatSpinnerSelection(selectedItem);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void formatSpinnerSelection(String selectedItem) {
        String[] parts = selectedItem.split(" - ");
        if (parts.length == 2) {
            servicioSeleccionado = parts[0].trim();
            String priceString = parts[1].trim().replace("€", "").trim();
            try {
                precioDeServicio = Float.parseFloat(priceString);
            } catch (NumberFormatException e) {
                Log.e("ReservarActivity", "Error al convertir el precio del servicio a float: " + e.getMessage());
            }
        } else {
            Log.e("ReservarActivity", "Error al dividir la cadena seleccionada del spinner.");
        }
    }

    private void setupCalendario() {
        Calendar calendar = Calendar.getInstance();
        calendario.setMinDate(calendar.getTimeInMillis());
    }

    private void setupRadioGroupHoras() {
        radioGroupHoras.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton radioButton = findViewById(checkedId);
                if (radioButton != null) {
                    horaSeleccionada = radioButton.getText().toString();
                }
            }
        });
    }

    private void limpiarCampos() {
        servicioSeleccionado = "";
        fechaSeleccionada = "";
        horaSeleccionada = "";
        anotaciones = "";

        spinnerServicios.setSelection(0);
        radioGroupHoras.clearCheck();
        etAnotaciones.setText("");

        radioGroupHoras.removeAllViews();
        findViewById(R.id.cvHora).setVisibility(View.GONE);
        findViewById(R.id.textViewServicio).setVisibility(View.VISIBLE);
        findViewById(R.id.spinnerServicios).setVisibility(View.VISIBLE);
        findViewById(R.id.cvCalendario).setVisibility(View.VISIBLE);
    }

    private void mostrarHorasDisponibles(List<String> horasReservadas, Calendar fechaElegida) {
        radioGroupHoras.removeAllViews();

        Calendar fechaActual = Calendar.getInstance();

        // Verificar si el día seleccionado es sábado
        if (fechaElegida.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
            // Iterar sobre las horas disponibles
            boolean horasDisponiblesEncontradas = false;
            for (String hora : horasDisponibles) {
                // Obtener la hora seleccionada
                int horaSeleccionada = Integer.parseInt(hora.split(":")[0]);
                // Verificar si la hora es menor o igual a 13:00 y no está reservada
                if (horaSeleccionada <= 13 && !horasReservadas.contains(hora)) {
                    RadioButton radioButton = new RadioButton(ReservarActivity.this);
                    radioButton.setText(hora);
                    radioGroupHoras.addView(radioButton);
                    horasDisponiblesEncontradas = true;
                }
            }
            // Si no se encontraron horas disponibles
            if (!horasDisponiblesEncontradas) {
                mostrarMensajeNoDisponibles();
                return;
            }
        } else if (fechaElegida.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY || fechaElegida.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
            // Si es domingo o lunes, mostrar mensaje de no disponibles
            mostrarMensajeNoDisponibles();
            return;
        } else { // Para otros días de la semana
            if (horasReservadas.size() == horasDisponibles.length) {
                findViewById(R.id.textNoReservas).setVisibility(View.VISIBLE);
                etAnotaciones.setVisibility(View.GONE);
                buttonReservar.setVisibility(View.GONE);
            } else {
                findViewById(R.id.textNoReservas).setVisibility(View.GONE);
                etAnotaciones.setVisibility(View.VISIBLE);
                buttonReservar.setVisibility(View.VISIBLE);

                for (String hora : horasDisponibles) {
                    int horaSeleccionada = Integer.parseInt(hora.split(":")[0]);

                    if (fechaElegida.get(Calendar.YEAR) == fechaActual.get(Calendar.YEAR) &&
                            fechaElegida.get(Calendar.MONTH) == fechaActual.get(Calendar.MONTH) &&
                            fechaElegida.get(Calendar.DAY_OF_MONTH) == fechaActual.get(Calendar.DAY_OF_MONTH)) {
                        int horaActual = fechaActual.get(Calendar.HOUR_OF_DAY);
                        if (horaSeleccionada > horaActual && !horasReservadas.contains(hora)) {
                            RadioButton radioButton = new RadioButton(ReservarActivity.this);
                            radioButton.setText(hora);
                            radioGroupHoras.addView(radioButton);
                        }
                    } else {
                        if (!horasReservadas.contains(hora)) {
                            RadioButton radioButton = new RadioButton(ReservarActivity.this);
                            radioButton.setText(hora);
                            radioGroupHoras.addView(radioButton);
                        }
                    }
                }
            }
        }

        findViewById(R.id.cvCalendario).setVisibility(View.GONE);
        findViewById(R.id.textViewServicio).setVisibility(View.GONE);
        findViewById(R.id.spinnerServicios).setVisibility(View.GONE);
        findViewById(R.id.cvHora).setVisibility(View.VISIBLE);
    }

    private void mostrarMensajeNoDisponibles() {
        mostrarToast("Lo sentimos, no hay reservas disponibles para ese día.");
    }

    public static void redirectActivity(Activity activity, Class secondActivity, String usuarioActivo) {
        Intent intent = new Intent(activity, secondActivity);
        intent.putExtra("usuarioActivo", usuarioActivo);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    private void crearCanalNotificacion() {
        CharSequence nombreCanal = "Reservas";
        int importancia = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel canal = new NotificationChannel(CHANNEL_ID, nombreCanal, importancia);
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(canal);
        mostrarNotificacionReserva();
    }

    @SuppressLint("MissingPermission")
    private void mostrarNotificacionReserva() {
        // Construir notificación
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle("Peluquería Lucía")
                .setContentText("Reserva confirmada!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(true);

        // PendingIntent nulo para que al tocar la notificación no haga nada
        PendingIntent pendingIntent = null;

        builder.setContentIntent(pendingIntent);

        // Mostrar la notificación
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(1, builder.build());
    }

    private void agregarEventoCalendario() {
        if (fechaHoraInicio != null) {
            Calendar fechaHoraFin = (Calendar) fechaHoraInicio.clone();
            fechaHoraFin.add(Calendar.HOUR_OF_DAY, 1); // La cita finalizará por defecto 1 hora después de la hora de inicio

            DecimalFormat decimalFormat = new DecimalFormat("0.00€");
            String precioFormateado = decimalFormat.format(precioDeServicio);

            String descripcionEvento = "Reserva confirmada en Salón Estética y Peluquería Lucía García.";
            if (!servicioSeleccionado.isEmpty()) {
                descripcionEvento += "\nServicio: " + servicioSeleccionado + "\nPrecio: " + precioFormateado;
            }
            if (!anotaciones.isEmpty()) {
                descripcionEvento += "\nAnotaciones: " + anotaciones;
            }

            Intent intent = new Intent(Intent.ACTION_EDIT)
                    .setType("vnd.android.cursor.item/event")
                    .putExtra(CalendarContract.Events.TITLE, "Reserva de peluquería")
                    .putExtra(CalendarContract.Events.DESCRIPTION, descripcionEvento)
                    .putExtra(CalendarContract.Events.EVENT_LOCATION, "Salón Estética y Peluquería Lucía García")
                    .putExtra(CalendarContract.Events.ALL_DAY, false)
                    .putExtra(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID())
                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, fechaHoraInicio.getTimeInMillis())
                    .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, fechaHoraFin.getTimeInMillis());

            startActivity(intent);

        } else {
            mostrarToast("No se ha seleccionado una fecha y hora para agregar al calendario.");
        }
    }

    private void configurarAlarmas(Calendar fechaHoraInicio) {
        // Obtener una instancia del AlarmManager
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        // Configurar la alarma para un día antes de la cita
        Calendar horaAlarmaDiaAntes = (Calendar) fechaHoraInicio.clone();
        horaAlarmaDiaAntes.add(Calendar.DAY_OF_MONTH, -1);

        // Crear un intent para la clase BroadcastReceiver que manejará la alarma del día anterior
        Intent intentDiaAntes = new Intent(this, AlarmReceiver.class);
        intentDiaAntes.putExtra("mensaje", "¡Mañana tiene una cita agendada! No lo olvide.");
        intentDiaAntes.putExtra("canal", CHANNEL_ID);

        // Crear un PendingIntent para la alarma del día anterior
        PendingIntent pendingIntentDiaAntes = PendingIntent.getBroadcast(this, 0, intentDiaAntes, PendingIntent.FLAG_IMMUTABLE);

        // Configurar la alarma del día anterior con el tiempo calculado
        alarmManager.set(AlarmManager.RTC_WAKEUP, horaAlarmaDiaAntes.getTimeInMillis(), pendingIntentDiaAntes);

        // Configurar la alarma para una hora antes de la cita
        Calendar horaAlarmaHoraAntes = (Calendar) fechaHoraInicio.clone();
        horaAlarmaHoraAntes.add(Calendar.HOUR_OF_DAY, -1);

        // Crear un intent para la clase BroadcastReceiver que manejará la alarma de una hora antes
        Intent intentHoraAntes = new Intent(this, AlarmReceiver.class);
        intentHoraAntes.putExtra("mensaje", "¡Su cita comenzará en 1 hora! " + servicioSeleccionado);
        intentHoraAntes.putExtra("canal", CHANNEL_ID);

        // Crear un PendingIntent para la alarma de una hora antes
        PendingIntent pendingIntentHoraAntes = PendingIntent.getBroadcast(this, 1, intentHoraAntes, PendingIntent.FLAG_IMMUTABLE);

        // Configurar la alarma de una hora antes con el tiempo calculado
        alarmManager.set(AlarmManager.RTC_WAKEUP, horaAlarmaHoraAntes.getTimeInMillis(), pendingIntentHoraAntes);
    }

    private void mostrarToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }


    public static void openDrawer(DrawerLayout drawerLayout) {
        drawerLayout.openDrawer(GravityCompat.START);
    }

    // Método para cerrar el DrawerLayout si está abierto
    public static void closeDrawer(DrawerLayout drawerLayout) {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeDrawer(drawerLayout);
    }

    // Evita que se salga por error de la app
    @Override
    public void onBackPressed() {

        if (pressedTime + 2000 > System.currentTimeMillis()) {
            super.onBackPressed();
            firebaseManager.signOut();
            mGoogleSignInClient.signOut();
            finish();
        } else {
            mostrarToast("Presiona nuevamente para salir");
        }
        pressedTime = System.currentTimeMillis();
    }
}
