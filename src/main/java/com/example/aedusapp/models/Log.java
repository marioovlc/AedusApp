package com.example.aedusapp.models;

import java.sql.Timestamp;

// Modelo que representa un registro de log del sistema
public class Log {
    private int id;
    private String usuarioId;
    private String usuarioNombre; // Para mostrar en la UI
    private String accion;
    private String categoria;
    private String descripcion;
    private String ipAddress;
    private Timestamp fechaCreacion;

    // Constructores
    public Log() {
    }

    public Log(String usuarioId, String accion, String categoria, String descripcion) {
        this.usuarioId = usuarioId;
        this.accion = accion;
        this.categoria = categoria;
        this.descripcion = descripcion;
    }

    public Log(String usuarioId, String accion, String categoria, String descripcion, String ipAddress) {
        this.usuarioId = usuarioId;
        this.accion = accion;
        this.categoria = categoria;
        this.descripcion = descripcion;
        this.ipAddress = ipAddress;
    }

    // Getters y Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getUsuarioNombre() {
        return usuarioNombre;
    }

    public void setUsuarioNombre(String usuarioNombre) {
        this.usuarioNombre = usuarioNombre;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Timestamp getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Timestamp fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
