package com.example.aedusapp.models;

import java.sql.Timestamp;

public class Mensaje {
    private int id;
    private int incidenciaId;
    private String usuarioId;   // ID of the user who sent it
    private String nombre;      // Name of the user (for UI)
    private byte[] avatarDatos; // Avatar data (for UI)
    private String texto;
    private String imagenUrl;
    private String audioUrl;
    private String receptorId;  // Add this for direct messages
    private Integer ticketLinkId; // Add this for shared tickets
    private Timestamp fecha;

    private boolean leido;
    private boolean isSoporte;

    public Mensaje(int id, int incidenciaId, String usuarioId, String nombre, byte[] avatarDatos, String texto, String imagenUrl, Timestamp fecha, boolean leido, boolean isSoporte) {
        this.id = id;
        this.incidenciaId = incidenciaId;
        this.usuarioId = usuarioId;
        this.nombre = nombre;
        this.avatarDatos = avatarDatos;
        this.texto = texto;
        this.imagenUrl = imagenUrl;
        this.audioUrl = null;
        this.receptorId = null;
        this.ticketLinkId = null;
        this.fecha = fecha;
        this.leido = leido;
        this.isSoporte = isSoporte;
    }

    public void setReceptorId(String receptorId) { this.receptorId = receptorId; }
    public String getReceptorId() { return receptorId; }
    public void setTicketLinkId(Integer ticketLinkId) { this.ticketLinkId = ticketLinkId; }
    public Integer getTicketLinkId() { return ticketLinkId; }

    public int getId() { return id; }
    public void setIncidenciaId(int incidenciaId) { this.incidenciaId = incidenciaId; }
    public int getIncidenciaId() { return incidenciaId; }
    public String getUsuarioId() { return usuarioId; }
    public String getNombre() { return nombre; }
    public byte[] getAvatarDatos() { return avatarDatos; }
    public String getTexto() { return texto; }
    public String getImagenUrl() { return imagenUrl; }
    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
    public Timestamp getFecha() { return fecha; }
    public boolean isLeido() { return leido; }
    public void setLeido(boolean leido) { this.leido = leido; }
    public boolean isSoporte() { return isSoporte; }
    public void setSoporte(boolean isSoporte) { this.isSoporte = isSoporte; }
}
