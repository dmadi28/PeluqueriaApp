/*
Proyecto: Lucía García BeautyBoard
--------------------------------------------------------------------
Autor: David Maestre Díaz
--------------------------------------------------------------------
Versión: 1.0
--------------------------------------------------------------------
Descripción: La clase ConsultarActivityAdmin es responsable de gestionar la interfaz de usuario para consultar todas las citas desde una perspectiva administrativa en la aplicación "Lucía García BeautyBoard".

Esta actividad incluye funcionalidades como:
- Mostrar una lista de todas las citas disponibles.
- Filtrar citas por usuario y fecha.
- Permitir la anulación de citas.
- Implementar un gesto de deslizamiento para actualizar la lista de citas.

La actividad también incluye un menú lateral con opciones de navegación a otras partes de la aplicación, como la consulta de citas, información adicional y funciones de administración de usuarios.
Además, la actividad gestiona el cierre de sesión del administrador y evita que la aplicación se cierre accidentalmente al presionar el botón de retroceso.
*/

package com.example.peluqueriaapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConsultarActivityAdmin extends AppCompatActivity implements View.OnClickListener {

    DrawerLayout drawerLayout;
    ImageView menu;
    TextView titulo;
    ListView lvCitas;
    LinearLayout home, citas, info, qr, admin, user, logout;
    SwipeRefreshLayout swipeRefreshLayout;
    String usuarioActivo = "";
    Spinner spinnerUsuarios;
    Button buttonBuscar, buttonFiltrarFecha, buttonVolver;
    TextView textViewNoCitas, textViewEligeUsuarios;
    FirebaseManager firebaseManager;
    GoogleSignInClient mGoogleSignInClient;
    long pressedTime;

    // Crear un Handler para manejar el refresco automático
    private final Handler handler = new Handler();
    private static final long REFRESH_INTERVAL = 60 * 1000; // 60 segundos
    private boolean isFiltered = false;
    private List<Cita> citasFiltradas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_consultar_admin);

        // Restaurar el estado de la búsqueda filtrada si hay datos guardados
        if (savedInstanceState != null) {
            isFiltered = savedInstanceState.getBoolean("isFiltered", false);
            citasFiltradas = (List<Cita>) savedInstanceState.getSerializable("citasFiltradas");
        }

        firebaseManager = new FirebaseManager();
        mGoogleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN);

        usuarioActivo = getIntent().getStringExtra("usuarioActivo");

        // Asignar las vistas de los layout a los elementos
        setupViews();
        // Actualizar título de ventana
        updateTitle();
        // Configurar OnClickListener de los botones
        setupOnClickListeners();
        // Configurar Spinner de usuarios
        setupSpinnerUsuarios();
        // Configurar OnItemClickListener para el ListView
        setupListViewClickListener();
        // Agregar listener para el gesto de deslizamiento para refrescar
        setupRefreshListener();
        // Crear el adaptador inicial para el ListView
        crearAdaptador();
    }

    // Método para guardar el estado de la búsqueda filtrada
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Guardar el estado de la búsqueda filtrada
        outState.putBoolean("isFiltered", isFiltered);
        if (isFiltered && citasFiltradas != null) {
            outState.putSerializable("citasFiltradas", new ArrayList<>(citasFiltradas));
        }
    }

    private void setupViews() {
        titulo = findViewById(R.id.title);

        drawerLayout = findViewById(R.id.drawerLayout);
        menu = findViewById(R.id.menu);
        home = findViewById(R.id.home);
        citas = findViewById(R.id.citas);
        info = findViewById(R.id.info);
        qr = findViewById(R.id.qr);
        admin = findViewById(R.id.add_admin);
        user = findViewById(R.id.add_user);
        logout = findViewById(R.id.logout);

        textViewNoCitas = findViewById(R.id.textViewNoCitas);
        textViewEligeUsuarios = findViewById(R.id.textViewEligeUsuario);
        spinnerUsuarios = findViewById(R.id.spinnerUsuarios);
        buttonBuscar = findViewById(R.id.btnBuscar);
        buttonFiltrarFecha = findViewById(R.id.btnFiltrarFecha);
        buttonVolver = findViewById(R.id.btnVolver);
        lvCitas = findViewById(R.id.lvCitas);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
    }

    private void updateTitle() {
        titulo.setText(R.string.consultar_todas_las_citas);
    }

    private void setupOnClickListeners() {
        menu.setOnClickListener(this);
        home.setOnClickListener(this);
        citas.setOnClickListener(this);
        info.setOnClickListener(this);
        qr.setOnClickListener(this);
        admin.setOnClickListener(this);
        user.setOnClickListener(this);
        logout.setOnClickListener(this);

        buttonBuscar.setOnClickListener(this);
        buttonFiltrarFecha.setOnClickListener(this);
        buttonVolver.setOnClickListener(this);
    }

    private void setupSpinnerUsuarios() {
        firebaseManager.obtenerUsuariosConRolUsuario(new FirebaseManager.UsuariosCallback() {
            @Override
            public void onUsuariosObtenidos(List<Usuario> usuarios) {
                if (!usuarios.isEmpty()) {
                    UsuariosArrayAdapter adapter = new UsuariosArrayAdapter(ConsultarActivityAdmin.this,
                            android.R.layout.simple_spinner_item, usuarios);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerUsuarios.setAdapter(adapter);
                } else {
                    Log.e("ConsultarActivityAdmin", "No se encontraron usuarios con rol 'usuario'.");
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e("ConsultarActivityAdmin", "Error al obtener usuarios con rol 'usuario': " + errorMessage);
            }
        });
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.menu) {
            openDrawer(drawerLayout);
        } else if (v.getId() == R.id.home) {
            redirectActivity(this, ReservarActivityAdmin.class);
        } else if (v.getId() == R.id.citas) {
            recreate();
        } else if (v.getId() == R.id.info) {
            redirectActivity(this, InfoActivityAdmin.class, usuarioActivo);
        } else if (v.getId() == R.id.qr) {
            redirectActivity(this, QrActivityAdmin.class, usuarioActivo);
        } else if (v.getId() == R.id.add_admin) {
            redirectActivity(this, AddAdminActivity.class, usuarioActivo);
        } else if (v.getId() == R.id.add_user) {
            redirectActivity(this, AddUserActivity.class, usuarioActivo);
        } else if (v.getId() == R.id.logout) {
            firebaseManager.signOut();
            mGoogleSignInClient.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        } else if (v.getId() == R.id.btnBuscar) {
            filtrarCitasUsuario();
            // Mostrar el botón de volver
            buttonVolver.setVisibility(View.VISIBLE);
            // No mostrar el botón de filtrar
            buttonFiltrarFecha.setVisibility(View.GONE);
        } else if (v.getId() == R.id.btnFiltrarFecha) {
            mostrarDatePickerDialog();
            buttonBuscar.setVisibility(View.GONE);
            buttonVolver.setVisibility(View.VISIBLE);
            textViewEligeUsuarios.setVisibility(View.GONE);
            spinnerUsuarios.setVisibility(View.GONE);
        } else if (v.getId() == R.id.btnVolver) {
            // Volver a cargar todas las citas
            cargarTodasLasCitas();
            buttonBuscar.setVisibility(View.VISIBLE);
            buttonFiltrarFecha.setVisibility(View.VISIBLE);
            buttonVolver.setVisibility(View.GONE);
            textViewEligeUsuarios.setVisibility(View.VISIBLE);
            spinnerUsuarios.setVisibility(View.VISIBLE);
        }
    }

    private void mostrarDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                ConsultarActivityAdmin.this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(year, month, dayOfMonth);
                        filtrarCitasPorFecha(selectedDate.getTime());
                    }
                },
                year, month, day
        );

        // Establecer la fecha mínima en el DatePicker a la fecha actual
        datePickerDialog.getDatePicker().setMinDate(calendar.getTimeInMillis());
        datePickerDialog.show();
    }

    private void filtrarCitasPorFecha(Date fecha) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String fechaString = sdf.format(fecha);

        firebaseManager.obtenerCitasDesdeFecha(fechaString, new FirebaseManager.CitasCallback() {
            @Override
            public void onCitasObtenidas(List<Cita> citasList) {
                isFiltered = true;
                citasFiltradas = citasList;
                AdaptadorCitas adapter = new AdaptadorCitas(ConsultarActivityAdmin.this, citasList);
                lvCitas.setAdapter(adapter);

                if (citasList.isEmpty()) {
                    textViewNoCitas.setVisibility(View.VISIBLE);
                } else {
                    textViewNoCitas.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e("ConsultarActivityAdmin", "Error al obtener citas por fecha: " + errorMessage);
            }
        });
    }

    private void setupListViewClickListener() {
        lvCitas.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Obtener la cita seleccionada
                Cita citaSeleccionada = (Cita) parent.getItemAtPosition(position);

                // Realizar una consulta en Firebase para encontrar la cita correspondiente
                firebaseManager.buscarCitaPorUsuarioFechaHora(citaSeleccionada, new FirebaseManager.CitaCallback() {
                    @Override
                    public void onSuccess(Cita cita) {
                        // Mostrar un diálogo de confirmación para anular la cita
                        showConfirmationDialog(cita);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        mostrarToast(getString(R.string.error_al_buscar_la_cita));
                    }
                });
            }
        });
    }

    private void setupRefreshListener() {
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                crearAdaptador();
                // Deshabilitar el indicador de actualización una vez que la actualización esté completa
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void showConfirmationDialog(Cita cita) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ConsultarActivityAdmin.this);
        builder.setTitle(R.string.anular_cita);

        // Agregar el nombre del usuario al mensaje del diálogo de confirmación
        String mensaje = getString(R.string.desea_anular_cita) + " " + cita.getUsuario();
        builder.setMessage(mensaje);

        builder.setPositiveButton(R.string.si, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Llamar al método del FirebaseManager para eliminar la cita
                firebaseManager.eliminarCita(cita, new FirebaseManager.Callback() {
                    @Override
                    public void onSuccess() {
                        // Mostrar un mensaje de éxito
                        mostrarToast(getString(R.string.cita_anulada_exitosamente));
                        // Enviar correo de anulación con detalles de la cita
                        enviarCorreoAnulacion(cita);

                        // Actualizar la lista de citas
                        if (isFiltered) {
                            cargarCitasFiltradas();
                        } else {
                            cargarTodasLasCitas();
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        // Mostrar un mensaje de error
                        mostrarToast(getString(R.string.error_al_anular_la_cita));
                    }
                });
            }
        });
        builder.setNegativeButton("No", null);
        builder.show();
    }

    // Método para construir el mensaje de anulación de la cita
    private String construirMensajeAnulacion(Cita cita) {
        StringBuilder mensaje = new StringBuilder();
        mensaje.append(getString(R.string.mensaje_anulacion_1)).append(cita.getUsuario()).append(",\n\n")
                .append(getString(R.string.mensaje_anulacion_2))
                .append(getString(R.string.mensaje_anulacion_3));

        // Formatear la fecha
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date date = inputFormat.parse(cita.getFecha());
            String formattedDate = outputFormat.format(date);
            mensaje.append(getString(R.string.mensaje_anulacion_4)).append(formattedDate).append("\n");
        } catch (ParseException e) {
            e.printStackTrace();
            mensaje.append(getString(R.string.mensaje_anulacion_4)).append(cita.getFecha()).append("\n");
        }

        mensaje.append(getString(R.string.mensaje_anulacion_5)).append(cita.getServicio()).append("\n");

        // Añadir anotaciones si no están vacías
        if (cita.getAnotaciones() != null && !cita.getAnotaciones().isEmpty()) {
            mensaje.append(getString(R.string.mensaje_anulacion_6));
            mensaje.append(cita.getAnotaciones());
            mensaje.append("\n");
        }

        mensaje.append(getString(R.string.mensaje_anulacion_7))
                .append(getString(R.string.mensaje_anulacion_8))
                .append(getString(R.string.mensaje_anulacion_9));

        return mensaje.toString();
    }

    // Método para enviar correo de anulación con detalles de la cita
    private void enviarCorreoAnulacion(Cita cita) {
        String email = cita.getUsuario();
        String subject = getString(R.string.subject_mensaje_anulacion);
        String message = construirMensajeAnulacion(cita);
        String[] addresses = {email};
        composeEmail(addresses, subject, message);
    }

    // Método para enviar correo electrónico
    private void composeEmail(String[] addresses, String subject, String message) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_EMAIL, addresses);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, message);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Log.e("composeEmail", "No email clients installed.");
            mostrarToast(getString(R.string.no_email_clients));
        }
    }

    private void crearAdaptador() {
        startAutoRefresh();

        if (isFiltered) {
            // Si hay una búsqueda filtrada, cargar las citas filtradas
            cargarCitasFiltradas();
        } else {
            // Si no hay búsqueda filtrada, cargar todas las citas
            cargarTodasLasCitas();
        }
    }

    private void cargarCitasFiltradas() {
        // Utilizar las citas filtradas si están disponibles
        if (citasFiltradas != null && !citasFiltradas.isEmpty()) {
            AdaptadorCitas adapter = new AdaptadorCitas(ConsultarActivityAdmin.this, citasFiltradas);
            lvCitas.setAdapter(adapter);
            // Verificar si la lista de citas está vacía para mostrar el mensaje
            if (citasFiltradas.isEmpty()) {
                textViewNoCitas.setVisibility(View.VISIBLE);
            } else {
                textViewNoCitas.setVisibility(View.GONE);
            }
        } else {
            // Si no hay citas filtradas, cargar todas las citas
            cargarTodasLasCitas();
        }
    }

    private void cargarTodasLasCitas() {
        firebaseManager.obtenerTodasLasCitas(new FirebaseManager.CitasCallback() {
            @Override
            public void onCitasObtenidas(List<Cita> citasList) {
                // Actualizar el estado de la búsqueda filtrada
                isFiltered = false;

                TextView textViewAnularCita = findViewById(R.id.textViewAnularCita);
                if (citasList.isEmpty()) {
                    // Si no hay citas disponibles
                    lvCitas.setVisibility(View.GONE);
                    textViewNoCitas.setVisibility(View.VISIBLE);
                    textViewAnularCita.setVisibility(View.GONE);
                } else {
                    // Si hay citas disponibles
                    lvCitas.setVisibility(View.VISIBLE);
                    textViewNoCitas.setVisibility(View.GONE);
                    textViewAnularCita.setVisibility(View.VISIBLE);

                    // Configurar el adaptador con la lista de citas
                    AdaptadorCitas adapter = new AdaptadorCitas(ConsultarActivityAdmin.this, citasList);
                    lvCitas.setAdapter(adapter);
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e("ConsultarActivity", "Error al obtener todas las citas: " + errorMessage);
            }
        });
    }

    // Método para filtrar citas por usuario
    private void filtrarCitasUsuario() {
        // Obtener el usuario seleccionado del Spinner
        Usuario usuarioSeleccionado = (Usuario) spinnerUsuarios.getSelectedItem();
        String usuarioEmail = usuarioSeleccionado.getEmail();

        // Filtrar las citas por el usuario seleccionado
        firebaseManager.obtenerCitasPorUsuario(usuarioEmail, new FirebaseManager.CitasCallback() {
            @Override
            public void onCitasObtenidas(List<Cita> citasList) {
                // Actualizar el estado de la búsqueda filtrada
                isFiltered = true;
                citasFiltradas = citasList;

                // Actualizar la lista de citas con las citas filtradas
                AdaptadorCitas adapter = new AdaptadorCitas(ConsultarActivityAdmin.this, citasList);
                lvCitas.setAdapter(adapter);

                // Verificar si la lista de citas está vacía para mostrar el mensaje
                if (citasList.isEmpty()) {
                    textViewNoCitas.setVisibility(View.VISIBLE);
                } else {
                    textViewNoCitas.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e("ConsultarActivityAdmin", "Error al obtener citas del usuario seleccionado: " + errorMessage);
            }
        });
    }

    private void startAutoRefresh() {
        // Utilizar el Handler para ejecutar la actualización periódicamente
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Realizar la actualización de datos
                crearAdaptador();
                // Programar el siguiente refresco
                handler.postDelayed(this, REFRESH_INTERVAL);
            }
        }, REFRESH_INTERVAL);
    }

    private void mostrarToast(String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
    }

    public static void openDrawer(DrawerLayout drawerLayout) {
        drawerLayout.openDrawer(GravityCompat.START);
    }

    public static void closeDrawer(DrawerLayout drawerLayout) {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)){
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    public static void redirectActivity(Activity activity, Class secondActivity) {
        Intent intent = new Intent(activity, secondActivity);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    public static void redirectActivity(Activity activity, Class secondActivity, String usuarioActivo) {
        Intent intent = new Intent(activity, secondActivity);
        intent.putExtra("usuarioActivo", usuarioActivo);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeDrawer(drawerLayout);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
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
