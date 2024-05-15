package com.example.peluqueriaapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.util.Calendar;
import java.util.Date;

public class GenerarQrActivity extends AppCompatActivity implements View.OnClickListener {

    DrawerLayout drawerLayout;
    ImageView menu;
    TextView titulo;
    LinearLayout home, citas, info, qr, logout;
    CardView cardViewQr;
    Button buttonGenerarQr;
    Spinner spinnerPorcentaje, spinnerDuracion;
    ImageView imagenQr;
    FloatingActionButton fabCompartirQR;
    String porcentajeSeleccionado = "";
    String duracionSeleccionada = "";
    String usuarioActivo = "";
    FirebaseManager firebaseManager;
    GoogleSignInClient mGoogleSignInClient;
    long pressedTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_generar_qr);

        usuarioActivo = getIntent().getStringExtra("usuarioActivo");

        firebaseManager = new FirebaseManager();
        mGoogleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN);

        // Asignar las vistas de los layout a los elementos
        setupViews();
        // Actualizar título de ventana
        updateTitle();
        // Configurar OnClickListener de los botones
        setupOnClickListeners();
        // Configurar Spinner de porcentaje
        setupSpinnerPorcentaje();
        // Configurar Spinner de duración
        setupSpinnerDuracion();
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

        spinnerPorcentaje = findViewById(R.id.spinnerPorcentaje);
        spinnerDuracion = findViewById(R.id.spinnerDuracion);
        buttonGenerarQr = findViewById(R.id.btnGenerarQR);
        cardViewQr = findViewById(R.id.cardViewQR);
        imagenQr = findViewById(R.id.imageQR);
        fabCompartirQR = findViewById(R.id.fabCompartirQR);
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
        logout.setOnClickListener(this);
        buttonGenerarQr.setOnClickListener(this);
        fabCompartirQR.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.menu) {
            openDrawer(drawerLayout);
        } else if (v.getId() == R.id.home) {
            firebaseManager.checkAdminUser(firebaseManager.getCurrentUserEmail(), isAdmin -> {
                if (isAdmin) {
                    redirectActivity(this, ReservarActivity.class);
                } else {
                    redirectActivity(this, ReservarActivityClient.class);
                }
            });
        } else if (v.getId() == R.id.citas) {
            firebaseManager.checkAdminUser(firebaseManager.getCurrentUserEmail(), isAdmin -> {
                if (isAdmin) {
                    redirectActivity(this, ConsultarActivity.class, usuarioActivo);
                } else {
                    redirectActivity(this, ConsultarActivityClient.class, usuarioActivo);
                }
            });
        } else if (v.getId() == R.id.info) {
            redirectActivity(this, InfoActivity.class, usuarioActivo);
        } else if (v.getId() == R.id.qr) {
            firebaseManager.checkAdminUser(firebaseManager.getCurrentUserEmail(), isAdmin -> {
                if (isAdmin) {
                    redirectActivity(this, QrActivity.class, usuarioActivo);
                } else {
                    redirectActivity(this, QrActivityClient.class, usuarioActivo);
                }
            });
        } else if (v.getId() == R.id.logout) {
            firebaseManager.signOut();
            mGoogleSignInClient.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        } else if (v.getId() == R.id.btnGenerarQR) {
            generarQr();
        } else if (v.getId() == R.id.fabCompartirQR) {
            compartirQr();
        }
    }

    // Método para configurar el Spinner de porcentaje
    private void setupSpinnerPorcentaje() {
        // Opciones de porcentajes de descuento
        String[] descuentos = getResources().getStringArray(R.array.descuentos);

        // Crea un adaptador para el Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, descuentos);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Asigna el adaptador al Spinner
        spinnerPorcentaje.setAdapter(adapter);

        // Configura el listener para el Spinner
        spinnerPorcentaje.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                porcentajeSeleccionado = descuentos[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    // Método para configurar el Spinner de duración
    private void setupSpinnerDuracion() {
        // Opciones de duración
        String[] duraciones = getResources().getStringArray(R.array.duraciones);

        // Crea un adaptador para el Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, duraciones);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Asigna el adaptador al Spinner
        spinnerDuracion.setAdapter(adapter);

        // Configura el listener para el Spinner
        spinnerDuracion.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                duracionSeleccionada = duraciones[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    // Método para guardar la info del código de descuento y generar el Qr
    private void generarQr() {
        // Hacer visible el botón de compartir
        fabCompartirQR.setVisibility(View.VISIBLE);

        // Calcular la fecha de expiración basada en la duración seleccionada en el Spinner
        Calendar calendar = Calendar.getInstance();
        if (duracionSeleccionada.equals(getString(R.string.one_day))) {
            calendar.add(Calendar.DATE, 1);
        } else if (duracionSeleccionada.equals(getString(R.string.three_months))) {
            calendar.add(Calendar.MONTH, 3);
        } else if (duracionSeleccionada.equals(getString(R.string.six_months))) {
            calendar.add(Calendar.MONTH, 6);
        } else if (duracionSeleccionada.equals(getString(R.string.one_year))) {
            calendar.add(Calendar.YEAR, 1);
        } else if (duracionSeleccionada.equals(getString(R.string.two_years))) {
            calendar.add(Calendar.YEAR, 2);
        }
        Date fechaExpiracion = calendar.getTime();

        // Formatear la fecha como un texto con el formato "año-mes-día"
        String textoFechaExpiracion = formatDate(fechaExpiracion);

        // Generar el código QR con el porcentaje seleccionado
        String textoQR = getString(R.string.porcentaje_de_descuento_a_aplicar) + porcentajeSeleccionado + "\n" +
                getString(R.string.fecha_de_expiracion_del_codigo) + textoFechaExpiracion + "\n";
        generarCodigoQR(textoQR);

        firebaseManager.crearCodigo(porcentajeSeleccionado, textoFechaExpiracion);

        mostrarToast(getString(R.string.codigo_generado_correctamente));

        // Cambiar la visibilidad de la tarjeta
        cardViewQr.setVisibility(View.VISIBLE);
    }

    // Método para formatear una fecha como un texto con el formato "año-mes-día"
    private String formatDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1; // Sumar 1 porque los meses van de 0 a 11
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        return String.format("%d-%02d-%02d", year, month, day);
    }

    // Método para generar el código QR
    private void generarCodigoQR(String texto) {
        int tamanio = 800;
        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(texto,BarcodeFormat.QR_CODE, tamanio, tamanio);
            imagenQr.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Método para compartir el código QR
    private void compartirQr() {
        // Obtener el URI de la imagen del código QR
        Uri uri = getUriFromImageView(imagenQr);

        // Crear un Intent para compartir
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.compartir_codigo)));
    }

    // Método para obtener el URI de un ImageView
    private Uri getUriFromImageView(ImageView imageView) {
        BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
        Bitmap bitmap = drawable.getBitmap();

        // Guardar el bitmap en el almacenamiento interno y obtener su URI
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "QR_Code", null);
        return Uri.parse(path);
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