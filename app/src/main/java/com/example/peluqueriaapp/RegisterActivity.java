package com.example.peluqueriaapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText editTextNombre, editTextEmail, editTextPassword, editTextPassword2, editTextTelefono;
    private Spinner spinnerGenero;
    private Button buttonRegister;
    private FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        firebaseManager = new FirebaseManager();

        editTextNombre = findViewById(R.id.editTextNombreR);
        editTextEmail = findViewById(R.id.editTextEmailR);
        editTextPassword = findViewById(R.id.editTextPasswordR);
        editTextPassword2 = findViewById(R.id.editTextPassword2R);
        editTextTelefono = findViewById(R.id.editTextTelefonoR);
        spinnerGenero = findViewById(R.id.spinnerGeneroR);
        buttonRegister = findViewById(R.id.buttonRegisterR);

        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Validar los campos antes de registrar al usuario
                if (validateFields()) {
                    String nombre = editTextNombre.getText().toString();
                    String email = editTextEmail.getText().toString();
                    String password = editTextPassword.getText().toString();
                    String telefono = editTextTelefono.getText().toString();
                    String genero = spinnerGenero.getSelectedItem().toString();

                    firebaseManager.registerUser(RegisterActivity.this, nombre, email, password, telefono, genero);
                }
            }
        });
    }

    // Método para validar todos los campos
    private boolean validateFields() {
        // Nombre
        String nombre = editTextNombre.getText().toString();
        if (TextUtils.isEmpty(nombre)) {
            editTextNombre.setError(getString(R.string.nombre_vacio));
            return false;
        }

        // Email
        String email = editTextEmail.getText().toString();
        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError(getString(R.string.email_vacio));
            return false;
        }
        if (!isValidEmail(email)) {
            editTextEmail.setError(getString(R.string.formato_email_incorrecto));
            return false;
        }

        // Contraseña
        String password = editTextPassword.getText().toString();
        String confirmPassword = editTextPassword2.getText().toString();
        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError(getString(R.string.contrasena_vacio));
            return false;
        }
        if (password.length() < 6) {
            editTextPassword.setError(getString(R.string.contrasena_caracteres));
            return false;
        }
        if (!password.equals(confirmPassword)) {
            editTextPassword2.setError(getString(R.string.contrasenas_no_coinciden));
            return false;
        }

        // Teléfono
        String telefono = editTextTelefono.getText().toString();
        if (TextUtils.isEmpty(telefono)) {
            editTextTelefono.setError(getString(R.string.telefono_vacio));
            return false;
        }
        if (!isValidPhoneNumber(telefono)) {
            editTextTelefono.setError(getString(R.string.formato_telefono_incorrecto));
            return false;
        }

        return true; // Todos los campos son válidos
    }

    // Método para validar el formato del correo electrónico
    private boolean isValidEmail(CharSequence target) {
        return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    // Método para validar el formato del número de teléfono
    private boolean isValidPhoneNumber(String phoneNumber) {
        String regex = "^[0-9]{9}$";
        return phoneNumber.matches(regex);
    }
}
