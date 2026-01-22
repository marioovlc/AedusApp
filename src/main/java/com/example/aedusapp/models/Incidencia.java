package com.example.aedusapp.models;

// Modelo que representa una Incidencia (problema reportado)
public class Incidencia {
    private int id;
    private String titulo;
    private String descripcion;
    private String estado;
    private int usuarioId;
    private int aulaId;
    private int categoriaId;
    private java.sql.Timestamp fechaCreacion;
    private String imagenRuta; // Ruta de la imagen adjunta
    private String categoriaNombre; // Nombre de la categoría (para mostrar)
    private String aulaNombre; // Nombre del aula (para mostrar)

    public Incidencia() {
    }

    public Incidencia(int id, String titulo, String descripcion, String estado, int usuarioId, int aulaId,
            int categoriaId, java.sql.Timestamp fechaCreacion) {
        this.id = id;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.estado = estado;
        this.usuarioId = usuarioId;
        this.aulaId = aulaId;
        this.categoriaId = categoriaId;
        this.fechaCreacion = fechaCreacion;
    }

    // Constructor de compatibilidad
    public Incidencia(int id, String titulo, String descripcion, String estado) {
        this(id, titulo, descripcion, estado, 0, 0, 0, null);
    }

    // Getters y setters
    public int getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getEstado() {
        return estado;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public int getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(int usuarioId) {
        this.usuarioId = usuarioId;
    }

    public int getAulaId() {
        return aulaId;
    }

    public void setAulaId(int aulaId) {
        this.aulaId = aulaId;
    }

    public int getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(int categoriaId) {
        this.categoriaId = categoriaId;
    }

    public java.sql.Timestamp getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(java.sql.Timestamp fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    // Nuevo campo para mostrar quién creó la incidencia
    private String creadorNombre;

    public String getCreadorNombre() {
        return creadorNombre;
    }

    public void setCreadorNombre(String creadorNombre) {
        this.creadorNombre = creadorNombre;
    }

    public String getImagenRuta() {
        return imagenRuta;
    }

    public void setImagenRuta(String imagenRuta) {
        this.imagenRuta = imagenRuta;
    }

    public String getCategoriaNombre() {
        return categoriaNombre;
    }

    public void setCategoriaNombre(String categoriaNombre) {
        this.categoriaNombre = categoriaNombre;
    }

    public String getAulaNombre() {
        return aulaNombre;
    }

    public void setAulaNombre(String aulaNombre) {
        this.aulaNombre = aulaNombre;
    }
}
