package com.example.aedusapp.services;

import com.example.aedusapp.database.LogDAO;
import com.example.aedusapp.models.Log;
import com.example.aedusapp.models.Usuario;

// Servicio para registrar eventos del sistema de manera simple
public class LogService {
    private static final LogDAO logDAO = new LogDAO();

    // Categorías de logs
    public static final String CAT_LOGIN = "LOGIN";
    public static final String CAT_INCIDENCIA = "INCIDENCIA";
    public static final String CAT_USUARIO = "USUARIO";
    public static final String CAT_SISTEMA = "SISTEMA";
    public static final String CAT_ERROR = "ERROR";

    // Acciones comunes
    public static final String ACC_LOGIN = "LOGIN";
    public static final String ACC_LOGOUT = "LOGOUT";
    public static final String ACC_CREAR = "CREAR";
    public static final String ACC_ACTUALIZAR = "ACTUALIZAR";
    public static final String ACC_ELIMINAR = "ELIMINAR";
    public static final String ACC_EDITAR = "EDITAR";

    // Login de usuario
    public static void logLogin(Usuario usuario, String ip) {
        Log log = new Log(
                usuario.getId(),
                ACC_LOGIN,
                CAT_LOGIN,
                "Usuario '" + usuario.getNombre() + "' ha iniciado sesión",
                ip);
        logDAO.registrarLog(log);
    }

    // Logout de usuario
    public static void logLogout(Usuario usuario) {
        Log log = new Log(
                usuario.getId(),
                ACC_LOGOUT,
                CAT_LOGIN,
                "Usuario '" + usuario.getNombre() + "' ha cerrado sesión");
        logDAO.registrarLog(log);
    }

    // Crear incidencia
    public static void logCrearIncidencia(Usuario usuario, int incidenciaId, String titulo) {
        Log log = new Log(
                usuario.getId(),
                ACC_CREAR,
                CAT_INCIDENCIA,
                "Creó la incidencia #" + incidenciaId + ": " + titulo);
        logDAO.registrarLog(log);
    }

    // Cambiar estado de incidencia
    public static void logCambiarEstado(Usuario usuario, int incidenciaId, String titulo, String nuevoEstado) {
        Log log = new Log(
                usuario.getId(),
                ACC_ACTUALIZAR,
                CAT_INCIDENCIA,
                "Cambió el estado de la incidencia #" + incidenciaId + " (" + titulo + ") a: " + nuevoEstado);
        logDAO.registrarLog(log);
    }

    // Eliminar incidencia
    public static void logEliminarIncidencia(Usuario usuario, int incidenciaId, String titulo) {
        Log log = new Log(
                usuario.getId(),
                ACC_ELIMINAR,
                CAT_INCIDENCIA,
                "Eliminó la incidencia #" + incidenciaId + ": " + titulo);
        logDAO.registrarLog(log);
    }

    // Crear usuario
    public static void logCrearUsuario(Usuario administrador, String nombreNuevoUsuario) {
        Log log = new Log(
                administrador.getId(),
                ACC_CREAR,
                CAT_USUARIO,
                "Creó el usuario: " + nombreNuevoUsuario);
        logDAO.registrarLog(log);
    }

    // Editar usuario
    public static void logEditarUsuario(Usuario administrador, String nombreUsuarioEditado) {
        Log log = new Log(
                administrador.getId(),
                ACC_EDITAR,
                CAT_USUARIO,
                "Editó el usuario: " + nombreUsuarioEditado);
        logDAO.registrarLog(log);
    }

    // Eliminar usuario
    public static void logEliminarUsuario(Usuario administrador, String nombreUsuarioEliminado) {
        Log log = new Log(
                administrador.getId(),
                ACC_ELIMINAR,
                CAT_USUARIO,
                "Eliminó el usuario: " + nombreUsuarioEliminado);
        logDAO.registrarLog(log);
    }

    // Registrar error del sistema
    public static void logError(String descripcion) {
        Log log = new Log(
                0, // Sin usuario asociado
                "ERROR",
                CAT_ERROR,
                descripcion);
        logDAO.registrarLog(log);
    }

    // Evento general del sistema
    public static void logSistema(String accion, String descripcion) {
        Log log = new Log(
                0,
                accion,
                CAT_SISTEMA,
                descripcion);
        logDAO.registrarLog(log);
    }

    // Método genérico para registrar cualquier log
    public static void log(int usuarioId, String accion, String categoria, String descripcion) {
        Log log = new Log(usuarioId, accion, categoria, descripcion);
        logDAO.registrarLog(log);
    }
}
