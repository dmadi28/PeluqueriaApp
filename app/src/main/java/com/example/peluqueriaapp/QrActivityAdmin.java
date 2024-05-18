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

public class QrActivityAdmin extends AppCompatActivity implements View.OnClickListener {

    DrawerLayout drawerLayout;
    ImageView menu;
    TextView titulo;
    LinearLayout home, citas, info, qr, admin, user, logout;
    String usuarioActivo = "";
    ImageButton buttonGenerar, buttonEscanear;
    FirebaseManager firebaseManager;
    GoogleSignInClient mGoogleSignInClient;
    long pressedTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_qr_admin);

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
        admin = findViewById(R.id.add_admin);
        user = findViewById(R.id.add_user);
        logout = findViewById(R.id.logout);

        buttonGenerar = findViewById(R.id.btnGenerarQR);
        buttonEscanear = findViewById(R.id.btnEscanearQR);
    }

    private void updateTitle() {
        titulo.setText(R.string.generador_codigos_descuento);
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

        buttonGenerar.setOnClickListener(this);
        buttonEscanear.setOnClickListener(this);
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
            recreate();
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
        } else if (v.getId() == R.id.btnGenerarQR) {
            // Redirige a la actividad para generar códigos
            redirectActivity(this, GenerarQrActivity.class, usuarioActivo);
        } else if (v.getId() == R.id.btnEscanearQR) {
            abrirScanner();
        }
    }

    private void abrirScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt(getString(R.string.leyendo_codigo_descuento));
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
        // Extraer el ID único del código QR
        String idUnico = extraerIdUnico(codigoQR);

        // Verificar la existencia del código QR en Firebase
        firebaseManager.comprobarExistenciaCodigoQR(idUnico, new FirebaseManager.OnCodigoQRComprobadoListener() {
            @Override
            public void onCodigoQRComprobado(boolean existe) {
                if (existe) {
                    // Si el código QR ya existe en Firebase
                    String fechaExpiracion = extraerFechaExpiracion(codigoQR);
                    if (!isFechaExpirada(fechaExpiracion)) {
                        // Si la fecha de expiración no ha pasado todavía
                        mostrarToast(getString(R.string.codigo_valido));
                        mostrarInformacionQR(codigoQR);
                        // Eliminar el documento correspondiente al código QR válido
                        firebaseManager.eliminarCodigoQR(idUnico);
                    } else {
                        // Si la fecha de expiración ya ha pasado
                        mostrarToast(getString(R.string.codigo_caducado));
                        // Eliminar el documento correspondiente al código QR inválido
                        firebaseManager.eliminarCodigoQR(idUnico);
                    }
                } else {
                    // Si el código QR no existe en Firebase
                    mostrarToast(getString(R.string.codigo_no_existe));
                }
            }
        });
    }

    // Método para extraer el ID único del código QR
    private String extraerIdUnico(String codigoQR) {
        String idLabel = "ID: ";
        int inicio = codigoQR.indexOf(idLabel) + idLabel.length();
        if (inicio >= 0) {
            return codigoQR.substring(inicio).trim();
        }
        return null;
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

    // Método para mostrar la información del código QR
    private void mostrarInformacionQR(String codigoQR) {
        // Eliminar la tercera línea (la línea del ID)
        String textoQRSinId = codigoQR.substring(0, codigoQR.lastIndexOf("\n"));

        // Crear y mostrar un AlertDialog con la información filtrada del código QR
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.codigo_activo);
        builder.setMessage(textoQRSinId);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    // Método para eliminar el ID del texto de información del código QR
    private String quitarID(String codigoQR) {
        // Buscar la posición del primer salto de línea (indicando el final del ID)
        int indiceSaltoLinea = codigoQR.indexOf("\n");
        if (indiceSaltoLinea != -1) {
            // Extraer el contenido después del primer salto de línea (eliminando el ID)
            return codigoQR.substring(indiceSaltoLinea + 1);
        } else {
            // Si no se encuentra un salto de línea, devolver el código QR sin cambios
            return codigoQR;
        }
    }

    private void mostrarToast(String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
    }

    private String extraerPorcentaje(String codigoQR) {
        // Extraer el porcentaje del código QR
        String porcentajeLabel = getString(R.string.porcentaje_de_descuento_a_aplicar);
        int inicio = codigoQR.indexOf(porcentajeLabel) + porcentajeLabel.length();
        int fin = codigoQR.indexOf("%", inicio);
        if (inicio >= 0 && fin >= 0 && fin > inicio) {
            return codigoQR.substring(inicio, fin + 1);
        }
        return null;
    }

    private String extraerFechaExpiracion(String codigoQR) {
        // Extraer la fecha de expiración del código QR
        String fechaLabel = getString(R.string.fecha_de_expiracion_del_codigo);
        int inicio = codigoQR.indexOf(fechaLabel) + fechaLabel.length();
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
            mostrarToast(getString(R.string.press_again_to_exit));
        }
        pressedTime = System.currentTimeMillis();
    }
}