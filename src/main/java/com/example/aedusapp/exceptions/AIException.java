package com.example.aedusapp.exceptions;

/**
 * Excepción personalizada que representa fallos al comunicarse con la API de Inteligencia Artificial.
 * Permite que los controladores distingan los errores de IA de otros errores del sistema.
 */
public class AIException extends RuntimeException {

    private final int httpStatus;

    public AIException(String message) {
        super(message);
        this.httpStatus = -1;
    }

    public AIException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = -1;
    }

    public AIException(String message, int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    /** Devuelve el código HTTP de la respuesta fallida, o -1 si fue un error de red/conexión. */
    public int getHttpStatus() {
        return httpStatus;
    }

    /** Indica si el error fue un fallo transitorio de servidor que puede reintentarse. */
    public boolean isRetryable() {
        return httpStatus == 429 || httpStatus == 500 || httpStatus == 503;
    }
}
