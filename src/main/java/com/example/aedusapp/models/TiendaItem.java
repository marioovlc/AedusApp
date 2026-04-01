package com.example.aedusapp.models;

public class TiendaItem {
    private int id;
    private String nombre;
    private int coste;
    private String descripcion;
    private String icon;
    private String color;

    public TiendaItem(int id, String nombre, int coste, String descripcion, String icon, String color) {
        this.id = id;
        this.nombre = nombre;
        this.coste = coste;
        this.descripcion = descripcion;
        this.icon = icon;
        this.color = color;
    }

    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public int getCoste() { return coste; }
    public String getDescripcion() { return descripcion; }
    public String getIcon() { return icon; }
    public String getColor() { return color; }

    @Override
    public String toString() {
        return nombre + " (" + coste + " AC)";
    }
}
