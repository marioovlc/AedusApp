package com.example.aedusapp.models;

// =============================================
//  MODELO: Usuario
//  Representa a un usuario del sistema Aedus.
//  Contiene toda la información de perfil:
//    - Datos de autenticación (email, password hash)
//    - Rol (admin / user / mantenimiento)
//    - Estado de cuenta (ACTIVE, PENDING, INACTIVE)
//    - Moneda virtual (Aeducoins)
//    - Datos de perfil (foto, bio, teléfono)
// =============================================
public class Usuario {

    // --- CAMPOS DE IDENTIDAD Y AUTENTICACIÓN ---
    private String id;
    private String nombre;
    private String email;
    private String password;

    // Estado de la cuenta: ACTIVE, PENDING o INACTIVE
    private String status;

    // Rol del usuario: "admin", "user" o "mantenimiento"
    private String role;

    // --- CAMPOS DE GAMIFICACIÓN ---
    // Se usa IntegerProperty para poder enlazar con la UI JavaFX (binding reactivo)
    private javafx.beans.property.IntegerProperty aeducoins;

    // --- CAMPOS DE PERFIL ---
    private String fotoPerfil;       // URL de la foto almacenada en la nube
    private byte[] fotoPerfilDatos;  // Bytes de la foto cargada localmente (miniatura)
    private String telefono;
    private String bio;
    private String avatarUrl;        // Columna unificada con la Web (avatar_url)

    // =============================================
    //  CONSTRUCTORES
    //  Encadenados de más completo a más simple
    //  para facilitar la creación desde distintos DAOs.
    // =============================================

    // Constructor completo (incluye teléfono y bio)
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

    // Constructor sin teléfono ni bio
    public Usuario(String id, String nombre, String email, String password, String status, String role, int aeducoins, String fotoPerfil, byte[] fotoPerfilDatos) {
        this(id, nombre, email, password, status, role, aeducoins, fotoPerfil, fotoPerfilDatos, null, null);
    }

    // Constructor sin datos binarios de foto
    public Usuario(String id, String nombre, String email, String password, String status, String role, int aeducoins, String fotoPerfil) {
        this(id, nombre, email, password, status, role, aeducoins, fotoPerfil, null);
    }

    // Constructor mínimo (sin foto)
    public Usuario(String id, String nombre, String email, String password, String status, String role, int aeducoins) {
        this(id, nombre, email, password, status, role, aeducoins, null);
    }

    // Constructor de compatibilidad heredado: mapea roleId (int) al nuevo sistema de String roles
    public Usuario(String id, String nombre, String email, String password, String status, int roleId) {
        this(id, nombre, email, password, status, roleId == 1 ? "admin" : "user", 0);
    }

    // =============================================
    //  GETTERS Y SETTERS
    // =============================================

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // Devuelve el rol actual como String (ej: "admin", "user", "mantenimiento")
    public String getRole() { return role; }

    // Devuelve el rol como entero para compatibilidad con código legado:
    // 1 = admin, 3 = mantenimiento, 2 = user (por defecto)
    public int getRoleId() {
        if ("admin".equals(role)) return 1;
        if ("mantenimiento".equals(role)) return 3;
        return 2;
    }

    // Comprueba si el usuario tiene el rol indicado (por ID numérico)
    public boolean hasRole(int roleId) { return getRoleId() == roleId; }

    // Comprueba si el usuario tiene el rol indicado (por nombre, sin distinción mayúsculas)
    public boolean hasRole(String roleName) { return role != null && role.equalsIgnoreCase(roleName); }

    // --- Aeducoins (moneda virtual, con soporte de binding JavaFX) ---
    public int getAeducoins() { return aeducoins.get(); }
    public void setAeducoins(int aeducoins) { this.aeducoins.set(aeducoins); }
    public javafx.beans.property.IntegerProperty aeducoinsProperty() { return aeducoins; }

    // --- Foto de perfil ---
    public String getFotoPerfil() { return fotoPerfil; }
    public void setFotoPerfil(String fotoPerfil) { this.fotoPerfil = fotoPerfil; }
    public byte[] getFotoPerfilDatos() { return fotoPerfilDatos; }
    public void setFotoPerfilDatos(byte[] fotoPerfilDatos) { this.fotoPerfilDatos = fotoPerfilDatos; }

    // --- Datos adicionales de perfil ---
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}
