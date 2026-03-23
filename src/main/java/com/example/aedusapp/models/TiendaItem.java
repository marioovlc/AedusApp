package com.example.aedusapp.models;

public class TiendaItem {
    private int id;
    private String nombre;
    private int coste;
    private String descripcion;

    public TiendaItem(int id, String nombre, int coste, String descripcion) {
        this.id = id;
        this.nombre = nombre;
        this.coste = coste;
        this.descripcion = descripcion;
    }

    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public int getCoste() { return coste; }
    public String getDescripcion() { return descripcion; }

    @Override
    public String toString() {
        return nombre + " (" + coste + " AC)";
    }
}
