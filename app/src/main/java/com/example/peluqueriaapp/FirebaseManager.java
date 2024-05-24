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
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.FirebaseNetworkException;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.SignInMethodQueryResult;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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

    // Método para registrar una cita en la base de datos
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

    // Método para obtener de la base de datos el nombre del usuario a partir del email
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

    // Método para registrar al usuario en los usuarios de Firebase y por ende, en la lista de usuarios de la base de datos
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

    // Método para agregar a la base de datos un usuario cuando se logea desde la app
    private void agregarDatosUsuarioFirestore(final Context context, String nombre, String email, String telefono, String genero) {
        // Crear un nuevo documento con un ID único generado automáticamente
        Map<String, Object> usuario = new HashMap<>();
        usuario.put("nombre", nombre);
        usuario.put("email", email);
        usuario.put("telefono", telefono);
        usuario.put("genero", genero);
        usuario.put("rol", "usuario");

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

    // Método para agregar a la base de datos un usuario cuando se logea con Google
    private void agregarDatosGoogleFirestore(String email) {
        // Crear un nuevo documento con un ID igual al email
        Map<String, Object> usuario = new HashMap<>();
        usuario.put("nombre", "");
        usuario.put("email", email);
        usuario.put("telefono", "");
        usuario.put("genero", "");
        usuario.put("rol", "usuario");

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

    // Método para restablecer la contraseña en Firebase enviando un correo electrónico de restablecimiento de contraseña
    public void resetPassword(String email, ResetPasswordListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference usersCollection = db.collection("usuarios");

        // Realizar la consulta para verificar si el correo electrónico existe en la base de datos
        usersCollection.whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (task.getResult() != null && !task.getResult().isEmpty()) {
                                // El correo electrónico existe en la base de datos
                                // Envía el correo electrónico para restablecer la contraseña
                                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    listener.onResetSuccess("Correo electrónico enviado");
                                                } else {
                                                    // Error al enviar el correo electrónico de restablecimiento
                                                    listener.onResetFailure("Error al enviar el correo electrónico");
                                                }
                                            }
                                        });
                            } else {
                                // El correo electrónico no existe en la base de datos
                                listener.onResetFailure("El correo electrónico no está registrado");
                            }
                        } else {
                            // Error al realizar la consulta
                            listener.onResetFailure("Error al verificar el correo electrónico");
                        }
                    }
                });
    }

    public interface ResetPasswordListener {
        void onResetSuccess(String message);
        void onResetFailure(String errorMessage);
    }

    // Método para obtener los datos de servicios desde Firestore
    public void obtenerServicios(Context context, final ServiciosCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference serviciosRef;

        // Obtener el código de idioma del dispositivo
        String languageCode = Locale.getDefault().getLanguage();

        // Determinar la colección según el idioma del dispositivo
        if (languageCode.equals("es")) {
            serviciosRef = db.collection("servicios");
        } else {
            serviciosRef = db.collection("serviciosEN");
        }
        serviciosRef.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                List<String> servicios = new ArrayList<>();
                for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                    String nombreDocumento = documentSnapshot.getId();
                    if (nombreDocumento.equals("peluqueria")) {
                        servicios.add(context.getString(R.string.servicios_de_peluqueria));
                    } else if (nombreDocumento.equals("estetica")) {
                        servicios.add(context.getString(R.string.servicios_de_estetica));
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

    // Método para verificar las horas disponibles en una fecha
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

    // Método para obtener las citas de un usuario específico
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

    public void obtenerCitasDesdeFecha(String fecha, CitasCallback callback) {
        CollectionReference citasRef = db.collection("citas");

        citasRef.whereGreaterThanOrEqualTo("fecha", fecha)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
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

    // Método para obtener todas las citas de la base de datos
    public void obtenerTodasLasCitas(CitasCallback callback) {
        CollectionReference citasRef = db.collection("citas");

        citasRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
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

    // Método para buscar una cita a partir de la pinchada en el listView
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

    // Método para eliminar una cita manualmente
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

    // Método para eliminar citas que han pasado ya en el tiempo
    public void eliminarCitasPasadas() {
        // Obtener la fecha y hora actual
        Calendar fechaHoraActual = Calendar.getInstance();

        // Consultar los documentos de la colección "citas"
        db.collection("citas")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                // Obtener la fecha y hora de la cita
                                String fechaCita = document.getString("fecha");
                                String horaCita = document.getString("hora");

                                // Convertir la fecha y hora de la cita a objetos Calendar
                                Calendar fechaHoraCita = convertirFechaHora(fechaCita, horaCita);

                                // Sumar una hora a la fecha y hora de la cita
                                fechaHoraCita.add(Calendar.HOUR_OF_DAY, 1);

                                // Verificar si la fecha y hora actual es posterior a la fecha y hora de la cita + 2 horas
                                if (fechaHoraActual.after(fechaHoraCita)) {
                                    // Eliminar la cita
                                    document.getReference().delete()
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Log.d(TAG, "Cita eliminada correctamente");
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.e(TAG, "Error al eliminar la cita: " + e.getMessage());
                                                }
                                            });
                                }
                            }
                        } else {
                        }
                    }
                });
    }

    // Método para convertir la fecha y hora de String a Calendar
    private Calendar convertirFechaHora(String fecha, String hora) {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        try {
            Date date = sdf.parse(fecha + " " + hora);
            calendar.setTime(date);
        } catch (ParseException e) {
            Log.e(TAG, "Error al convertir la fecha y hora: " + e.getMessage());
        }
        return calendar;
    }

    // Método para crear un nuevo código de desuento
    public void crearCodigo(String porcentaje, String fechaExpiracion, String idUnico) {
        // Crear un nuevo documento en la colección "codigos" con un ID autogenerado
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> codigo = new HashMap<>();
        codigo.put("porcentaje", porcentaje);
        codigo.put("fechaExpiracion", fechaExpiracion);
        codigo.put("id", idUnico);

        db.collection("codigos")
                .add(codigo)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Código de descuento generado con ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error al generar el código", e);
                });
    }

    // Método para comprobar la existencia del código QR en la base de datos
    public void comprobarExistenciaCodigoQR(String idUnico, OnCodigoQRComprobadoListener listener) {
        if (idUnico != null) {
            // Consultar la base de datos para verificar la existencia del código QR
            db.collection("codigos")
                    .whereEqualTo("id", idUnico)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            boolean codigoValido = !task.getResult().isEmpty();
                            Log.d("FirebaseManager", "Código QR encontrado: " + codigoValido);
                            listener.onCodigoQRComprobado(codigoValido);
                        } else {
                            listener.onCodigoQRComprobado(false);
                            Log.e("FirebaseManager", "Error al comprobar la existencia del código QR:", task.getException());
                        }
                    });
        } else {
            listener.onCodigoQRComprobado(false);
            Log.d("FirebaseManager", "ID único nulo. Código QR inválido.");
        }
    }

    // Interfaz para manejar el resultado de la comprobación del código QR
    public interface OnCodigoQRComprobadoListener {
        void onCodigoQRComprobado(boolean existe);
    }

    // Método para eliminar un código QR de la base de datos
    public void eliminarCodigoQR(String idUnico) {
        db.collection("codigos")
                .whereEqualTo("id", idUnico)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            document.getReference().delete()
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Log.d("FirebaseManager", "Documento eliminado correctamente");
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.e("FirebaseManager", "Error al eliminar el documento", e);
                                        }
                                    });
                        }
                    } else {
                        Log.e("FirebaseManager", "Error al obtener documentos", task.getException());
                    }
                });
    }

    // Método para verificar si un usuario es administrador
    public void checkAdminUser(String userEmail, final AdminCheckCallback callback) {
        // Accede a la colección de usuarios en Firebase
        CollectionReference usersCollection = FirebaseFirestore.getInstance().collection("usuarios");

        // Realiza una consulta para encontrar al usuario con el correo electrónico proporcionado
        Query query = usersCollection.whereEqualTo("email", userEmail);

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Verifica si se encontró algún usuario con el correo electrónico proporcionado
                if (!task.getResult().isEmpty()) {
                    // Obtiene el primer documento (debería haber solo uno)
                    DocumentSnapshot document = task.getResult().getDocuments().get(0);

                    // Verifica si el usuario es administrador
                    boolean isAdmin = false;
                    if (document.contains("rol")) {
                        String rol = document.getString("rol");
                        if (rol != null && rol.equals("admin")) {
                            isAdmin = true;
                        }
                    }

                    // Llama al callback con el resultado de la verificación
                    callback.onAdminCheckResult(isAdmin);
                } else {
                    // Si no se encontró ningún usuario con el correo electrónico proporcionado, no es admin
                    callback.onAdminCheckResult(false);
                }
            } else {
                // Si hubo un error en la consulta, se asume que el usuario no es admin
                Log.e("FirebaseManager", "Error al verificar el usuario administrador:", task.getException());
                callback.onAdminCheckResult(false);
            }
        });
    }

    // Interfaz para manejar el resultado de la comprobación de administrador
    public interface AdminCheckCallback {
        void onAdminCheckResult(boolean isAdmin);
    }

    // Método para obtener los usuarios con rol usuario
    public void obtenerUsuariosConRolUsuario(final FirebaseManager.UsuariosCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference usuariosRef = db.collection("usuarios");

        usuariosRef.whereEqualTo("rol", "usuario")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        List<Usuario> usuarios = new ArrayList<>();
                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            Usuario usuario = documentSnapshot.toObject(Usuario.class);
                            usuarios.add(usuario);
                        }
                        callback.onUsuariosObtenidos(usuarios);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        callback.onError(e.getMessage());
                    }
                });
    }

    // Método para obtener los usuarios con rol admin
    public void obtenerUsuariosConRolAdmin(final FirebaseManager.UsuariosCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference usuariosRef = db.collection("usuarios");

        usuariosRef.whereEqualTo("rol", "admin")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        List<Usuario> usuarios = new ArrayList<>();
                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            Usuario usuario = documentSnapshot.toObject(Usuario.class);
                            usuarios.add(usuario);
                        }
                        callback.onUsuariosObtenidos(usuarios);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        callback.onError(e.getMessage());
                    }
                });
    }

    // Interfaz para manejar el resultado de la obtención de usuarios
    public interface UsuariosCallback {
        void onUsuariosObtenidos(List<Usuario> usuarios);
        void onError(String errorMessage);
    }

    // Método para cambiar el rol de un usuario a administrador
    public void cambiarRolUsuarioAdmin(String userEmail, final FirebaseCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference usuariosRef = db.collection("usuarios");

        usuariosRef.whereEqualTo("email", userEmail).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            // Cambiar el rol del usuario a administrador
                            document.getReference().update("rol", "admin")
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            callback.onSuccess();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            callback.onError(e.getMessage());
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        callback.onError(e.getMessage());
                    }
                });
    }

    // Método para cambiar el rol de un administrador a usuario
    public void cambiarRolAdminUsuario(String userEmail, final FirebaseCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference usuariosRef = db.collection("usuarios");

        usuariosRef.whereEqualTo("email", userEmail).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            // Cambiar el rol del administrador a usuario
                            document.getReference().update("rol", "usuario")
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            callback.onSuccess();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            callback.onError(e.getMessage());
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        callback.onError(e.getMessage());
                    }
                });
    }

    // Interfaz de devolución de llamada para manejar el éxito o el error de las operaciones de Firebase
    public interface FirebaseCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

}
