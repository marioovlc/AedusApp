package com.example.aedusapp.models;

// Modelo que representa un Recurso (material del instituto)
public class Recurso {
    private int id;
    private String nombre;
    private int cantidad;
    private String ubicacion;

    // Constructor
    public Recurso(int id, String nombre, int cantidad, String ubicacion) {
        this.id = id;
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.ubicacion = ubicacion;
    }

    // Getters y setters
    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public int getCantidad() {
        return cantidad;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }
}
