package com.example.aedusapp.models;

/**
 * Enum que representa las categorías posibles de una Incidencia.
 * Relaciona el nombre visible con el ID almacenado en la base de datos,
 * eliminando los switch/case con Strings mágicos del Controlador.
 */
public enum CategoriaIncidencia {
    HARDWARE("Hardware", 1),
    SOFTWARE("Software", 2),
    CONECTIVIDAD("Conectividad", 3),
    MOBILIARIO("Mobiliario", 4);

    private final String nombre;
    private final int id;

    CategoriaIncidencia(String nombre, int id) {
        this.nombre = nombre;
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public int getId() {
        return id;
    }

    /**
     * Convierte un String de nombre a su Enum correspondiente.
     * @throws IllegalArgumentException si el nombre no es reconocido.
     */
    public static CategoriaIncidencia fromNombre(String nombre) {
        if (nombre == null) throw new IllegalArgumentException("El nombre de categoría no puede ser nulo.");
        for (CategoriaIncidencia c : values()) {
            if (c.nombre.equalsIgnoreCase(nombre)) return c;
        }
        throw new IllegalArgumentException("Categoría desconocida: " + nombre);
    }

    @Override
    public String toString() {
        return nombre;
    }
}
