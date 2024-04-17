package com.example.peluqueriaapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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
    private EditText editTextEmail, editTextPassword;
    private Button buttonLogin, buttonRegister;
    private ImageButton buttonLoginGoogle;
    private FirebaseManager firebaseManager;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseManager = new FirebaseManager();

        // Asignar las vistas de los layout a los elementos
        setupViews();
        // Configurar OnClickListener de los botones
        setupOnClickListeners();
    }

    private void setupViews() {
        editTextEmail = findViewById(R.id.editTextEmailL);
        editTextPassword = findViewById(R.id.editTextPasswordL);
        buttonLogin = findViewById(R.id.buttonLoginL);
        buttonLoginGoogle = findViewById(R.id.buttonLoginGoogleL);
        buttonRegister = findViewById(R.id.buttonRegisterL);
    }

    private void setupOnClickListeners() {
        buttonLogin.setOnClickListener(this);
        buttonLoginGoogle.setOnClickListener(this);
        buttonRegister.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.buttonLoginL) {
            handleButtonLogin();
        } else if (v.getId() == R.id.buttonLoginGoogleL) {
            handleButtonLoginGoogle();
        } else if (v.getId() == R.id.buttonRegisterL) {
            handleButtonReservar();
        }
    }

    private void handleButtonLogin() {
        String email = editTextEmail.getText().toString();
        String password = editTextPassword.getText().toString();

        // Verificar que el correo electrónico y la contraseña no estén vacíos
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(LoginActivity.this, "Por favor, ingresa tu correo electrónico y contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseManager.signInWithEmailAndPassword(email, password, new FirebaseManager.AuthListener() {
            @Override
            public void onAuthSuccess() {
                Toast.makeText(LoginActivity.this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LoginActivity.this, ReservarActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onAuthFailure(String errorMessage) {
                Toast.makeText(LoginActivity.this, "" + errorMessage, Toast.LENGTH_LONG).show();
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
                        Toast.makeText(LoginActivity.this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, ReservarActivity.class);
                        startActivity(intent);
                        finish();
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
}
