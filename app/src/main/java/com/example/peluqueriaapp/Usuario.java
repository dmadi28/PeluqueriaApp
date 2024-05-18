package com.example.peluqueriaapp;

public class Usuario {
    private String email;
    private String genero;
    private String nombre;
    private String rol;
    private String telefono;

    public Usuario() {
        // Constructor vacío requerido para Firebase
    }

    public Usuario(String email, String genero, String nombre, String rol, String telefono) {
        this.email = email;
        this.genero = genero;
        this.nombre = nombre;
        this.rol = rol;
        this.telefono = telefono;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }
}

