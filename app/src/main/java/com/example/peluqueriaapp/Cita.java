/*
Proyecto: Lucía García BeautyBoard
--------------------------------------------------------------------
Autor: David Maestre Díaz
--------------------------------------------------------------------
Versión: 1.0
--------------------------------------------------------------------
Descripción: La clase Cita representa una cita en la aplicación Lucía García BeautyBoard.

Cada cita tiene los siguientes atributos: usuario (el nombre del usuario que reservó la cita), servicio (el tipo de servicio reservado), precio (el precio del servicio), fecha (la fecha de la cita), hora (la hora de la cita) y anotaciones (cualquier nota adicional asociada a la cita).

La clase incluye un constructor vacío necesario para la deserialización de Firebase, así como un constructor que inicializa todos los atributos de la cita.
Además, se proporcionan métodos getters y setters para acceder y modificar cada atributo de la cita.
*/

package com.example.peluqueriaapp;

public class Cita {
    private String usuario;
    private String servicio;
    private float precio;
    private String fecha;
    private String hora;
    private String anotaciones;

    // Constructor vacío necesario para la deserialización de Firebase
    public Cita() {
    }

    // Constructor
    public Cita(String usuario, String servicio, float precio, String fecha, String hora, String anotaciones) {
        this.usuario = usuario;
        this.servicio = servicio;
        this.precio = precio;
        this.fecha = fecha;
        this.hora = hora;
        this.anotaciones = anotaciones;
    }

    // Métodos getters y setters
    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getServicio() {
        return servicio;
    }

    public void setServicio(String servicio) {
        this.servicio = servicio;
    }

    public float getPrecio() {
        return precio;
    }

    public void setPrecio(float precio) {
        this.precio = precio;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getHora() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora = hora;
    }

    public String getAnotaciones() {
        return anotaciones;
    }

    public void setAnotaciones(String anotaciones) {
        this.anotaciones = anotaciones;
    }
}

