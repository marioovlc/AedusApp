package com.example.aedusapp.utils.config;

import com.example.aedusapp.models.Usuario;
import java.io.*;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton class to manage the global session of the current logged-in user.
 */
public class SessionManager {

    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);
    private static final String SESSION_FILE = "session.properties";
    
    private static SessionManager instance;
    private Usuario usuarioActual;

    private SessionManager() {
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

    public void saveSession(Usuario usuario) {
        this.usuarioActual = usuario;
        Properties props = new Properties();
        props.setProperty("last_user_id", usuario.getId());
        props.setProperty("last_user_email", usuario.getEmail());
        props.setProperty("timestamp", String.valueOf(System.currentTimeMillis()));
        
        try (FileOutputStream out = new FileOutputStream(SESSION_FILE)) {
            props.store(out, "Aedus Persistent Session");
            logger.info("Sesión guardada para el usuario: {}", usuario.getEmail());
        } catch (IOException e) {
            logger.error("Error al guardar la sesión: {}", e.getMessage());
        }
    }

    public String getSavedUserId() {
        File file = new File(SESSION_FILE);
        if (!file.exists()) return null;
        
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            props.load(in);
            return props.getProperty("last_user_id");
        } catch (IOException e) {
            return null;
        }
    }

    public void cleanSession() {
        this.usuarioActual = null;
        File file = new File(SESSION_FILE);
        if (file.exists()) file.delete();
        logger.info("Sesión persistente eliminada.");
    }

    public boolean isUserLoggedIn() {
        return usuarioActual != null;
    }
}
