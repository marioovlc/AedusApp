package com.example.aedusapp.models;

public class Aula {
    private int id;
    private String nombre;
    private String tipo; // e.g., Informática, General...
    private int capacidad;

    public Aula() {}

    public Aula(int id, String nombre, String tipo, int capacidad) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.capacidad = capacidad;
    }

    // Constructor de compatibilidad
    public Aula(int id, String nombre, String tipo) {
        this(id, nombre, tipo, 30);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public int getCapacidad() { return capacidad; }
    public void setCapacidad(int capacidad) { this.capacidad = capacidad; }

    @Override
    public String toString() {
        return nombre + " (" + (tipo != null ? tipo : "General") + ")";
    }
}
