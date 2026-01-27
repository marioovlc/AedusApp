package com.example.aedusapp.database;

import com.example.aedusapp.models.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Clase DAO (Data Access Object) para gestionar Usuarios en la Base de Datos
public class UsuarioDAO {

    // Obtener una lista de usuarios filtrada por su estado (ACTIVE, PENDING, etc.)
    public List<Usuario> obtenerUsuariosPorEstado(String status) {
        List<Usuario> usuarios = new ArrayList<>();
        String sql = "SELECT * FROM usuarios WHERE status = ?";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                usuarios.add(new Usuario(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("status"),
                        rs.getInt("rol_id")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return usuarios;
    }

    // Obtener TODOS los usuarios sin importar el estado
    public List<Usuario> obtenerTodosLosUsuarios() {
        List<Usuario> usuarios = new ArrayList<>();
        String sql = "SELECT * FROM usuarios";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                usuarios.add(new Usuario(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("status"),
                        rs.getInt("rol_id")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return usuarios;
    }

    // Actualizar los datos de un usuario existente
    public boolean actualizarUsuario(Usuario usuario) {
        String sql = "UPDATE usuarios SET nombre=?, email=?, status=?, password=? WHERE id=?";
        try (Connection conn = DBConnection.getConnection()) {
            if (conn != null) {
                conn.setAutoCommit(false); // Iniciar transacción
                try {
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setString(1, usuario.getNombre());
                    stmt.setString(2, usuario.getEmail());
                    stmt.setString(3, usuario.getStatus());
                    stmt.setString(4, usuario.getPassword());
                    stmt.setInt(5, usuario.getId());
                    stmt.executeUpdate();

                    // Actualizar roles
                    String sqlDeleteRoles = "DELETE FROM usuario_roles WHERE usuario_id = ?";
                    try (PreparedStatement delStmt = conn.prepareStatement(sqlDeleteRoles)) {
                        delStmt.setInt(1, usuario.getId());
                        delStmt.executeUpdate();
                    }

                    String sqlInsertRol = "INSERT INTO usuario_roles (usuario_id, rol_id) VALUES (?, ?)";
                    try (PreparedStatement insStmt = conn.prepareStatement(sqlInsertRol)) {
                        for (Integer roleId : usuario.getRoles()) {
                            insStmt.setInt(1, usuario.getId());
                            insStmt.setInt(2, roleId);
                            insStmt.executeUpdate();
                        }
                    }

                    conn.commit();
                    return true;
                } catch (SQLException e) {
                    conn.rollback();
                    e.printStackTrace();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private List<Integer> obtenerRolesUsuario(Connection conn, int userId) throws SQLException {
        List<Integer> roles = new ArrayList<>();
        String sql = "SELECT rol_id FROM usuario_roles WHERE usuario_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                roles.add(rs.getInt("rol_id"));
            }
        }
        return roles;
    }

    // Eliminar un usuario por su ID
    public boolean eliminarUsuario(int id) {
        String sql = "DELETE FROM usuarios WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Validar usuario (Login): Busca email y contraseña y que esté ACTIVO
    public Usuario validarUsuario(String email, String password) {
        String sql = "SELECT * FROM usuarios WHERE email = ? AND password = ? AND status = 'ACTIVE'";
        Usuario usuario = null;

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            stmt.setString(2, password);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");

                    // Obtener roles
                    List<Integer> roles = new ArrayList<>();
                    String sqlRoles = "SELECT rol_id FROM usuario_roles WHERE usuario_id = ?";
                    try (PreparedStatement stmtRoles = conn.prepareStatement(sqlRoles)) {
                        stmtRoles.setInt(1, id);
                        try (ResultSet rsRoles = stmtRoles.executeQuery()) {
                            while (rsRoles.next()) {
                                roles.add(rsRoles.getInt("rol_id"));
                            }
                        }
                    }

                    if (roles.isEmpty() && rs.getInt("rol_id") != 0) {
                        roles.add(rs.getInt("rol_id"));
                    }

                    usuario = new Usuario(
                            id,
                            rs.getString("nombre"),
                            rs.getString("email"),
                            rs.getString("password"),
                            rs.getString("status"),
                            roles);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return usuario;
    }

    // Registrar un nuevo usuario (Por defecto PENDING)
    public boolean registrarUsuario(Usuario usuario) {
        String sql = "INSERT INTO usuarios (nombre, email, password, status) VALUES (?, ?, ?, 'PENDING') RETURNING id";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, usuario.getNombre());
            stmt.setString(2, usuario.getEmail());
            stmt.setString(3, usuario.getPassword());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int newUserId = rs.getInt(1);

                    String sqlRol = "INSERT INTO usuario_roles (usuario_id, rol_id) VALUES (?, ?)";
                    try (PreparedStatement stmtRol = conn.prepareStatement(sqlRol)) {
                        List<Integer> roles = usuario.getRoles();
                        if (roles == null || roles.isEmpty()) {
                            int legacyRole = usuario.getRoleId();
                            if (legacyRole > 0) {
                                stmtRol.setInt(1, newUserId);
                                stmtRol.setInt(2, legacyRole);
                                stmtRol.executeUpdate();
                            }
                        } else {
                            for (Integer roleId : roles) {
                                stmtRol.setInt(1, newUserId);
                                stmtRol.setInt(2, roleId);
                                stmtRol.executeUpdate();
                            }
                        }
                    }
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
