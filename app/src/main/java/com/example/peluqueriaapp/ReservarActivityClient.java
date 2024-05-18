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

public class ReservarActivityClient extends AppCompatActivity implements View.OnClickListener {

    DrawerLayout drawerLayout;
    ImageView menu;
    TextView titulo;
    LinearLayout home, citas, info, qr, logout;
    String[] horasDisponibles;
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
        setContentView(R.layout.activity_reservar_client);
        horasDisponibles = getResources().getStringArray(R.array.horas_disponibles);

        firebaseManager = new FirebaseManager();
        mGoogleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN);

        usuarioActivo = firebaseManager.getCurrentUserEmail();

        // Asignar las vistas de los layout a los elementos
        setupViews();
        // Obtener el nombre del usuario y actualizar el título
        updateTitle();
        // Configurar OnClickListener de los botones
        setupOnClickListeners();
        // Configurar Spinner de servicios
        setupSpinnerServicios();
        // Configurar el calendario de citas
        setupCalendario();
        // Configurar RadioGroup de horas disponibles
        setupRadioGroupHoras();
    }

    private void updateTitle() {
        firebaseManager.getCurrentUserName(firebaseManager.getCurrentUserEmail(), new FirebaseManager.UserNameCallback() {
            @Override
            public void onUserNameReceived(String userName) {
                // Actualizar título de ventana
                updateTitle(userName);
            }
        });
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
            titulo.setText(getString(R.string.bienvenido) + userName);
        } else {
            titulo.setText(getString(R.string.bienvenido) + firebaseManager.getCurrentUserEmail());
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
            redirectActivity(this, ConsultarActivityClient.class, usuarioActivo);
        } else if (v.getId() == R.id.info) {
            redirectActivity(this, InfoActivityClient.class, usuarioActivo);
        } else if (v.getId() == R.id.qr) {
            redirectActivity(this, QrActivityClient.class, usuarioActivo);
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
            mostrarToast(getString(R.string.seleccione_un_servicio));
            return;
        }

        int dia = calendario.getDayOfMonth();
        int mes = calendario.getMonth();
        int anyo = calendario.getYear();

        Calendar fechaElegida = Calendar.getInstance();
        fechaElegida.set(anyo, mes, dia);
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
            mostrarToast(getString(R.string.seleccione_fecha_hora));
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
        firebaseManager.obtenerServicios(ReservarActivityClient.this, new FirebaseManager.ServiciosCallback() {
            @Override
            public void onServiciosObtenidos(List<String> servicios) {
                if (!servicios.isEmpty()) {
                    ServiciosArrayAdapter adapter = new ServiciosArrayAdapter(ReservarActivityClient.this,
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

    // Método para mostrar las horas disponibles y ocultar los elementos relacionados con la selección de servicios y fechas
    private void mostrarHorasDisponibles(List<String> horasReservadas, Calendar fechaElegida) {
        radioGroupHoras.removeAllViews();

        Calendar fechaActual = Calendar.getInstance();

        // Verificar si el día seleccionado es sábado para reducir la jornada
        if (fechaElegida.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
            boolean alMenosUnaHoraDisponible = false;

            for (String hora : horasDisponibles) {
                // Obtener la hora seleccionada
                int horaSeleccionada = Integer.parseInt(hora.split(":")[0]);
                // Verificar si la hora es menor o igual a 13:00 y no está reservada
                if (horaSeleccionada <= 13 && !horasReservadas.contains(hora)) {
                    RadioButton radioButton = new RadioButton(ReservarActivityClient.this);
                    radioButton.setText(hora);
                    radioGroupHoras.addView(radioButton);
                    alMenosUnaHoraDisponible = true;
                }
            }

            if (!alMenosUnaHoraDisponible) {
                mostrarMensajeNoDisponibles();
                return;
            }
        } else if (fechaElegida.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY || fechaElegida.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
            // Si es domingo o lunes, mostrar mensaje de no disponibles
            mostrarMensajeNoDisponibles();
            return;
        } else { // Para otros días de la semana
            boolean alMenosUnaHoraDisponible = false;

            for (String hora : horasDisponibles) {
                int horaSeleccionada = Integer.parseInt(hora.split(":")[0]);

                if (fechaElegida.get(Calendar.YEAR) == fechaActual.get(Calendar.YEAR) &&
                        fechaElegida.get(Calendar.MONTH) == fechaActual.get(Calendar.MONTH) &&
                        fechaElegida.get(Calendar.DAY_OF_MONTH) == fechaActual.get(Calendar.DAY_OF_MONTH)) {
                    int horaActual = fechaActual.get(Calendar.HOUR_OF_DAY);
                    if (horaSeleccionada > horaActual && !horasReservadas.contains(hora)) {
                        RadioButton radioButton = new RadioButton(ReservarActivityClient.this);
                        radioButton.setText(hora);
                        radioGroupHoras.addView(radioButton);
                        alMenosUnaHoraDisponible = true;
                    }
                } else {
                    if (!horasReservadas.contains(hora)) {
                        RadioButton radioButton = new RadioButton(ReservarActivityClient.this);
                        radioButton.setText(hora);
                        radioGroupHoras.addView(radioButton);
                        alMenosUnaHoraDisponible = true;
                    }
                }
            }

            if (!alMenosUnaHoraDisponible) {
                mostrarMensajeNoDisponibles();
                return;
            }
        }

        // Si se han agregado botones de radio, mostrar las horas disponibles y ocultar los elementos relacionados con la selección de servicios y fechas
        findViewById(R.id.cvCalendario).setVisibility(View.GONE);
        findViewById(R.id.textViewServicio).setVisibility(View.GONE);
        findViewById(R.id.spinnerServicios).setVisibility(View.GONE);
        findViewById(R.id.cvHora).setVisibility(View.VISIBLE);
    }

    // Método para mostrar un mensaje de que no hay horas disponibles para la fecha seleccionada
    private void mostrarMensajeNoDisponibles() {
        mostrarToast(getString(R.string.no_reservas_disponibles));
    }

    public static void redirectActivity(Activity activity, Class secondActivity, String usuarioActivo) {
        Intent intent = new Intent(activity, secondActivity);
        intent.putExtra("usuarioActivo", usuarioActivo);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    // Método para crear el canal de notificación
    private void crearCanalNotificacion() {
        CharSequence nombreCanal = "Reservas";
        int importancia = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel canal = new NotificationChannel(CHANNEL_ID, nombreCanal, importancia);
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(canal);
        mostrarNotificacionReserva();
    }

    // Método para mostrar la notificación de reserva
    @SuppressLint("MissingPermission")
    private void mostrarNotificacionReserva() {
        // Construir notificación
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(getString(R.string.title_notification))
                .setContentText(getString(R.string.text_notification))
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

    // Método para agregar el evento al calendario
    private void agregarEventoCalendario() {
        if (fechaHoraInicio != null) {
            Calendar fechaHoraFin = (Calendar) fechaHoraInicio.clone();
            fechaHoraFin.add(Calendar.HOUR_OF_DAY, 1); // La cita finalizará por defecto 1 hora después de la hora de inicio

            DecimalFormat decimalFormat = new DecimalFormat("0.00€");
            String precioFormateado = decimalFormat.format(precioDeServicio);

            String descripcionEvento = getString(R.string.calendar_description);
            if (!servicioSeleccionado.isEmpty()) {
                descripcionEvento += "\n" + getString(R.string.servicio) + " " + servicioSeleccionado + "\n" + getString(R.string.precio) + " " + precioFormateado;
            }
            if (!anotaciones.isEmpty()) {
                descripcionEvento += "\n" + getString(R.string.anotaciones) + " " + anotaciones;
            }

            Intent intent = new Intent(Intent.ACTION_EDIT)
                    .setType("vnd.android.cursor.item/event")
                    .putExtra(CalendarContract.Events.TITLE, getString(R.string.calendar_title))
                    .putExtra(CalendarContract.Events.DESCRIPTION, descripcionEvento)
                    .putExtra(CalendarContract.Events.EVENT_LOCATION, getString(R.string.calendar_location))
                    .putExtra(CalendarContract.Events.ALL_DAY, false)
                    .putExtra(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID())
                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, fechaHoraInicio.getTimeInMillis())
                    .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, fechaHoraFin.getTimeInMillis());

            startActivity(intent);

        } else {
            mostrarToast(getString(R.string.seleccione_fecha_hora));
        }
    }

    // Método para configurar las alarmas para las citas
    private void configurarAlarmas(Calendar fechaHoraInicio) {
        // Obtener una instancia del AlarmManager
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        // Obtener el calendario actual
        Calendar calendarioActual = Calendar.getInstance();

        // Configurar la alarma para un día antes de la cita solo si la cita no es para el día actual
        if (!esMismoDia(calendarioActual, fechaHoraInicio)) {
            Calendar horaAlarmaDiaAntes = (Calendar) fechaHoraInicio.clone();
            horaAlarmaDiaAntes.add(Calendar.DAY_OF_MONTH, -1);

            // Crear un intent para la clase BroadcastReceiver que manejará la alarma del día anterior
            Intent intentDiaAntes = new Intent(this, AlarmReceiver.class);
            intentDiaAntes.putExtra("mensaje", getString(R.string.dia_alarma));
            intentDiaAntes.putExtra("canal", CHANNEL_ID);

            // Crear un PendingIntent para la alarma del día anterior
            PendingIntent pendingIntentDiaAntes = PendingIntent.getBroadcast(this, 0, intentDiaAntes, PendingIntent.FLAG_IMMUTABLE);

            // Configurar la alarma del día anterior con el tiempo calculado
            alarmManager.set(AlarmManager.RTC_WAKEUP, horaAlarmaDiaAntes.getTimeInMillis(), pendingIntentDiaAntes);
        }

        // Configurar la alarma para una hora antes de la cita
        Calendar horaAlarmaHoraAntes = (Calendar) fechaHoraInicio.clone();
        horaAlarmaHoraAntes.add(Calendar.HOUR_OF_DAY, -1);

        // Crear un intent para la clase BroadcastReceiver que manejará la alarma de una hora antes
        Intent intentHoraAntes = new Intent(this, AlarmReceiver.class);
        intentHoraAntes.putExtra("mensaje", getString(R.string.hora_alarma) + servicioSeleccionado);
        intentHoraAntes.putExtra("canal", CHANNEL_ID);

        // Crear un PendingIntent para la alarma de una hora antes
        PendingIntent pendingIntentHoraAntes = PendingIntent.getBroadcast(this, 1, intentHoraAntes, PendingIntent.FLAG_IMMUTABLE);

        // Configurar la alarma de una hora antes con el tiempo calculado
        alarmManager.set(AlarmManager.RTC_WAKEUP, horaAlarmaHoraAntes.getTimeInMillis(), pendingIntentHoraAntes);
    }

    // Método para verificar si dos fechas están en el mismo día
    private boolean esMismoDia(Calendar calendario1, Calendar calendario2) {
        return calendario1.get(Calendar.YEAR) == calendario2.get(Calendar.YEAR) &&
                calendario1.get(Calendar.MONTH) == calendario2.get(Calendar.MONTH) &&
                calendario1.get(Calendar.DAY_OF_MONTH) == calendario2.get(Calendar.DAY_OF_MONTH);
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
            mostrarToast(getString(R.string.press_again_to_exit));
        }
        pressedTime = System.currentTimeMillis();
    }
}
