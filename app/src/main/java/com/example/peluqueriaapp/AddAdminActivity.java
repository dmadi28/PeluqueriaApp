package com.example.peluqueriaapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
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

public class AddAdminActivity extends AppCompatActivity implements View.OnClickListener {

    DrawerLayout drawerLayout;
    ImageView menu;
    TextView titulo;
    LinearLayout home, citas, info, qr, admin, user, logout;
    String usuarioActivo = "";
    Button buttonAddAdmin;
    Spinner spinnerUsuarios;
    FirebaseManager firebaseManager;
    GoogleSignInClient mGoogleSignInClient;
    long pressedTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_admin);

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
        setupSpinnerUsuarios();
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

        buttonAddAdmin = findViewById(R.id.btnAddAdmin);
        spinnerUsuarios = findViewById(R.id.spinnerUsuarios);
    }

    private void updateTitle() {
        titulo.setText(R.string.agregar_administrador);
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

        buttonAddAdmin.setOnClickListener(this);
    }

    private void setupSpinnerUsuarios() {
        firebaseManager.obtenerUsuariosConRolUsuario(new FirebaseManager.UsuariosCallback() {
            @Override
            public void onUsuariosObtenidos(List<Usuario> usuarios) {
                if (!usuarios.isEmpty()) {
                    UsuariosArrayAdapter adapter = new UsuariosArrayAdapter(AddAdminActivity.this,
                            android.R.layout.simple_spinner_item, usuarios);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerUsuarios.setAdapter(adapter);
                } else {
                    Log.e("AddAdminActivity", "No se encontraron usuarios con rol 'usuario'.");
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e("AddAdminActivity", "Error al obtener usuarios con rol 'usuario': " + errorMessage);
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
            recreate();
        } else if (v.getId() == R.id.add_user) {
            redirectActivity(this, AddUserActivity.class, usuarioActivo);
        } else if (v.getId() == R.id.btnAddAdmin) {
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
        Usuario usuarioSeleccionado = (Usuario) spinnerUsuarios.getSelectedItem();
        if (usuarioSeleccionado != null) {
            String userEmail = usuarioSeleccionado.getEmail();
            firebaseManager.cambiarRolUsuarioAdmin(userEmail, new FirebaseManager.FirebaseCallback() {
                @Override
                public void onSuccess() {
                    mostrarToast(getString(R.string.usuario_a_administrador));
                }

                @Override
                public void onError(String errorMessage) {
                    mostrarToast(getString(R.string.error_usuario_a_administrador));
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