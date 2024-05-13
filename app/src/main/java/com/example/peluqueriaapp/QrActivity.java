package com.example.peluqueriaapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class QrActivity extends AppCompatActivity implements View.OnClickListener {

    DrawerLayout drawerLayout;
    ImageView menu;
    TextView titulo;
    LinearLayout home, citas, info, qr, logout;
    String usuarioActivo = "";
    ImageButton buttonGenerar, buttonEscanear;
    FirebaseManager firebaseManager;
    GoogleSignInClient mGoogleSignInClient;
    long pressedTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_qr);

        usuarioActivo = getIntent().getStringExtra("usuarioActivo");

        firebaseManager = new FirebaseManager();
        mGoogleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN);

        // Asignar las vistas de los layout a los elementos
        setupViews();
        // Actualizar título de ventana
        updateTitle();
        // Configurar OnClickListener de los botones
        setupOnClickListeners();
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

        buttonGenerar = findViewById(R.id.btnGenerarQR);
        buttonEscanear = findViewById(R.id.btnEscanearQR);
    }

    private void updateTitle() {
        titulo.setText("Generador de códigos de descuento");
    }

    private void setupOnClickListeners() {
        menu.setOnClickListener(this);
        home.setOnClickListener(this);
        citas.setOnClickListener(this);
        info.setOnClickListener(this);
        qr.setOnClickListener(this);
        logout.setOnClickListener(this);

        buttonGenerar.setOnClickListener(this);
        buttonEscanear.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.menu) {
            openDrawer(drawerLayout);
        } else if (v.getId() == R.id.home) {
            redirectActivity(this, ReservarActivity.class);
        } else if (v.getId() == R.id.citas) {
            redirectActivity(this, ConsultarActivity.class, usuarioActivo);
        } else if (v.getId() == R.id.info) {
            redirectActivity(this, InfoActivity.class, usuarioActivo);
        } else if (v.getId() == R.id.qr) {
            recreate();
        } else if (v.getId() == R.id.btnGenerarQR) {
            // Redirige a la actividad para generar códigos
            redirectActivity(this, GenerarCodigoActivity.class, usuarioActivo);
        } else if (v.getId() == R.id.btnEscanearQR) {
            abrirScanner();
        } else if (v.getId() == R.id.logout) {
            firebaseManager.signOut();
            mGoogleSignInClient.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void abrirScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Leyendo código de descuento...");
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(true);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                // Se leyó correctamente el contenido del código
                comprobarCodigoQR(result.getContents());
                //mostrarInformacionQR(result.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void comprobarCodigoQR(String codigoQR) {
        // Extraer porcentaje y fecha de expiración
        String porcentaje = extraerPorcentaje(codigoQR);
        String fechaExpiracion = extraerFechaExpiracion(codigoQR);

        // Verificar la existencia del código QR en Firebase
        firebaseManager.comprobarExistenciaCodigoQR(porcentaje, fechaExpiracion, new FirebaseManager.OnCodigoQRComprobadoListener() {
            @Override
            public void onCodigoQRComprobado(boolean existe) {
                if (existe) {
                    // Si el código QR ya existe en Firebase
                    if (!isFechaExpirada(fechaExpiracion)) {
                        // Si la fecha de expiración no ha pasado todavía
                        mostrarToast("¡Felicidades, código VÁLIDO!");
                        mostrarInformacionQR(codigoQR);
                        // Eliminar el documento correspondiente al código QR válido
                        firebaseManager.eliminarCodigoQR(porcentaje, fechaExpiracion);
                    } else {
                        // Si la fecha de expiración ya ha pasado
                        mostrarToast("¡Código CADUCADO!");
                        // Eliminar el documento correspondiente al código QR inválido
                        firebaseManager.eliminarCodigoQR(porcentaje, fechaExpiracion);
                    }
                } else {
                    // Si el código QR no existe en Firebase
                    mostrarToast("ERROR. Código no existe");
                }
            }
        });
    }

    // Método para verificar si la fecha de expiración ha pasado
    private boolean isFechaExpirada(String fechaExpiracion) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            Date fechaExpiracionDate = dateFormat.parse(fechaExpiracion);
            Date fechaActual = new Date();
            return fechaActual.after(fechaExpiracionDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return true;
    }

    private void mostrarInformacionQR(String codigoQR) {
        // Crear y mostrar un AlertDialog con la información del código QR
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Código de descuento activo");
        builder.setMessage(codigoQR);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void mostrarToast(String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
    }

    private String extraerPorcentaje(String codigoQR) {
        // Extraer el porcentaje del código QR
        int inicio = codigoQR.indexOf("Porcentaje de descuento a aplicar: ") + "Porcentaje de descuento a aplicar: ".length();
        int fin = codigoQR.indexOf("%", inicio);
        if (inicio >= 0 && fin >= 0 && fin > inicio) {
            return codigoQR.substring(inicio, fin);
        }
        return null;
    }

    private String extraerFechaExpiracion(String codigoQR) {
        // Extraer la fecha de expiración del código QR
        int inicio = codigoQR.indexOf("Fecha de expiración del código: ") + "Fecha de expiración del código: ".length();
        int fin = codigoQR.indexOf("\n", inicio);
        if (inicio >= 0 && fin >= 0 && fin > inicio) {
            return codigoQR.substring(inicio, fin);
        }
        return null;
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
            mostrarToast("Presiona nuevamente para salir");
        }
        pressedTime = System.currentTimeMillis();
    }
}