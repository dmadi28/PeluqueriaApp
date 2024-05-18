package com.example.peluqueriaapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    int RC_SIGN_IN = 1;
    String TAG = "GoogleSignIn";
    EditText textInputEditTextEmail, textInputEditTextPassword;
    private TextView textViewForgotPassword;
    private Button buttonLogin, buttonRegister;
    private ImageButton buttonLoginGoogle;
    private FirebaseManager firebaseManager;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Sleep para simular una carga lenta de la Splash Screen
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // Establecer el tema de la aplicación
        setTheme(R.style.Base_Theme_NavDrawerActivity);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseManager = new FirebaseManager();

        firebaseManager.eliminarCitasPasadas();

        // Asignar las vistas de los layout a los elementos
        setupViews();
        // Configurar OnClickListener de los botones
        setupOnClickListeners();
    }

    private void setupViews() {
        textInputEditTextEmail = findViewById(R.id.editTextEmailL);
        textInputEditTextPassword = findViewById(R.id.editTextPasswordL);
        textViewForgotPassword = findViewById(R.id.textViewForgotPassword);
        buttonLogin = findViewById(R.id.buttonLoginL);
        buttonLoginGoogle = findViewById(R.id.buttonLoginGoogleL);
        buttonRegister = findViewById(R.id.buttonRegisterL);
    }

    private void setupOnClickListeners() {
        buttonLogin.setOnClickListener(this);
        buttonLoginGoogle.setOnClickListener(this);
        buttonRegister.setOnClickListener(this);
        textViewForgotPassword.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.buttonLoginL) {
            handleButtonLogin();
        } else if (v.getId() == R.id.buttonLoginGoogleL) {
            handleButtonLoginGoogle();
        } else if (v.getId() == R.id.buttonRegisterL) {
            handleButtonReservar();
        } else if (v.getId() == R.id.textViewForgotPassword) {
            handleForgotPassword();
        }
    }

    private void handleButtonLogin() {
        String email = textInputEditTextEmail.getText().toString();
        String password = textInputEditTextPassword.getText().toString();

        // Verificar que el correo electrónico y la contraseña no estén vacíos
        if (email.isEmpty() || password.isEmpty()) {
            mostrarToast(getString(R.string.ingresa_correo_y_contrasena));
            return;
        }

        firebaseManager.signInWithEmailAndPassword(email, password, new FirebaseManager.AuthListener() {
            @Override
            public void onAuthSuccess() {
                // Inicio de sesión exitoso
                inicioSesionCorrecto();
            }

            @Override
            public void onAuthFailure(String errorMessage) {
                mostrarToast(getString(R.string.inicio_de_sesion_fallido));
            }
        });
    }

    private void handleButtonLoginGoogle() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        
        signInWithGoogle();
    }

    private void handleButtonReservar() {
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(intent);
    }

    private void handleForgotPassword() {
        // Crear un cuadro de diálogo para que el usuario ingrese la nueva contraseña
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.reset_contrasena);
        View view = getLayoutInflater().inflate(R.layout.dialog_forgot_password, null);
        builder.setView(view);

        EditText editTextConfirmEmail = view.findViewById(R.id.editTextEmail);
        Button buttonResetPassword = view.findViewById(R.id.buttonResetPassword);

        AlertDialog dialog = builder.create();
        buttonResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Obtener el correo electrónico del nuevo EditText
                String email = editTextConfirmEmail.getText().toString();

                // Usar el email obtenido para restablecer la contraseña
                firebaseManager.resetPassword(email, new FirebaseManager.ResetPasswordListener() {
                    @Override
                    public void onResetSuccess(String message) {
                        mostrarToast(message);
                        dialog.dismiss();
                    }

                    @Override
                    public void onResetFailure(String errorMessage) {
                        mostrarToast(errorMessage);
                    }
                });
            }
        });

        dialog.show();
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseManager.signInWithGoogle(account.getIdToken(), new FirebaseManager.AuthListener() {
                    @Override
                    public void onAuthSuccess() {
                        // Inicio de sesión exitoso
                        inicioSesionCorrecto();
                    }

                    @Override
                    public void onAuthFailure(String errorMessage) {
                        Log.w(TAG, "signInWithGoogle:failure");
                    }
                });
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
            }
        }
    }

    private void inicioSesionCorrecto() {
        mostrarToast(getString(R.string.inicio_de_sesion_exitoso));

        firebaseManager.checkAdminUser(firebaseManager.getCurrentUserEmail(), isAdmin -> {
            if (isAdmin) {
                Intent intent = new Intent(LoginActivity.this, ReservarActivityAdmin.class);
                startActivity(intent);
            } else {
                Intent intent = new Intent(LoginActivity.this, ReservarActivityClient.class);
                startActivity(intent);
            }
            finish();
        });
    }


    private void mostrarToast(String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
    }
}
