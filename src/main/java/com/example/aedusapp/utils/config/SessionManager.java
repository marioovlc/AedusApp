package com.example.aedusapp.utils.config;

import com.example.aedusapp.models.Usuario;

/**
 * Singleton class to manage the global session of the current logged-in user.
 */
public class SessionManager {

    private static SessionManager instance;
    private Usuario usuarioActual;

    private SessionManager() {
        // Constructor privado para el patrón Singleton
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public Usuario getUsuarioActual() {
        return usuarioActual;
    }

    public void setUsuarioActual(Usuario usuario) {
        this.usuarioActual = usuario;
    }

    public void cleanSession() {
        this.usuarioActual = null;
    }

    public boolean isUserLoggedIn() {
        return usuarioActual != null;
    }
}
