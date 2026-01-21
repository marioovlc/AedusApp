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

        try {
            Connection conn = DBConnection.getConnection(); // Conectar a la BD
            if (conn != null) {
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, status);
                ResultSet rs = stmt.executeQuery(); // Ejecutar consulta

                // Recorrer los resultados y crear objetos Usuario
                while (rs.next()) {
                    usuarios.add(new Usuario(
                            rs.getInt("id"),
                            rs.getString("nombre"),
                            rs.getString("email"),
                            rs.getString("password"),
                            rs.getString("status"),
                            rs.getInt("rol_id")));
                }
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

        try {
            Connection conn = DBConnection.getConnection();
            if (conn != null) {
                PreparedStatement stmt = conn.prepareStatement(sql);
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
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return usuarios;
    }

    // Actualizar los datos de un usuario existente
    public boolean actualizarUsuario(Usuario usuario) {
        String sql = "UPDATE usuarios SET nombre=?, email=?, rol_id=?, status=?, password=? WHERE id=?";
        try {
            Connection conn = DBConnection.getConnection();
            if (conn != null) {
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, usuario.getNombre());
                stmt.setString(2, usuario.getEmail());
                stmt.setInt(3, usuario.getRoleId());
                stmt.setString(4, usuario.getStatus());
                stmt.setString(5, usuario.getPassword());
                stmt.setInt(6, usuario.getId());
                return stmt.executeUpdate() > 0; // Devuelve true si se actualizó algo
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Eliminar un usuario por su ID
    public boolean eliminarUsuario(int id) {
        String sql = "DELETE FROM usuarios WHERE id=?";
        try {
            Connection conn = DBConnection.getConnection();
            if (conn != null) {
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setInt(1, id);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Validar usuario (Login): Busca email y contraseña y que esté ACTIVO
    public Usuario validarUsuario(String email, String password) {
        String sql = "SELECT * FROM usuarios WHERE email = ? AND password = ? AND status = 'ACTIVE'";
        Usuario usuario = null;

        try {
            Connection conn = DBConnection.getConnection();
            if (conn != null) {
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, email);
                stmt.setString(2, password);

                ResultSet rs = stmt.executeQuery();

                // Si encuentra un resultado, crea el objeto Usuario
                if (rs.next()) {
                    usuario = new Usuario(
                            rs.getInt("id"),
                            rs.getString("nombre"),
                            rs.getString("email"),
                            rs.getString("password"),
                            rs.getString("status"),
                            rs.getInt("rol_id"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return usuario;
    }

    // Registrar un nuevo usuario (Por defecto PENDING)
    public boolean registrarUsuario(Usuario usuario) {
        String sql = "INSERT INTO usuarios (nombre, email, password, status, rol_id) VALUES (?, ?, ?, 'PENDING', ?)";

        try {
            Connection conn = DBConnection.getConnection();
            if (conn != null) {
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, usuario.getNombre());
                stmt.setString(2, usuario.getEmail());
                stmt.setString(3, usuario.getPassword());
                stmt.setInt(4, usuario.getRoleId());

                int rowsInserted = stmt.executeUpdate();
                return rowsInserted > 0; // Devuelve true si se insertó fila
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
