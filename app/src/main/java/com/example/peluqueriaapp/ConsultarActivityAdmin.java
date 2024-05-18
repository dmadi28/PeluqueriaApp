package com.example.peluqueriaapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
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

import java.util.ArrayList;
import java.util.List;

public class ConsultarActivityAdmin extends AppCompatActivity implements View.OnClickListener {

    DrawerLayout drawerLayout;
    ImageView menu;
    TextView titulo;
    ListView lvCitas;
    LinearLayout home, citas, info, qr, admin, user, logout;
    SwipeRefreshLayout swipeRefreshLayout;
    String usuarioActivo = "";
    Spinner spinnerUsuarios;
    Button buttonBuscar, buttonVolver;
    TextView textViewNoCitas;
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
        spinnerUsuarios = findViewById(R.id.spinnerUsuarios);
        buttonBuscar = findViewById(R.id.btnBuscar);
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
            filtrarCitas();
            // Mostrar el botón de volver
            buttonVolver.setVisibility(View.VISIBLE);
        } else if (v.getId() == R.id.btnVolver) {
            buttonVolver.setVisibility(View.GONE);
            // Volver a cargar todas las citas
            cargarTodasLasCitas();
        }
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
        builder.setMessage(R.string.desea_anular_cita);
        builder.setPositiveButton(R.string.si, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Llamar al método del FirebaseManager para eliminar la cita
                firebaseManager.eliminarCita(cita, new FirebaseManager.Callback() {
                    @Override
                    public void onSuccess() {
                        // Mostrar un mensaje de éxito
                        mostrarToast(getString(R.string.cita_anulada_exitosamente));
                        // Actualizar la lista de citas
                        // Si las citas están filtradas, volver a cargarlas
                        // Si no, volver a cargar todas las citas
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

    private void filtrarCitas() {
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
