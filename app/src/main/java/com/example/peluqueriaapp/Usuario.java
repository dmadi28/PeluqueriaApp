/*
Proyecto: Lucía García BeautyBoard
--------------------------------------------------------------------
Autor: David Maestre Díaz
--------------------------------------------------------------------
Versión: 1.0
--------------------------------------------------------------------
Descripción: La clase Usuario representa un usuario en la aplicación Lucía García BeautyBoard.

Cada usuario tiene los siguientes atributos: email (la dirección de correo electrónico del usuario), genero (el género del usuario), nombre (el nombre del usuario), rol (el rol del usuario, como administrador o cliente) y telefono (el número de teléfono del usuario).

La clase incluye un constructor vacío necesario para la deserialización de Firebase, así como un constructor que inicializa todos los atributos del usuario.
Además, se proporcionan métodos getters y setters para acceder y modificar cada atributo del usuario.
*/

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

