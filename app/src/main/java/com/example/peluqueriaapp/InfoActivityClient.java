/*
Proyecto: Lucía García BeautyBoard
--------------------------------------------------------------------
Autor: David Maestre Díaz
--------------------------------------------------------------------
Versión: 1.0
--------------------------------------------------------------------
Descripción: La clase InfoActivityClient gestiona la interfaz de usuario para mostrar información y contacto de la aplicación "Lucía García BeautyBoard" desde una perspectiva de cliente.

Esta actividad incluye funcionalidades como:
- Visualizar información de contacto, ubicación y equipo.
- Mostrar imágenes de la peluquería en un ViewPager con auto-scroll.
- Permitir la apertura de Google Maps para ver la ubicación del salón.
- Ofrecer enlaces directos a WhatsApp, Instagram y Facebook.

La actividad también incluye un menú lateral con opciones de navegación a otras partes de la aplicación, como la consulta de citas, información adicional y funciones de cierre de sesión.
Además, la actividad gestiona el cierre de sesión del cliente y evita que la aplicación se cierre accidentalmente al presionar el botón de retroceso.
*/

package com.example.peluqueriaapp;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import java.util.ArrayList;
import java.util.List;

public class InfoActivityClient extends AppCompatActivity implements View.OnClickListener, OnMapReadyCallback {

    DrawerLayout drawerLayout;
    ImageView menu;
    TextView titulo;
    LinearLayout home, citas, info, qr, logout;
    String usuarioActivo = "";
    double latitud = 36.683453637083545;
    double longitud = -4.5371127822408175;
    ViewPager2 viewPagerPeluqueria;
    ImageView imageEquipo;
    SupportMapFragment mapFragment;
    Button buttonEquipo, buttonWsp, buttonInstagram, buttonFacebook;
    ImageButton buttonAbrirMapa;
    FirebaseManager firebaseManager;
    GoogleSignInClient mGoogleSignInClient;
    long pressedTime;
    private Handler handler;
    private Runnable runnable;
    private static final long DELAY_MS = 3000; // Tiempo de retardo entre imágenes


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_info_client);
        // Inicializar el controlador de la interfaz
        handler = new Handler();

        // Obtener el usuario activo desde el intent
        usuarioActivo = getIntent().getStringExtra("usuarioActivo");

        firebaseManager = new FirebaseManager();
        mGoogleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN);

        // Asignar las vistas de los layout a los elementos
        setupViews();
        // Actualizar título de ventana
        updateTitle();
        // Configurar OnClickListener de los botones
        setupOnClickListeners();
        // Configurar ViewPager
        setupViewPager();
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

        viewPagerPeluqueria = findViewById(R.id.viewPagerPeluqueria);
        imageEquipo = findViewById(R.id.imageViewEquipo);
        buttonAbrirMapa = findViewById(R.id.btnAbrirMapa);
        buttonEquipo = findViewById(R.id.btnEquipo);
        buttonWsp = findViewById(R.id.btnWhatsApp);
        buttonInstagram = findViewById(R.id.btnInstagram);
        buttonFacebook = findViewById(R.id.btnFacebook);

        // Inicializar el mapa
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void updateTitle() {
        titulo.setText(R.string.informacion_de_contacto);
    }

    private void setupOnClickListeners() {
        menu.setOnClickListener(this);
        home.setOnClickListener(this);
        citas.setOnClickListener(this);
        info.setOnClickListener(this);
        qr.setOnClickListener(this);
        logout.setOnClickListener(this);
        buttonAbrirMapa.setOnClickListener(this);
        buttonEquipo.setOnClickListener(this);
        imageEquipo.setOnClickListener(this);
        buttonWsp.setOnClickListener(this);
        buttonInstagram.setOnClickListener(this);
        buttonFacebook.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.menu) {
            openDrawer(drawerLayout);
        } else if (v.getId() == R.id.home) {
            redirectActivity(this, ReservarActivityClient.class);
        } else if (v.getId() == R.id.citas) {
            redirectActivity(this, ConsultarActivityClient.class, usuarioActivo);
        } else if (v.getId() == R.id.info) {
            recreate();
        } else if (v.getId() == R.id.qr) {
            redirectActivity(this, QrActivityClient.class, usuarioActivo);
        } else if (v.getId() == R.id.logout) {
            firebaseManager.signOut();
            mGoogleSignInClient.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        } else if (v.getId() == R.id.btnAbrirMapa) {
            openMaps();
        } else if (v.getId() == R.id.btnEquipo) {
            // Mostrar carrusel de información del equipo
            if (imageEquipo.getVisibility() == View.VISIBLE) {
                imageEquipo.setVisibility(View.GONE);
            } else {
                imageEquipo.setVisibility(View.VISIBLE);
            }
        } else if (v.getId() == R.id.imageViewEquipo) {
            // Abrir post Equipo
            openUrl(getString(R.string.url_equipo_instagram));
        } else if (v.getId() == R.id.btnWhatsApp) {
            // Abrir WhatsApp
            openUrl(getString(R.string.url_whatsapp));
        } else if (v.getId() == R.id.btnInstagram) {
            // Abrir Instagram
            openUrl(getString(R.string.url_instagram));
        } else if (v.getId() == R.id.btnFacebook) {
            // Abrir Facebook
            openUrl(getString(R.string.url_facebook));
        }
    }

    private void setupViewPager() {
        // Lista de IDs de recursos de imágenes de la Peluquería
        List<Integer> imagenesPeluqueria = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            int resourceId = getResources().getIdentifier("carrusel" + i, "drawable", getPackageName());
            imagenesPeluqueria.add(resourceId);
        }
        // Crear un adaptador para el ViewPager de la Peluquería
        ViewPager2Adapter adapterPeluqueria = new ViewPager2Adapter(imagenesPeluqueria);
        // Establecer el adaptador en el ViewPager de la Peluquería
        viewPagerPeluqueria.setAdapter(adapterPeluqueria);
        // Iniciar el auto-scroll de las imágenes de la Peluquería
        startAutoScroll(imagenesPeluqueria);
    }

    private void startAutoScroll(List<Integer> imagenes) {
        if (runnable == null) {
            runnable = new Runnable() {
                public void run() {
                    int position = viewPagerPeluqueria.getCurrentItem();
                    position = position == imagenes.size() - 1 ? 0 : position + 1;
                    viewPagerPeluqueria.setCurrentItem(position, true);
                    handler.postDelayed(this, DELAY_MS);
                }
            };
        }
        handler.postDelayed(runnable, DELAY_MS);
    }

    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    private void openMaps() {
        // Crear URI con las coordenadas
        Uri gmmIntentUri = Uri.parse("geo:" + latitud + "," + longitud + "?q=Salón Estética y Peluquería Lucía García");

        // Crear un intent para abrir Google Maps con las coordenadas
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        // Verificar si la aplicación de Google Maps está instalada
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            mostrarToast(getString(R.string.no_google_maps));
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        LatLng peluqueriaLatLng = new LatLng(latitud, longitud); // Coordenadas de la peluquería
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(peluqueriaLatLng, 20));
    }

    private void mostrarToast(String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
    }

    public static void openDrawer(DrawerLayout drawerLayout) {
        drawerLayout.openDrawer(GravityCompat.START);
    }

    public static void closeDrawer(DrawerLayout drawerLayout) {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
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