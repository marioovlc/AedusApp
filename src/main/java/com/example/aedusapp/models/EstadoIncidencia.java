package com.example.aedusapp.models;

/**
 * Enum que representa los estados posibles de una Incidencia.
 * Elimina los "String mágicos" poco seguros (ej. "ACABADO") del código.
 */
public enum EstadoIncidencia {
    NO_LEIDO("NO LEIDO"),
    LEIDO("LEIDO"),
    EN_REVISION("EN REVISION"),
    ACABADO("ACABADO");

    private final String dbValue;

    EstadoIncidencia(String dbValue) {
        this.dbValue = dbValue;
    }

    /** Devuelve el valor exacto almacenado en la base de datos. */
    public String getDbValue() {
        return dbValue;
    }

    /**
     * Convierte un String de la BBDD al Enum correspondiente.
     * Si no reconoce el valor, devuelve null en lugar de lanzar una excepción.
     */
    public static EstadoIncidencia fromDbValue(String value) {
        if (value == null) return null;
        for (EstadoIncidencia e : values()) {
            if (e.dbValue.equalsIgnoreCase(value)) {
                return e;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return dbValue;
    }
}
