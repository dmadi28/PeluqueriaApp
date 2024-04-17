package com.example.peluqueriaapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
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
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ReservarActivity extends AppCompatActivity implements View.OnClickListener {

    DrawerLayout drawerLayout;
    ImageView menu;
    TextView titulo;
    LinearLayout home, citas, info, logout;
    String[] horasDisponibles = {"9:00", "10:00", "11:00", "12:00", "13:00", "15:00", "16:00", "17:00", "18:00", "19:00"};
    String usuarioActivo = "";
    String servicioSeleccionado = "";

    float precioDeServicio;
    String fechaSeleccionada = "";
    String horaSeleccionada = "";
    String anotaciones = "";
    Spinner spinnerServicios;
    DatePicker calendario;
    RadioGroup radioGroupHoras;
    EditText etAnotaciones;
    Button buttonSiguiente, buttonReservar;
    ImageButton buttonBack;

    FirebaseManager firebaseManager;
    GoogleSignInClient mGoogleSignInClient;

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
            Toast.makeText(this, "Por favor, seleccione un servicio.", Toast.LENGTH_SHORT).show();
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
        if (horaSeleccionada.isEmpty()) {
            Toast.makeText(this, "Por favor, seleccione una hora.", Toast.LENGTH_SHORT).show();
            return;
        }

        anotaciones = etAnotaciones.getText().toString();

        Cita nuevaCita = new Cita(usuarioActivo, servicioSeleccionado, precioDeServicio, fechaSeleccionada, horaSeleccionada, anotaciones);

        firebaseManager.registrarCita(nuevaCita);

        Toast.makeText(this, "Cita reservada con éxito.", Toast.LENGTH_SHORT).show();
        mostrarNotificacionReserva();

        limpiarCampos();

        radioGroupHoras.removeAllViews();
        findViewById(R.id.cvHora).setVisibility(View.GONE);
        findViewById(R.id.textViewServicio).setVisibility(View.VISIBLE);
        findViewById(R.id.spinnerServicios).setVisibility(View.VISIBLE);
        findViewById(R.id.cvCalendario).setVisibility(View.VISIBLE);
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
        int añoActual = calendar.get(Calendar.YEAR);
        int mesActual = calendar.get(Calendar.MONTH);
        int diaActual = calendar.get(Calendar.DAY_OF_MONTH);

        calendario.setMinDate(calendar.getTimeInMillis());

        calendario.init(añoActual, mesActual, diaActual, null);
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
    }

    private void mostrarHorasDisponibles(List<String> horasReservadas, Calendar fechaElegida) {
        radioGroupHoras.removeAllViews();

        Calendar fechaActual = Calendar.getInstance();

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

        findViewById(R.id.cvCalendario).setVisibility(View.GONE);
        findViewById(R.id.textViewServicio).setVisibility(View.GONE);
        findViewById(R.id.spinnerServicios).setVisibility(View.GONE);
        findViewById(R.id.cvHora).setVisibility(View.VISIBLE);
    }

    public static void redirectActivity(Activity activity, Class secondActivity, String usuarioActivo) {
        Intent intent = new Intent(activity, secondActivity);
        intent.putExtra("usuarioActivo", usuarioActivo);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    private void crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence nombreCanal = "Reservas";
            String descripcionCanal = "Canal para notificaciones de reservas";
            int importancia = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel canal = new NotificationChannel("com.example.peluqueriaapp.reservas", nombreCanal, importancia);
            canal.setDescription(descripcionCanal);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(canal);
        }
    }

    private void mostrarNotificacionReserva() {
        crearCanalNotificacion();

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), "com.example.peluqueriaapp.reservas");
        int icono = R.mipmap.ic_launcher;
        Intent intent = new Intent(ReservarActivity.this, LoginActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(ReservarActivity.this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        mBuilder = mBuilder.setContentIntent(pendingIntent)
                .setSmallIcon(icono)
                .setContentTitle("Peluquería Lucía")
                .setContentText("Reserva confirmada!")
                .setVibrate(new long[]{100, 250, 100, 500})
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(1, mBuilder.build());
    }

    public static void openDrawer(DrawerLayout drawerLayout) {
        drawerLayout.openDrawer(GravityCompat.START);
    }

    // Método para cerrar el DrawerLayout si está abierto
    public static void closeDrawer(DrawerLayout drawerLayout) {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)){
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeDrawer(drawerLayout);
    }
}
