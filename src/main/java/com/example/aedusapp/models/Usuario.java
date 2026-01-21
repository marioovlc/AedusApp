package com.example.aedusapp.models;

// Modelo que representa a un Usuario en el sistema
public class Usuario {
    private int id;
    private String nombre;
    private String email;
    private String password;
    private String status; // EJ: ACTIVE, PENDING
    private int roleId; // 1=Admin, 2=Profesor

    // Constructor: Crea un nuevo objeto Usuario con estos datos
    public Usuario(int id, String nombre, String email, String password, String status, int roleId) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.password = password;
        this.status = status;
        this.roleId = roleId;
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

    public int getRoleId() {
        return roleId;
    }
}
