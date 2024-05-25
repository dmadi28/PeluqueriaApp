/*
Proyecto: Lucía García BeautyBoard
--------------------------------------------------------------------
Autor: David Maestre Díaz
--------------------------------------------------------------------
Versión: 1.0
--------------------------------------------------------------------
Descripción: La clase AddUserActivity es responsable de manejar la funcionalidad relacionada con la adición de nuevos usuarios en la aplicación "Lucía García BeautyBoard".

Esta actividad incluye las siguientes funcionalidades:
- Mostrar una lista de usuarios con rol de administrador para seleccionar como nuevos usuarios.
- Permitir la selección de un usuario y cambiar su rol a usuario estándar.

La actividad también incluye un menú lateral con opciones de navegación a otras partes de la aplicación, como la consulta de citas, información adicional y funciones de administración de usuarios.
Además, la actividad gestiona el cierre de sesión del administrador y evita que la aplicación se cierre accidentalmente al presionar el botón de retroceso.
*/

package com.example.peluqueriaapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import java.util.List;

public class AddUserActivity extends AppCompatActivity implements View.OnClickListener {

    DrawerLayout drawerLayout;
    ImageView menu;
    TextView titulo;
    LinearLayout home, citas, info, qr, admin, user, logout;
    String usuarioActivo = "";
    Button buttonAddUser;
    Spinner spinnerAdministradores;
    FirebaseManager firebaseManager;
    GoogleSignInClient mGoogleSignInClient;
    long pressedTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_user);

        usuarioActivo = getIntent().getStringExtra("usuarioActivo");

        firebaseManager = new FirebaseManager();
        mGoogleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN);

        // Asignar las vistas de los layout a los elementos
        setupViews();
        // Actualizar título de ventana
        updateTitle();
        // Configurar OnClickListener de los botones
        setupOnClickListeners();
        // Configurar Spinner de usuarios
        setupSpinnerAdministradores();
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

        buttonAddUser = findViewById(R.id.btnAddUser);
        spinnerAdministradores = findViewById(R.id.spinnerAdministradores);
    }

    private void updateTitle() {
        titulo.setText(R.string.agregar_usuario);
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

        buttonAddUser.setOnClickListener(this);
    }

    private void setupSpinnerAdministradores() {
        firebaseManager.obtenerUsuariosConRolAdmin(new FirebaseManager.UsuariosCallback() {
            @Override
            public void onUsuariosObtenidos(List<Usuario> usuarios) {
                // Filtrar el usuario activo para que no aparezca en el spinner
                String emailUsuarioActivo = firebaseManager.getCurrentUserEmail();
                Usuario usuarioActivo = null;
                for (Usuario usuario : usuarios) {
                    if (usuario.getEmail().equals(emailUsuarioActivo)) {
                        usuarioActivo = usuario;
                        break;
                    }
                }
                if (usuarioActivo != null) {
                    usuarios.remove(usuarioActivo);
                }

                if (!usuarios.isEmpty()) {
                    UsuariosArrayAdapter adapter = new UsuariosArrayAdapter(AddUserActivity.this,
                            android.R.layout.simple_spinner_item, usuarios);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerAdministradores.setAdapter(adapter);
                    buttonAddUser.setEnabled(true); // Habilitar el botón
                } else {
                    Log.e("AddUserActivity", "No se encontraron usuarios con rol 'admin'.");
                    buttonAddUser.setEnabled(false); // Deshabilitar el botón
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e("AddUserActivity", "Error al obtener usuarios con rol 'admin': " + errorMessage);
                buttonAddUser.setEnabled(false); // Deshabilitar el botón en caso de error
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
            redirectActivity(this, ConsultarActivityAdmin.class, usuarioActivo);
        } else if (v.getId() == R.id.info) {
            redirectActivity(this, InfoActivityAdmin.class, usuarioActivo);
        } else if (v.getId() == R.id.qr) {
            redirectActivity(this, QrActivityAdmin.class, usuarioActivo);
        } else if (v.getId() == R.id.add_admin) {
            redirectActivity(this, AddAdminActivity.class, usuarioActivo);
        } else if (v.getId() == R.id.add_user) {
            recreate();
        } else if (v.getId() == R.id.btnAddUser) {
            cambiarRol();
        } else if (v.getId() == R.id.logout) {
            firebaseManager.signOut();
            mGoogleSignInClient.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void cambiarRol() {
        // Obtener el usuario seleccionado del Spinner
        Usuario usuarioSeleccionado = (Usuario) spinnerAdministradores.getSelectedItem();
        if (usuarioSeleccionado != null) {
            String userEmail = usuarioSeleccionado.getEmail();
            firebaseManager.cambiarRolAdminUsuario(userEmail, new FirebaseManager.FirebaseCallback() {
                @Override
                public void onSuccess() {
                    mostrarToast(getString(R.string.administrador_a_usuario));
                    // Recargar el Spinner para que el admin convertido no se muestre
                    setupSpinnerAdministradores();
                }

                @Override
                public void onError(String errorMessage) {
                    mostrarToast(getString(R.string.error_administrador_a_usuario));
                }
            });
        }
    }

    private void mostrarToast(String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
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