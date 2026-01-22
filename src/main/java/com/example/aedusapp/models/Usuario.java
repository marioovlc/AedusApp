package com.example.aedusapp.models;

// Modelo que representa a un Usuario en el sistema
public class Usuario {
    private int id;
    private String nombre;
    private String email;
    private String password;
    private String status; // EJ: ACTIVE, PENDING
    private java.util.List<Integer> roles; // 1=Admin, 2=Profesor, 3=Mantenimiento

    // Constructor actualizado
    public Usuario(int id, String nombre, String email, String password, String status, java.util.List<Integer> roles) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.password = password;
        this.status = status;
        this.roles = roles;
    }

    // Constructor de compatibilidad (convierte int único a lista)
    public Usuario(int id, String nombre, String email, String password, String status, int roleId) {
        this(id, nombre, email, password, status,
                new java.util.ArrayList<>(java.util.Collections.singletonList(roleId)));
    }

    public int getId() {
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
    public java.util.List<Integer> getRoles() {
        return roles;
    }

    // Método de compatibilidad: devuelve el primer rol (o 0 si no hay)
    public int getRoleId() {
        if (roles != null && !roles.isEmpty()) {
            return roles.get(0);
        }
        return 0;
    }

    public boolean hasRole(int roleId) {
        return roles != null && roles.contains(roleId);
    }
}
