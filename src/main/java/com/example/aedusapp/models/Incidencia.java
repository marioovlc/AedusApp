package com.example.aedusapp.models;

// Modelo que representa una Incidencia (problema reportado)
public class Incidencia {
    private int id;
    private String titulo;
    private String descripcion;
    private String estado;
    private String usuarioId;
    private int aulaId;
    private int categoriaId;
    private java.sql.Timestamp fechaCreacion;
    private String imagenUrl; // URL/ruta de la imagen adjunta (unificado con Web)
    private String categoriaNombre; // Nombre de la categoría (para mostrar)
    private String aulaNombre; // Nombre del aula (para mostrar)
    private String aulaTipo; // Tipo de aula (Informática, Matemáticas, etc.)
    private String resolucion;
    private String creadorNombre; // Nombre del usuario creador (para admin)

    public Incidencia() {
    }

    public Incidencia(int id, String titulo, String descripcion, String estado, String usuarioId, int aulaId,
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
        this(id, titulo, descripcion, estado, null, 0, 0, null);
    }

    // Getters y setters
    public String getResolucion() {
        return resolucion;
    }

    public void setResolucion(String resolucion) {
        this.resolucion = resolucion;
    }

    public String getCreadorNombre() {
        return creadorNombre;
    }

    public void setCreadorNombre(String creadorNombre) {
        this.creadorNombre = creadorNombre;
    }

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

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
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


    // Técnico asignado a resolver la incidencia
    private String asignadoNombre;

    public String getAsignadoNombre() {
        return asignadoNombre;
    }

    public void setAsignadoNombre(String asignadoNombre) {
        this.asignadoNombre = asignadoNombre;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
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

    public String getAulaTipo() {
        return aulaTipo;
    }

    public void setAulaTipo(String aulaTipo) {
        this.aulaTipo = aulaTipo;
    }
}
