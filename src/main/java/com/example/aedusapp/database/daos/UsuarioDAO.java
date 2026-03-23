package com.example.aedusapp.database.daos;

import com.example.aedusapp.database.config.DBConnection;
import com.example.aedusapp.exceptions.DatabaseException;
import com.example.aedusapp.models.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.crypto.bcrypt.BCrypt;

public class UsuarioDAO {

    // --- SQL CONSTANTS ---
    private static final String GET_USERS_ACTIVE = "SELECT * FROM neon_auth.user WHERE banned = false ORDER BY name";
    private static final String GET_USERS_INACTIVE = "SELECT * FROM neon_auth.user WHERE banned = true ORDER BY name";
    private static final String GET_ALL_USERS = "SELECT * FROM neon_auth.user";
    private static final String GET_USER_BY_ID = "SELECT * FROM neon_auth.user WHERE id = ?::uuid";
    private static final String UPDATE_USER_COINS = "UPDATE neon_auth.user SET aeducoins = ? WHERE id = ?::uuid";
    
    private static final String UPDATE_USER = 
        "UPDATE neon_auth.user SET name=?, email=?, banned=?, password=?, role=?, aeducoins=?, foto_perfil=?, foto_perfil_datos=?, telefono=?, bio=? WHERE id=?";
        
    private static final String DELETE_USER = "DELETE FROM neon_auth.user WHERE id=?";
    private static final String VALIDATE_USER = "SELECT * FROM neon_auth.user WHERE email = ? AND (banned IS NULL OR banned = false)";
    
    private static final String REGISTER_USER = 
        "INSERT INTO neon_auth.user (name, email, password, banned, role, \"emailVerified\", foto_perfil) VALUES (?, ?, ?, ?, ?, false, ?) RETURNING id";
        
    private static final String COUNT_TOTAL_USERS = "SELECT COUNT(*) as total FROM neon_auth.user";
    private static final String INIT_PRESENCE_SYSTEM = "ALTER TABLE neon_auth.user ADD COLUMN IF NOT EXISTS last_seen TIMESTAMP DEFAULT NOW()";
    private static final String UPDATE_LAST_SEEN = "UPDATE neon_auth.user SET last_seen = NOW() WHERE id = ?";
    
    private static final String GET_RECENTLY_ACTIVE_USERS = 
        "SELECT id FROM neon_auth.user WHERE last_seen > NOW() - (? * INTERVAL '1 second')";


    public List<Usuario> getUsersByStatus(String status) {
        List<Usuario> usuarios = new ArrayList<>();
        String sql = "ACTIVE".equalsIgnoreCase(status) ? GET_USERS_ACTIVE : GET_USERS_INACTIVE;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                usuarios.add(mapResultSetToUsuario(rs, false));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error obteniendo usuarios por estado: " + status, e);
        }
        return usuarios;
    }

    public List<Usuario> getAllUsers() {
        List<Usuario> usuarios = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_ALL_USERS);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                usuarios.add(mapResultSetToUsuario(rs, true));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error obteniendo todos los usuarios", e);
        }
        return usuarios;
    }

    public Usuario getUserById(String id) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_USER_BY_ID)) {
             stmt.setString(1, id);
             try (ResultSet rs = stmt.executeQuery()) {
                 if (rs.next()) return mapResultSetToUsuario(rs, false);
             }
        } catch (SQLException e) {
            System.err.println("Error getUserById: " + e.getMessage());
        }
        return null;
    }

    public boolean updateUserAeduCoins(String id, int coins) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_USER_COINS)) {
             stmt.setInt(1, coins);
             stmt.setString(2, id);
             return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updateUserAeduCoins: " + e.getMessage());
        }
        return false;
    }

    public boolean updateUser(Usuario usuario) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_USER)) {

            boolean isBanned = !"ACTIVE".equalsIgnoreCase(usuario.getStatus());

            stmt.setString(1, usuario.getNombre());
            stmt.setString(2, usuario.getEmail());
            stmt.setBoolean(3, isBanned);

            String passwordToSave = usuario.getPassword();
            if (passwordToSave != null && !passwordToSave.startsWith("$2a$") && !passwordToSave.isEmpty()) {
                passwordToSave = BCrypt.hashpw(passwordToSave, BCrypt.gensalt(12));
            }
            stmt.setString(4, passwordToSave);

            stmt.setString(5, usuario.getRole());
            stmt.setInt(6, usuario.getAeducoins());
            stmt.setString(7, usuario.getFotoPerfil());
            stmt.setBytes(8, usuario.getFotoPerfilDatos());
            stmt.setString(9, usuario.getTelefono());
            stmt.setString(10, usuario.getBio());
            stmt.setObject(11, java.util.UUID.fromString(usuario.getId())); 

            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            throw new DatabaseException("Error actualizando el usuario: " + usuario.getEmail(), e);
        }
    }

    public boolean deleteUser(String id) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_USER)) {

            stmt.setObject(1, java.util.UUID.fromString(id));
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DatabaseException("Error eliminando el usuario con id: " + id, e);
        }
    }

    public Usuario validateUser(String email, String password) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(VALIDATE_USER)) {

            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("password");
                    boolean passwordValida = false;

                    if (storedPassword != null) {
                        if (storedPassword.startsWith("$2a$")) {
                            passwordValida = BCrypt.checkpw(password, storedPassword);
                        } else {
                            passwordValida = storedPassword.equals(password);
                        }
                    }

                    if (passwordValida) {
                        return mapResultSetToUsuario(rs, true);
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error validando usuario/login", e);
        }
        return null;
    }

    public boolean registerUser(Usuario usuario) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(REGISTER_USER)) {

            stmt.setString(1, usuario.getNombre());
            stmt.setString(2, usuario.getEmail());
            
            String hashedPassword = BCrypt.hashpw(usuario.getPassword(), BCrypt.gensalt(12));
            stmt.setString(3, hashedPassword);

            boolean isBanned = "PENDING".equalsIgnoreCase(usuario.getStatus()) || "INACTIVE".equalsIgnoreCase(usuario.getStatus());
            stmt.setBoolean(4, isBanned);
            stmt.setString(5, usuario.getRole() == null ? "user" : usuario.getRole());
            stmt.setString(6, usuario.getFotoPerfil());

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error registrando un nuevo usuario", e);
        }
    }

    public int countTotalUsers() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(COUNT_TOTAL_USERS)) {
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error contando usuarios totales", e);
        }
        return 0;
    }

    public void initPresenceSystem() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(INIT_PRESENCE_SYSTEM);
        } catch (SQLException e) {
            throw new DatabaseException("Error inicializando sistema de presencia", e);
        }
    }

    public void updateLastSeen(String userId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_LAST_SEEN)) {
            stmt.setObject(1, java.util.UUID.fromString(userId));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error actualizando estado last_seen", e);
        }
    }

    public List<String> getRecentlyActiveUsers(int seconds) {
        List<String> activos = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_RECENTLY_ACTIVE_USERS)) {
            stmt.setInt(1, seconds);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) activos.add(rs.getString("id"));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error obteniendo usuarios recientes", e);
        }
        return activos;
    }

    private Usuario mapResultSetToUsuario(ResultSet rs, boolean fetchProfileData) throws SQLException {
        boolean banned = rs.getBoolean("banned");
        String fotoPerfil = null;
        byte[] fotoPerfilDatos = null;
        String telefono = null;
        String bio = null;
        
        try { 
            fotoPerfil = rs.getString("foto_perfil"); 
            telefono = rs.getString("telefono");
            bio = rs.getString("bio");
            if (fetchProfileData) {
                fotoPerfilDatos = rs.getBytes("foto_perfil_datos");
            }
        } catch (Exception ex) {} 

        return new Usuario(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("password"),
                banned ? "INACTIVE" : "ACTIVE",
                rs.getString("role"),
                rs.getInt("aeducoins"),
                fotoPerfil,
                fotoPerfilDatos,
                telefono,
                bio);
    }
}
