package com.example.aedusapp.models;

// Modelo que representa a un Usuario en el sistema
public class Usuario {
    private String id;
    private String nombre;
    private String email;
    private String password;
    private String status; // EJ: ACTIVE, PENDING
    private String role; // e.g. "admin", "user"
    private javafx.beans.property.IntegerProperty aeducoins;
    private String fotoPerfil;
    private byte[] fotoPerfilDatos;
    private String telefono;
    private String bio;

    // Constructor actualizado
    public Usuario(String id, String nombre, String email, String password, String status, String role, int aeducoins, String fotoPerfil, byte[] fotoPerfilDatos, String telefono, String bio) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.password = password;
        this.status = status;
        this.role = role;
        this.aeducoins = new javafx.beans.property.SimpleIntegerProperty(aeducoins);
        this.fotoPerfil = fotoPerfil;
        this.fotoPerfilDatos = fotoPerfilDatos;
        this.telefono = telefono;
        this.bio = bio;
    }

    public Usuario(String id, String nombre, String email, String password, String status, String role, int aeducoins, String fotoPerfil, byte[] fotoPerfilDatos) {
        this(id, nombre, email, password, status, role, aeducoins, fotoPerfil, fotoPerfilDatos, null, null);
    }

    public Usuario(String id, String nombre, String email, String password, String status, String role, int aeducoins, String fotoPerfil) {
        this(id, nombre, email, password, status, role, aeducoins, fotoPerfil, null);
    }

    public Usuario(String id, String nombre, String email, String password, String status, String role, int aeducoins) {
        this(id, nombre, email, password, status, role, aeducoins, null);
    }

    // Constructor de compatibilidad (ya no usa lista de ints para roles)
    public Usuario(String id, String nombre, String email, String password, String status, int roleId) {
        this(id, nombre, email, password, status, roleId == 1 ? "admin" : "user", 0);
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getStatus() {
        return status;
    }

    // Devuelve la lista de roles
    // Devuelve el rol actual como String
    public String getRole() {
        return role;
    }

    // Método de compatibilidad: devuelve 1 si es admin, 2 si es user
    public int getRoleId() {
        if ("admin".equalsIgnoreCase(role)) {
            return 1;
        } else if ("mantenimiento".equalsIgnoreCase(role)) {
            return 3;
        }
        return 2; // Por defecto "user" (Profesor)
    }

    public boolean hasRole(int roleId) {
        return getRoleId() == roleId;
    }

    public boolean hasRole(String roleName) {
        return role != null && role.equalsIgnoreCase(roleName);
    }

    public int getAeducoins() { return aeducoins.get(); }
    public void setAeducoins(int aeducoins) { this.aeducoins.set(aeducoins); }
    public javafx.beans.property.IntegerProperty aeducoinsProperty() { return aeducoins; }

    public String getFotoPerfil() { return fotoPerfil; }
    public void setFotoPerfil(String fotoPerfil) { this.fotoPerfil = fotoPerfil; }
    public byte[] getFotoPerfilDatos() { return fotoPerfilDatos; }
    public void setFotoPerfilDatos(byte[] fotoPerfilDatos) { this.fotoPerfilDatos = fotoPerfilDatos; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}
