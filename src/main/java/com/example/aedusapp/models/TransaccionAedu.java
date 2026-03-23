package com.example.aedusapp.models;

import java.time.LocalDateTime;

public class TransaccionAedu {
    private int id;
    private String usuarioId;
    private int cantidad;
    private String motivo;
    private LocalDateTime fecha;

    public TransaccionAedu(int id, String usuarioId, int cantidad, String motivo, LocalDateTime fecha) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.cantidad = cantidad;
        this.motivo = motivo;
        this.fecha = fecha;
    }

    public int getId() { return id; }
    public String getUsuarioId() { return usuarioId; }
    public int getCantidad() { return cantidad; }
    public String getMotivo() { return motivo; }
    public LocalDateTime getFecha() { return fecha; }
}
