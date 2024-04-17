package com.example.peluqueriaapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.FirebaseNetworkException;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseManager {

    private static final String TAG = "FirebaseManager";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    public FirebaseManager() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    // Método para iniciar sesión con correo electrónico y contraseña
    public void signInWithEmailAndPassword(String email, String password, final AuthListener listener) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            listener.onAuthSuccess();
                        } else {
                            Exception exception = task.getException();
                            if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                                // Correo electrónico o contraseña incorrectos
                                listener.onAuthFailure("Correo electrónico o contraseña incorrectos");
                            } else if (exception instanceof FirebaseNetworkException) {
                                // Error de conexión
                                listener.onAuthFailure("Error de red. Verifica tu conexión a internet");
                            } else {
                                // Error desconocido
                                listener.onAuthFailure("Error desconocido al iniciar sesión");
                            }
                        }
                    }
                });
    }

    // Método para iniciar sesión con Google
    public void signInWithGoogle(String idToken, final AuthListener listener) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                final String email = user.getEmail();
                                // Comprobar si ya existe un documento con el mismo email como nombre de documento
                                db.collection("usuarios").document(email)
                                        .get()
                                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                            @Override
                                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                if (documentSnapshot.exists()) {
                                                    // El usuario ya está registrado en Firestore
                                                    listener.onAuthSuccess();
                                                } else {
                                                    // El usuario no está registrado, agregarlo a Firestore
                                                    agregarDatosGoogleFirestore(email);
                                                    listener.onAuthSuccess();
                                                }
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                // Error al comprobar la existencia del usuario
                                                listener.onAuthFailure(e.getMessage());
                                            }
                                        });
                            } else {
                                listener.onAuthFailure("No se pudo obtener el usuario actual.");
                            }
                        } else {
                            listener.onAuthFailure(task.getException().getMessage());
                        }
                    }
                });
    }

    // Método para obtener el correo electrónico del usuario actual
    public String getCurrentUserEmail() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            return user.getEmail();
        }
        return null;
    }

    public void registrarCita(Cita cita) {
        // Obtener la referencia de la colección "citas" en Firestore
        CollectionReference citasRef = db.collection("citas");

        citasRef.add(cita)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "Cita añadida con ID: " + documentReference.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error al añadir cita", e);
                    }
                });
    }

    // Método para obtener el correo electrónico del usuario actual
    public interface UserNameCallback {
        void onUserNameReceived(String userName);
    }

    public void getCurrentUserName(String email, final UserNameCallback callback) {
        db.collection("usuarios").document(email)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            String userName = documentSnapshot.getString("nombre");
                            callback.onUserNameReceived(userName);
                        } else {
                            callback.onUserNameReceived(null);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error al obtener el nombre: " + e.getMessage());
                        callback.onUserNameReceived(null);
                    }
                });
    }

    // Interfaz para manejar el éxito o el fallo de la autenticación
    public interface AuthListener {
        void onAuthSuccess();
        void onAuthFailure(String errorMessage);
    }

    // Método para deslogear al usuario de la base de datos
    public void signOut() {
        mAuth.signOut();
    }

    public void registerUser(final Context context, final String nombre, final String email, String password, final String telefono, final String genero) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Registro exitoso en Firebase Authentication
                            agregarDatosUsuarioFirestore(context, nombre, email.toLowerCase(), telefono, genero);
                        } else {
                            // Error en el registro en Firebase Authentication
                            Exception exception = task.getException();
                            if (exception instanceof FirebaseAuthUserCollisionException) {
                                // Correo ya registrado
                                Toast.makeText(context, "El correo ya está registrado", Toast.LENGTH_LONG).show();
                            } else if (exception instanceof FirebaseNetworkException) {
                                // Error de red
                                Toast.makeText(context, "Error de red. Verifica tu conexión a internet", Toast.LENGTH_LONG).show();
                            } else {
                                // Otro tipo de error
                                Toast.makeText(context, "Error: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
    }

    private void agregarDatosUsuarioFirestore(final Context context, String nombre, String email, String telefono, String genero) {
        // Crear un nuevo documento con un ID único generado automáticamente
        Map<String, Object> usuario = new HashMap<>();
        usuario.put("nombre", nombre);
        usuario.put("email", email);
        usuario.put("telefono", telefono);
        usuario.put("genero", genero);

        // Añadir el nuevo documento a la colección "usuarios"
        db.collection("usuarios").document(email)
                .set(usuario)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            // Registro exitoso en Firestore
                            Toast.makeText(context, "Registro exitoso", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(context, LoginActivity.class);
                            context.startActivity(intent);
                            ((Activity) context).finish();
                        } else {
                            // Error en el registro en Firestore
                            Toast.makeText(context, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void agregarDatosGoogleFirestore(String email) {
        // Crear un nuevo documento con un ID igual al email
        Map<String, Object> usuario = new HashMap<>();
        usuario.put("nombre", "");
        usuario.put("email", email);
        usuario.put("telefono", "");
        usuario.put("genero", "");

        // Añadir el nuevo documento a la colección "usuarios"
        db.collection("usuarios").document(email)
                .set(usuario)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            // Registro exitoso en Firestore
                            Log.d(TAG, "Usuario agregado a Firestore con éxito");
                        } else {
                            // Error en el registro en Firestore
                            Log.e(TAG, "Error al agregar usuario a Firestore: " + task.getException().getMessage());
                        }
                    }
                });
    }

    // Método para obtener los datos de servicios desde Firestore
    public void obtenerServicios(final ServiciosCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference serviciosRef = db.collection("servicios");

        serviciosRef.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                List<String> servicios = new ArrayList<>();
                for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                    String nombreDocumento = documentSnapshot.getId();
                    if (nombreDocumento.equals("peluqueria")) {
                        servicios.add("Servicios de Peluquería");
                    } else if (nombreDocumento.equals("estetica")) {
                        servicios.add("Servicios de Estética");
                    }

                    // Agregar los servicios específicos del documento
                    Map<String, Object> data = documentSnapshot.getData();
                    for (Map.Entry<String, Object> entry : data.entrySet()) {
                        String servicio = entry.getValue().toString();
                        if (servicio != null) {
                            servicios.add(servicio);
                        }
                    }
                }
                if (!servicios.isEmpty()) {
                    callback.onServiciosObtenidos(servicios);
                } else {
                    callback.onError("No se encontraron datos de servicios.");
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                callback.onError("Error al obtener los datos de servicios: " + e.getMessage());
            }
        });
    }

    public interface ServiciosCallback {
        void onServiciosObtenidos(List<String> servicios);
        void onError(String errorMessage);
    }

    public void verificarCitasPorFecha(String fecha, CitasPorFechaCallback callback) {
        // Consulta para obtener documentos de citas con la fecha proporcionada
        Query query = db.collection("citas").whereEqualTo("fecha", fecha);

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<String> horasReservadas = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    // Obtener la hora de cada cita
                    String hora = document.getString("hora");
                    if (hora != null) {
                        horasReservadas.add(hora);
                    }
                }
                // Llamar al callback con la lista de horas reservadas
                callback.onCitasPorFechaVerificadas(horasReservadas);
            } else {
                // Manejar el error si la consulta no es exitosa
                callback.onError(task.getException().getMessage());
            }
        });
    }

    public interface CitasPorFechaCallback {
        void onCitasPorFechaVerificadas(List<String> horasReservadas);
        void onError(String errorMessage);
    }

    // Método para obtener citas por usuario
    public void obtenerCitasPorUsuario(String usuarioActivo, CitasCallback callback) {
        CollectionReference citasRef = db.collection("citas");

        citasRef.whereEqualTo("usuario", usuarioActivo)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<Cita> citasList = new ArrayList<>();
                            for (DocumentSnapshot document : task.getResult()) {
                                Cita cita = document.toObject(Cita.class);
                                citasList.add(cita);
                            }
                            callback.onCitasObtenidas(citasList);
                        } else {
                            callback.onError(task.getException().getMessage());
                        }
                    }
                });
    }

    // Interfaz para manejar el resultado de obtener citas
    public interface CitasCallback {
        void onCitasObtenidas(List<Cita> citasList);
        void onError(String errorMessage);
    }

    public void buscarCitaPorUsuarioFechaHora(Cita cita, final CitaCallback callback) {
        // Realizar una consulta para encontrar la cita con los datos proporcionados
        CollectionReference citasRef = db.collection("citas");

        citasRef.whereEqualTo("usuario", cita.getUsuario())
                .whereEqualTo("fecha", cita.getFecha())
                .whereEqualTo("hora", cita.getHora())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                // Encontrar la cita correspondiente y llamar al callback
                                Cita citaEncontrada = document.toObject(Cita.class);
                                callback.onSuccess(citaEncontrada);
                                return;
                            }
                        } else {
                            // Llamar al callback en caso de error
                            callback.onError(task.getException().getMessage());
                        }
                    }
                });
    }

    public interface CitaCallback {
        void onSuccess(Cita cita);
        void onError(String errorMessage);
    }

    public void eliminarCita(Cita cita, final Callback callback) {
        // Realizar una consulta para encontrar la cita con los datos proporcionados
        CollectionReference citasRef = db.collection("citas");

        citasRef.whereEqualTo("usuario", cita.getUsuario())
                .whereEqualTo("fecha", cita.getFecha())
                .whereEqualTo("hora", cita.getHora())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                // Encontrar la cita correspondiente y eliminarla
                                document.getReference().delete()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                // Llamar al callback en caso de éxito
                                                callback.onSuccess();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                // Llamar al callback en caso de error
                                                callback.onFailure(e.getMessage());
                                            }
                                        });
                                return;
                            }
                            // Llamar al callback si no se encontró ninguna cita
                            callback.onFailure("No se encontró la cita");
                        } else {
                            // Llamar al callback en caso de error
                            callback.onFailure(task.getException().getMessage());
                        }
                    }
                });
    }

    public interface Callback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

}
