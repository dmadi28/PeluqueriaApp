/*
Proyecto: Lucía García BeautyBoard
--------------------------------------------------------------------
Autor: David Maestre Díaz
--------------------------------------------------------------------
Versión: 1.0
--------------------------------------------------------------------
Descripción: La clase ConsultarActivityClient es responsable de gestionar la interfaz de usuario para consultar todas las citas desde una perspectiva de cliente en la aplicación "Lucía García BeautyBoard".

Esta actividad incluye funcionalidades como:
- Mostrar una lista de todas las citas disponibles para el cliente activo.
- Permitir la anulación de citas programadas por el cliente.
- Implementar un gesto de deslizamiento para actualizar la lista de citas.

La actividad también incluye un menú lateral con opciones de navegación a otras partes de la aplicación, como la página principal, información adicional y funciones de cierre de sesión.
Además, la actividad gestiona el cierre de sesión del cliente y evita que la aplicación se cierre accidentalmente al presionar el botón de retroceso.
*/

package com.example.peluqueriaapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import java.util.List;

public class ConsultarActivityClient extends AppCompatActivity implements View.OnClickListener {

    DrawerLayout drawerLayout;
    ImageView menu;
    TextView titulo;
    ListView lvCitas;
    LinearLayout home, citas, info, qr, logout;
    SwipeRefreshLayout swipeRefreshLayout;
    String usuarioActivo = "";

    FirebaseManager firebaseManager;
    GoogleSignInClient mGoogleSignInClient;
    long pressedTime;

    // Crear un Handler para manejar el refresco automático
    private final Handler handler = new Handler();
    private static final long REFRESH_INTERVAL = 60 * 1000; // 60 segundos

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_consultar_client);

        firebaseManager = new FirebaseManager();
        mGoogleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN);

        usuarioActivo = getIntent().getStringExtra("usuarioActivo");

        // Asignar las vistas de los layout a los elementos
        setupViews();
        // Actualizar título de ventana
        updateTitle();
        // Configurar OnClickListener de los botones
        setupOnClickListeners();
        // Configurar OnItemClickListener para el ListView
        setupListViewClickListener();
        // Agregar listener para el gesto de deslizamiento para refrescar
        setupRefreshListener();
        // Crear el adaptador inicial para el ListView
        crearAdaptador();
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

        lvCitas = findViewById(R.id.lvCitas);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        startAutoRefresh();
    }

    private void updateTitle() {
        titulo.setText(getString(R.string.consultar_citas_de) + usuarioActivo);
    }

    private void setupOnClickListeners() {
        menu.setOnClickListener(this);
        home.setOnClickListener(this);
        citas.setOnClickListener(this);
        info.setOnClickListener(this);
        qr.setOnClickListener(this);
        logout.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.menu) {
            openDrawer(drawerLayout);
        } else if (v.getId() == R.id.home) {
            redirectActivity(this, ReservarActivityClient.class);
        } else if (v.getId() == R.id.citas) {
            recreate();
        } else if (v.getId() == R.id.info) {
            redirectActivity(this, InfoActivityClient.class, usuarioActivo);
        }  else if (v.getId() == R.id.qr) {
            redirectActivity(this, QrActivityClient.class, usuarioActivo);
        } else if (v.getId() == R.id.logout) {
            firebaseManager.signOut();
            mGoogleSignInClient.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
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
        AlertDialog.Builder builder = new AlertDialog.Builder(ConsultarActivityClient.this);
        builder.setTitle(getString(R.string.anular_cita));
        builder.setMessage(getString(R.string.desea_anular_citaClient));
        builder.setPositiveButton(getString(R.string.si), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Llamar al método del FirebaseManager para eliminar la cita
                firebaseManager.eliminarCita(cita, new FirebaseManager.Callback() {
                    @Override
                    public void onSuccess() {
                        // Mostrar un mensaje de éxito
                        mostrarToast(getString(R.string.cita_anulada_exitosamente));
                        // Actualizar la lista de citas
                        crearAdaptador();
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
        firebaseManager.obtenerCitasPorUsuario(usuarioActivo, new FirebaseManager.CitasCallback() {
            @Override
            public void onCitasObtenidas(List<Cita> citasList) {
                TextView textViewNoCitas = findViewById(R.id.textViewNoCitas);
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
                    AdaptadorCitas adapter = new AdaptadorCitas(ConsultarActivityClient.this, citasList);
                    lvCitas.setAdapter(adapter);
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e("ConsultarActivity", "Error al consultar citas del usuario." + errorMessage);
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